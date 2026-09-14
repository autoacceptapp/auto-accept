cat << 'INNER_EOF' > /tmp/AutoAcceptUpdate.kt
    private fun startForegroundNotification() {
        try {
            val accActive = isServiceConnected()
            val notifActive = isNotificationListenerEnabled(this)
            val batActive = isBatteryOptimizationIgnored(this)
            val masterOn = isAutomationEnabled(this)
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            
            if (accActive && notifActive && batActive && masterOn) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    stopForeground(true)
                }
                notificationManager.cancel(NOTIFICATION_ID)
                Log.i(TAG, "All services healthy. Removed persistent foreground notification.")
            } else {
                ServiceStatusNotificationManager.startOrUpdateForeground(this)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to manage foreground notification: ${e.message}", e)
        }

        if (isAutomationEnabled(this)) {
            KeepAliveService.start(this)
        }
        ServiceStatusNotificationManager.updateStatus(this)
    }
INNER_EOF
# We need to replace the old startForegroundNotification block with the new one.
# First, find the lines for the old block
START_LINE=$(grep -n "private fun startForegroundNotification() {" app/src/main/java/com/example/AutoAcceptService.kt | cut -d: -f1)
END_LINE=$(grep -n -A 30 "private fun startForegroundNotification() {" app/src/main/java/com/example/AutoAcceptService.kt | grep -n "^ *ServiceStatusNotificationManager.updateStatus(this)" | cut -d: -f1)
END_LINE=$((START_LINE + END_LINE))

# It's better to just use sed to replace the content
# Wait, I previously changed the call at 1839 to `ServiceStatusNotificationManager.startOrUpdateForeground(this)`. Let's revert that.
sed -i 's/ServiceStatusNotificationManager.startOrUpdateForeground(this)/startForegroundNotification()/g' app/src/main/java/com/example/AutoAcceptService.kt

