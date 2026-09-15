import re

with open("app/src/main/java/com/example/DashboardTabs.kt", "r") as f:
    content = f.read()

old_dialog = """    // Apply Referral Code Dialog
    if (showReferralDialog) {
        AlertDialog("""

new_dialog = """    // Apply Referral Code Dialog
    if (showReferralDialog) {
        val sharedPrefs = context.getSharedPreferences("subscription_prefs", android.content.Context.MODE_PRIVATE)
        val isDeviceClaimed = sharedPrefs.getBoolean("KEY_REFERRAL_CLAIMED_ON_DEVICE", false)
        
        AlertDialog("""

content = content.replace(old_dialog, new_dialog)

old_text = """                    if (currentUser == null) {
                        Text(
                            text = "⚠️ Please sign in with Google to claim your referral points.",
                            fontSize = 12.sp,
                            color = Amber400
                        )
                    }
                }
            },
            confirmButton = {"""

new_text = """                    if (currentUser == null) {
                        Text(
                            text = "⚠️ Please sign in with Google to claim your referral points.",
                            fontSize = 12.sp,
                            color = Amber400
                        )
                    } else if (isDeviceClaimed) {
                        Text(
                            text = "⚠️ Referral bonus already claimed on this device.",
                            fontSize = 12.sp,
                            color = Amber400
                        )
                    }
                }
            },
            confirmButton = {"""

content = content.replace(old_text, new_text)

old_button = """            confirmButton = {
                Button(
                    onClick = {"""

new_button = """            confirmButton = {
                Button(
                    enabled = !isApplyingReferral && !isDeviceClaimed,
                    onClick = {"""

content = content.replace(old_button, new_button)

with open("app/src/main/java/com/example/DashboardTabs.kt", "w") as f:
    f.write(content)
