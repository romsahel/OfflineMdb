package com.rsahel.offlinemdb

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun checkAndAskForPermissions(activity: ComponentActivity) {
    val requestPermissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}
    val permission = Manifest.permission.POST_NOTIFICATIONS
    when {
        ContextCompat.checkSelfPermission(
            activity.applicationContext, permission
        ) == PackageManager.PERMISSION_GRANTED -> {
        }

        ActivityCompat.shouldShowRequestPermissionRationale(activity, permission) -> {
            requestPermissionLauncher.launch(permission)
        }

        else -> {
            requestPermissionLauncher.launch(permission)
        }
    }
}