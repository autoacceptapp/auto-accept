package com.example

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

object FirebaseHelper {
    fun initialize(context: Context) {
        if (FirebaseApp.getApps(context).isEmpty()) {
            try {
                FirebaseApp.initializeApp(context)
                Log.d("FirebaseHelper", "FirebaseApp initialized via default google-services configuration")
            } catch (e: Exception) {
                Log.w("FirebaseHelper", "Default FirebaseApp init failed (${e.message}), using fallback FirebaseOptions", e)
                try {
                    val options = FirebaseOptions.Builder()
                        .setApplicationId("1:675050958546:android:1e14e7154a92402c4f470b")
                        .setProjectId("auto-accept-app")
                        .setApiKey("AIzaSyD-49I1kwAqY94qoNKroVQY21wpsYyy06c")
                        .setDatabaseUrl("https://auto-accept-app-default-rtdb.asia-southeast1.firebasedatabase.app")
                        .setStorageBucket("auto-accept-app.firebasestorage.app")
                        .setGcmSenderId("675050958546")
                        .build()
                    FirebaseApp.initializeApp(context, options)
                    Log.i("FirebaseHelper", "FirebaseApp initialized successfully with explicit FirebaseOptions")
                } catch (fallbackEx: Exception) {
                    Log.e("FirebaseHelper", "Explicit FirebaseApp init failed: ${fallbackEx.message}", fallbackEx)
                }
            }
        }
    }
}
