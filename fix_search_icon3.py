import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

# Search icon requires material-icons-extended, but we only have core.
# Let's replace it with Icons.Default.Info or another icon that is in core.
# Icons.Default.Search was not imported properly. Let's just use Icons.Default.Search? No, it's not in core.
# Wait, Icons.Default.Search IS in material-icons-core in compose. Let's check imports.
# In compose, Search is actually `Icons.Default.Search`.
# Let's import it. Wait, the error is `Unresolved reference 'Search'`. That means `Icons.Default.Search` failed.
# Let's just use `Icons.Default.Settings` since we know it exists. Or `Icons.Default.Info`.

content = content.replace("androidx.compose.material.icons.filled.Search", "Icons.Default.Info")

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)

