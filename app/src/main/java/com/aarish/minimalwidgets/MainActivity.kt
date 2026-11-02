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
        enableEdgeToEdge(
            statusBarStyle=androidx.activity.SystemBarStyle.dark(android.graphics.Color.BLACK),
            navigationBarStyle=androidx.activity.SystemBarStyle.dark(android.graphics.Color.BLACK)
        )
        val store=AccentStore(applicationContext)
        setContent {
            val appearance by store.appearance.collectAsStateWithLifecycle(initialValue=Appearance())
            MaterialTheme(colorScheme=darkColorScheme(primary=Color(appearance.accent),onPrimary=Color.Black,background=Color.Black,surface=Color(0xFF19191F))) {
                HomeScreen(appearance) { transform -> store.update(transform);AccentRefreshWorker.enqueue(applicationContext) }
            }
        }
    }
}

