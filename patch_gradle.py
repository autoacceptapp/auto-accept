import re

with open("app/build.gradle.kts", "r") as f:
    content = f.read()

if "generativeai" not in content:
    content = content.replace("dependencies {", "dependencies {\n  implementation(\"com.google.ai.client.generativeai:generativeai:0.9.0\")")

with open("app/build.gradle.kts", "w") as f:
    f.write(content)
