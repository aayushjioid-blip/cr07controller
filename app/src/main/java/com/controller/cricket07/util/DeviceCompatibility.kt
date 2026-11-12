package com.controller.cricket07.util

import android.os.Build

/**
 * Universal Mobile Device Compatibility & Auto-Detection:
 * Accurately detects phone manufacturer, model, and OS version
 * (e.g. OnePlus 13R, Samsung Galaxy S24, Xiaomi, Google Pixel, etc.)
 */
object DeviceCompatibility {
    const val APP_VERSION = "v2.5.0"

    val manufacturer: String = Build.MANUFACTURER.replaceFirstChar { 
        if (it.isLowerCase()) it.titlecase() else it.toString() 
    }
    val model: String = Build.MODEL
    val brand: String = Build.BRAND.replaceFirstChar { 
        if (it.isLowerCase()) it.titlecase() else it.toString() 
    }
    val androidRelease: String = Build.VERSION.RELEASE
    val sdkInt: Int = Build.VERSION.SDK_INT

    /**
     * Human-friendly device name (e.g. "OnePlus 13R", "Samsung Galaxy S24", "Xiaomi Redmi Note 13")
     */
    val displayName: String by lazy {
        if (model.startsWith(manufacturer, ignoreCase = true)) {
            model
        } else {
            "$manufacturer $model"
        }
    }

    /**
     * Full device string for UI badges
     */
    val summaryText: String by lazy {
        "$displayName (Android $androidRelease)"
    }

    val fullDeviceInfo: String get() = summaryText
}
