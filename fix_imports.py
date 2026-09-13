with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

import_lines = """import androidx.compose.foundation.layout.fillMaxHeight
import com.example.ui.theme.Emerald600
"""

content = content.replace("import com.example.ui.theme.Emerald400", "import com.example.ui.theme.Emerald400\n" + import_lines)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)
