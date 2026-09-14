#!/bin/bash
cat << 'INNER' > patch.diff
--- app/src/main/java/com/example/RideNotificationService.kt
+++ app/src/main/java/com/example/RideNotificationService.kt
@@ -69,17 +69,19 @@
         val text = rawText.lowercase()
 
+        if (packageName == "com.rapido.rider") {
+            return
+        }
+
         // Check if the notification is from allowed Rapido/Driver apps
-        if (AutoAcceptService.ALLOWED_RAPIDO_PACKAGES.contains(packageName)) {
-            val combinedText = "$title$text"
+        if (packageName == AutoAcceptService.RAPIDO_CAPTAIN_PACKAGE) {
+            val combinedText = "$title $text"
+
+            val promotionalKeywords = listOf("promise", "service", "update", "discount", "offer", "cashback", "earnings", "tips")
+            if (promotionalKeywords.any { combinedText.contains(it) }) {
+                return
+            }
 
             // Checking common incoming order keywords
-            if (combinedText.contains("new") ||
-                combinedText.contains("incoming") ||
-                combinedText.contains("accept") ||
-                combinedText.contains("request") ||
-                combinedText.contains("ride") ||
-                combinedText.contains("order") ||
-                combinedText.contains("booking")
-            ) {
+            val orderKeywords = listOf("new order", "incoming order", "captain, you have a new ride", "new ride request", "pickup")
+            if (orderKeywords.any { combinedText.contains(it) }) {
                 Log.i(TAG, "Genuine Ride Notification detected from $packageName")
                 DebugLogManager.logNotification(
INNER
patch -p0 < patch.diff
