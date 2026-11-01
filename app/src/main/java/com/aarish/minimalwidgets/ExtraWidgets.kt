package com.aarish.minimalwidgets

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.graphics.BitmapFactory
import androidx.compose.runtime.*
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.*
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.*
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import kotlinx.coroutines.flow.first
import java.io.File

enum class WidgetKind { PHOTO, FOLDER, VOLUME, WIFI, DATA, NOTE }
fun widgetPrefs(context: Context) = context.getSharedPreferences("widget-config", Context.MODE_PRIVATE)
fun configIntent(context: Context, id: Int, kind: WidgetKind) = Intent(context, WidgetConfigActivity::class.java)
    .putExtra("widgetId", id).putExtra("kind", kind.name)

open class ExtraWidget(private val kind: WidgetKind) : GlanceAppWidget() {
    override val stateDefinition = androidx.glance.state.PreferencesGlanceStateDefinition
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val accent = AccentStore(context).accent.first()
        val widgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        provideContent {
            val savedAccent by AccentStore(context).accent.collectAsState(initial = accent)
            val accent = savedAccent
            val revision = currentState<Preferences>()[refreshKey] ?: 0L
        val prefs = widgetPrefs(context)
        val note = prefs.getString("note-$widgetId", "Tap to write a note").orEmpty()
        val apps = prefs.getString("apps-$widgetId", "").orEmpty().split('|').filter { it.isNotEmpty() }
        val photo = remember(revision) { if (kind == WidgetKind.PHOTO) BitmapFactory.decodeFile(File(context.filesDir, "photo-$widgetId.jpg").path) else null }
        val edit = actionStartActivity(configIntent(context, widgetId, kind))
        val audio = context.getSystemService(AudioManager::class.java)
        val volume = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)

            Column(GlanceModifier.fillMaxSize().background(Color(0xFF191919)).padding(12.dp),
                verticalAlignment = Alignment.Vertical.CenterVertically,
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally) {
                when (kind) {
                    WidgetKind.PHOTO -> if (photo != null) {
                        Image(ImageProvider(photo), "Your photo; tap to change", modifier = GlanceModifier.fillMaxSize().clickable(edit), contentScale = ContentScale.Crop)
                    } else Label("Choose photo", accent, edit)
                    WidgetKind.NOTE -> Label(note.ifBlank { "Tap to write a note" }, accent, edit)
                    WidgetKind.FOLDER -> {
                        if (apps.isEmpty()) Label("Choose apps", accent, edit)
                        apps.take(4).chunked(2).forEach { row ->
                            Row(GlanceModifier.fillMaxWidth()) {
                                row.forEach { packageName ->
                                    val launch = context.packageManager.getLaunchIntentForPackage(packageName)
                                    val name = runCatching { context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(packageName, 0)).toString() }.getOrDefault("Unavailable")
                                    Text(name, modifier = GlanceModifier.defaultWeight().padding(4.dp).clickable(if (launch != null) actionStartActivity(launch) else edit), maxLines = 1,
                                        style = TextStyle(color = ColorProvider(Color(accent)), fontSize = 14.sp))
                                }
                            }
                        }
                        Text("Edit folder", modifier = GlanceModifier.clickable(edit).padding(4.dp), style = TextStyle(color = ColorProvider(Color.LightGray), fontSize = 12.sp))
                    }
                    WidgetKind.VOLUME -> {
                        Text("Media volume", style = TextStyle(color = ColorProvider(Color(accent)), fontSize = 16.sp))
                        Row {
                            Label("−", accent, actionRunCallback<LowerVolume>())
                            Label("+", accent, actionRunCallback<RaiseVolume>())
                            Label("Settings", accent, actionStartActivity(Intent(context, SettingsActivity::class.java).putExtra("panel", "volume")))
                        }
                    }
                    WidgetKind.WIFI, WidgetKind.DATA -> Label(if (kind == WidgetKind.WIFI) "Wi-Fi settings" else "Mobile data settings", accent,
                        actionStartActivity(Intent(context, SettingsActivity::class.java).putExtra("panel", if (kind == WidgetKind.WIFI) "wifi" else "data")))
                }
            }
        }
    }
}
@Composable
private fun Label(text: String, accent: Int, action: Action) {
    Text(text, modifier = GlanceModifier.clickable(action).padding(6.dp), maxLines = 6,
        style = TextStyle(color = ColorProvider(Color(accent)), fontSize = 18.sp))
}
class PhotoWidget : ExtraWidget(WidgetKind.PHOTO)
class FolderWidget : ExtraWidget(WidgetKind.FOLDER)
class VolumeWidget : ExtraWidget(WidgetKind.VOLUME)
class WifiWidget : ExtraWidget(WidgetKind.WIFI)
class DataWidget : ExtraWidget(WidgetKind.DATA)
class NoteWidget : ExtraWidget(WidgetKind.NOTE)
class PhotoReceiver : CleanWidgetReceiver() { override val glanceAppWidget = PhotoWidget() }
class FolderReceiver : CleanWidgetReceiver() { override val glanceAppWidget = FolderWidget() }
class VolumeReceiver : CleanWidgetReceiver() { override val glanceAppWidget = VolumeWidget() }
class WifiReceiver : CleanWidgetReceiver() { override val glanceAppWidget = WifiWidget() }
class DataReceiver : CleanWidgetReceiver() { override val glanceAppWidget = DataWidget() }
class NoteReceiver : CleanWidgetReceiver() { override val glanceAppWidget = NoteWidget() }
class LowerVolume : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) = changeVolume(context, AudioManager.ADJUST_LOWER)
}
class RaiseVolume : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) = changeVolume(context, AudioManager.ADJUST_RAISE)
}
private suspend fun changeVolume(context: Context, direction: Int) {
    val audio = context.getSystemService(AudioManager::class.java)
    if (!audio.isVolumeFixed) audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
    refreshExtraWidgets(context)
}

val refreshKey = longPreferencesKey("refresh_version")
suspend fun refreshExtraWidgets(context: Context) {
    val widgets = listOf(PhotoWidget(), FolderWidget(), VolumeWidget(), WifiWidget(), DataWidget(), NoteWidget())
    val manager = GlanceAppWidgetManager(context)
    for (widget in widgets) {
        for (id in manager.getGlanceIds(widget.javaClass)) {
            updateAppWidgetState(context, id) { it[refreshKey] = (it[refreshKey] ?: 0L) + 1L }
            widget.update(context, id)
        }
    }
}

open class CleanWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NoteWidget()
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val prefs = widgetPrefs(context).edit()
        appWidgetIds.forEach { id ->
            File(context.filesDir, "photo-$id.jpg").delete()
            prefs.remove("note-$id").remove("apps-$id")
        }
        prefs.apply()
        super.onDeleted(context, appWidgetIds)
    }
}
