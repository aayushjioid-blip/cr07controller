package com.controller.cricket07

import org.junit.Assert.*
import org.junit.Test

class HidReportPacketTest {

    @Test
    fun testGamepad7BytePacketStructure() {
        val leftStickX = 128.toByte()
        val leftStickY = 128.toByte()
        val hatSwitch = 8.toByte() // Released / Centered
        val buttonsMask = 0.toShort()

        val report = ByteArray(7)
        report[0] = leftStickX
        report[1] = leftStickY
        report[2] = leftStickX // Z Axis
        report[3] = leftStickY // Rz Axis
        report[4] = (hatSwitch.toInt() and 0x0F).toByte()
        report[5] = (buttonsMask.toInt() and 0xFF).toByte()
        report[6] = ((buttonsMask.toInt() shr 8) and 0xFF).toByte()

        assertEquals("Neutral Left Stick X must be 128", 128.toByte(), report[0])
        assertEquals("Neutral Left Stick Y must be 128", 128.toByte(), report[1])
        assertEquals("Neutral Z must mirror X", 128.toByte(), report[2])
        assertEquals("Neutral Rz must mirror Y", 128.toByte(), report[3])
        assertEquals("Neutral Hat switch must be 8", 8.toByte(), report[4])
        assertEquals("Buttons 1-8 must be 0", 0.toByte(), report[5])
        assertEquals("Buttons 9-16 must be 0", 0.toByte(), report[6])
    }

    @Test
    fun testGamepadButtonBitmask() {
        // Test Button 1 (A/Space) and Button 6 (Shift) pressed
        val button1 = 1
        val button6 = 6

        var buttonsMask = 0
        buttonsMask = buttonsMask or (1 shl (button1 - 1))
        buttonsMask = buttonsMask or (1 shl (button6 - 1))

        val report = ByteArray(7)
        report[5] = (buttonsMask and 0xFF).toByte()
        report[6] = ((buttonsMask shr 8) and 0xFF).toByte()

        // Button 1 is bit 0 (0x01), Button 6 is bit 5 (0x20). 0x01 | 0x20 = 0x21 (33)
        assertEquals(0x21.toByte(), report[5])
        assertEquals(0x00.toByte(), report[6])
    }

    @Test
    fun testKeyboard8BytePacketStructure() {
        val modifierMask: Byte = 0x02 // Left Shift
        val keyW: Byte = 0x1A // W key HID scancode

        val report = ByteArray(8)
        report[0] = modifierMask
        report[1] = 0x00 // Reserved
        report[2] = keyW

        assertEquals(0x02.toByte(), report[0])
        assertEquals(0x00.toByte(), report[1])
        assertEquals(0x1A.toByte(), report[2])
    }
}
