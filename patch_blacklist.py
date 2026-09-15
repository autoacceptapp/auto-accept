import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

# Let's search for the Blacklist card
# Look for "BLACKLIST KEYWORDS CARD" or similar

