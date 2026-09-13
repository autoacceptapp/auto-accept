import re

with open("app/src/main/java/com/example/AutoAcceptService.kt", "r") as f:
    content = f.read()

# Add constants for the new notification channel
content = content.replace(
    'const val NOTIFICATION_ID = 1',
    '''const val NOTIFICATION_ID = 1
        const val ALERT_CHANNEL_ID = "order_alerts_channel"
        const val ALERT_CHANNEL_NAME = "Order Alerts"
        const val ALERT_NOTIFICATION_ID = 2'''
)

# In onServiceConnected, create the new channel
content = re.sub(
    r'(fun startForegroundNotification\(\) \{[\s\S]*?val manager = getSystemService\(NotificationManager::class\.java\))',
    r'\1\n        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {\n            val alertChannel = NotificationChannel(\n                ALERT_CHANNEL_ID,\n                ALERT_CHANNEL_NAME,\n                NotificationManager.IMPORTANCE_HIGH\n            )\n            manager.createNotificationChannel(alertChannel)\n        }',
    content
)

with open("app/src/main/java/com/example/AutoAcceptService.kt", "w") as f:
    f.write(content)
