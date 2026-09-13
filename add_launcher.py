import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

launcher_code = """
    val soundPickerLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri: android.net.Uri? = result.data?.getParcelableExtra(android.media.RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            onCustomSoundChange(uri?.toString())
        }
    }
"""

content = content.replace("    var newAppText by remember { mutableStateOf(\"\") }", "    var newAppText by remember { mutableStateOf(\"\") }" + launcher_code)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)
