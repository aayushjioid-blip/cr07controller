package com.controller.cricket07

import com.controller.cricket07.hid.HidDescriptor
import org.junit.Assert.*
import org.junit.Test

class DescriptorValidationTest {

    @Test
    fun testWindowsDescriptorStructure() {
        val desc = HidDescriptor.WINDOWS_COMPOSITE_DESCRIPTOR
        assertNotNull("Windows descriptor must not be null", desc)
        assertTrue("Windows descriptor must have valid byte length", desc.size > 80)

        // Verify Report ID 1 (Keyboard) exists
        var foundReportId1 = false
        var foundReportId2 = false

        for (i in 0 until desc.size - 1) {
            if (desc[i] == 0x85.toByte() && desc[i + 1] == 0x01.toByte()) {
                foundReportId1 = true
            }
            if (desc[i] == 0x85.toByte() && desc[i + 1] == 0x02.toByte()) {
                foundReportId2 = true
            }
        }

        assertTrue("Windows descriptor must declare Report ID 1 (Keyboard)", foundReportId1)
        assertTrue("Windows descriptor must declare Report ID 2 (Gamepad)", foundReportId2)
    }

    @Test
    fun testMacDescriptorStructure() {
        val desc = HidDescriptor.MAC_GAMEPAD_DESCRIPTOR
        assertNotNull("Mac descriptor must not be null", desc)
        assertTrue("Mac descriptor must have valid byte length", desc.size > 80)

        // Verify Gamepad is Collection 1 (first collection)
        // 0x05, 0x01 (Usage Page Generic Desktop), 0x09, 0x05 (Usage Gamepad), 0xA1, 0x01 (Collection Application)
        assertEquals(0x05.toByte(), desc[0])
        assertEquals(0x01.toByte(), desc[1])
        assertEquals(0x09.toByte(), desc[2])
        assertEquals(0x05.toByte(), desc[3])
        assertEquals(0xA1.toByte(), desc[4])
        assertEquals(0x01.toByte(), desc[5])

        // Verify Report ID 2 is in Gamepad collection
        assertEquals(0x85.toByte(), desc[6])
        assertEquals(0x02.toByte(), desc[7])
    }
}
