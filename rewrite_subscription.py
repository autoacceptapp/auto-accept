import re

with open("app/src/main/java/com/example/SubscriptionManager.kt", "r") as f:
    content = f.read()

# Let's locate the full applyReferralCode block
start_idx = content.find("suspend fun applyReferralCode(")
end_idx = content.find("Result.success(\"🎉 Referral Applied! You earned 50 Points and your friend earned 20 Points!\")")

# We will replace everything from start_idx to the end of applyReferralCode
# Let's just do a regex replace or manual patch.
