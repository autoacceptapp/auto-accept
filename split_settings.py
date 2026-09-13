import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

# 1. Update the SettingsTabContent signature
old_sig = """fun SettingsTabContent(
    isPassActive: Boolean,
    isWakeLockOn: Boolean,"""
new_sig = """fun SettingsTabContent(
    isPassActive: Boolean,
    isDistanceFilterOn: Boolean,
    onDistanceFilterChange: (Boolean) -> Unit,
    maxDistanceInput: String,
    onMaxDistanceChange: (String) -> Unit,
    isPriceFilterOn: Boolean,
    onPriceFilterChange: (Boolean) -> Unit,
    minPriceInput: String,
    onMinPriceChange: (String) -> Unit,
    maxPriceInput: String,
    onMaxPriceChange: (String) -> Unit,
    isBlacklistFilterOn: Boolean,
    onBlacklistFilterChange: (Boolean) -> Unit,
    blacklistInput: String,
    onBlacklistChange: (String) -> Unit,
    isWakeLockOn: Boolean,"""
content = content.replace(old_sig, new_sig)

# 2. Update the call to SettingsTabContent
old_call = """        SettingsTabContent(
            isPassActive = isPassActive,
            isWakeLockOn = isWakeLockOn,"""
new_call = """        SettingsTabContent(
            isPassActive = isPassActive,
            isDistanceFilterOn = isDistanceFilterOn,
            onDistanceFilterChange = { checked ->
                if (isPassActive) {
                    isDistanceFilterOn = checked
                    AutoAcceptService.setDistanceFilterEnabled(context, checked)
                }
            },
            maxDistanceInput = maxDistanceInput,
            onMaxDistanceChange = { newValue ->
                if (isPassActive) {
                    val filtered = newValue.filter { it.isDigit() || it == '.' }
                    maxDistanceInput = filtered
                    filtered.toFloatOrNull()?.let { dist ->
                        AutoAcceptService.setMaxDistanceKm(context, dist)
                    }
                }
            },
            isPriceFilterOn = isPriceFilterOn,
            onPriceFilterChange = { checked ->
                if (isPassActive) {
                    isPriceFilterOn = checked
                    AutoAcceptService.setPriceFilterEnabled(context, checked)
                }
            },
            minPriceInput = minPriceInput,
            onMinPriceChange = { newValue ->
                if (isPassActive) {
                    val filtered = newValue.filter { it.isDigit() }
                    minPriceInput = filtered
                    filtered.toFloatOrNull()?.let { p ->
                        AutoAcceptService.setMinPrice(context, p)
                    }
                }
            },
            maxPriceInput = maxPriceInput,
            onMaxPriceChange = { newValue ->
                if (isPassActive) {
                    val filtered = newValue.filter { it.isDigit() }
                    maxPriceInput = filtered
                    filtered.toFloatOrNull()?.let { p ->
                        AutoAcceptService.setMaxPrice(context, p)
                    }
                }
            },
            isBlacklistFilterOn = isBlacklistFilterOn,
            onBlacklistFilterChange = { checked ->
                if (isPassActive) {
                    isBlacklistFilterOn = checked
                    AutoAcceptService.setBlacklistEnabled(context, checked)
                }
            },
            blacklistInput = blacklistInput,
            onBlacklistChange = { newValue ->
                if (isPassActive) {
                    blacklistInput = newValue
                    AutoAcceptService.setBlacklistKeywords(context, newValue)
                }
            },
            isWakeLockOn = isWakeLockOn,"""
content = content.replace(old_call, new_call)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)
print("Signature and call updated.")
