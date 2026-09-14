package com.example

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder

class KeepAliveService : Service() {
    companion object {
        fun start(context: Context) {
            // No-op to avoid ForegroundServiceDidNotStartInTimeException
        }

        fun stop(context: Context) {
            // No-op
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        stopSelf()
        return START_NOT_STICKY
    }
}
