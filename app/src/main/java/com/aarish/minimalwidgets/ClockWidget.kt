package com.aarish.minimalwidgets

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import android.content.Context
import android.widget.RemoteViews
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.fillMaxSize
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first

class ClockWidget : GlanceAppWidget() {
    override val sizeMode = androidx.glance.appwidget.SizeMode.Exact
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val initialAppearance = AccentStore(context).appearance.first()
        provideContent {
            val appearance by AccentStore(context).appearance.collectAsState(initial = initialAppearance)
            val views = RemoteViews(context.packageName, R.layout.widget_clock).apply { setTextColor(R.id.time, appearance.accent); setTextColor(R.id.date, appearance.textColor()); setInt(R.id.widget_root, "setBackgroundColor", appearance.background) }
            AndroidRemoteViews(views, modifier = GlanceModifier.fillMaxSize().clickable(actionStartActivity(widgetDestination(context, "clock"))))
        }
    }
}
class ClockWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ClockWidget()
}
class AccentRefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = try {
        ClockWidget().updateAll(applicationContext)
        refreshBatteryWidgets(applicationContext)
        refreshExtraWidgets(applicationContext)
        refreshContactWidgets(applicationContext)
        refreshQuickWidgets(applicationContext)
        Result.success()
    } catch (cancelled: kotlinx.coroutines.CancellationException) {
        throw cancelled
    } catch (_: Exception) { Result.retry() }

    companion object {
        fun enqueue(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                "refresh-accent", ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<AccentRefreshWorker>().build()
            )
        }
    }
}




