package com.controller.cricket07.model

import com.controller.cricket07.hid.HidConsts

data class ControllerButton(
    val id: String,
    val label: String,
    val subLabel: String = "",
    var xPercent: Float,
    var yPercent: Float,
    var radiusDp: Float,
    val hidKeyCode: Byte = 0,
    val isModifier: Boolean = false,
    val modifierMask: Byte = 0,
    val gamepadButtonIndex: Int = 0, // 1 to 16 for Gamepad DirectInput
    val extraGamepadButtons: List<Int> = emptyList(),
    val defaultXPercent: Float = xPercent,
    val defaultYPercent: Float = yPercent,
    val defaultRadiusDp: Float = radiusDp
) {
    val allGamepadButtonIndices: List<Int>
        get() {
            val list = mutableListOf<Int>()
            if (gamepadButtonIndex in 1..16) list.add(gamepadButtonIndex)
            for (idx in extraGamepadButtons) {
                if (idx in 1..16 && !list.contains(idx)) list.add(idx)
            }
            return list
        }
}

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
     * Fully optimized for EA Sports Cricket 07.
     * All top buttons (Esc, Enter, Ctrl, Shift) sit safely at or below y = 0.22f below top HUD.
     */
    fun createCricket07Preset() = ControllerPreset(
        id = "cricket_07_day_out",
        name = "Cricket 07 Controller",
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
            // Left Side Controls (Ctrl at y=0.22f level with Esc/Enter; Space below it at y=0.38f)
            ControllerButton("ctrl", "Ctrl", "Block", 0.06f, 0.22f, 28f, isModifier = true, modifierMask = HidConsts.MOD_LEFT_CTRL, gamepadButtonIndex = 7),
            ControllerButton("space", "Space", "Cancel", 0.06f, 0.38f, 28f, hidKeyCode = HidConsts.KEY_SPACE, gamepadButtonIndex = 1),

            // Center Menu Buttons (Shifted DOWN to y=0.22f to avoid overlapping top status text)
            ControllerButton("esc", "Esc", "Pause", 0.44f, 0.22f, 24f, hidKeyCode = HidConsts.KEY_ESCAPE, gamepadButtonIndex = 9),
            ControllerButton("enter", "Enter", "OK", 0.56f, 0.22f, 24f, hidKeyCode = HidConsts.KEY_ENTER, gamepadButtonIndex = 10),

            // Right Side Triggers / Modifiers (Shift at y=0.22f level with Esc/Enter; Alt below it at y=0.38f)
            ControllerButton("shift", "Shift", "Loft (Six)", 0.88f, 0.22f, 36f, isModifier = true, modifierMask = HidConsts.MOD_LEFT_SHIFT, gamepadButtonIndex = 6, extraGamepadButtons = listOf(5)),
            ControllerButton("alt", "Alt", "Advance", 0.88f, 0.38f, 28f, isModifier = true, modifierMask = HidConsts.MOD_LEFT_ALT, gamepadButtonIndex = 5),

            // Right Thumb Action Diamond (W, A, S, D - 100% EA Sports Cricket 07 DirectInput & Keyboard parity)
            ControllerButton("btn_w", "W", "Back / Dive", 0.83f, 0.58f, 30f, hidKeyCode = HidConsts.KEY_W, gamepadButtonIndex = 4),
            ControllerButton("btn_a", "A", "Cancel", 0.73f, 0.72f, 30f, hidKeyCode = HidConsts.KEY_A, gamepadButtonIndex = 1),
            ControllerButton("btn_d", "D", "Run", 0.93f, 0.72f, 30f, hidKeyCode = HidConsts.KEY_D, gamepadButtonIndex = 3, extraGamepadButtons = listOf(8)),
            ControllerButton("btn_s", "S", "Front / Shot", 0.83f, 0.86f, 30f, hidKeyCode = HidConsts.KEY_S, gamepadButtonIndex = 2)
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
            // Top Bumpers & Triggers (at y=0.22f level with Select/Start)
            ControllerButton("btn_lt", "LT", "", 0.08f, 0.22f, 26f, isModifier = true, modifierMask = HidConsts.MOD_LEFT_SHIFT, gamepadButtonIndex = 7),
            ControllerButton("btn_lb", "LB", "", 0.22f, 0.22f, 26f, HidConsts.KEY_Q, gamepadButtonIndex = 5),
            ControllerButton("btn_rb", "RB", "", 0.78f, 0.22f, 26f, HidConsts.KEY_E, gamepadButtonIndex = 6),
            ControllerButton("btn_rt", "RT", "", 0.92f, 0.22f, 26f, isModifier = true, modifierMask = HidConsts.MOD_LEFT_CTRL, gamepadButtonIndex = 8),

            // Center Menu Buttons (at y=0.22f)
            ControllerButton("btn_select", "SELECT", "", 0.44f, 0.22f, 24f, HidConsts.KEY_TAB, gamepadButtonIndex = 9),
            ControllerButton("btn_start", "START", "", 0.56f, 0.22f, 24f, HidConsts.KEY_ESCAPE, gamepadButtonIndex = 10),

            // Action Diamond (A, B, X, Y)
            ControllerButton("btn_y", "Y", "", 0.84f, 0.52f, 32f, HidConsts.KEY_Y, gamepadButtonIndex = 4),
            ControllerButton("btn_x", "X", "", 0.74f, 0.68f, 32f, HidConsts.KEY_X, gamepadButtonIndex = 3),
            ControllerButton("btn_b", "B", "", 0.94f, 0.68f, 32f, HidConsts.KEY_B, gamepadButtonIndex = 2),
            ControllerButton("btn_a_pad", "A", "", 0.84f, 0.84f, 32f, HidConsts.KEY_SPACE, gamepadButtonIndex = 1)
        )
    )

    /**
     * 3. WASD Action Controller (FPS / Adventure PC Games)
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
            ControllerButton("w_shift", "SPRINT", "", 0.08f, 0.22f, 30f, isModifier = true, modifierMask = HidConsts.MOD_LEFT_SHIFT, gamepadButtonIndex = 6),
            ControllerButton("w_ctrl", "CROUCH", "", 0.22f, 0.22f, 30f, isModifier = true, modifierMask = HidConsts.MOD_LEFT_CTRL, gamepadButtonIndex = 5),
            ControllerButton("w_esc", "ESC", "", 0.50f, 0.22f, 24f, HidConsts.KEY_ESCAPE, gamepadButtonIndex = 9),
            ControllerButton("w_r", "R (Reload)", "", 0.74f, 0.46f, 28f, HidConsts.KEY_R, gamepadButtonIndex = 3),
            ControllerButton("w_e", "E (Use)", "", 0.88f, 0.46f, 28f, HidConsts.KEY_E, gamepadButtonIndex = 4),
            ControllerButton("w_space", "JUMP (Space)", "", 0.80f, 0.72f, 38f, HidConsts.KEY_SPACE, gamepadButtonIndex = 1),
            ControllerButton("w_f", "F (Action)", "", 0.93f, 0.72f, 28f, HidConsts.KEY_F, gamepadButtonIndex = 2)
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
            ControllerButton("nitro", "NITRO", "", 0.10f, 0.22f, 30f, isModifier = true, modifierMask = HidConsts.MOD_LEFT_SHIFT, gamepadButtonIndex = 6),
            ControllerButton("camera", "CAM", "", 0.24f, 0.22f, 26f, HidConsts.KEY_C, gamepadButtonIndex = 4),
            ControllerButton("pause", "PAUSE", "", 0.50f, 0.22f, 24f, HidConsts.KEY_ESCAPE, gamepadButtonIndex = 9),
            ControllerButton("gas", "GAS (▲)", "", 0.88f, 0.48f, 40f, HidConsts.KEY_UP_ARROW, gamepadButtonIndex = 1),
            ControllerButton("brake", "BRAKE (▼)", "", 0.74f, 0.76f, 36f, HidConsts.KEY_DOWN_ARROW, gamepadButtonIndex = 2),
            ControllerButton("handbrake", "E-BRAKE", "", 0.90f, 0.80f, 32f, HidConsts.KEY_SPACE, gamepadButtonIndex = 7)
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
            ControllerButton("kb_ctrl", "Ctrl", "", 0.08f, 0.22f, 26f, isModifier = true, modifierMask = HidConsts.MOD_LEFT_CTRL, gamepadButtonIndex = 5),
            ControllerButton("kb_shift", "Shift", "", 0.22f, 0.22f, 26f, isModifier = true, modifierMask = HidConsts.MOD_LEFT_SHIFT, gamepadButtonIndex = 6),
            ControllerButton("kb_esc", "Esc", "", 0.44f, 0.22f, 24f, HidConsts.KEY_ESCAPE, gamepadButtonIndex = 9),
            ControllerButton("kb_enter", "Enter", "", 0.56f, 0.22f, 24f, HidConsts.KEY_ENTER, gamepadButtonIndex = 10),
            ControllerButton("kb_1", "1", "", 0.74f, 0.44f, 28f, HidConsts.KEY_1, gamepadButtonIndex = 1),
            ControllerButton("kb_2", "2", "", 0.84f, 0.44f, 28f, HidConsts.KEY_2, gamepadButtonIndex = 2),
            ControllerButton("kb_3", "3", "", 0.94f, 0.44f, 28f, HidConsts.KEY_3, gamepadButtonIndex = 3),
            ControllerButton("kb_tab", "Tab", "", 0.74f, 0.66f, 28f, HidConsts.KEY_TAB, gamepadButtonIndex = 4),
            ControllerButton("kb_space", "Space", "", 0.88f, 0.66f, 36f, HidConsts.KEY_SPACE, gamepadButtonIndex = 7),
            ControllerButton("kb_back", "Back", "", 0.84f, 0.86f, 28f, HidConsts.KEY_BACKSPACE, gamepadButtonIndex = 8)
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
