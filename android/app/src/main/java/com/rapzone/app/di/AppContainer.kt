package com.rapzone.app.di

import android.content.Context
import com.rapzone.app.data.RapZoneRepository
import com.rapzone.app.db.RapDatabase
import com.rapzone.app.download.DownloadManager
import com.rapzone.app.network.ApiClient
import com.rapzone.app.playback.PlaybackManager

/**
 * Simple manual dependency container (no Hilt).
 * Created once in [com.rapzone.app.RapZoneApplication].
 */
class AppContainerImpl(context: Context) {

    private val appContext = context.applicationContext

    val db: RapDatabase = RapDatabase.getInstance(appContext)

    val repository: RapZoneRepository =
        RapZoneRepository(appContext, db, ApiClient)

    val playback: PlaybackManager = PlaybackManager(appContext)

    val downloadManager: DownloadManager =
        DownloadManager(appContext, db, ApiClient)
}

/** Global accessor used by the service (which outlives the Activity). */
object AppContainer {
    @Volatile
    private var INSTANCE: AppContainerImpl? = null

    fun init(app: AppContainerImpl): AppContainerImpl {
        if (INSTANCE == null) {
            synchronized(this) {
                if (INSTANCE == null) INSTANCE = app
            }
        }
        return app
    }

    fun instance(): AppContainerImpl? = INSTANCE

    fun require(): AppContainerImpl = instance()
        ?: throw IllegalStateException("AppContainer not initialized")
}
