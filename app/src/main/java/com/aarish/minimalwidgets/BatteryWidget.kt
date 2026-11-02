package com.aarish.minimalwidgets

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.runtime.*
import androidx.datastore.preferences.core.Preferences
import androidx.glance.*
import androidx.glance.appwidget.*
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.layout.*
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import kotlinx.coroutines.flow.first

/** No time-remaining estimate is fabricated. Refreshes follow Android's widget schedule. */
class BatteryWidget : GlanceAppWidget() {
    override val stateDefinition=androidx.glance.state.PreferencesGlanceStateDefinition
    override suspend fun provideGlance(context:Context,id:GlanceId) {
        val initial=AccentStore(context).appearance.first()
        val widgetId=GlanceAppWidgetManager(context).getAppWidgetId(id)
        provideContent {
            val appearance by AccentStore(context).appearance.collectAsState(initial=initial)
            val revision=currentState<Preferences>()[refreshKey]?:0L
            val style=context.getSharedPreferences("battery-designs",Context.MODE_PRIVATE).getInt("style-$widgetId",1).coerceIn(1,19)
            val battery=remember(revision){context.registerReceiver(null,IntentFilter(Intent.ACTION_BATTERY_CHANGED))}
            val level=battery?.getIntExtra(BatteryManager.EXTRA_LEVEL,-1)?:-1
            val scale=battery?.getIntExtra(BatteryManager.EXTRA_SCALE,100)?:100
            val percent=if(level>=0 && scale>0)level*100/scale else -1
            val status=battery?.getIntExtra(BatteryManager.EXTRA_STATUS,-1)
            val charging=status==BatteryManager.BATTERY_STATUS_CHARGING || status==BatteryManager.BATTERY_STATUS_FULL
            val d=designCatalog.first{it.category=="Battery" && it.style==style}
            val bitmap=remember(appearance,revision,style){DesignRenderer.render(d,appearance,percent,charging)}
            Image(ImageProvider(bitmap),if(percent>=0)"Battery $percent percent" else "Battery unavailable",modifier=GlanceModifier.fillMaxSize().clickable(actionStartActivity(Intent(context,MainActivity::class.java))),contentScale=ContentScale.Fit)
        }
    }
}
suspend fun refreshBatteryWidgets(context:Context) {
    val widget=BatteryWidget()
    for(id in GlanceAppWidgetManager(context).getGlanceIds(BatteryWidget::class.java)) {
        updateAppWidgetState(context,id){it[refreshKey]=(it[refreshKey]?:0L)+1L}
        widget.update(context,id)
    }
}
class BatteryReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget=BatteryWidget()
    override fun onDeleted(context:Context,appWidgetIds:IntArray) {
        val edit=context.getSharedPreferences("battery-designs",Context.MODE_PRIVATE).edit()
        appWidgetIds.forEach{edit.remove("style-$it")};edit.apply()
        super.onDeleted(context,appWidgetIds)
    }
}
class BatteryPinReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context:Context,intent:Intent) {
        val id=intent.getIntExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID,-1)
        val provider=android.appwidget.AppWidgetManager.getInstance(context).getAppWidgetInfo(id)?.provider
        if(id<0 || provider?.className!=BatteryReceiver::class.java.name)return
        context.getSharedPreferences("battery-designs",Context.MODE_PRIVATE).edit().putInt("style-$id",intent.getIntExtra("style",1).coerceIn(1,19)).apply()
        AccentRefreshWorker.enqueue(context)
    }
}
