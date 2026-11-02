package com.aarish.minimalwidgets

import android.content.Context
import android.graphics.Color
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import java.io.IOException

private val Context.settings by preferencesDataStore(name = "appearance")
data class Appearance(
    val accent: Int = AccentStore.DEFAULT,
    val background: Int = Color.BLACK,
    val secondary: Int = 0xFF222225.toInt(),
    val text: Int = Color.WHITE,
    val reactive: String = "Standard",
    val mode: String = "Ask every time",
    val theme: String = "Default",
    val favorites: Set<String> = emptySet()
) {
    fun accentDim(): Int = Color.rgb(Color.red(accent)/3, Color.green(accent)/3, Color.blue(accent)/3)
    fun textColor(): Int = when(reactive) {
        "Always black" -> Color.BLACK
        "Always white" -> Color.WHITE
        "Accent color" -> accent
        "Background color" -> background
        "Auto contrast" -> if((Color.red(background)*299+Color.green(background)*587+Color.blue(background)*114)/1000>140)Color.BLACK else Color.WHITE
        else -> text
    }
    fun json(): String = JSONObject().put("version",1).put("accent",accent).put("background",background).put("secondary",secondary).put("text",text)
        .put("reactive",reactive).put("mode",mode).put("theme",theme).put("favorites",org.json.JSONArray(favorites.sorted())).toString(2)
    companion object {
        val reactiveModes = listOf("Standard", "Auto contrast", "Always black", "Always white", "Accent color", "Background color")
        val widgetModes = listOf("Ask every time", "Responsive", "Fixed")
        fun parse(value: String): Appearance {
            require(value.length<=65536) { "Backup is too large" }
            val j=JSONObject(value);require(j.getInt("version")==1) { "Unsupported backup version" }
            fun color(key:String):Int { val n=j.getLong(key);require(n in Int.MIN_VALUE.toLong()..0xFFFFFFFFL);return n.toInt() or 0xFF000000.toInt() }
            val reactive=j.getString("reactive");val mode=j.getString("mode")
            require(reactive in reactiveModes && mode in widgetModes) { "Invalid preferences" }
            val arr=j.optJSONArray("favorites");val favorites=buildSet { if(arr!=null) {require(arr.length()<=500);repeat(arr.length()){val id=arr.getString(it);if(designCatalog.any { d->d.id==id })add(id)}} }
            return Appearance(color("accent"),color("background"),color("secondary"),color("text"),reactive,mode,j.optString("theme","Imported").take(64),favorites)
        }
    }
}
class AccentStore(private val context: Context) {
    private val data=context.settings.data.catch { if(it is IOException)emit(emptyPreferences()) else throw it }
    val appearance=data.map { p->Appearance(p[KEY]?:DEFAULT,p[BG]?:android.graphics.Color.BLACK,p[SECONDARY]?:0xFF222225.toInt(),p[TEXT]?:android.graphics.Color.WHITE,p[REACTIVE]?:"Standard",p[MODE]?:"Ask every time",p[THEME]?:"Default",p[FAVORITES]?:emptySet()) }
    val accent=appearance.map { it.accent }
    suspend fun save(color:Int) { context.settings.edit { it[KEY]=color or 0xFF000000.toInt() } }
    suspend fun update(transform:(Appearance)->Appearance) { context.settings.edit { p->
        val old=Appearance(p[KEY]?:DEFAULT,p[BG]?:android.graphics.Color.BLACK,p[SECONDARY]?:0xFF222225.toInt(),p[TEXT]?:android.graphics.Color.WHITE,p[REACTIVE]?:"Standard",p[MODE]?:"Ask every time",p[THEME]?:"Default",p[FAVORITES]?:emptySet())
        val a=transform(old);p[KEY]=a.accent;p[BG]=a.background;p[SECONDARY]=a.secondary;p[TEXT]=a.text;p[REACTIVE]=a.reactive;p[MODE]=a.mode;p[THEME]=a.theme;p[FAVORITES]=a.favorites
    } }
    companion object {
        const val DEFAULT = 0xFFFFC000.toInt()
        private val KEY=intPreferencesKey("accent_color")
        private val BG=intPreferencesKey("background");private val SECONDARY=intPreferencesKey("secondary");private val TEXT=intPreferencesKey("text")
        private val REACTIVE=stringPreferencesKey("reactive");private val MODE=stringPreferencesKey("mode");private val THEME=stringPreferencesKey("theme")
        private val FAVORITES=stringSetPreferencesKey("favorites")
    }
}
