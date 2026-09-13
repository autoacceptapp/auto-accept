with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

content = content.replace("            if (tripHistory.isNotEmpty()) {\n                Card(", "                Card(")
content = content.replace("""                    }
                }
            }

            // ========================================================================
            // 1. MASTER AUTO-ACCEPT""", """                    }
                }

            // ========================================================================
            // 1. MASTER AUTO-ACCEPT""")

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)
