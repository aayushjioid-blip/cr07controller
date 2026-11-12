package com.controller.cricket07

import com.controller.cricket07.model.PresetRepository
import org.junit.Assert.*
import org.junit.Test

class ButtonLayoutValidationTest {

    @Test
    fun testCricket07ButtonsAtOrBelowEscAndEnterLevel() {
        val preset = PresetRepository.createCricket07Preset()
        assertNotNull(preset)
        assertEquals("cricket_07_day_out", preset.id)

        // Find Esc and Enter buttons
        val escBtn = preset.buttons.find { it.id == "esc" }
        val enterBtn = preset.buttons.find { it.id == "enter" }

        assertNotNull("Esc button must exist", escBtn)
        assertNotNull("Enter button must exist", enterBtn)

        val escY = escBtn!!.yPercent
        val enterY = enterBtn!!.yPercent

        assertEquals("Esc must be at y=0.22f", 0.22f, escY, 0.001f)
        assertEquals("Enter must be at y=0.22f", 0.22f, enterY, 0.001f)

        // Verify that NO button in the preset is above Esc/Enter (y < 0.22f)
        for (btn in preset.buttons) {
            assertTrue(
                "Button '${btn.id}' (${btn.label}) must have y >= 0.22f to sit at or below Esc/Enter, but was ${btn.yPercent}",
                btn.yPercent >= 0.22f
            )
        }

        // Verify specific key placements
        val ctrlBtn = preset.buttons.find { it.id == "ctrl" }
        val shiftBtn = preset.buttons.find { it.id == "shift" }
        assertNotNull(ctrlBtn)
        assertNotNull(shiftBtn)

        assertEquals("Ctrl must sit at y=0.22f level with Esc/Enter", 0.22f, ctrlBtn!!.yPercent, 0.001f)
        assertEquals("Shift must sit at y=0.22f level with Esc/Enter", 0.22f, shiftBtn!!.yPercent, 0.001f)
    }

    @Test
    fun testAllPresetsHaveValidCoordinates() {
        for (preset in PresetRepository.PRESETS) {
            for (btn in preset.buttons) {
                assertTrue("Button x must be between 0 and 1", btn.xPercent in 0.0f..1.0f)
                assertTrue("Button y must be at or below 0.22f", btn.yPercent >= 0.22f)
                assertTrue("Button radius must be positive", btn.radiusDp > 0f)
                assertEquals("Default radius must match initial radius", btn.radiusDp, btn.defaultRadiusDp, 0.001f)
            }
        }
    }
}
