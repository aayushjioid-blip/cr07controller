package com.controller.cricket07.hid

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.util.Log
import java.util.concurrent.Executors

@SuppressLint("MissingPermission")
class BluetoothHidController(private val context: Context) {

    companion object {
        private const val TAG = "Cricket07Hid"
    }

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var hidDevice: BluetoothHidDevice? = null
    private var connectedHostDevice: BluetoothDevice? = null
    private val executor = Executors.newSingleThreadExecutor()

    var isProfileSupported: Boolean = false
        private set

    var isAppRegistered: Boolean = false
        private set

    var isHostConnected: Boolean = false
        private set

    var onStatusChangedListener: ((statusMessage: String, isConnected: Boolean) -> Unit)? = null

    private val serviceListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                Log.d(TAG, "Bluetooth HID Device profile proxy connected")
                hidDevice = proxy as BluetoothHidDevice
                isProfileSupported = true
                registerHidApp()
                notifyStatus("HID Service Ready. Waiting for host...", false)
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                Log.d(TAG, "Bluetooth HID Device profile disconnected")
                hidDevice = null
                isAppRegistered = false
                isHostConnected = false
                notifyStatus("HID Service Disconnected", false)
            }
        }
    }

    private val hidCallback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            isAppRegistered = registered
            Log.d(TAG, "onAppStatusChanged: registered = $registered")
            if (registered) {
                notifyStatus("Controller registered. Ready for pairing.", false)
            } else {
                notifyStatus("HID app registration failed", false)
            }
        }

        override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
            when (state) {
                BluetoothProfile.STATE_CONNECTED -> {
                    connectedHostDevice = device
                    isHostConnected = true
                    val deviceName = device?.name ?: "PC / Mac"
                    Log.d(TAG, "Connected to host: $deviceName [${device?.address}]")
                    notifyStatus("Connected to $deviceName", true)
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    connectedHostDevice = null
                    isHostConnected = false
                    Log.d(TAG, "Disconnected from host")
                    notifyStatus("Disconnected. Ready to pair.", false)
                }
                BluetoothProfile.STATE_CONNECTING -> {
                    notifyStatus("Connecting...", false)
                }
                BluetoothProfile.STATE_DISCONNECTING -> {
                    notifyStatus("Disconnecting...", false)
                }
            }
        }

        override fun onGetReport(device: BluetoothDevice?, type: Byte, id: Byte, bufferSize: Int) {
            hidDevice?.reportError(device, BluetoothHidDevice.ERROR_RSP_SUCCESS)
        }

        override fun onSetReport(device: BluetoothDevice?, type: Byte, id: Byte, data: ByteArray?) {
            hidDevice?.reportError(device, BluetoothHidDevice.ERROR_RSP_SUCCESS)
        }

        override fun onSetProtocol(device: BluetoothDevice?, protocol: Byte) {
            hidDevice?.reportError(device, BluetoothHidDevice.ERROR_RSP_SUCCESS)
        }
    }

    fun init() {
        if (bluetoothAdapter == null) {
            notifyStatus("Bluetooth not available on this device", false)
            return
        }

        val success = bluetoothAdapter.getProfileProxy(
            context.applicationContext,
            serviceListener,
            BluetoothProfile.HID_DEVICE
        )

        if (!success) {
            notifyStatus("OnePlus 13R did not grant HID profile access", false)
        }
    }

    private fun registerHidApp() {
        val sdpSettings = BluetoothHidDeviceAppSdpSettings(
            "Cricket 07 Controller",
            "OnePlus 13R Wireless HID Gamepad",
            "OnePlus",
            BluetoothHidDevice.SUBCLASS2_GAMEPAD,
            HidDescriptor.KEYBOARD_DESCRIPTOR
        )

        hidDevice?.registerApp(
            sdpSettings,
            null,
            null,
            executor,
            hidCallback
        )
    }

    fun sendKeyboardReport(modifierMask: Byte, pressedKeys: Set<Byte>): Boolean {
        val device = connectedHostDevice ?: return false
        val hid = hidDevice ?: return false

        val report = ByteArray(8)
        report[0] = modifierMask
        report[1] = 0x00

        var index = 2
        for (key in pressedKeys) {
            if (index >= 8) break
            report[index] = key
            index++
        }

        return hid.sendReport(device, HidConsts.REPORT_ID_KEYBOARD.toInt(), report)
    }

    fun releaseAllKeys(): Boolean {
        val device = connectedHostDevice ?: return false
        val hid = hidDevice ?: return false
        return hid.sendReport(device, HidConsts.REPORT_ID_KEYBOARD.toInt(), ByteArray(8))
    }

    private fun notifyStatus(message: String, isConnected: Boolean) {
        onStatusChangedListener?.invoke(message, isConnected)
    }

    fun close() {
        if (hidDevice != null && bluetoothAdapter != null) {
            bluetoothAdapter.closeProfileProxy(BluetoothProfile.HID_DEVICE, hidDevice)
        }
    }
}
