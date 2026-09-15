import re

with open("app/src/main/java/com/example/AutoAcceptService.kt", "r") as f:
    content = f.read()

# Replace the pattern with the correct one
pattern1 = r'val pattern = Regex\("""\^\(\?:swipe\\s\+to\\s\+accept\|accept\(\?:\\s\+\(\?:order\|ride\)\)\?\|take\\s\+order\|confirm\\s\+order\|go\|chalo\|shuru\|yes\)\(\?:\\s\*\[\\\\\(>→»\\\\d\\\\w\\\\s\]\*\)\?\$""", RegexOption\.IGNORE_CASE\)'
pattern1_replacement = 'val pattern = Regex("""^(?:swipe\\\\s+to\\\\s+accept|accept(?:\\\\s+(?:order|ride))?|take\\\\s+(?:order|ride)|confirm(?:\\\\s+order)?)(?:\\\\s*[\\\\(>→»\\\\d\\\\w\\\\s]*)?$""", RegexOption.IGNORE_CASE)'

content = re.sub(pattern1, pattern1_replacement, content)

pattern2 = r'val fuzzyRootPattern = Regex\("""\\b\(\?:accept\|swipe\|take\|confirm\|go\|chalo\|shuru\|yes\)\\b""", RegexOption\.IGNORE_CASE\)'
pattern2_replacement = 'val fuzzyRootPattern = Regex("""\\\\b(?:accept|swipe|take|confirm)\\\\b""", RegexOption.IGNORE_CASE)'
content = re.sub(pattern2, pattern2_replacement, content)

with open("app/src/main/java/com/example/AutoAcceptService.kt", "w") as f:
    f.write(content)
