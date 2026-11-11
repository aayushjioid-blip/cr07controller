package com.controller.cricket07.ui

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.controller.cricket07.R
import com.controller.cricket07.hid.HidDescriptor
import com.controller.cricket07.model.PresetRepository
import com.controller.cricket07.util.DeviceCompatibility
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "Cricket07Main"
        private const val PREFS_SETTINGS = "controller_settings"
        private const val KEY_ACTIVE_PRESET = "active_preset_id"
        private const val KEY_IS_GAMEPAD_MODE = "is_gamepad_mode"
    }

    private lateinit var canvasView: ControllerCanvasView
    private lateinit var topBarNormal: RelativeLayout
    private lateinit var topBarEdit: RelativeLayout
    private lateinit var btnModeToggle: LinearLayout
    private lateinit var tvActiveModeTitle: TextView
    private lateinit var tvActivePresetName: TextView
    private lateinit var btnConnectionStatus: LinearLayout
    private lateinit var tvStatus: TextView
    private lateinit var tvDeviceInfo: TextView
    private lateinit var btnEditLayout: TextView
    private lateinit var btnResetLayout: TextView
    private lateinit var btnSaveLayout: TextView

    // Mode state: Default to true (Controller / Gamepad Mode for Multiplayer Player 2)
    private var isGamepadMode: Boolean = true

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
    private val mainHandler = Handler(Looper.getMainLooper())

    private var isRegistered = false

    private val serviceListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                try {
                    hidDevice = proxy as? BluetoothHidDevice
                    if (hidDevice != null) {
                        Log.d(TAG, "HID Device proxy obtained. Registering app...")
                        mainHandler.postDelayed({ registerHidApp() }, 300)
                    } else {
                        updateStatus("Device does not support Bluetooth HID Device profile", false)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error obtaining HID proxy", e)
                    updateStatus("HID Error: ${e.message}", false)
                }
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                hidDevice = null
                connectedHostDevice = null
                isRegistered = false
                updateStatus("Bluetooth HID disconnected", false)
            }
        }
    }

    private val hidCallback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            Log.d(TAG, "onAppStatusChanged: registered = $registered")
            isRegistered = registered

            runOnUiThread {
                if (registered) {
                    val bonded = getBondedDevices()
                    if (bonded.isNotEmpty()) {
                        val firstTarget = bonded.first()
                        val targetName = getDeviceName(firstTarget)
                        updateStatus("REGISTERED! Connecting to $targetName...", false)
                        connectToDevice(firstTarget)
                    } else {
                        updateStatus("REGISTERED! Pair in Windows Bluetooth Settings", true)
                        requestDiscoverable()
                    }
                } else {
                    updateStatus("HID Registration Pending. Tap to retry", false)
                }
            }
        }

        override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
            val devName = getDeviceName(device)
            Log.d(TAG, "onConnectionStateChanged: $devName -> state $state")

            runOnUiThread {
                when (state) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        connectedHostDevice = device
                        val modeLabel = if (isGamepadMode) "Gamepad P2" else "Keyboard"
                        updateStatus("CONNECTED: $devName ($modeLabel)", true)
                        Toast.makeText(this@MainActivity, "Connected to $devName as $modeLabel", Toast.LENGTH_SHORT).show()
                    }
                    BluetoothProfile.STATE_CONNECTING -> {
                        updateStatus("Connecting to $devName...", false)
                    }
                    BluetoothProfile.STATE_DISCONNECTING -> {
                        updateStatus("Disconnecting from $devName...", false)
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        connectedHostDevice = null
                        updateStatus("Disconnected. Tap to reconnect to $devName", false)
                    }
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
        topBarNormal = findViewById(R.id.top_bar_normal)
        topBarEdit = findViewById(R.id.top_bar_edit)
        btnModeToggle = findViewById(R.id.btn_mode_toggle)
        tvActiveModeTitle = findViewById(R.id.tv_active_mode_title)
        tvActivePresetName = findViewById(R.id.tv_active_preset_name)
        btnConnectionStatus = findViewById(R.id.btn_connection_status)
        tvStatus = findViewById(R.id.tv_status)
        tvDeviceInfo = findViewById(R.id.tv_device_info)
        btnEditLayout = findViewById(R.id.btn_edit_layout)
        btnResetLayout = findViewById(R.id.btn_reset_layout)
        btnSaveLayout = findViewById(R.id.btn_save_layout)

        // Ensure top HUD bars are always on top of canvas and touchable
        topBarNormal.bringToFront()
        topBarEdit.bringToFront()

        // Display detected mobile model and compatibility
        tvDeviceInfo.text = "📱 Detected: ${DeviceCompatibility.displayName} • P2 Ready"

        // Restore saved settings
        val settingsPrefs = getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE)
        isGamepadMode = settingsPrefs.getBoolean(KEY_IS_GAMEPAD_MODE, true)
        val savedPresetId = settingsPrefs.getString(KEY_ACTIVE_PRESET, PresetRepository.createCricket07Preset().id)
        val initialPreset = PresetRepository.PRESETS.find { it.id == savedPresetId } ?: PresetRepository.PRESETS.first()
        canvasView.setPreset(initialPreset)

        updateModeUI()

        // Bind HID Callbacks
        canvasView.onGamepadStateChanged = { leftStickX, leftStickY, hatSwitch, buttonsMask ->
            if (isGamepadMode) {
                sendGamepadReport(leftStickX, leftStickY, hatSwitch, buttonsMask)
            }
        }

        canvasView.onKeyboardStateChanged = { modifierMask, keys ->
            if (!isGamepadMode) {
                sendKeyboardReport(modifierMask, keys)
            }
        }

        // Mode Switcher / Preset dialog
        btnModeToggle.setOnClickListener {
            showModeSelectionDialog()
        }

        // Bluetooth connection dialog
        btnConnectionStatus.setOnClickListener {
            showDeviceConnectionDialog()
        }

        // Edit Layout
        btnEditLayout.setOnClickListener {
            enterEditMode()
        }

        btnSaveLayout.setOnClickListener {
            exitEditMode(save = true)
        }

        btnResetLayout.setOnClickListener {
            showResetLayoutDialog()
        }

        // Handle System Back Button
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (canvasView.isEditMode) {
                    exitEditMode(save = true)
                } else {
                    finish()
                }
            }
        })

        checkPermissionsAndStart()
    }

    private fun updateModeUI() {
        if (isGamepadMode) {
            tvActiveModeTitle.text = "🎮 CONTROLLER (P2)"
            tvActiveModeTitle.setTextColor(Color.parseColor("#00E5FF"))
            btnModeToggle.setBackgroundResource(R.drawable.bg_pill_cyan)
        } else {
            tvActiveModeTitle.text = "⌨️ KEYBOARD (Solo)"
            tvActiveModeTitle.setTextColor(Color.parseColor("#69F0AE"))
            btnModeToggle.setBackgroundResource(R.drawable.bg_pill_green)
        }
        tvActivePresetName.text = "${canvasView.activePreset.name} ▼"

        val settingsPrefs = getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE)
        settingsPrefs.edit().putBoolean(KEY_IS_GAMEPAD_MODE, isGamepadMode).apply()

        connectedHostDevice?.let { dev ->
            val devName = getDeviceName(dev)
            val modeLabel = if (isGamepadMode) "Gamepad P2" else "Keyboard"
            updateStatus("CONNECTED: $devName ($modeLabel)", true)
        }
    }

    private fun showModeSelectionDialog() {
        val options = arrayOf(
            "🎮 Game Controller Mode (Player 2 for PC Multiplayer - DirectInput)",
            "⌨️ Keyboard Mode (Solo Play - Standard Keystrokes)",
            "🎨 Switch Button Layout Preset (${canvasView.activePreset.name})"
        )

        val currentSelection = if (isGamepadMode) 0 else 1

        AlertDialog.Builder(this)
            .setTitle("Device Operation Mode")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        isGamepadMode = true
                        updateModeUI()
                        Toast.makeText(this, "Active Mode: Game Controller (Player 2)", Toast.LENGTH_SHORT).show()
                    }
                    1 -> {
                        isGamepadMode = false
                        updateModeUI()
                        Toast.makeText(this, "Active Mode: Keyboard (Solo)", Toast.LENGTH_SHORT).show()
                    }
                    2 -> {
                        showPresetSelectionDialog()
                    }
                }
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showPresetSelectionDialog() {
        val presetNames = PresetRepository.PRESETS.map { "${it.name}\n${it.description}" }.toTypedArray()
        val currentIndex = PresetRepository.PRESETS.indexOfFirst { it.id == canvasView.activePreset.id }.coerceAtLeast(0)

        AlertDialog.Builder(this)
            .setTitle("Select Button Layout")
            .setSingleChoiceItems(presetNames, currentIndex) { dialog, which ->
                val selected = PresetRepository.PRESETS[which]
                canvasView.setPreset(selected)
                updateModeUI()

                val settingsPrefs = getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE)
                settingsPrefs.edit().putString(KEY_ACTIVE_PRESET, selected.id).apply()

                Toast.makeText(this, "Preset: ${selected.name}", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun enterEditMode() {
        canvasView.isEditMode = true
        topBarNormal.visibility = View.GONE
        topBarEdit.visibility = View.VISIBLE
        topBarEdit.bringToFront()
        Toast.makeText(this, "Edit Mode: Drag any button or dial to move", Toast.LENGTH_SHORT).show()
    }

    private fun exitEditMode(save: Boolean) {
        if (save) {
            canvasView.saveCustomPositions()
            Toast.makeText(this, "Layout Saved!", Toast.LENGTH_SHORT).show()
        }
        canvasView.isEditMode = false
        topBarEdit.visibility = View.GONE
        topBarNormal.visibility = View.VISIBLE
        topBarNormal.bringToFront()
    }

    private fun showResetLayoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Reset Layout")
            .setMessage("Reset all buttons and the analog dial to default positions for '${canvasView.activePreset.name}'?")
            .setPositiveButton("Reset") { _, _ ->
                canvasView.resetToDefaults()
                Toast.makeText(this, "Reset to defaults", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
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
            updateStatus("Bluetooth not available on this device", false)
            return
        }

        if (!adapter.isEnabled) {
            updateStatus("Bluetooth is OFF. Turn ON in Android Settings", false)
            return
        }

        try {
            updateStatus("Initializing Bluetooth Gamepad...", false)
            val success = adapter.getProfileProxy(
                applicationContext,
                serviceListener,
                BluetoothProfile.HID_DEVICE
            )
            if (!success) {
                updateStatus("${DeviceCompatibility.displayName} did not expose HID profile", false)
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
        if (isRegistered) return

        try {
            // Automatically detect phone name and brand for Windows discovery
            val sdpSettings = BluetoothHidDeviceAppSdpSettings(
                "Cricket 07 Controller",
                "${DeviceCompatibility.displayName} Controller",
                DeviceCompatibility.manufacturer,
                BluetoothHidDevice.SUBCLASS2_GAMEPAD,
                HidDescriptor.COMPOSITE_DESCRIPTOR
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
            updateStatus("Register error: ${e.message}", false)
        }
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(device: BluetoothDevice) {
        val hid = hidDevice
        if (hid == null) {
            Toast.makeText(this, "HID Service not ready yet", Toast.LENGTH_SHORT).show()
            return
        }

        val devName = getDeviceName(device)
        updateStatus("Connecting to $devName...", false)
        try {
            val success = hid.connect(device)
            Log.d(TAG, "hid.connect($devName) returned $success")
            if (!success) {
                Toast.makeText(this, "Connection request queued for $devName", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to $devName", e)
            updateStatus("Connection error: ${e.message}", false)
        }
    }

    @SuppressLint("MissingPermission")
    private fun showDeviceConnectionDialog() {
        val bonded = getBondedDevices()
        val deviceNames = bonded.map { getDeviceName(it) }.toMutableList()
        deviceNames.add("➕ Make Phone Discoverable (Pair New PC)")

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Bluetooth HID Connection\n(${DeviceCompatibility.fullDeviceInfo})")
        builder.setItems(deviceNames.toTypedArray()) { _, which ->
            if (which < bonded.size) {
                val selectedDevice = bonded[which]
                connectToDevice(selectedDevice)
            } else {
                requestDiscoverable()
                registerHidApp()
            }
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    @SuppressLint("MissingPermission")
    private fun getBondedDevices(): List<BluetoothDevice> {
        return try {
            bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    @SuppressLint("MissingPermission")
    private fun getDeviceName(device: BluetoothDevice?): String {
        return try {
            device?.name ?: device?.address ?: "PC / Laptop"
        } catch (_: Exception) {
            "PC / Laptop"
        }
    }

    /**
     * Sends Gamepad Report (Report ID 2) for DirectInput / XInput:
     * - Left Stick X & Y axes (0 to 255, center 127)
     * - POV Hat Switch (0 to 8)
     * - Buttons 1 to 16
     * Recognized by Windows Game Controllers and EA Sports Cricket 07 as Controller 1 / Gamepad!
     */
    @SuppressLint("MissingPermission")
    private fun sendGamepadReport(leftStickX: Byte, leftStickY: Byte, hatSwitch: Byte, buttonsMask: Short) {
        val device = connectedHostDevice ?: return
        val hid = hidDevice ?: return

        try {
            val report = ByteArray(7)
            report[0] = (buttonsMask.toInt() and 0xFF).toByte()
            report[1] = ((buttonsMask.toInt() shr 8) and 0xFF).toByte()
            report[2] = (hatSwitch.toInt() and 0x0F).toByte()
            report[3] = leftStickX
            report[4] = leftStickY
            report[5] = 127.toByte() // Right stick Z neutral
            report[6] = 127.toByte() // Right stick Rz neutral

            hid.sendReport(device, HidDescriptor.REPORT_ID_GAMEPAD.toInt(), report)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending Gamepad report", e)
        }
    }

    /**
     * Sends standard 8-byte USB HID boot keyboard report (Report ID 1)
     */
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

            hid.sendReport(device, HidDescriptor.REPORT_ID_KEYBOARD.toInt(), report)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending Keyboard report", e)
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestDiscoverable() {
        try {
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
            }
            startActivity(intent)
            Toast.makeText(this, "Phone is discoverable for 5 minutes", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {}
    }

    private fun updateStatus(message: String, isSuccess: Boolean) {
        runOnUiThread {
            tvStatus.text = message
            tvStatus.setTextColor(if (isSuccess) Color.parseColor("#4CAF50") else Color.parseColor("#E0E0E0"))
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
