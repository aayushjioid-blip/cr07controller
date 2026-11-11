package com.controller.cricket07.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.controller.cricket07.hid.BluetoothHidController
import com.controller.cricket07.ui.MainActivity

class HidControllerService : Service() {

    companion object {
        const val CHANNEL_ID = "cricket07_controller_service"
        const val NOTIFICATION_ID = 1001
    }

    private val binder = LocalBinder()
    lateinit var hidController: BluetoothHidController
        private set

    private var wakeLock: PowerManager.WakeLock? = null

    inner class LocalBinder : Binder() {
        fun getService(): HidControllerService = this@HidControllerService
    }

    override fun onCreate() {
        super.onCreate()
        hidController = BluetoothHidController(this)
        hidController.init()

        createNotificationChannel()
        startInForeground()

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Cricket07::WakeLock")
        wakeLock?.acquire(12 * 60 * 60 * 1000L)
    }

    private fun startInForeground() {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Cricket 07 HID Controller Active")
            .setContentText("Connected to PC/Mac via Native Bluetooth HID")
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Bluetooth Controller Session",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Maintains persistent Bluetooth HID connection during gameplay"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        wakeLock?.let { if (it.isHeld) it.release() }
        hidController.destroy()
        super.onDestroy()
    }
}
