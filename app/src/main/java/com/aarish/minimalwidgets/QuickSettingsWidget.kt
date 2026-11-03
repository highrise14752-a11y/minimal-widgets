package com.aarish.minimalwidgets

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.*
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.layout.*
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.unit.ColorProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

enum class QuickControl(val label: String, val symbol: String, val panel: String?) {
    WIFI("Wi-Fi", "◉", "wifi"), DATA("Mobile data", "⇅", "data"),
    BLUETOOTH("Bluetooth", "ᛒ", "bluetooth"), AIRPLANE("Airplane mode", "✈", "airplane"),
    ALARM("Alarm", "◷", "clock"), DARK("Dark theme", "☼", "dark"),
    DND("Do Not Disturb", "⊖", "dnd"), HOTSPOT("Hotspot", "◎", "hotspot"),
    LOCATION("Location", "⌖", "location"), ROTATE("Auto-rotate", "↻", "rotate"),
    SOUND("Sound", "♪", "volume"), VIBRATE("Vibrate", "≋", null), TORCH("Torch", "ϟ", null),
    RINGER("Alert slider", "≋", null), SILENT("Silent", "⊘", null)
}
private val familyControls = QuickControl.entries.take(13)
data class QuickDesign(val design: Design, val control: QuickControl, val family: Int)
fun quickDesign(designId: String?): QuickDesign? {
    val design = designCatalog.find { it.id == designId && it.category == "Quick Settings" } ?: return null
    return when(design.style) {
        1 -> QuickDesign(design, QuickControl.RINGER, 1)
        2 -> QuickDesign(design, QuickControl.RINGER, 2)
        3 -> QuickDesign(design, QuickControl.SILENT, 2)
        in 4..55 -> QuickDesign(design, familyControls[(design.style - 4) % 13], (design.style - 4) / 13 + 1)
        else -> null
    }
}
fun savedQuickDesign(context: Context, id: Int): QuickDesign = quickDesign(
    widgetPrefs(context).getString("quick-design-$id", null)) ?: quickDesign("quick-settings-4")!!
fun ownsQuickWidget(context: Context, id: Int): Boolean =
    AppWidgetManager.getInstance(context).getAppWidgetInfo(id)?.provider?.let {
        it.packageName == context.packageName && it.className == QuickSettingsReceiver::class.java.name
    } == true
fun quickTapIntent(context: Context, id: Int, configure: Boolean = false) =
    Intent(context, QuickActionActivity::class.java).putExtra("widgetId", id).putExtra("configure", configure)

/** Phone ringer changes are attempted only on a user tap, with settings as an OEM fallback. */
fun nextRingerMode(control: QuickControl, current: Int): Int = when(control) {
    QuickControl.VIBRATE -> if(current == AudioManager.RINGER_MODE_VIBRATE) AudioManager.RINGER_MODE_NORMAL else AudioManager.RINGER_MODE_VIBRATE
    QuickControl.SILENT -> if(current == AudioManager.RINGER_MODE_SILENT) AudioManager.RINGER_MODE_NORMAL else AudioManager.RINGER_MODE_SILENT
    else -> when(current) { AudioManager.RINGER_MODE_NORMAL -> AudioManager.RINGER_MODE_VIBRATE; AudioManager.RINGER_MODE_VIBRATE -> AudioManager.RINGER_MODE_SILENT; else -> AudioManager.RINGER_MODE_NORMAL }
}

class QuickSettingsWidget : GlanceAppWidget() {
    override val stateDefinition = androidx.glance.state.PreferencesGlanceStateDefinition
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val widgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val initial = AccentStore(context).appearance.first()
        provideContent {
            val appearance by AccentStore(context).appearance.collectAsState(initial)
            val revision = currentState<Preferences>()[refreshKey] ?: 0L
            val selected = remember(revision) { savedQuickDesign(context, widgetId) }
            val compact = LocalSize.current.width < 120.dp || LocalSize.current.height < 100.dp
            val action = actionStartActivity(quickTapIntent(context, widgetId))
            val iconColor = if(selected.family <= 3) appearance.accent else appearance.textColor()
            val background = if(selected.family == 4) appearance.secondary else appearance.background
            Column(GlanceModifier.fillMaxSize().background(Color(background)).clickable(action)
                .semantics { contentDescription = "${selected.control.label}; ${if(selected.control.panel != null) "open settings" else "tap to change"}" }.padding(if(compact) 2.dp else 8.dp),
                verticalAlignment = androidx.glance.layout.Alignment.Vertical.CenterVertically,
                horizontalAlignment = androidx.glance.layout.Alignment.Horizontal.CenterHorizontally) {
                if(selected.family == 1 && !compact) Row(verticalAlignment = androidx.glance.layout.Alignment.Vertical.CenterVertically) {
                    Text(selected.control.symbol, style = TextStyle(color = ColorProvider(Color(iconColor)), fontSize = 32.sp))
                    Text(selected.control.label, GlanceModifier.padding(8.dp), maxLines = 2,
                        style = TextStyle(color = ColorProvider(Color(appearance.textColor())), fontSize = 18.sp))
                } else {
                    Text(if(selected.family == 3) selected.control.symbol else selected.control.label,
                        GlanceModifier.defaultWeight().padding(2.dp), maxLines = if(compact) 1 else 2,
                        style = TextStyle(color = ColorProvider(Color(iconColor)), fontSize = if(selected.family == 3) 26.sp else if(compact) 12.sp else 20.sp))
                    if(!compact && selected.family != 3) Text(if(selected.control.panel != null) "Open settings" else "Tap to change",
                        style = TextStyle(color = ColorProvider(Color(appearance.textColor())), fontSize = 12.sp))
                }
                Text("Edit", GlanceModifier.clickable(actionStartActivity(quickTapIntent(context, widgetId, true))).padding(2.dp),
                    style = TextStyle(color = ColorProvider(Color(appearance.textColor())), fontSize = 10.sp))
            }
        }
    }
}
class QuickSettingsReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = QuickSettingsWidget()
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val editor = widgetPrefs(context).edit()
        appWidgetIds.forEach { editor.remove("quick-design-$it") }; editor.apply()
        super.onDeleted(context, appWidgetIds)
    }
}
class QuickPinReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
        val selected = quickDesign(intent.getStringExtra("designId")) ?: return
        if (!ownsQuickWidget(context, id) || selected.design.readyReceiver() != QuickSettingsReceiver::class.java) return
        widgetPrefs(context).edit().putString("quick-design-$id", selected.design.id).apply()
        AccentRefreshWorker.enqueue(context)
    }
}
suspend fun refreshQuickWidgets(context: Context) {
    for(id in GlanceAppWidgetManager(context).getGlanceIds(QuickSettingsWidget::class.java)) {
        updateAppWidgetState(context, id) { it[refreshKey] = (it[refreshKey] ?: 0L) + 1L }
        QuickSettingsWidget().update(context, id)
    }
}

/** Internal bridge. Never accepts an arbitrary control or camera ID from an external intent. */
class QuickActionActivity : ComponentActivity() {
    private var widgetId = -1
    private val cameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        toast(if(granted) "Camera permission granted. Tap the torch widget again to toggle it." else "Camera permission is needed for the torch.")
        finish()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        widgetId = intent.getIntExtra("widgetId", -1)
        if (!ownsQuickWidget(this, widgetId)) { finish(); return }
        if (intent.getBooleanExtra("configure", false)) { showConfiguration(); return }
        // A recreated activity must not repeat a torch or ringer toggle.
        if (savedInstanceState != null) { finish(); return }
        val selected = savedQuickDesign(this, widgetId)
        val control = selected.control
        if (control.panel != null) { openPanel(control.panel); finish(); return }
        if (control == QuickControl.TORCH) {
            if(checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) { showCameraPermission(); return }
            lifecycleScope.launch {
                try { toggleTorch() }
                catch(cancelled: CancellationException) { throw cancelled }
                catch(_: Exception) { toast("Torch unavailable. The camera may be busy or restricted.") }
                finally { finish() }
            }
        } else {
            val audio = getSystemService(AudioManager::class.java)
            try {
                if(audio.isVolumeFixed) { toast("This device manages sound through system settings."); openPanel("sound") }
                else {
                    val target = nextRingerMode(control, audio.ringerMode)
                    audio.ringerMode = target
                    if(audio.ringerMode == target) toast(when(target) { AudioManager.RINGER_MODE_SILENT -> "Silent"; AudioManager.RINGER_MODE_VIBRATE -> "Vibrate"; else -> "Sound on" })
                    else { toast("Android did not allow this sound mode. Choose it in settings."); openPanel("sound") }
                }
            } catch(_: SecurityException) { toast("Android requires this sound change in system settings."); openPanel("sound") }
            AccentRefreshWorker.enqueue(this)
            finish()
        }
    }
    private fun toast(message: String) { Toast.makeText(this, message, Toast.LENGTH_LONG).show() }
    private fun openPanel(panel: String) {
        if(!openWidgetDestination(panel) { startActivity(it) }) toast("No settings app is available for this control.")
    }
    private fun showCameraPermission() {
        setContent { MaterialTheme(colorScheme = darkColorScheme()) { Surface(Modifier.fillMaxSize()) {
            androidx.compose.foundation.layout.Column(Modifier.systemBarsPadding().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                androidx.compose.material3.Text("Allow torch control", style = MaterialTheme.typography.headlineSmall)
                androidx.compose.material3.Text("Android needs camera permission to control the flash. The app does not take photos. Granting permission does not turn the torch on.")
                Button(onClick = { cameraPermission.launch(Manifest.permission.CAMERA) }) { androidx.compose.material3.Text("Allow camera permission") }
                TextButton(onClick = { startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.fromParts("package", packageName, null))) }) { androidx.compose.material3.Text("App permissions") }
                TextButton(onClick = { finish() }) { androidx.compose.material3.Text("Cancel") }
            }
        } } }
    }
    private fun showConfiguration() {
        setContent { MaterialTheme(colorScheme = darkColorScheme()) { Surface(Modifier.fillMaxSize()) {
            androidx.compose.foundation.layout.Column(Modifier.systemBarsPadding().padding(20.dp)) {
                androidx.compose.material3.Text("Choose quick control", style = MaterialTheme.typography.headlineSmall)
                androidx.compose.material3.Text("Tap a control to save it for this widget. This does not execute the action.")
                LazyColumn(Modifier.weight(1f)) {
                    items(designCatalog.filter { it.category == "Quick Settings" && it.readyReceiver() == QuickSettingsReceiver::class.java }, key = { it.id }) { design ->
                        TextButton(onClick = {
                            widgetPrefs(this@QuickActionActivity).edit().putString("quick-design-$widgetId", design.id).apply()
                            AccentRefreshWorker.enqueue(this@QuickActionActivity); finish()
                        }) { androidx.compose.material3.Text(design.name) }
                    }
                }
                TextButton(onClick = { finish() }) { androidx.compose.material3.Text("Cancel") }
            }
        } } }
    }
    private suspend fun toggleTorch() {
        if(checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            toast("Camera permission was revoked. Tap the torch widget again to allow it."); return
        }
        val manager = getSystemService(CameraManager::class.java)
        val ids = manager.cameraIdList.filter { manager.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true }
        val id = ids.firstOrNull { manager.getCameraCharacteristics(it).get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK } ?: ids.firstOrNull()
        if(id == null) { toast("This device has no camera flash."); return }
        val state = withTimeoutOrNull(2000) { currentTorchState(manager, id) }
        if(state == null) { toast("Torch is busy or unavailable. Try again when the camera is free."); return }
        manager.setTorchMode(id, !state)
        toast(if(state) "Torch off" else "Torch on")
    }
}

/** Read the hardware callback instead of a stored boolean, which goes stale after other apps use it. */
private suspend fun currentTorchState(manager: CameraManager, id: String): Boolean? = suspendCancellableCoroutine { continuation ->
    val callback = object : CameraManager.TorchCallback() {
        private fun complete(state: Boolean?) {
            if(continuation.isActive) { manager.unregisterTorchCallback(this); continuation.resume(state) }
        }
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) { if(cameraId == id) complete(enabled) }
        override fun onTorchModeUnavailable(cameraId: String) { if(cameraId == id) complete(null) }
    }
    manager.registerTorchCallback(callback, Handler(Looper.getMainLooper()))
    continuation.invokeOnCancellation { manager.unregisterTorchCallback(callback) }
}
