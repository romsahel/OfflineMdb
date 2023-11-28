package com.rsahel.offlinemdb

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class RefreshNotification(val context: Context, cancelPendingIntent: PendingIntent) {
    private val maxProgress = 100
    private val channelId = "OMDbNotificationChannel"

    val notificationId = 1
    var builder: NotificationCompat.Builder

    init {
        createChannel()
        builder = NotificationCompat.Builder(context, channelId).apply {
            setSmallIcon(R.drawable.ic_launcher_foreground)
            setContentTitle(context.getString(R.string.notification_title))
            priority = NotificationCompat.PRIORITY_LOW
            setOngoing(true)
            addAction(android.R.drawable.ic_delete, "Cancel", cancelPendingIntent)
        }
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            channelId,
            "OMDb",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        channel.description = context.getString(R.string.notification_description)

        NotificationManagerCompat.from(context).createNotificationChannel(
            channel
        )
    }

    fun show(progress: Int) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        NotificationManagerCompat.from(context).apply {
            builder.setProgress(maxProgress, progress, false)
            notify(notificationId, builder.build())
        }
    }

    fun hide() {
        NotificationManagerCompat.from(context).apply {
            cancel(notificationId)
        }
    }
}