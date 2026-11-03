package com.aarish.minimalwidgets

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.content.ActivityNotFoundException
import android.provider.Settings
import android.provider.AlarmClock
import android.widget.Toast

/** Ordered public intents; OEMs can omit a panel, so try the next supported screen. */
fun widgetDestinationActions(panel: String?): List<String> = when(panel) {
    "battery" -> listOf(Intent.ACTION_POWER_USAGE_SUMMARY, Settings.ACTION_BATTERY_SAVER_SETTINGS, Settings.ACTION_SETTINGS)
    "clock" -> listOf(AlarmClock.ACTION_SHOW_ALARMS, AlarmClock.ACTION_SHOW_TIMERS)
    "wifi" -> listOf(Settings.Panel.ACTION_WIFI, Settings.ACTION_WIFI_SETTINGS, Settings.ACTION_SETTINGS)
    "data" -> listOf(Settings.Panel.ACTION_INTERNET_CONNECTIVITY, Settings.ACTION_DATA_ROAMING_SETTINGS, Settings.ACTION_SETTINGS)
    "bluetooth" -> listOf(Settings.ACTION_BLUETOOTH_SETTINGS, Settings.ACTION_WIRELESS_SETTINGS, Settings.ACTION_SETTINGS)
    "airplane" -> listOf(Settings.ACTION_AIRPLANE_MODE_SETTINGS, Settings.ACTION_WIRELESS_SETTINGS, Settings.ACTION_SETTINGS)
    "dark", "rotate" -> listOf(Settings.ACTION_DISPLAY_SETTINGS, Settings.ACTION_SETTINGS)
    "dnd" -> listOf("android.settings.ZEN_MODE_SETTINGS", Settings.ACTION_SOUND_SETTINGS, Settings.ACTION_SETTINGS)
    "hotspot" -> listOf("android.settings.TETHER_SETTINGS", Settings.ACTION_WIRELESS_SETTINGS, Settings.ACTION_SETTINGS)
    "location" -> listOf(Settings.ACTION_LOCATION_SOURCE_SETTINGS, Settings.ACTION_SETTINGS)
    "sound" -> listOf(Settings.ACTION_SOUND_SETTINGS, Settings.ACTION_SETTINGS)
    else -> listOf(Settings.Panel.ACTION_VOLUME, Settings.ACTION_SOUND_SETTINGS, Settings.ACTION_SETTINGS)
}

fun widgetDestination(context: android.content.Context, panel: String) =
    Intent(context, SettingsActivity::class.java).putExtra("panel", panel)

/** A launcher boundary keeps fallback behavior testable without launching external apps. */
fun openWidgetDestination(panel: String?, launch: (Intent) -> Unit): Boolean {
    for (action in widgetDestinationActions(panel)) {
        try { launch(Intent(action)); return true }
        catch (_: ActivityNotFoundException) { /* Try the OEM fallback. */ }
        catch (_: SecurityException) { /* Some OEM screens require a privileged permission. */ }
    }
    return false
}

class SettingsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val opened = openWidgetDestination(intent.getStringExtra("panel")) { startActivity(it) }
        if (!opened) Toast.makeText(this, "No app is available to open this widget action.", Toast.LENGTH_LONG).show()
        finish()
    }
}
