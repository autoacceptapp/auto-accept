import re

with open("app/src/main/java/com/example/AutoAcceptService.kt", "r") as f:
    content = f.read()

pattern_old = r'val pattern = Regex\("""\^\(\?:swipe\\s\+to\\s\+accept\|accept\(\?:\\s\+\(\?:order\|ride\)\)\?\|take\\s\+order\|confirm\\s\+order\|go\|chalo\|shuru\|yes\)\(\?:\\s\*\[\\\\\(>→»\\\\d\\\\w\\\\s\]\*\)\?\$""", RegexOption.IGNORE_CASE\)'
pattern_new = r'val pattern = Regex("""^(?:swipe\s+to\s+accept|accept(?:\s+(?:order|ride))?|take\s+(?:order|ride)|confirm(?:\s+order)?)(?:\s*[\(>→»\d\w\s]*)?$""", RegexOption.IGNORE_CASE)'

fuzzy_old = r'val fuzzyRootPattern = Regex\("""\\b\(\?:accept\|swipe\|take\|confirm\|go\|chalo\|shuru\|yes\)\\b""", RegexOption.IGNORE_CASE\)'
fuzzy_new = r'val fuzzyRootPattern = Regex("""\b(?:accept|swipe|take|confirm)\b""", RegexOption.IGNORE_CASE)'

# I will just use string replacement for safety
content = content.replace("""val pattern = Regex(\"\"\"^(?:swipe\s+to\s+accept|accept(?:\s+(?:order|ride))?|take\s+order|confirm\s+order|go|chalo|shuru|yes)(?:\s*[\(>→»\d\w\s]*)?$\"\"\", RegexOption.IGNORE_CASE)""",
                          """val pattern = Regex(\"\"\"^(?:swipe\s+to\s+accept|accept(?:\s+(?:order|ride))?|take\s+(?:order|ride)|confirm(?:\s+order)?)(?:\s*[\(>→»\d\w\s]*)?$\"\"\", RegexOption.IGNORE_CASE)""")

content = content.replace("""val fuzzyRootPattern = Regex(\"\"\"\b(?:accept|swipe|take|confirm|go|chalo|shuru|yes)\b\"\"\", RegexOption.IGNORE_CASE)""",
                          """val fuzzyRootPattern = Regex(\"\"\"\b(?:accept|swipe|take|confirm)\b\"\"\", RegexOption.IGNORE_CASE)""")

with open("app/src/main/java/com/example/AutoAcceptService.kt", "w") as f:
    f.write(content)
