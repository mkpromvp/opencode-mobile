package com.opencode.mobile

import android.app.Application
import com.opencode.mobile.data.PreferencesManager

class OpenCodeApp : Application() {
    lateinit var preferencesManager: PreferencesManager
        private set

    override fun onCreate() {
        super.onCreate()
        preferencesManager = PreferencesManager(this)
    }
}
