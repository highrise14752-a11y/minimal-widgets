package com.aarish.minimalwidgets

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
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
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import kotlinx.coroutines.flow.first

/** Accept telephone numbers, never post-dial instructions, USSD codes, or arbitrary URIs. */
fun contactNumber(raw: String): String? = raw.filterNot { it in " -()." }
    .takeIf { Regex("\\+?[0-9]{3,20}").matches(it) }

fun contactActionIntent(number: String, message: Boolean): Intent? = contactNumber(number)?.let {
    Intent(if (message) Intent.ACTION_SENDTO else Intent.ACTION_CALL,
        Uri.fromParts(if (message) "smsto" else "tel", it, null))
}

private val contactProviders get() = setOf(ContactReceiver::class.java.name, CallReceiver::class.java.name, MessageReceiver::class.java.name)
fun ownsContactWidget(context: Context, id: Int): Boolean =
    AppWidgetManager.getInstance(context).getAppWidgetInfo(id)?.provider?.let {
        it.packageName == context.packageName && it.className in contactProviders
    } == true

fun contactConfigIntent(context: Context, id: Int) = Intent(context, ContactConfigActivity::class.java).putExtra("widgetId", id)
private fun contactTapIntent(context: Context, id: Int, message: Boolean) =
    Intent(context, ContactActionActivity::class.java).putExtra("widgetId", id).putExtra("message", message)

open class ContactWidget(private val mode: String = "both") : GlanceAppWidget() {
    override val sizeMode = androidx.glance.appwidget.SizeMode.Exact
    override val stateDefinition = androidx.glance.state.PreferencesGlanceStateDefinition
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val initial = AccentStore(context).appearance.first()
        val widgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        provideContent {
            val appearance by AccentStore(context).appearance.collectAsState(initial)
            val revision = currentState<Preferences>()[refreshKey] ?: 0L
            val prefs = widgetPrefs(context)
            val number = remember(revision) { prefs.getString("contact-number-$widgetId", "").orEmpty() }
            val name = remember(revision) { prefs.getString("contact-name-$widgetId", "Contact").orEmpty().ifBlank { "Contact" } }
            val edit = actionStartActivity(contactConfigIntent(context, widgetId))
            val compact = LocalSize.current.height < 90.dp || LocalSize.current.width < 100.dp
            Column(GlanceModifier.fillMaxSize().background(Color(appearance.background)).padding(if(compact) 2.dp else 8.dp),
                verticalAlignment = androidx.glance.layout.Alignment.Vertical.CenterVertically,
                horizontalAlignment = androidx.glance.layout.Alignment.Horizontal.CenterHorizontally) {
                if (number.isBlank()) {
                    Text("Set contact", GlanceModifier.clickable(edit), style = TextStyle(color = ColorProvider(Color(appearance.accent)), fontSize = 12.sp))
                } else {
                    val primary = actionStartActivity(contactTapIntent(context, widgetId, mode == "message"))
                    Text(if(compact) name else "$name\n${if(mode == "message") "Message" else "Call"}",
                        GlanceModifier.clickable(primary).padding(2.dp), maxLines = if(compact) 1 else 2,
                        style = TextStyle(color = ColorProvider(Color(appearance.accent)), fontSize = if(compact) 13.sp else 18.sp))
                    if (mode == "both" && !compact) Text("Message", GlanceModifier.clickable(actionStartActivity(contactTapIntent(context, widgetId, true))).padding(4.dp),
                        style = TextStyle(color = ColorProvider(Color(appearance.textColor())), fontSize = 14.sp))
                    Text("Edit", GlanceModifier.clickable(edit).padding(2.dp),
                        style = TextStyle(color = ColorProvider(Color(appearance.textColor())), fontSize = 10.sp))
                }
            }
        }
    }
}
class CallWidget : ContactWidget("call")
class MessageWidget : ContactWidget("message")
class ContactReceiver : CleanWidgetReceiver() { override val glanceAppWidget = ContactWidget() }
class CallReceiver : CleanWidgetReceiver() { override val glanceAppWidget = CallWidget() }
class MessageReceiver : CleanWidgetReceiver() { override val glanceAppWidget = MessageWidget() }

suspend fun refreshContactWidgets(context: Context) {
    val manager = GlanceAppWidgetManager(context)
    for(widget in listOf(ContactWidget(), CallWidget(), MessageWidget(), MusicWidget())) {
        for(id in manager.getGlanceIds(widget.javaClass)) {
            updateAppWidgetState(context, id) { it[refreshKey] = (it[refreshKey] ?: 0L) + 1L }
            widget.update(context, id)
        }
    }
}

class MusicWidget : GlanceAppWidget() {
    override val sizeMode = androidx.glance.appwidget.SizeMode.Exact
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val initial = AccentStore(context).appearance.first()
        provideContent {
            val appearance by AccentStore(context).appearance.collectAsState(initial)
            androidx.glance.layout.Box(GlanceModifier.fillMaxSize().background(Color(appearance.background))
                .clickable(androidx.glance.appwidget.action.actionRunCallback<PlayPauseMusic>()), contentAlignment = androidx.glance.layout.Alignment.Center) {
                Text("▶ / Ⅱ", style = TextStyle(color = ColorProvider(Color(appearance.accent)), fontSize = 22.sp))
            }
        }
    }
}
class MusicReceiver : CleanWidgetReceiver() { override val glanceAppWidget = MusicWidget() }
class PlayPauseMusic : androidx.glance.appwidget.action.ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: androidx.glance.action.ActionParameters) {
        val audio = context.getSystemService(android.media.AudioManager::class.java)
        // Match a headset media button: DOWN and UP go to Android's current media consumer.
        for(action in listOf(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.ACTION_UP)) {
            audio.dispatchMediaKeyEvent(android.view.KeyEvent(action, android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
        }
    }
}

/** Launched only by a widget tap. Configuration and permission grants never place a call. */
class ContactActionActivity : ComponentActivity() {
    private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        Toast.makeText(this, if(granted) "Phone permission granted. Tap the home-screen contact again to call." else "Phone permission is required for direct calling.", Toast.LENGTH_LONG).show()
        finish()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent.getIntExtra("widgetId", -1)
        if (!ownsContactWidget(this, id)) { finish(); return }
        // A recreated permission screen must not replay a call.
        if (savedInstanceState != null) { finish(); return }
        val number = widgetPrefs(this).getString("contact-number-$id", "").orEmpty()
        val action = contactActionIntent(number, intent.getBooleanExtra("message", false))
        if (action == null) { startActivity(contactConfigIntent(this, id)); finish(); return }
        if (action.action == Intent.ACTION_CALL && checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            setContent { MaterialTheme(colorScheme = darkColorScheme()) { Surface(Modifier.fillMaxSize()) {
                androidx.compose.foundation.layout.Column(Modifier.systemBarsPadding().padding(24.dp)) {
                    androidx.compose.material3.Text("Allow direct calls", style = MaterialTheme.typography.headlineSmall)
                    androidx.compose.material3.Text("Tapping this widget calls its saved number immediately. Android needs phone permission. Granting permission does not make a call.")
                    Button(onClick = { permission.launch(Manifest.permission.CALL_PHONE) }) { androidx.compose.material3.Text("Allow phone permission") }
                    TextButton(onClick = { finish() }) { androidx.compose.material3.Text("Cancel") }
                }
            } } }
            return
        }
        try { startActivity(action) }
        catch (_: android.content.ActivityNotFoundException) { Toast.makeText(this, "No phone or messaging app is available.", Toast.LENGTH_LONG).show() }
        catch (_: SecurityException) { Toast.makeText(this, "Android could not authorize this action. Check phone permissions.", Toast.LENGTH_LONG).show() }
        finish()
    }
}

class ContactConfigActivity : ComponentActivity() {
    private var widgetId = -1
    private var name by mutableStateOf("")
    private var number by mutableStateOf("")
    private var error by mutableStateOf<String?>(null)
    private var allowed by mutableStateOf(false)
    private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { allowed = it }
    private val picker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = result.data?.data
        if (result.resultCode == RESULT_OK && uri != null) {
            try {
                contentResolver.query(uri, arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) { name = cursor.getString(0).orEmpty().take(60); number = cursor.getString(1).orEmpty().take(40) }
                }
            } catch (_: Exception) { error = "Could not read that contact. Enter its number instead." }
        }
    }
    override fun onResume() {
        super.onResume()
        allowed = checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        widgetId = intent.getIntExtra("widgetId", -1)
        if (!ownsContactWidget(this, widgetId)) { finish(); return }
        val messageOnly = AppWidgetManager.getInstance(this).getAppWidgetInfo(widgetId)?.provider?.className == MessageReceiver::class.java.name
        val prefs = widgetPrefs(this)
        name = savedInstanceState?.getString("name") ?: prefs.getString("contact-name-$widgetId", "").orEmpty()
        number = savedInstanceState?.getString("number") ?: prefs.getString("contact-number-$widgetId", "").orEmpty()
        setContent { MaterialTheme(colorScheme = darkColorScheme()) { Surface(Modifier.fillMaxSize()) {
            androidx.compose.foundation.layout.Column(Modifier.systemBarsPadding().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                androidx.compose.material3.Text("Configure contact", style = MaterialTheme.typography.headlineSmall)
                androidx.compose.material3.Text(if(messageOnly) "Tap your widget to compose a message." else "Tap your widget to call this number immediately. The phone app may show the ongoing call; the contacts app is skipped.")
                OutlinedTextField(name, { name = it.take(60) }, label = { androidx.compose.material3.Text("Name") }, singleLine = true)
                OutlinedTextField(number, { number = it.take(40) }, label = { androidx.compose.material3.Text("Phone number") }, singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone))
                TextButton(onClick = {
                    try { picker.launch(Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)) }
                    catch (_: android.content.ActivityNotFoundException) { error = "No contacts picker is available. Enter a number instead." }
                }) { androidx.compose.material3.Text("Choose from contacts") }
                if (!messageOnly && !allowed) {
                    androidx.compose.material3.Text("Direct calling needs Android phone permission. You can change it in app settings if previously denied.")
                    Button(onClick = { permission.launch(Manifest.permission.CALL_PHONE) }) { androidx.compose.material3.Text("Allow direct calls") }
                    TextButton(onClick = { startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))) }) { androidx.compose.material3.Text("App permissions") }
                }
                error?.let { androidx.compose.material3.Text(it, color = MaterialTheme.colorScheme.error) }
                Button(enabled = contactNumber(number) != null && (messageOnly || allowed), onClick = {
                    prefs.edit().putString("contact-name-$widgetId", name.trim().ifBlank { "Contact" })
                        .putString("contact-number-$widgetId", contactNumber(number)).apply()
                    AccentRefreshWorker.enqueue(this@ContactConfigActivity)
                    finish()
                }) { androidx.compose.material3.Text("Save contact") }
                TextButton(onClick = { finish() }) { androidx.compose.material3.Text("Cancel") }
            }
        } } }
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("name", name); outState.putString("number", number)
        super.onSaveInstanceState(outState)
    }
}
