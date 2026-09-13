with open("app/src/main/java/com/example/AutoAcceptService.kt", "r") as f:
    content = f.read()

# Fix the method signature
wrong_signature = """        private fun cleanStaleAcceptedRides()
                            incrementDailyTripCount(this@AutoAcceptService) {"""
correct_signature = """        private fun cleanStaleAcceptedRides() {"""
content = content.replace(wrong_signature, correct_signature)

with open("app/src/main/java/com/example/AutoAcceptService.kt", "w") as f:
    f.write(content)
print("Fixed method signature")
