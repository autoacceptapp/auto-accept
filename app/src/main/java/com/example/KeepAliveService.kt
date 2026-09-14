package com.example

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log

class KeepAliveService : Service() {
    companion object {
        private const val TAG = "KeepAliveService"

        fun start(context: Context) {
            try {
                val intent = Intent(context, KeepAliveService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start KeepAliveService: ${e.message}")
            }
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, KeepAliveService::class.java)
                context.stopService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop KeepAliveService: ${e.message}")
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = ServiceStatusNotificationManager.buildNotification(this)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                try {
                    startForeground(
                        ServiceStatusNotificationManager.NOTIFICATION_ID, 
                        notification, 
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    )
                } catch (e: Throwable) {
                    startForeground(ServiceStatusNotificationManager.NOTIFICATION_ID, notification)
                }
            } else {
                startForeground(ServiceStatusNotificationManager.NOTIFICATION_ID, notification)
            }
            Log.i(TAG, "KeepAliveService foreground started successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground on KeepAliveService: ${e.message}", e)
        }
        
        // Return START_STICKY to ensure the OS tries to restart this service if it gets killed
        return START_STICKY
    }
}
