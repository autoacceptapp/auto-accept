import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

args_to_replace = """    isTtsOn: Boolean,
    onTtsChange: (Boolean) -> Unit,
    userNameInput: String,"""
    
args_replacement = """    isTtsOn: Boolean,
    onTtsChange: (Boolean) -> Unit,
    customSoundUri: String?,
    onCustomSoundChange: (String?) -> Unit,
    userNameInput: String,"""

content = content.replace(args_to_replace, args_replacement)

caller_to_replace = """            isTtsOn = isTtsOn,
            onTtsChange = { checked ->
                if (isPassActive) {
                    isTtsOn = checked
                    AutoAcceptService.setTtsEnabled(context, checked)
                }
            },
            userNameInput = userNameInput,"""
            
caller_replacement = """            isTtsOn = isTtsOn,
            onTtsChange = { checked ->
                if (isPassActive) {
                    isTtsOn = checked
                    AutoAcceptService.setTtsEnabled(context, checked)
                }
            },
            customSoundUri = customSoundUri,
            onCustomSoundChange = { uri ->
                if (isPassActive) {
                    customSoundUri = uri
                    AutoAcceptService.setCustomSoundUri(context, uri)
                }
            },
            userNameInput = userNameInput,"""

content = content.replace(caller_to_replace, caller_replacement)

var_to_replace = """    var ttsLanguage by remember { mutableStateOf(AutoAcceptService.getTtsLanguage(context)) }"""
var_replacement = """    var ttsLanguage by remember { mutableStateOf(AutoAcceptService.getTtsLanguage(context)) }
    var customSoundUri by remember { mutableStateOf(AutoAcceptService.getCustomSoundUri(context)) }"""

content = content.replace(var_to_replace, var_replacement)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)
