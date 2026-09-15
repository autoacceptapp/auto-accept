import re

with open("app/src/main/java/com/example/AutoAcceptService.kt", "r") as f:
    content = f.read()

# Remove fake defaults
fake_defaults_old = """            val parsedPickup = extractPickupLocation(cardTexts) ?: "Nearby Pickup"
            val parsedDrop = extractDropLocation(cardTexts) ?: "Destination Drop"

            val rideSignature = generateRideSignature(sourcePackage, parsedPrice, parsedDistance, parsedPickup, parsedDrop)"""
fake_defaults_new = """            val parsedPickup = extractPickupLocation(cardTexts)
            val parsedDrop = extractDropLocation(cardTexts)

            val rideSignature = generateRideSignature(sourcePackage, parsedPrice, parsedDistance, parsedRating, parsedPickup, parsedDrop)"""
content = content.replace(fake_defaults_old, fake_defaults_new)

log_drop_old = """                    details = "Drop: $parsedDrop","""
log_drop_new = """                    details = "Drop: ${parsedDrop ?: "Unknown"}","""
content = content.replace(log_drop_old, log_drop_new)

log_ride_old = """                logRideLocally(this, parsedPrice ?: 0f, parsedDistance ?: 0f, parsedPickup, parsedDrop, "IGNORED", filterDecision.reason, sourcePackage)"""
log_ride_new = """                logRideLocally(this, parsedPrice ?: 0f, parsedDistance ?: 0f, parsedPickup ?: "Unknown", parsedDrop ?: "Unknown", "IGNORED", filterDecision.reason, sourcePackage)"""
content = content.replace(log_ride_old, log_ride_new)

reval_extract_old = """                        val cPickup = extractPickupLocation(cCardTexts) ?: "Nearby Pickup"
                        val cDrop = extractDropLocation(cCardTexts) ?: "Destination Drop"
                        
                        val currentSignature = generateRideSignature(sourcePackage, cPrice, cDist, cPickup, cDrop)"""
reval_extract_new = """                        val cPickup = extractPickupLocation(cCardTexts)
                        val cDrop = extractDropLocation(cCardTexts)
                        
                        val currentSignature = generateRideSignature(sourcePackage, cPrice, cDist, cRating, cPickup, cDrop)"""
content = content.replace(reval_extract_old, reval_extract_new)

announcement_old = """                                    dropLocation = capturedRide.drop
                                )"""
announcement_new = """                                    dropLocation = capturedRide.drop ?: "Unknown"
                                )"""
content = content.replace(announcement_old, announcement_new)

announcement_old2 = """                            dropLocation = capturedRide.drop
                        )"""
announcement_new2 = """                            dropLocation = capturedRide.drop ?: "Unknown"
                        )"""
content = content.replace(announcement_old2, announcement_new2)

logRide_old2 = """                                dropLocation = capturedRide.drop,
                                status = "DISPATCHED","""
logRide_new2 = """                                dropLocation = capturedRide.drop ?: "Unknown",
                                status = "DISPATCHED","""
content = content.replace(logRide_old2, logRide_new2)

send_noti_old = """                                message = "Dist: $finalDist | Drop: ${capturedRide.drop}"
                            )"""
send_noti_new = """                                message = "Dist: $finalDist | Drop: ${capturedRide.drop ?: "Unknown"}"
                            )"""
content = content.replace(send_noti_old, send_noti_new)


with open("app/src/main/java/com/example/AutoAcceptService.kt", "w") as f:
    f.write(content)
