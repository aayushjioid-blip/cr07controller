package com.controller.cricket07

import android.app.Application
import android.util.Log

class CricketApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("CricketCrash", "UNCAUGHT CRASH on ${thread.name}: ${throwable.message}", throwable)
        }
    }
}
