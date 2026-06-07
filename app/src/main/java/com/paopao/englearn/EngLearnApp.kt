package com.paopao.englearn

import android.app.Application
import com.paopao.englearn.data.preferences.SettingsDataStore

/**
 * Application class — holds the manual dependency injection container.
 * Avoiding Hilt/Dagger for MVP simplicity. All singletons live here.
 */
class EngLearnApp : Application() {

    lateinit var settingsDataStore: SettingsDataStore
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize singletons
        settingsDataStore = SettingsDataStore(this)
    }

    companion object {
        lateinit var instance: EngLearnApp
            private set
    }
}
