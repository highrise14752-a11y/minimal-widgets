package com.aarish.minimalwidgets

import android.os.Bundle
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.ImageDecoder
import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class WidgetConfigActivity : ComponentActivity() {
    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var kind = WidgetKind.NOTE
    private var error by mutableStateOf<String?>(null)
    private var busy by mutableStateOf(false)
    private val picker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) lifecycleScope.launch {
            busy = true
            try {
                withContext(Dispatchers.IO) {
                    val source = ImageDecoder.createSource(contentResolver, uri)
                    val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                        val scale = 600f / maxOf(info.size.width, info.size.height).coerceAtLeast(600)
                        decoder.setTargetSize((info.size.width * scale).toInt().coerceAtLeast(1), (info.size.height * scale).toInt().coerceAtLeast(1))
                        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    }
                    val target = File(filesDir, "photo-$widgetId.jpg")
                    val temp = File(filesDir, "photo-$widgetId.tmp")
                    try {
                        temp.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it)) }
                        check(temp.renameTo(target))
                    } finally { bitmap.recycle(); temp.delete() }
                }
                complete()
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) { error = "Could not read that photo. Choose another image." }
            finally { busy = false }
        }
    }
    private fun complete() {
        AccentRefreshWorker.enqueue(applicationContext)
        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
        finish()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        widgetId = intent.getIntExtra("widgetId", intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID))
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return }
        kind = intent.getStringExtra("kind")?.let { runCatching { WidgetKind.valueOf(it) }.getOrNull() } ?: when (
            AppWidgetManager.getInstance(this).getAppWidgetInfo(widgetId)?.provider?.className?.substringAfterLast('.')) {
            "PhotoReceiver" -> WidgetKind.PHOTO
            "FolderReceiver" -> WidgetKind.FOLDER
            else -> WidgetKind.NOTE
        }
        // Only configure widgets owned by this application.
        if (AppWidgetManager.getInstance(this).getAppWidgetInfo(widgetId)?.provider?.packageName != packageName) { finish(); return }
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.systemBarsPadding().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Configure ${kind.name.lowercase()}", style = MaterialTheme.typography.headlineSmall)
                        when(kind) {
                            WidgetKind.PHOTO -> {
                                Text("Choose a photo. A small copy is stored privately for this widget.")
                                Button(enabled = !busy, onClick = { picker.launch(arrayOf("image/*")) }) { Text(if (busy) "Saving…" else "Choose photo") }
                            }
                            WidgetKind.FOLDER -> {
                                var apps by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
                                LaunchedEffect(Unit) {
                                    apps = withContext(Dispatchers.IO) {
                                        packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
                                            .map { it.activityInfo.packageName to it.loadLabel(packageManager).toString() }
                                            .distinctBy { it.first }.sortedBy { it.second.lowercase() }
                                    }
                                }
                                var selected by remember { mutableStateOf(widgetPrefs(this@WidgetConfigActivity).getString("apps-$widgetId", "").orEmpty().split('|').filter { it.isNotEmpty() }.toSet()) }
                                Text("Choose up to four apps (${selected.size}/4)")
                                Button(enabled = selected.isNotEmpty(), onClick = {
                                    widgetPrefs(this@WidgetConfigActivity).edit().putString("apps-$widgetId", selected.joinToString("|")).apply(); complete()
                                }) { Text("Save folder") }
                                LazyColumn { items(apps, key = { it.first }) { (pkg, label) ->
                                    Row { Checkbox(checked = pkg in selected, onCheckedChange = { checked ->
                                        if (!checked) selected = selected - pkg else if (selected.size < 4) selected = selected + pkg
                                    }); Text(label, Modifier.padding(top = 12.dp)) }
                                } }
                            }
                            else -> {
                                var text by remember { mutableStateOf(widgetPrefs(this@WidgetConfigActivity).getString("note-$widgetId", "").orEmpty()) }
                                OutlinedTextField(text, { text = it.take(240) }, label = { Text("Personal note") }, supportingText = { Text("${text.length}/240") })
                                Button(onClick = { widgetPrefs(this@WidgetConfigActivity).edit().putString("note-$widgetId", text).apply(); complete() }) { Text("Save note") }
                            }
                        }
                        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        TextButton(enabled = !busy, onClick = { finish() }) { Text("Cancel") }
                    }
                }
            }
        }
    }
}
