package com.aarish.minimalwidgets

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = AccentStore(applicationContext)
        setContent {
            val accent by store.accent.collectAsStateWithLifecycle(initialValue = AccentStore.DEFAULT)
            MaterialTheme(colorScheme = darkColorScheme(primary = Color(accent), background = Color.Black, surface = Color(0xFF191919))) {
                HomeScreen(accent) { color ->
                    store.save(color)
                    AccentRefreshWorker.enqueue(applicationContext)
                }
            }
        }
    }
}
