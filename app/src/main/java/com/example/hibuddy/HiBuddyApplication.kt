package com.example.hibuddy

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class HiBuddyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "HiBuddy updates",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )
        }
    }

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "hibuddy_updates"
    }
}
