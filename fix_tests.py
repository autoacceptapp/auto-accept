import re

with open("app/src/test/java/com/example/ExampleUnitTest.kt", "r") as f:
    content = f.read()

content = re.sub(
    r'distanceKm\s*=\s*(.*?),\s*pickup\s*=',
    r'distanceKm = \1, rating = null, pickup =',
    content
)

with open("app/src/test/java/com/example/ExampleUnitTest.kt", "w") as f:
    f.write(content)
