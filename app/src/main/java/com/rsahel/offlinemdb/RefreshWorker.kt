package com.rsahel.offlinemdb

import android.content.Context
import android.content.pm.ServiceInfo
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RefreshWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    private val notification = RefreshNotification(
        applicationContext,
        WorkManager.getInstance(applicationContext).createCancelPendingIntent(id)
    )

    companion object {
        const val Tag = "Refresh"
        const val Progress = "Progress"

        fun buildAndEnqueue(context: Context) {
            val workManager = WorkManager.getInstance(context)
            val workRequest: WorkRequest = OneTimeWorkRequestBuilder<RefreshWorker>()
                .addTag(Tag)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
            workManager.enqueue(workRequest)
        }

        fun observe(
            context: Context,
            owner: LifecycleOwner,
            observer: (WorkInfo) -> Unit,
        ) {
            val workManager = WorkManager.getInstance(context)
            workManager.pruneWork()
            workManager
                .getWorkInfosByTagLiveData(Tag)
                .observe(owner) {
                    if (it.isNotEmpty()) {
                        observer(it.first())
                    }
                }
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(
            notification.notificationId,
            notification.builder.build(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
    }

    override suspend fun doWork(): Result {
        val dbHelper = DatabaseHelper.getInstance()!!

        suspend fun updateProgress(progress: Int) {
            this@RefreshWorker.setProgress(workDataOf(Progress to progress))
        }

        notification.show(0)
        try {
            withContext(Dispatchers.Default) {
                dbHelper.updateDatabase(applicationContext) { progress ->
                    notification.show(progress)
                    launch(Dispatchers.Main) {
                        updateProgress(progress)
                    }
                }
            }
        } catch (e: CancellationException) {
            println("Work cancelled!")
        } finally {
            println("Clean up!")
            notification.hide()
        }

        return Result.success()
    }

}
