package com.rapzone.app

import android.app.Application
import com.rapzone.app.di.AppContainer
import com.rapzone.app.di.AppContainerImpl

class RapZoneApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppContainer.init(AppContainerImpl(this))
    }

    override fun onTerminate() {
        super.onTerminate()
        AppContainer.instance()?.playback?.release()
    }
}
