package com.controller.cricket07.ui

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
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
        private const val KEY_IS_MAC_HOST = "is_mac_host"
        private const val KEY_LAST_CONNECTED_DEVICE_MAC = "last_connected_device_mac"
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
    // Host Profile: false = Windows (Default, verified v2.3.0 parity), true = Mac (Collection 1 Gamepad for macOS GCController)
    private var isMacHost: Boolean = false

    private var isPendingReRegistration: Boolean = false

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
    private var connectingWatchdog: Runnable? = null

    private fun startConnectingWatchdog(targetDevice: BluetoothDevice) {
        cancelConnectingWatchdog()
        connectingWatchdog = Runnable {
            if (connectedHostDevice == null && hidDevice?.getConnectionState(targetDevice) != BluetoothProfile.STATE_CONNECTED) {
                val devName = getDeviceName(targetDevice)
                Log.w(TAG, "Connection to $devName timed out after 4 seconds. Resetting connecting state.")
                try {
                    hidDevice?.disconnect(targetDevice)
                } catch (_: Exception) {}
                val hostLabel = if (isMacHost) "Mac" else "Windows"
                updateStatus("Ready! Click 'Connect' in $hostLabel Bluetooth Settings", false)
                Toast.makeText(
                    this@MainActivity,
                    "To connect: Click 'Connect' on your $hostLabel Bluetooth settings",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        mainHandler.postDelayed(connectingWatchdog!!, 4000)
    }

    private fun cancelConnectingWatchdog() {
        connectingWatchdog?.let {
            mainHandler.removeCallbacks(it)
            connectingWatchdog = null
        }
    }

    @SuppressLint("MissingPermission")
    private fun checkAndBindConnectedHost(): Boolean {
        val hid = hidDevice ?: return false

        // 1. Check getConnectedDevices()
        val connected = getConnectedHidDevices()
        if (connected.isNotEmpty()) {
            handleHostConnected(connected.first())
            return true
        }

        // 2. Check all bonded devices for STATE_CONNECTED on HID profile
        val bonded = getBondedDevices()
        for (dev in bonded) {
            try {
                if (hid.getConnectionState(dev) == BluetoothProfile.STATE_CONNECTED) {
                    handleHostConnected(dev)
                    return true
                }
            } catch (_: Exception) {}
        }

        return false
    }

    private val serviceListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                try {
                    hidDevice = proxy as? BluetoothHidDevice
                    if (hidDevice != null) {
                        Log.d(TAG, "HID Device proxy obtained. Registering app...")
                        mainHandler.postDelayed({ registerHidApp() }, 200)
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
                updateStatus("Bluetooth HID service disconnected", false)
            }
        }
    }

    private val hidCallback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            Log.d(TAG, "onAppStatusChanged: registered = $registered, plugged = ${pluggedDevice?.name ?: pluggedDevice?.address}")
            isRegistered = registered

            if (!registered && isPendingReRegistration) {
                isPendingReRegistration = false
                mainHandler.postDelayed({ registerHidApp() }, 300)
                return
            }

            runOnUiThread {
                if (registered) {
                    // Priority 1: Check if a host was already plugged when registration completed
                    if (pluggedDevice != null) {
                        handleHostConnected(pluggedDevice)
                        return@runOnUiThread
                    }

                    // Priority 2: Check getConnectedDevices() and bonded devices in STATE_CONNECTED
                    if (checkAndBindConnectedHost()) {
                        return@runOnUiThread
                    }

                    // Priority 3: Ready to connect!
                    // In Bluetooth HID architecture, Hosts (Windows PC & Mac) initiate connections to peripherals.
                    // DO NOT call hid.connect() automatically on startup, as unsolicited outgoing connections
                    // cause desktop hosts to reject or hang in STATE_CONNECTING.
                    val hostLabel = if (isMacHost) "Mac" else "Windows"
                    val bonded = getBondedDevices()
                    val computers = bonded.filter { dev ->
                        dev.bluetoothClass?.majorDeviceClass == BluetoothClass.Device.Major.COMPUTER
                    }
                    if (computers.isNotEmpty()) {
                        val hostNames = computers.map { getDeviceName(it) }.joinToString(", ")
                        updateStatus("Ready! Click 'Connect' on your $hostLabel ($hostNames)", false)
                    } else if (bonded.isNotEmpty()) {
                        val allNames = bonded.map { getDeviceName(it) }.joinToString(", ")
                        updateStatus("Ready! Click 'Connect' on your $hostLabel ($allNames)", false)
                    } else {
                        updateStatus("READY TO PAIR! Connect in $hostLabel Bluetooth Settings", false)
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
                        cancelConnectingWatchdog()
                        if (device != null) {
                            handleHostConnected(device)
                        }
                    }
                    BluetoothProfile.STATE_CONNECTING -> {
                        updateStatus("Connecting to $devName...", false)
                        if (device != null) {
                            startConnectingWatchdog(device)
                        }
                    }
                    BluetoothProfile.STATE_DISCONNECTING -> {
                        cancelConnectingWatchdog()
                        updateStatus("Disconnecting from $devName...", false)
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        cancelConnectingWatchdog()
                        if (connectedHostDevice?.address == device?.address) {
                            connectedHostDevice = null
                        }
                        val hostLabel = if (isMacHost) "Mac" else "Windows"
                        updateStatus("Disconnected. Connect in $hostLabel Bluetooth Settings", false)
                    }
                }
            }
        }

        override fun onGetReport(device: BluetoothDevice?, type: Byte, id: Byte, bufferSize: Int) {
            Log.d(TAG, "onGetReport: type=$type, id=$id, bufferSize=$bufferSize")
            try {
                val neutralGamepad = byteArrayOf(
                    128.toByte(), 128.toByte(), 128.toByte(), 128.toByte(),
                    0x08.toByte(), // Hat Switch: 8 = Released / Centered
                    0x00.toByte(), 0x00.toByte() // Buttons: 0
                )
                val neutralKeyboard = ByteArray(8)

                if (id == HidDescriptor.REPORT_ID_GAMEPAD) {
                    hidDevice?.replyReport(device, type, id, neutralGamepad)
                } else if (id == HidDescriptor.REPORT_ID_KEYBOARD) {
                    hidDevice?.replyReport(device, type, id, neutralKeyboard)
                } else if (id == 0.toByte()) {
                    // Host requested without report ID; return according to active mode
                    if (isGamepadMode) {
                        hidDevice?.replyReport(device, type, HidDescriptor.REPORT_ID_GAMEPAD, neutralGamepad)
                    } else {
                        hidDevice?.replyReport(device, type, HidDescriptor.REPORT_ID_KEYBOARD, neutralKeyboard)
                    }
                } else {
                    hidDevice?.reportError(device, BluetoothHidDevice.ERROR_RSP_SUCCESS)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in onGetReport", e)
            }
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

    private fun handleHostConnected(device: BluetoothDevice) {
        connectedHostDevice = device
        val devName = getDeviceName(device)
        val modeLabel = if (isGamepadMode) "P-2 Controller" else "P-1 Keyboard"
        val hostLabel = if (isMacHost) "🍎 Mac" else "🪟 Win"

        // Remember as last successfully connected host
        val settingsPrefs = getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE)
        settingsPrefs.edit().putString(KEY_LAST_CONNECTED_DEVICE_MAC, device.address).apply()

        updateStatus("CONNECTED: $devName ($modeLabel • $hostLabel)", true)
        Toast.makeText(this@MainActivity, "Connected to $devName as $modeLabel", Toast.LENGTH_SHORT).show()

        // Calibrate host by sending initial neutral state
        if (isGamepadMode) {
            mainHandler.postDelayed({
                sendGamepadReport(128.toByte(), 128.toByte(), 8.toByte(), 0)
            }, 100)
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

        topBarNormal.bringToFront()
        topBarEdit.bringToFront()

        tvDeviceInfo.text = "📱 ${DeviceCompatibility.displayName} • ${DeviceCompatibility.APP_VERSION} (Multiplayer)"

        // Restore saved settings
        val settingsPrefs = getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE)
        isGamepadMode = settingsPrefs.getBoolean(KEY_IS_GAMEPAD_MODE, true)
        isMacHost = settingsPrefs.getBoolean(KEY_IS_MAC_HOST, false)
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

        btnModeToggle.setOnClickListener {
            showModeSelectionDialog()
        }

        btnConnectionStatus.setOnClickListener {
            showDeviceConnectionDialog()
        }

        btnEditLayout.setOnClickListener {
            enterEditMode()
        }

        btnSaveLayout.setOnClickListener {
            exitEditMode(save = true)
        }

        btnResetLayout.setOnClickListener {
            showResetLayoutDialog()
        }

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

    override fun onResume() {
        super.onResume()
        try {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowInsetsControllerCompat(window, window.decorView).let { controller ->
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } catch (_: Exception) {}

        // Check active connection whenever app resumes into foreground
        if (hidDevice != null) {
            if (!isRegistered) {
                registerHidApp()
            } else {
                if (!checkAndBindConnectedHost()) {
                    if (connectedHostDevice != null) {
                        val state = hidDevice?.getConnectionState(connectedHostDevice)
                        if (state != BluetoothProfile.STATE_CONNECTED) {
                            connectedHostDevice = null
                            val hostLabel = if (isMacHost) "Mac" else "Windows"
                            updateStatus("Ready to connect. Connect in $hostLabel Bluetooth Settings", false)
                        }
                    }
                }
            }
        } else {
            setupBluetoothHid()
        }
    }

    private fun updateModeUI() {
        val hostTag = if (isMacHost) "🍎 Mac" else "🪟 Win"
        if (isGamepadMode) {
            tvActiveModeTitle.text = "🎮 P-2: CONTROLLER ($hostTag)"
            tvActiveModeTitle.setTextColor(Color.parseColor("#00E5FF"))
            btnModeToggle.setBackgroundResource(R.drawable.bg_pill_cyan)
        } else {
            tvActiveModeTitle.text = "⌨️ P-1: KEYBOARD ($hostTag)"
            tvActiveModeTitle.setTextColor(Color.parseColor("#69F0AE"))
            btnModeToggle.setBackgroundResource(R.drawable.bg_pill_green)
        }
        tvActivePresetName.text = "${canvasView.activePreset.name} ▼"

        val settingsPrefs = getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE)
        settingsPrefs.edit()
            .putBoolean(KEY_IS_GAMEPAD_MODE, isGamepadMode)
            .putBoolean(KEY_IS_MAC_HOST, isMacHost)
            .apply()

        connectedHostDevice?.let { dev ->
            val devName = getDeviceName(dev)
            val modeLabel = if (isGamepadMode) "P-2 Controller" else "P-1 Keyboard"
            updateStatus("CONNECTED: $devName ($modeLabel • $hostTag)", true)
        }
    }

    private fun showModeSelectionDialog() {
        val hostProfileTitle = if (isMacHost) {
            "🖥️ Host Profile: 🍎 Mac (Collection 1 Gamepad - Tap to switch to 🪟 Windows)"
        } else {
            "🖥️ Host Profile: 🪟 Windows (DirectInput Default - Tap to switch to 🍎 Mac)"
        }

        val options = arrayOf(
            "🎮 P-2: Game Controller (Player 2 for PC Multiplayer - DirectInput)",
            "⌨️ P-1: PC Keyboard (Solo Play - Direct Keystrokes)",
            hostProfileTitle,
            "🎨 Switch Button Layout Preset (${canvasView.activePreset.name})"
        )

        AlertDialog.Builder(this)
            .setTitle("Select Player & Host Profile")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        isGamepadMode = true
                        updateModeUI()
                        sendGamepadReport(128.toByte(), 128.toByte(), 8.toByte(), 0)
                        Toast.makeText(this, "Active: P-2 Game Controller (Multiplayer)", Toast.LENGTH_SHORT).show()
                    }
                    1 -> {
                        isGamepadMode = false
                        updateModeUI()
                        sendKeyboardReport(0, emptySet())
                        Toast.makeText(this, "Active: P-1 PC Keyboard (Solo)", Toast.LENGTH_SHORT).show()
                    }
                    2 -> {
                        toggleHostProfile()
                    }
                    3 -> {
                        showPresetSelectionDialog()
                    }
                }
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun toggleHostProfile() {
        cancelConnectingWatchdog()
        isMacHost = !isMacHost
        val settingsPrefs = getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE)
        settingsPrefs.edit().putBoolean(KEY_IS_MAC_HOST, isMacHost).apply()
        updateModeUI()

        val hostName = if (isMacHost) "🍎 Mac Profile (Collection 1 Gamepad)" else "🪟 Windows Profile (DirectInput Default)"
        Toast.makeText(this, "Profile: $hostName. Updating HID Service...", Toast.LENGTH_SHORT).show()

        val hid = hidDevice ?: return
        try {
            connectedHostDevice = null
            if (isRegistered) {
                isPendingReRegistration = true
                hid.unregisterApp()
            } else {
                registerHidApp()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling host profile", e)
        }
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
        Toast.makeText(this, "Edit Mode: Drag to move • Pinch in/out to resize", Toast.LENGTH_SHORT).show()
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
            .setMessage("Reset all buttons and the analog dial to default positions and sizes for '${canvasView.activePreset.name}'?")
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
            val descriptor = if (isMacHost) HidDescriptor.MAC_GAMEPAD_DESCRIPTOR else HidDescriptor.WINDOWS_COMPOSITE_DESCRIPTOR
            val hostLabel = if (isMacHost) "Mac" else "Windows"

            val sdpSettings = BluetoothHidDeviceAppSdpSettings(
                "Cricket 07 Controller",
                "${DeviceCompatibility.displayName} Controller ${DeviceCompatibility.APP_VERSION} ($hostLabel)",
                DeviceCompatibility.manufacturer,
                BluetoothHidDevice.SUBCLASS2_GAMEPAD, // Standard SUBCLASS2_GAMEPAD for 100% Android stack reliability
                descriptor
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
            Toast.makeText(this, "HID Service initializing...", Toast.LENGTH_SHORT).show()
            setupBluetoothHid()
            return
        }

        val devName = getDeviceName(device)

        // Check if device is already connected at the HID profile layer
        val currentState = hid.getConnectionState(device)
        if (currentState == BluetoothProfile.STATE_CONNECTED) {
            handleHostConnected(device)
            return
        }

        updateStatus("Connecting to $devName...", false)
        startConnectingWatchdog(device)

        try {
            val success = hid.connect(device)
            Log.d(TAG, "hid.connect($devName) returned $success")
            if (!success) {
                Toast.makeText(this, "Connecting to $devName...", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to $devName", e)
            cancelConnectingWatchdog()
            updateStatus("Connection error: ${e.message}", false)
        }
    }

    @SuppressLint("MissingPermission")
    private fun showDeviceConnectionDialog() {
        val bonded = getBondedDevices()
        val currentlyConnected = getConnectedHidDevices()
        val connectedMacs = currentlyConnected.map { it.address }.toSet()

        val items = mutableListOf<String>()
        val deviceTargets = mutableListOf<BluetoothDevice>()

        for (dev in bonded) {
            val isDevConnected = connectedMacs.contains(dev.address) ||
                    (hidDevice?.getConnectionState(dev) == BluetoothProfile.STATE_CONNECTED) ||
                    (connectedHostDevice?.address == dev.address)
            val isComputer = dev.bluetoothClass?.majorDeviceClass == BluetoothClass.Device.Major.COMPUTER
            val icon = if (isComputer) "💻" else "📱"
            val statusTag = if (isDevConnected) " [● CONNECTED]" else ""
            items.add("$icon ${getDeviceName(dev)}$statusTag")
            deviceTargets.add(dev)
        }

        items.add("➕ Make Phone Discoverable (Pair New PC / Mac)")
        items.add("↺ Re-register Bluetooth HID Service")

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Bluetooth HID Connection (${DeviceCompatibility.displayName})")
        builder.setItems(items.toTypedArray()) { _, which ->
            when {
                which < deviceTargets.size -> {
                    val selectedDevice = deviceTargets[which]
                    if (hidDevice?.getConnectionState(selectedDevice) == BluetoothProfile.STATE_CONNECTED) {
                        handleHostConnected(selectedDevice)
                        Toast.makeText(this, "Connected to ${getDeviceName(selectedDevice)}", Toast.LENGTH_SHORT).show()
                    } else {
                        connectToDevice(selectedDevice)
                    }
                }
                which == deviceTargets.size -> {
                    requestDiscoverable()
                }
                which == deviceTargets.size + 1 -> {
                    try {
                        cancelConnectingWatchdog()
                        hidDevice?.unregisterApp()
                        isRegistered = false
                        connectedHostDevice = null
                        mainHandler.postDelayed({ registerHidApp() }, 400)
                        Toast.makeText(this, "Resetting Bluetooth HID...", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error resetting HID", e)
                    }
                }
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
    private fun getConnectedHidDevices(): List<BluetoothDevice> {
        return try {
            hidDevice?.connectedDevices ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    @SuppressLint("MissingPermission")
    private fun getDeviceName(device: BluetoothDevice?): String {
        return try {
            device?.name ?: device?.address ?: "PC / Mac"
        } catch (_: Exception) {
            "PC / Mac"
        }
    }

    /**
     * Sends Gamepad Report (Report ID 2) for DirectInput / XInput / macOS GameController:
     * - Left Stick X & Y axes (0 to 255, center 128)
     * - POV Hat Switch (0 to 8)
     * - Buttons 1 to 16
     * Recognized by Windows DirectInput and macOS as Controller 1 / Gamepad!
     */
    @SuppressLint("MissingPermission")
    private fun sendGamepadReport(leftStickX: Byte, leftStickY: Byte, hatSwitch: Byte, buttonsMask: Short) {
        val device = connectedHostDevice ?: return
        val hid = hidDevice ?: return

        try {
            val report = ByteArray(7)
            report[0] = leftStickX
            report[1] = leftStickY
            report[2] = leftStickX // Mirror to Century Stick Z for direct shot picking
            report[3] = leftStickY // Mirror to Century Stick Rz for direct shot picking
            report[4] = (hatSwitch.toInt() and 0x0F).toByte()
            report[5] = (buttonsMask.toInt() and 0xFF).toByte()
            report[6] = ((buttonsMask.toInt() shr 8) and 0xFF).toByte()

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
        cancelConnectingWatchdog()
        try {
            hidDevice?.unregisterApp()
            if (hidDevice != null && bluetoothAdapter != null) {
                bluetoothAdapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, hidDevice)
            }
        } catch (_: Exception) {}
        super.onDestroy()
    }
}
