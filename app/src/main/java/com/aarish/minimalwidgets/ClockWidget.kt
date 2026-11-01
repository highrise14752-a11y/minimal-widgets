package com.aarish.minimalwidgets

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
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first

class ClockWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val accent = AccentStore(context).accent.first()
        val views = RemoteViews(context.packageName, R.layout.widget_clock).apply {
            setTextColor(R.id.time, accent)
        }
        // TextClock runs in the launcher process and keeps time without minute polling.
        provideContent {
            AndroidRemoteViews(views, modifier = GlanceModifier.clickable(actionStartActivity<MainActivity>()))
        }
    }
}
class ClockWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ClockWidget()
}
class AccentRefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = try {
        ClockWidget().updateAll(applicationContext)
        listOf(PhotoWidget(), FolderWidget(), VolumeWidget(), WifiWidget(), DataWidget(), NoteWidget()).forEach { it.updateAll(applicationContext) }
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
