package com.controller.cricket07.hid

/**
 * Standard USB HID Keyboard Usage Tables (Hut1_12v2)
 */
object HidConsts {
    const val REPORT_ID_KEYBOARD: Byte = 1

    // Modifier bitmasks (Byte 0 in Keyboard Input Report)
    const val MOD_LEFT_CTRL: Byte   = 0x01.toByte()
    const val MOD_LEFT_SHIFT: Byte  = 0x02.toByte()
    const val MOD_LEFT_ALT: Byte    = 0x04.toByte()
    const val MOD_LEFT_GUI: Byte    = 0x08.toByte() // Windows Key / Mac Command
    const val MOD_RIGHT_CTRL: Byte  = 0x10.toByte()
    const val MOD_RIGHT_SHIFT: Byte = 0x20.toByte()
    const val MOD_RIGHT_ALT: Byte   = 0x40.toByte()
    const val MOD_RIGHT_GUI: Byte   = 0x80.toByte()

    // Standard Non-Modifier HID Keycodes
    const val KEY_A: Byte = 0x04.toByte()
    const val KEY_D: Byte = 0x07.toByte()
    const val KEY_S: Byte = 0x16.toByte()
    const val KEY_W: Byte = 0x1A.toByte()

    const val KEY_SPACE: Byte = 0x2C.toByte()
    const val KEY_ENTER: Byte = 0x28.toByte()
    const val KEY_ESCAPE: Byte = 0x29.toByte()

    // Arrow keys
    const val KEY_RIGHT_ARROW: Byte = 0x4F.toByte()
    const val KEY_LEFT_ARROW: Byte  = 0x50.toByte()
    const val KEY_DOWN_ARROW: Byte  = 0x51.toByte()
    const val KEY_UP_ARROW: Byte    = 0x52.toByte()

    const val MAX_KEYS_PER_REPORT = 6
}
