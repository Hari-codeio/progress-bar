package app.becoming.work

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.becoming.appContainer
import app.becoming.widget.BecomingWidget
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/**
 * Refreshes the widget just after local midnight (so "Voted today" resets and missed-day messages
 * appear), then schedules itself for the next midnight.
 */
class MidnightWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        applicationContext.appContainer.repository.refreshToday()
        BecomingWidget().updateAll(applicationContext)
        schedule(applicationContext, ExistingWorkPolicy.REPLACE)
        return Result.success()
    }

    companion object {
        private const val UNIQUE_NAME = "becoming-midnight-refresh"

        /**
         * App start and widget placement use KEEP so they never cancel a pending midnight run (the
         * process may have been started precisely to run it); the worker re-enqueues with REPLACE.
         */
        fun schedule(context: Context, policy: ExistingWorkPolicy = ExistingWorkPolicy.KEEP) {
            val now = LocalDateTime.now()
            val nextRun = now.toLocalDate().plusDays(1).atStartOfDay().plusMinutes(1)
            val delayMillis = Duration.between(now, nextRun).toMillis().coerceAtLeast(0L)
            val request = OneTimeWorkRequestBuilder<MidnightWorker>()
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(UNIQUE_NAME, policy, request)
        }
    }
}
