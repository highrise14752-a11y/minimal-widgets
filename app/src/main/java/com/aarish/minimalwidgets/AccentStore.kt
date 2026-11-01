package com.aarish.minimalwidgets

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import androidx.datastore.preferences.core.emptyPreferences
import java.io.IOException

private val Context.settings by preferencesDataStore(name = "appearance")
class AccentStore(private val context: Context) {
    val accent = context.settings.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY] ?: DEFAULT }
    suspend fun save(color: Int) { context.settings.edit { it[KEY] = color or 0xFF000000.toInt() } }
    companion object {
        const val DEFAULT = 0xFFFFC000.toInt()
        private val KEY = intPreferencesKey("accent_color")
    }
}
