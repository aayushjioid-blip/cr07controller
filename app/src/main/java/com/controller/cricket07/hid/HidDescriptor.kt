package com.controller.cricket07.hid

/**
 * Standard USB HID Descriptors for:
 * 1. Boot Keyboard (Report ID 1)
 * 2. DirectInput / XInput Gamepad (Report ID 2)
 *
 * Plug & Play on Windows 10/11 and macOS:
 * Allows the mobile device to act as an independent Game Controller (Player 2)
 * alongside the PC physical keyboard (Player 1) for EA Sports Cricket 07 and PC gaming.
 */
object HidDescriptor {
    const val REPORT_ID_KEYBOARD: Byte = 1
    const val REPORT_ID_GAMEPAD: Byte = 2

    val COMPOSITE_DESCRIPTOR = byteArrayOf(
        // -------------------------------------------------------------
        // COLLECTION 1: Keyboard (Report ID = 1)
        // -------------------------------------------------------------
        0x05.toByte(), 0x01.toByte(), // USAGE_PAGE (Generic Desktop)
        0x09.toByte(), 0x06.toByte(), // USAGE (Keyboard)
        0xA1.toByte(), 0x01.toByte(), // COLLECTION (Application)
        0x85.toByte(), 0x01.toByte(), //   REPORT_ID (1)
        0x05.toByte(), 0x07.toByte(), //   USAGE_PAGE (Keyboard/Keypad)
        0x19.toByte(), 0xE0.toByte(), //   USAGE_MINIMUM (Keyboard LeftControl)
        0x29.toByte(), 0xE7.toByte(), //   USAGE_MAXIMUM (Keyboard Right GUI)
        0x15.toByte(), 0x00.toByte(), //   LOGICAL_MINIMUM (0)
        0x25.toByte(), 0x01.toByte(), //   LOGICAL_MAXIMUM (1)
        0x75.toByte(), 0x01.toByte(), //   REPORT_SIZE (1)
        0x95.toByte(), 0x08.toByte(), //   REPORT_COUNT (8)
        0x81.toByte(), 0x02.toByte(), //   INPUT (Data,Var,Abs) -> Modifier Byte
        0x95.toByte(), 0x01.toByte(), //   REPORT_COUNT (1)
        0x75.toByte(), 0x08.toByte(), //   REPORT_SIZE (8)
        0x81.toByte(), 0x01.toByte(), //   INPUT (Cnst,Var,Abs) -> Reserved Byte
        0x95.toByte(), 0x05.toByte(), //   REPORT_COUNT (5)
        0x75.toByte(), 0x01.toByte(), //   REPORT_SIZE (1)
        0x05.toByte(), 0x08.toByte(), //   USAGE_PAGE (LEDs)
        0x19.toByte(), 0x01.toByte(), //   USAGE_MINIMUM (Num Lock)
        0x29.toByte(), 0x05.toByte(), //   USAGE_MAXIMUM (Kana)
        0x91.toByte(), 0x02.toByte(), //   OUTPUT (Data,Var,Abs) -> LED Report
        0x95.toByte(), 0x01.toByte(), //   REPORT_COUNT (1)
        0x75.toByte(), 0x03.toByte(), //   REPORT_SIZE (3)
        0x91.toByte(), 0x03.toByte(), //   OUTPUT (Cnst,Var,Abs) -> LED Padding
        0x95.toByte(), 0x06.toByte(), //   REPORT_COUNT (6)
        0x75.toByte(), 0x08.toByte(), //   REPORT_SIZE (8)
        0x15.toByte(), 0x00.toByte(), //   LOGICAL_MINIMUM (0)
        0x25.toByte(), 0x65.toByte(), //   LOGICAL_MAXIMUM (101)
        0x05.toByte(), 0x07.toByte(), //   USAGE_PAGE (Keyboard/Keypad)
        0x19.toByte(), 0x00.toByte(), //   USAGE_MINIMUM (Reserved)
        0x29.toByte(), 0x65.toByte(), //   USAGE_MAXIMUM (Keyboard Application)
        0x81.toByte(), 0x00.toByte(), //   INPUT (Data,Ary,Abs) -> 6-Key Array
        0xC0.toByte(),                // END_COLLECTION

        // -------------------------------------------------------------
        // COLLECTION 2: Gamepad (Report ID = 2)
        // -------------------------------------------------------------
        0x05.toByte(), 0x01.toByte(), // USAGE_PAGE (Generic Desktop)
        0x09.toByte(), 0x05.toByte(), // USAGE (Gamepad)
        0xA1.toByte(), 0x01.toByte(), // COLLECTION (Application)
        0x85.toByte(), 0x02.toByte(), //   REPORT_ID (2)

        // 16 Buttons (Button 1 through Button 16)
        0x05.toByte(), 0x09.toByte(), //   USAGE_PAGE (Button)
        0x19.toByte(), 0x01.toByte(), //   USAGE_MINIMUM (Button 1)
        0x29.toByte(), 0x10.toByte(), //   USAGE_MAXIMUM (Button 16)
        0x15.toByte(), 0x00.toByte(), //   LOGICAL_MINIMUM (0)
        0x25.toByte(), 0x01.toByte(), //   LOGICAL_MAXIMUM (1)
        0x75.toByte(), 0x01.toByte(), //   REPORT_SIZE (1)
        0x95.toByte(), 0x10.toByte(), //   REPORT_COUNT (16)
        0x81.toByte(), 0x02.toByte(), //   INPUT (Data,Var,Abs) -> 2 Bytes Buttons

        // POV Hat Switch (4 bits: 0=Null, 1=Up, 2=Up-Right, 3=Right, 4=Down-Right, 5=Down, 6=Down-Left, 7=Left, 8=Up-Left)
        0x05.toByte(), 0x01.toByte(), //   USAGE_PAGE (Generic Desktop)
        0x09.toByte(), 0x39.toByte(), //   USAGE (Hat switch)
        0x15.toByte(), 0x01.toByte(), //   LOGICAL_MINIMUM (1)
        0x25.toByte(), 0x08.toByte(), //   LOGICAL_MAXIMUM (8)
        0x35.toByte(), 0x00.toByte(), //   PHYSICAL_MINIMUM (0)
        0x46.toByte(), 0x3B.toByte(), 0x01.toByte(), // PHYSICAL_MAXIMUM (315)
        0x65.toByte(), 0x14.toByte(), //   UNIT (Degrees)
        0x75.toByte(), 0x04.toByte(), //   REPORT_SIZE (4)
        0x95.toByte(), 0x01.toByte(), //   REPORT_COUNT (1)
        0x81.toByte(), 0x02.toByte(), //   INPUT (Data,Var,Abs)

        // 4 bits padding for byte alignment
        0x75.toByte(), 0x04.toByte(), //   REPORT_SIZE (4)
        0x95.toByte(), 0x01.toByte(), //   REPORT_COUNT (1)
        0x81.toByte(), 0x01.toByte(), //   INPUT (Cnst,Ary,Abs) -> Padding

        // 4 Analog Axes: X, Y, Z, Rz (8-bit each, 0 to 255, center 127)
        0x05.toByte(), 0x01.toByte(), //   USAGE_PAGE (Generic Desktop)
        0x09.toByte(), 0x30.toByte(), //   USAGE (X - Left Stick X)
        0x09.toByte(), 0x31.toByte(), //   USAGE (Y - Left Stick Y)
        0x09.toByte(), 0x32.toByte(), //   USAGE (Z - Right Stick X)
        0x09.toByte(), 0x35.toByte(), //   USAGE (Rz - Right Stick Y)
        0x15.toByte(), 0x00.toByte(), //   LOGICAL_MINIMUM (0)
        0x26.toByte(), 0xFF.toByte(), 0x00.toByte(), // LOGICAL_MAXIMUM (255)
        0x75.toByte(), 0x08.toByte(), //   REPORT_SIZE (8)
        0x95.toByte(), 0x04.toByte(), //   REPORT_COUNT (4)
        0x81.toByte(), 0x02.toByte(), //   INPUT (Data,Var,Abs) -> 4 Bytes Axes

        0xC0.toByte()                 // END_COLLECTION
    )

    val KEYBOARD_DESCRIPTOR = COMPOSITE_DESCRIPTOR
}
