import re

with open("app/src/main/java/com/example/data/AppDatabase.kt", "r") as f:
    content = f.read()

content = content.replace("entities = [TargetApp::class, Keyword::class]", "entities = [TargetApp::class, Keyword::class, FilterSettings::class, TripHistoryRecord::class]")
content = content.replace("version = 1", "version = 2")

with open("app/src/main/java/com/example/data/AppDatabase.kt", "w") as f:
    f.write(content)
