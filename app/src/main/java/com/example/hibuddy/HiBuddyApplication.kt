package com.example.hibuddy

import android.app.Application

class HiBuddyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
    }
}
