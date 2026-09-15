import re

with open("app/src/main/java/com/example/AutoAcceptService.kt", "r") as f:
    content = f.read()

# Fix cRating
cRating_old = """                        val cPickup = extractPickupLocation(cCardTexts)
                        val cDrop = extractDropLocation(cCardTexts)
                        
                        val currentSignature = generateRideSignature(sourcePackage, cPrice, cDist, cRating, cPickup, cDrop)"""
cRating_new = """                        val cRating = extractPassengerRating(cCardTexts)
                        val cPickup = extractPickupLocation(cCardTexts)
                        val cDrop = extractDropLocation(cCardTexts)
                        
                        val currentSignature = generateRideSignature(sourcePackage, cPrice, cDist, cRating, cPickup, cDrop)"""
content = content.replace(cRating_old, cRating_new)

# Fix pickupLocation
pickup_old = """                                pickupLocation = capturedRide.pickup,
                                dropLocation = capturedRide.drop ?: "Unknown","""
pickup_new = """                                pickupLocation = capturedRide.pickup ?: "Unknown",
                                dropLocation = capturedRide.drop ?: "Unknown","""
content = content.replace(pickup_old, pickup_new)

with open("app/src/main/java/com/example/AutoAcceptService.kt", "w") as f:
    f.write(content)
