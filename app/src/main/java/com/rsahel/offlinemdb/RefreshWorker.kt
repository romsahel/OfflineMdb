package com.rsahel.offlinemdb

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
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
        val Tag = "Refresh"
        val Progress = "Progress"
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        createChannel()
        return ForegroundInfo(
            notification.notificationId,
            notification.builder.build(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            notification.channelId,
            "MdbOffline",
            NotificationManager.IMPORTANCE_DEFAULT
        )

        NotificationManagerCompat.from(applicationContext).createNotificationChannel(
            channel
        )
    }

    override suspend fun doWork(): Result {
        val dbHelper = DatabaseHelper.getInstance()!!

        suspend fun updateProgress(progress: Int) {
            this@RefreshWorker.setProgress(workDataOf(Progress to progress))
        }

        try {
            withContext(Dispatchers.Default) {
                dbHelper.updateDatabase(applicationContext) { progress ->
                    notification.show(progress)
                    launch(Dispatchers.Main) {
                        updateProgress(progress)
                    }
                }
            }
        } catch (e: CancellationException){
            println("Work cancelled!")
        } finally {
            println("Clean up!")
            notification.hide()
        }

        return Result.success()
    }

}
