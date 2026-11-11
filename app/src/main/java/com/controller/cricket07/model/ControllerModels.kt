package com.controller.cricket07.model

import com.controller.cricket07.hid.HidConsts

data class ControllerButton(
    val id: String,
    val label: String,
    val subLabel: String = "",
    val xPercent: Float,
    val yPercent: Float,
    val radiusDp: Float,
    val hidKeyCode: Byte = 0,
    val isModifier: Boolean = false,
    val modifierMask: Byte = 0
)

data class ControllerPreset(
    val id: String,
    val name: String,
    val description: String,
    val buttons: List<ControllerButton>
)

object PresetRepository {

    val CRICKET_07_PRESET = ControllerPreset(
        id = "cricket_07_day_out",
        name = "Cricket 07 Day Out",
        description = "Standard EA Sports Cricket 07 Batting, Bowling & Fielding Layout",
        buttons = listOf(
            // D-PAD: Directional controls (Left Thumb)
            ControllerButton("up", "▲", "Pitch/Line", 0.16f, 0.40f, 36f, HidConsts.KEY_UP_ARROW),
            ControllerButton("down", "▼", "Short/Bouncer", 0.16f, 0.76f, 36f, HidConsts.KEY_DOWN_ARROW),
            ControllerButton("left", "◀", "Off/Leg", 0.08f, 0.58f, 36f, HidConsts.KEY_LEFT_ARROW),
            ControllerButton("right", "▶", "Leg/Off", 0.24f, 0.58f, 36f, HidConsts.KEY_RIGHT_ARROW),

            // MODIFIERS: Batting footwork & shot power (Left Upper Thumb)
            ControllerButton("shift", "SHIFT", "Front Foot / Loft", 0.08f, 0.20f, 32f, isModifier = true, modifierMask = HidConsts.MOD_LEFT_SHIFT),
            ControllerButton("ctrl", "CTRL", "Back Foot / Defend", 0.22f, 0.20f, 32f, isModifier = true, modifierMask = HidConsts.MOD_LEFT_CTRL),

            // ACTION CLUSTER: Shots, Bowling types, Running (Right Thumb)
            ControllerButton("btn_w", "W", "Charge / Special", 0.82f, 0.35f, 36f, HidConsts.KEY_W),
            ControllerButton("btn_a", "A", "Cut / Glance", 0.73f, 0.55f, 36f, HidConsts.KEY_A),
            ControllerButton("btn_d", "D", "Drive / Run", 0.91f, 0.55f, 36f, HidConsts.KEY_D),
            ControllerButton("btn_s", "S", "Loft / Six / Cancel", 0.82f, 0.75f, 36f, HidConsts.KEY_S),

            // SPRINT / BOWLING RELEASE
            ControllerButton("space", "SPACE", "Fast Run / Timing", 0.82f, 0.92f, 28f, HidConsts.KEY_SPACE),

            // GAME UTILITY: Pause & Menu Confirm
            ControllerButton("esc", "ESC", "Pause", 0.45f, 0.12f, 24f, HidConsts.KEY_ESCAPE),
            ControllerButton("enter", "ENTER", "Select", 0.55f, 0.12f, 24f, HidConsts.KEY_ENTER)
        )
    )

    fun getAvailablePresets(): List<ControllerPreset> = listOf(CRICKET_07_PRESET)
}
