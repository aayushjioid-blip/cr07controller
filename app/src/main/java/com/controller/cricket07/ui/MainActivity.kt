package com.controller.cricket07.ui

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothProfile
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.controller.cricket07.R
import com.controller.cricket07.hid.HidConsts
import com.controller.cricket07.hid.HidDescriptor
import com.controller.cricket07.model.PresetRepository
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "Cricket07Main"
    }

    private lateinit var canvasView: ControllerCanvasView
    private lateinit var statusText: TextView

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        try {
            BluetoothAdapter.getDefaultAdapter()
        } catch (_: Exception) {
            null
        }
    }

    private var hidDevice: BluetoothHidDevice? = null
    private var connectedHostDevice: BluetoothDevice? = null
    private val executor = Executors.newSingleThreadExecutor()

    private val serviceListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                try {
                    hidDevice = proxy as? BluetoothHidDevice
                    if (hidDevice != null) {
                        registerHidApp()
                        updateStatus("HID Ready. Waiting for Windows / Mac host...", false)
                    } else {
                        updateStatus("Device does not support Bluetooth HID profile", false)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error registering HID proxy", e)
                    updateStatus("HID Init Error: ${e.message}", false)
                }
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                hidDevice = null
                connectedHostDevice = null
                updateStatus("Bluetooth HID disconnected", false)
            }
        }
    }

    private val hidCallback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            runOnUiThread {
                if (registered) {
                    updateStatus("Controller Registered. Pair with PC in Bluetooth Settings", false)
                } else {
                    updateStatus("HID registration pending...", false)
                }
            }
        }

        override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
            runOnUiThread {
                when (state) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        connectedHostDevice = device
                        val name = try { device?.name ?: "PC / Mac" } catch (_: Exception) { "PC / Mac" }
                        updateStatus("CONNECTED: $name", true)
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        connectedHostDevice = null
                        updateStatus("Disconnected. Ready to pair.", false)
                    }
                    BluetoothProfile.STATE_CONNECTING -> updateStatus("Connecting to host...", false)
                    BluetoothProfile.STATE_DISCONNECTING -> updateStatus("Disconnecting...", false)
                }
            }
        }

        override fun onGetReport(device: BluetoothDevice?, type: Byte, id: Byte, bufferSize: Int) {
            try {
                hidDevice?.reportError(device, BluetoothHidDevice.ERROR_RSP_SUCCESS)
            } catch (_: Exception) {}
        }

        override fun onSetReport(device: BluetoothDevice?, type: Byte, id: Byte, data: ByteArray?) {
            try {
                hidDevice?.reportError(device, BluetoothHidDevice.ERROR_RSP_SUCCESS)
            } catch (_: Exception) {}
        }

        override fun onSetProtocol(device: BluetoothDevice?, protocol: Byte) {
            try {
                hidDevice?.reportError(device, BluetoothHidDevice.ERROR_RSP_SUCCESS)
            } catch (_: Exception) {}
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        setupBluetoothHid()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowInsetsControllerCompat(window, window.decorView).let { controller ->
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } catch (_: Exception) {}

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContentView(R.layout.activity_main)

        canvasView = findViewById(R.id.controller_canvas)
        statusText = findViewById(R.id.tv_status)

        canvasView.setPreset(PresetRepository.CRICKET_07_PRESET)

        canvasView.onStateChanged = { modifierMask, keys ->
            sendKeyboardReport(modifierMask, keys)
        }

        statusText.setOnClickListener {
            requestDiscoverable()
        }

        checkPermissionsAndStart()
    }

    private fun checkPermissionsAndStart() {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
        } else {
            permissions.add(Manifest.permission.BLUETOOTH)
            permissions.add(Manifest.permission.BLUETOOTH_ADMIN)
        }

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isEmpty()) {
            setupBluetoothHid()
        } else {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    private fun setupBluetoothHid() {
        val adapter = bluetoothAdapter
        if (adapter == null) {
            updateStatus("Bluetooth hardware not available", false)
            return
        }

        if (!adapter.isEnabled) {
            updateStatus("Bluetooth is OFF. Turn ON in Android Settings", false)
            return
        }

        try {
            updateStatus("Initializing Bluetooth HID Profile...", false)
            val success = adapter.getProfileProxy(
                applicationContext,
                serviceListener,
                BluetoothProfile.HID_DEVICE
            )
            if (!success) {
                updateStatus("OnePlus 13R did not expose HID Device profile", false)
            }
        } catch (e: SecurityException) {
            updateStatus("Missing Bluetooth permissions. Please grant them.", false)
        } catch (e: Exception) {
            updateStatus("Bluetooth Init Error: ${e.message}", false)
        }
    }

    @SuppressLint("MissingPermission")
    private fun registerHidApp() {
        val hid = hidDevice ?: return
        try {
            val sdpSettings = BluetoothHidDeviceAppSdpSettings(
                "Cricket 07 Controller",
                "OnePlus 13R Wireless Gamepad",
                "OnePlus",
                BluetoothHidDevice.SUBCLASS1_COMBO,
                HidDescriptor.KEYBOARD_DESCRIPTOR
            )

            hid.registerApp(
                sdpSettings,
                null,
                null,
                executor,
                hidCallback
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error in registerApp", e)
            updateStatus("Error registering HID app: ${e.message}", false)
        }
    }

    @SuppressLint("MissingPermission")
    private fun sendKeyboardReport(modifierMask: Byte, pressedKeys: Set<Byte>) {
        val device = connectedHostDevice ?: return
        val hid = hidDevice ?: return

        try {
            val report = ByteArray(8)
            report[0] = modifierMask
            report[1] = 0x00

            var index = 2
            for (key in pressedKeys) {
                if (index >= 8) break
                report[index] = key
                index++
            }

            hid.sendReport(device, HidConsts.REPORT_ID_KEYBOARD.toInt(), report)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending HID report", e)
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestDiscoverable() {
        try {
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
            }
            startActivity(intent)
            Toast.makeText(this, "Phone is now discoverable for 5 minutes", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Please open Android Bluetooth Settings", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateStatus(message: String, isConnected: Boolean) {
        runOnUiThread {
            statusText.text = message
            statusText.setTextColor(if (isConnected) Color.parseColor("#4CAF50") else Color.parseColor("#AAAAAA"))
        }
    }

    override fun onDestroy() {
        try {
            hidDevice?.unregisterApp()
            if (hidDevice != null && bluetoothAdapter != null) {
                bluetoothAdapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, hidDevice)
            }
        } catch (_: Exception) {}
        super.onDestroy()
    }
}
