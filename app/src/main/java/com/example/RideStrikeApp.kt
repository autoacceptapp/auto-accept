package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp

class RideStrikeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            FirebaseHelper.initialize(this)
            Log.d("RideStrikeApp", "FirebaseApp initialized via FirebaseHelper in Application onCreate")
        } catch (e: Exception) {
            Log.e("RideStrikeApp", "Failed to initialize FirebaseApp in Application: ${e.message}", e)
            e.printStackTrace()
        }

        try {
            ServiceStatusNotificationManager.createNotificationChannel(this)
            ServiceStatusNotificationManager.updateStatus(this)
            AutoAcceptService.createNotificationChannels(this)
            AutoAcceptService.initCache(this)
            
            if (AutoAcceptService.isAutomationEnabled(this)) {
                KeepAliveService.start(this)
            }
        } catch (e: Exception) {
            Log.e("RideStrikeApp", "Failed to initialize ServiceStatusNotificationManager: ${e.message}", e)
        }
    }
}
