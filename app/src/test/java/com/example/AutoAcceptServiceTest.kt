package com.example

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AutoAcceptServiceTest {

    @Test
    fun testRideSignature_SameData() {
        val sig1 = AutoAcceptService.generateRideSignature("pkg1", 100f, 5f, 4.5f, "A", "B")
        val sig2 = AutoAcceptService.generateRideSignature("pkg1", 100f, 5f, 4.5f, "A", "B")
        assertEquals(sig1, sig2)
    }

    @Test
    fun testRideSignature_DifferentRating() {
        val sig1 = AutoAcceptService.generateRideSignature("pkg1", 100f, 5f, 4.5f, "A", "B")
        val sig2 = AutoAcceptService.generateRideSignature("pkg1", 100f, 5f, 4.8f, "A", "B")
        assertNotEquals(sig1, sig2)
    }

    @Test
    fun testRideSignature_MissingPickup() {
        val sig1 = AutoAcceptService.generateRideSignature("pkg1", 100f, 5f, 4.5f, null, "B")
        val sig2 = AutoAcceptService.generateRideSignature("pkg1", 100f, 5f, 4.5f, "A", "B")
        assertNotEquals(sig1, sig2)
    }
    
    @Test
    fun testRideSignature_MissingDestination() {
        val sig1 = AutoAcceptService.generateRideSignature("pkg1", 100f, 5f, 4.5f, "A", null)
        val sig2 = AutoAcceptService.generateRideSignature("pkg1", 100f, 5f, 4.5f, "A", "B")
        assertNotEquals(sig1, sig2)
    }
    
    @Test
    fun testRideSignature_DifferentPackage() {
        val sig1 = AutoAcceptService.generateRideSignature("pkg1", 100f, 5f, 4.5f, "A", "B")
        val sig2 = AutoAcceptService.generateRideSignature("pkg2", 100f, 5f, 4.5f, "A", "B")
        assertNotEquals(sig1, sig2)
    }

    @Test
    fun testRideSignature_ChangedFare() {
        val sig1 = AutoAcceptService.generateRideSignature("pkg1", 100f, 5f, 4.5f, "A", "B")
        val sig2 = AutoAcceptService.generateRideSignature("pkg1", 150f, 5f, 4.5f, "A", "B")
        assertNotEquals(sig1, sig2)
    }
    
    @Test
    fun testRideSignature_ChangedDistance() {
        val sig1 = AutoAcceptService.generateRideSignature("pkg1", 100f, 5.0f, 4.5f, "A", "B")
        val sig2 = AutoAcceptService.generateRideSignature("pkg1", 100f, 5.1f, 4.5f, "A", "B")
        assertNotEquals(sig1, sig2)
    }

    @Test
    fun testIsValidAcceptText_GenericGo() {
        assertFalse(AutoAcceptService.isValidAcceptText(null, "go"))
        assertFalse(AutoAcceptService.isValidAcceptText(null, "yes"))
        assertFalse(AutoAcceptService.isValidAcceptText(null, "chalo"))
    }
    
    @Test
    fun testIsValidAcceptText_Valid() {
        assertTrue(AutoAcceptService.isValidAcceptText(null, "Accept"))
        assertTrue(AutoAcceptService.isValidAcceptText(null, "Take Order"))
        assertTrue(AutoAcceptService.isValidAcceptText(null, "Swipe to Accept"))
    }
}
