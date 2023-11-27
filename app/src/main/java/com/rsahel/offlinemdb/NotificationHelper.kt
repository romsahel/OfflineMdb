package com.rsahel.offlinemdb

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class RefreshNotification(val context: Context, cancelPendingIntent: PendingIntent) {
    val channelId = "OfflineMdb"
    val notificationId = 1
    private val maxProgress = 100
    val builder = NotificationCompat.Builder(context, channelId).apply {
        setSmallIcon(R.drawable.ic_launcher_background)
        setContentTitle("Refresh in progress")
        priority = NotificationCompat.PRIORITY_LOW
        setOngoing(true)
        addAction(android.R.drawable.ic_delete, "Cancel", cancelPendingIntent)
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