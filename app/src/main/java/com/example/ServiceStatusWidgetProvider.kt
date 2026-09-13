package com.example

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.RemoteViews
import androidx.core.content.ContextCompat

/**
 * ServiceStatusWidgetProvider
 *
 * Android Home Screen Widget that monitors the status of AccessibilityService
 * and NotificationListenerService, displaying 'Active' or 'Inactive' with
 * quick-links to system settings to toggle them.
 */
class ServiceStatusWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH_WIDGET ||
            intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE ||
            intent.action == Intent.ACTION_USER_PRESENT
        ) {
            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
            val thisWidget = ComponentName(context, ServiceStatusWidgetProvider::class.java)
            val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget) ?: return
            for (widgetId in allWidgetIds) {
                updateAppWidget(context, appWidgetManager, widgetId)
            }
        }
    }

    companion object {
        const val ACTION_REFRESH_WIDGET = "com.example.ACTION_REFRESH_WIDGET"

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val isAccActive = ServiceStatusNotificationManager.isAccessibilityActive(context)
            val isNotifActive = ServiceStatusNotificationManager.isNotificationListenerActive(context)

            val views = RemoteViews(context.packageName, R.layout.widget_service_status)

            // 1. Accessibility Service Status
            if (isAccActive) {
                views.setTextViewText(R.id.tv_accessibility_status, "Active")
                views.setTextColor(
                    R.id.tv_accessibility_status,
                    ContextCompat.getColor(context, R.color.widget_active_green)
                )
                views.setInt(
                    R.id.tv_accessibility_status,
                    "setBackgroundResource",
                    R.drawable.widget_badge_active
                )
            } else {
                views.setTextViewText(R.id.tv_accessibility_status, "Inactive")
                views.setTextColor(
                    R.id.tv_accessibility_status,
                    ContextCompat.getColor(context, R.color.widget_inactive_red)
                )
                views.setInt(
                    R.id.tv_accessibility_status,
                    "setBackgroundResource",
                    R.drawable.widget_badge_inactive
                )
            }

            // 2. Notification Listener Service Status
            if (isNotifActive) {
                views.setTextViewText(R.id.tv_notification_status, "Active")
                views.setTextColor(
                    R.id.tv_notification_status,
                    ContextCompat.getColor(context, R.color.widget_active_green)
                )
                views.setInt(
                    R.id.tv_notification_status,
                    "setBackgroundResource",
                    R.drawable.widget_badge_active
                )
            } else {
                views.setTextViewText(R.id.tv_notification_status, "Inactive")
                views.setTextColor(
                    R.id.tv_notification_status,
                    ContextCompat.getColor(context, R.color.widget_inactive_red)
                )
                views.setInt(
                    R.id.tv_notification_status,
                    "setBackgroundResource",
                    R.drawable.widget_badge_inactive
                )
            }

            // Quick-Link 1: Accessibility Settings PendingIntent
            val accIntent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val accPendingIntent = PendingIntent.getActivity(
                context,
                201,
                accIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_accessibility_settings, accPendingIntent)

            // Quick-Link 2: Notification Listener Settings PendingIntent
            val notifIntent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val notifPendingIntent = PendingIntent.getActivity(
                context,
                202,
                notifIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_notification_settings, notifPendingIntent)

            // Quick Action: Refresh widget
            val refreshIntent = Intent(context, ServiceStatusWidgetProvider::class.java).apply {
                action = ACTION_REFRESH_WIDGET
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context,
                203,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_widget_refresh, refreshPendingIntent)

            // Tap root -> open app
            val appIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val appPendingIntent = PendingIntent.getActivity(
                context,
                204,
                appIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, appPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        /**
         * Triggers an immediate refresh on all placed instances of this widget.
         */
        fun updateAllWidgets(context: Context) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
                val thisWidget = ComponentName(context, ServiceStatusWidgetProvider::class.java)
                val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget) ?: return
                for (widgetId in allWidgetIds) {
                    updateAppWidget(context, appWidgetManager, widgetId)
                }
            } catch (_: Exception) {
            }
        }
    }
}
