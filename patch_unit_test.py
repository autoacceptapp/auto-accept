import re

with open("app/src/test/java/com/example/ExampleUnitTest.kt", "r") as f:
    content = f.read()

# Replace generateRideSignature(...) in ExampleUnitTest.kt
content = re.sub(
    r'generateRideSignature\((.*?)\s*,\s*(.*?)\s*,\s*(.*?)\s*,\s*(.*?)\s*,\s*(.*?)\)',
    r'generateRideSignature(\1, \2, \3, 4.5f, \4, \5)',
    content
)

with open("app/src/test/java/com/example/ExampleUnitTest.kt", "w") as f:
    f.write(content)
