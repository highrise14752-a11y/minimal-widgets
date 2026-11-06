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
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/** Accept telephone numbers, never post-dial instructions, USSD codes, or arbitrary URIs. */
fun contactNumber(raw: String): String? = raw.filterNot { it in " -()." }
    .takeIf { Regex("\\+?[0-9]{3,20}").matches(it) }

fun contactActionIntent(number: String, message: Boolean): Intent? = contactNumber(number)?.let {
    Intent(if (message) Intent.ACTION_SENDTO else Intent.ACTION_CALL,
        Uri.fromParts(if (message) "smsto" else "tel", it, null))
}

private val contactProviders get() = setOf(ContactReceiver::class.java.name, CallReceiver::class.java.name, MessageReceiver::class.java.name, ContactDesignReceiver::class.java.name)
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
        val initial=AccentStore(context).appearance.first()
        val widgetId=GlanceAppWidgetManager(context).getAppWidgetId(id)
        provideContent {
            val appearance by AccentStore(context).appearance.collectAsState(initial)
            val revision=currentState<Preferences>()[refreshKey]?:0L
            val size=androidx.glance.LocalSize.current
            val style=remember(revision){savedContactStyle(context,widgetId,when(mode){"call"->11;"message"->12;else->1})}
            val views=remember(appearance,revision,size,style){contactRemoteViews(context,widgetId,style,appearance,size.width.value,size.height.value)}
            androidx.glance.appwidget.AndroidRemoteViews(views,modifier=GlanceModifier.fillMaxSize())
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
    for(widget in listOf(ContactWidget(), CallWidget(), MessageWidget(), ContactDesignWidget(), MusicWidget())) {
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
        AccentRefreshWorker.enqueue(this)
        Toast.makeText(this, if(granted) "Phone permission granted. Tap the home-screen contact again to call." else "Phone permission is required for direct calling.", Toast.LENGTH_LONG).show()
        lifecycleScope.launch {
            try{refreshContactWidgets(applicationContext)}catch(cancelled:kotlinx.coroutines.CancellationException){throw cancelled}catch(_:Exception){}
            finish()
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent.getIntExtra("widgetId", -1)
        if (!ownsContactWidget(this, id)) { finish(); return }
        // A recreated permission screen must not replay a call.
        if (savedInstanceState != null) { finish(); return }
        val defaultStyle=when(AppWidgetManager.getInstance(this).getAppWidgetInfo(id)?.provider?.className){CallReceiver::class.java.name->11;MessageReceiver::class.java.name->12;else->1}
        val style=savedContactStyle(this,id,defaultStyle)
        val slot=intent.getIntExtra("slot",0)
        val kind=intent.getStringExtra("kind")?:if(intent.getBooleanExtra("message",false))"message"else"call"
        if(slot !in 0 until contactSlotCount(style) || kind !in contactAllowedActions(style)){finish();return}
        val tile=savedContact(this,id,slot)
        val action=contactDestination(tile,kind)
        if(action==null){startActivity(contactConfigIntent(this,id).putExtra("slot",slot));finish();return}
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

