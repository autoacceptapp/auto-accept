with open("processActiveWindowOriginal.kt", "r") as f:
    orig = f.read()

with open("processActiveWindow.kt", "r") as f:
    new_ver = f.read()

with open("app/src/main/java/com/example/AutoAcceptService.kt", "r") as f:
    content = f.read()

# Add sendAutoAcceptNotification helper
helper = """    private fun sendAutoAcceptNotification(context: Context, title: String, message: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val builder = androidx.core.app.NotificationCompat.Builder(context, ALERT_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
        manager.notify(ALERT_NOTIFICATION_ID, builder.build())
    }
"""

content = content.replace("    private fun processActiveWindow", helper + "\n    private fun processActiveWindow")
content = content.replace(orig, new_ver)

with open("app/src/main/java/com/example/AutoAcceptService.kt", "w") as f:
    f.write(content)
