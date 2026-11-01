package com.aarish.minimalwidgets

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.content.ActivityNotFoundException
import android.provider.Settings

class SettingsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val action = when(intent.getStringExtra("panel")) {
            "wifi" -> Settings.Panel.ACTION_WIFI
            "data" -> Settings.Panel.ACTION_INTERNET_CONNECTIVITY
            else -> Settings.Panel.ACTION_VOLUME
        }
        try { startActivity(Intent(action)) }
        catch (_: ActivityNotFoundException) { startActivity(Intent(Settings.ACTION_SETTINGS)) }
        finish()
    }
}
