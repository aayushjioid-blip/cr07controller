package com.controller.cricket07.model

import com.controller.cricket07.hid.HidConsts

data class ControllerButton(
    val id: String,
    val label: String,
    val subLabel: String = "",
    var xPercent: Float,
    var yPercent: Float,
    val radiusDp: Float,
    val hidKeyCode: Byte = 0,
    val isModifier: Boolean = false,
    val modifierMask: Byte = 0,
    val defaultXPercent: Float = xPercent,
    val defaultYPercent: Float = yPercent
)

data class ControllerPreset(
    val id: String,
    val name: String,
    val description: String,
    val hasAnalogStick: Boolean = true,
    var joystickXPercent: Float = 0.12f,
    var joystickYPercent: Float = 0.72f,
    val defaultJoystickX: Float = 0.12f,
    val defaultJoystickY: Float = 0.72f,
    val joystickKeyUp: Byte = HidConsts.KEY_UP_ARROW,
    val joystickKeyDown: Byte = HidConsts.KEY_DOWN_ARROW,
    val joystickKeyLeft: Byte = HidConsts.KEY_LEFT_ARROW,
    val joystickKeyRight: Byte = HidConsts.KEY_RIGHT_ARROW,
    val joystickLabel: String = "DIAL",
    val buttons: MutableList<ControllerButton>
)

object PresetRepository {

    /**
     * 1. Cricket 07 Day Out Preset
     * Optimized for EA Sports Cricket 07 batting shots & bowling directions
     */
    fun createCricket07Preset() = ControllerPreset(
        id = "cricket_07_day_out",
        name = "Cricket 07 Day Out",
        description = "Analog Dial + W, A, S, D, Shift, Alt, Ctrl, Space",
        hasAnalogStick = true,
        joystickXPercent = 0.12f,
        joystickYPercent = 0.72f,
        joystickKeyUp = HidConsts.KEY_UP_ARROW,
        joystickKeyDown = HidConsts.KEY_DOWN_ARROW,
        joystickKeyLeft = HidConsts.KEY_LEFT_ARROW,
        joystickKeyRight = HidConsts.KEY_RIGHT_ARROW,
        joystickLabel = "SHOT",
        buttons = mutableListOf(
            ControllerButton("ctrl", "Ctrl", "Block", 0.06f, 0.14f, 28f, isModifier = true, modifierMask = HidConsts.MOD_LEFT_CTRL),
            ControllerButton("space", "Space", "Cancel", 0.06f, 0.30f, 28f, HidConsts.KEY_SPACE),
            ControllerButton("esc", "Esc", "Pause", 0.46f, 0.08f, 22f, HidConsts.KEY_ESCAPE),
            ControllerButton("enter", "Enter", "OK", 0.54f, 0.08f, 22f, HidConsts.KEY_ENTER),
            ControllerButton("shift", "Shift", "Sprint", 0.88f, 0.14f, 36f, isModifier = true, modifierMask = HidConsts.MOD_LEFT_SHIFT),
            ControllerButton("alt", "Alt", "Loft", 0.88f, 0.32f, 28f, isModifier = true, modifierMask = HidConsts.MOD_LEFT_ALT),
            ControllerButton("btn_w", "W", "Front", 0.83f, 0.58f, 30f, HidConsts.KEY_W),
            ControllerButton("btn_a", "A", "Advance", 0.73f, 0.72f, 30f, HidConsts.KEY_A),
            ControllerButton("btn_d", "D", "Defend", 0.93f, 0.72f, 30f, HidConsts.KEY_D),
            ControllerButton("btn_s", "S", "Back", 0.83f, 0.86f, 30f, HidConsts.KEY_S)
        )
    )

    /**
     * 2. Standard Gamepad / Console Layout (Xbox / PlayStation style)
     */
    fun createStandardGamepadPreset() = ControllerPreset(
        id = "standard_gamepad",
        name = "Standard Gamepad (Xbox)",
        description = "Analog D-Pad + A, B, X, Y + Bumpers & Triggers",
        hasAnalogStick = true,
        joystickXPercent = 0.14f,
        joystickYPercent = 0.70f,
        joystickKeyUp = HidConsts.KEY_UP_ARROW,
        joystickKeyDown = HidConsts.KEY_DOWN_ARROW,
        joystickKeyLeft = HidConsts.KEY_LEFT_ARROW,
        joystickKeyRight = HidConsts.KEY_RIGHT_ARROW,
        joystickLabel = "D-PAD",
        buttons = mutableListOf(
            // Top Bumpers & Triggers
            ControllerButton("btn_lt", "LT", "", 0.08f, 0.14f, 26f, isModifier = true, modifierMask = HidConsts.MOD_LEFT_SHIFT),
            ControllerButton("btn_lb", "LB", "", 0.22f, 0.14f, 26f, HidConsts.KEY_Q),
            ControllerButton("btn_rb", "RB", "", 0.78f, 0.14f, 26f, HidConsts.KEY_E),
            ControllerButton("btn_rt", "RT", "", 0.92f, 0.14f, 26f, isModifier = true, modifierMask = HidConsts.MOD_LEFT_CTRL),

            // Start & Select
            ControllerButton("btn_select", "SELECT", "", 0.44f, 0.08f, 22f, HidConsts.KEY_TAB),
            ControllerButton("btn_start", "START", "", 0.56f, 0.08f, 22f, HidConsts.KEY_ESCAPE),

            // Action Diamond (A, B, X, Y)
            ControllerButton("btn_y", "Y", "", 0.84f, 0.52f, 32f, HidConsts.KEY_Y),
            ControllerButton("btn_x", "X", "", 0.74f, 0.68f, 32f, HidConsts.KEY_X),
            ControllerButton("btn_b", "B", "", 0.94f, 0.68f, 32f, HidConsts.KEY_B),
            ControllerButton("btn_a_pad", "A", "", 0.84f, 0.84f, 32f, HidConsts.KEY_SPACE)
        )
    )

    /**
     * 3. WASD Action Controller (FPS / Adventure PC Games)
     * Joystick sends W, A, S, D directly!
     */
    fun createFpsWasdPreset() = ControllerPreset(
        id = "fps_wasd",
        name = "WASD Action Controller",
        description = "WASD Movement Joystick + Jump, Sprint, Crouch, E, R, F",
        hasAnalogStick = true,
        joystickXPercent = 0.14f,
        joystickYPercent = 0.68f,
        joystickKeyUp = HidConsts.KEY_W,
        joystickKeyDown = HidConsts.KEY_S,
        joystickKeyLeft = HidConsts.KEY_A,
        joystickKeyRight = HidConsts.KEY_D,
        joystickLabel = "WASD",
        buttons = mutableListOf(
            ControllerButton("w_shift", "SPRINT", "", 0.08f, 0.16f, 30f, isModifier = true, modifierMask = HidConsts.MOD_LEFT_SHIFT),
            ControllerButton("w_ctrl", "CROUCH", "", 0.22f, 0.16f, 30f, isModifier = true, modifierMask = HidConsts.MOD_LEFT_CTRL),
            ControllerButton("w_esc", "ESC", "", 0.50f, 0.08f, 22f, HidConsts.KEY_ESCAPE),
            ControllerButton("w_r", "R (Reload)", "", 0.74f, 0.46f, 28f, HidConsts.KEY_R),
            ControllerButton("w_e", "E (Use)", "", 0.88f, 0.46f, 28f, HidConsts.KEY_E),
            ControllerButton("w_space", "JUMP (Space)", "", 0.80f, 0.72f, 38f, HidConsts.KEY_SPACE),
            ControllerButton("w_f", "F (Action)", "", 0.93f, 0.72f, 28f, HidConsts.KEY_F)
        )
    )

    /**
     * 4. Racing Game Controller Preset
     */
    fun createRacingPreset() = ControllerPreset(
        id = "racing_gamepad",
        name = "Racing Controller",
        description = "Steering Stick + Gas, Brake, Handbrake, Nitro, Cam",
        hasAnalogStick = true,
        joystickXPercent = 0.14f,
        joystickYPercent = 0.68f,
        joystickKeyUp = HidConsts.KEY_UP_ARROW,
        joystickKeyDown = HidConsts.KEY_DOWN_ARROW,
        joystickKeyLeft = HidConsts.KEY_LEFT_ARROW,
        joystickKeyRight = HidConsts.KEY_RIGHT_ARROW,
        joystickLabel = "STEER",
        buttons = mutableListOf(
            ControllerButton("nitro", "NITRO", "", 0.10f, 0.16f, 30f, isModifier = true, modifierMask = HidConsts.MOD_LEFT_SHIFT),
            ControllerButton("camera", "CAM", "", 0.24f, 0.16f, 26f, HidConsts.KEY_C),
            ControllerButton("pause", "PAUSE", "", 0.50f, 0.08f, 22f, HidConsts.KEY_ESCAPE),
            ControllerButton("gas", "GAS (▲)", "", 0.88f, 0.48f, 40f, HidConsts.KEY_UP_ARROW),
            ControllerButton("brake", "BRAKE (▼)", "", 0.74f, 0.76f, 36f, HidConsts.KEY_DOWN_ARROW),
            ControllerButton("handbrake", "E-BRAKE", "", 0.90f, 0.80f, 32f, HidConsts.KEY_SPACE)
        )
    )

    /**
     * 5. Classic Keyboard Mode (Arrow Pad + Action Keys)
     */
    fun createKeyboardPreset() = ControllerPreset(
        id = "keyboard_pad",
        name = "Keyboard (Arrows & Actions)",
        description = "Arrow Keys Dial + Space, Enter, Esc, 1, 2, 3, Tab",
        hasAnalogStick = true,
        joystickXPercent = 0.14f,
        joystickYPercent = 0.70f,
        joystickKeyUp = HidConsts.KEY_UP_ARROW,
        joystickKeyDown = HidConsts.KEY_DOWN_ARROW,
        joystickKeyLeft = HidConsts.KEY_LEFT_ARROW,
        joystickKeyRight = HidConsts.KEY_RIGHT_ARROW,
        joystickLabel = "ARROWS",
        buttons = mutableListOf(
            ControllerButton("kb_ctrl", "Ctrl", "", 0.08f, 0.16f, 26f, isModifier = true, modifierMask = HidConsts.MOD_LEFT_CTRL),
            ControllerButton("kb_shift", "Shift", "", 0.22f, 0.16f, 26f, isModifier = true, modifierMask = HidConsts.MOD_LEFT_SHIFT),
            ControllerButton("kb_esc", "Esc", "", 0.44f, 0.08f, 22f, HidConsts.KEY_ESCAPE),
            ControllerButton("kb_enter", "Enter", "", 0.56f, 0.08f, 22f, HidConsts.KEY_ENTER),
            ControllerButton("kb_1", "1", "", 0.74f, 0.44f, 28f, HidConsts.KEY_1),
            ControllerButton("kb_2", "2", "", 0.84f, 0.44f, 28f, HidConsts.KEY_2),
            ControllerButton("kb_3", "3", "", 0.94f, 0.44f, 28f, HidConsts.KEY_3),
            ControllerButton("kb_tab", "Tab", "", 0.74f, 0.66f, 28f, HidConsts.KEY_TAB),
            ControllerButton("kb_space", "Space", "", 0.88f, 0.66f, 36f, HidConsts.KEY_SPACE),
            ControllerButton("kb_back", "Back", "", 0.84f, 0.86f, 28f, HidConsts.KEY_BACKSPACE)
        )
    )

    val PRESETS = listOf(
        createCricket07Preset(),
        createStandardGamepadPreset(),
        createFpsWasdPreset(),
        createRacingPreset(),
        createKeyboardPreset()
    )
}
