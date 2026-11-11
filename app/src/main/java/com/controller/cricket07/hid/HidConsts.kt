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
    const val MOD_LEFT_GUI: Byte    = 0x08.toByte()
    const val MOD_RIGHT_CTRL: Byte  = 0x10.toByte()
    const val MOD_RIGHT_SHIFT: Byte = 0x20.toByte()
    const val MOD_RIGHT_ALT: Byte   = 0x40.toByte()
    const val MOD_RIGHT_GUI: Byte   = 0x80.toByte()

    // Standard Non-Modifier HID Keycodes
    const val KEY_A: Byte = 0x04.toByte()
    const val KEY_B: Byte = 0x05.toByte()
    const val KEY_C: Byte = 0x06.toByte()
    const val KEY_D: Byte = 0x07.toByte()
    const val KEY_E: Byte = 0x08.toByte()
    const val KEY_F: Byte = 0x09.toByte()
    const val KEY_Q: Byte = 0x14.toByte()
    const val KEY_R: Byte = 0x15.toByte()
    const val KEY_S: Byte = 0x16.toByte()
    const val KEY_T: Byte = 0x17.toByte()
    const val KEY_W: Byte = 0x1A.toByte()
    const val KEY_X: Byte = 0x1B.toByte()
    const val KEY_Y: Byte = 0x1C.toByte()
    const val KEY_Z: Byte = 0x1D.toByte()

    const val KEY_1: Byte = 0x1E.toByte()
    const val KEY_2: Byte = 0x1F.toByte()
    const val KEY_3: Byte = 0x20.toByte()
    const val KEY_4: Byte = 0x21.toByte()

    const val KEY_ENTER: Byte     = 0x28.toByte()
    const val KEY_ESCAPE: Byte    = 0x29.toByte()
    const val KEY_BACKSPACE: Byte = 0x2A.toByte()
    const val KEY_TAB: Byte       = 0x2B.toByte()
    const val KEY_SPACE: Byte     = 0x2C.toByte()

    // Arrow keys
    const val KEY_RIGHT_ARROW: Byte = 0x4F.toByte()
    const val KEY_LEFT_ARROW: Byte  = 0x50.toByte()
    const val KEY_DOWN_ARROW: Byte  = 0x51.toByte()
    const val KEY_UP_ARROW: Byte    = 0x52.toByte()

    const val MAX_KEYS_PER_REPORT = 6
}
