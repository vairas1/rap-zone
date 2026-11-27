package com.rapzone.app.playback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.rapzone.app.MainActivity
import com.rapzone.app.R
import com.rapzone.app.di.AppContainer
import com.rapzone.app.di.AppContainerImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Foreground service that keeps playback alive in the background and shows
 * a media notification with transport controls.
 *
 * - Notification buttons are routed back to this service and forwarded to
 *   the shared [PlaybackManager].
 * - Lock-screen and wired media buttons are handled by the
 *   [PlaybackManager]'s MediaSessionCompat.
 */
class PlaybackService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var notifier: kotlinx.coroutines.Job? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        val container = AppContainer.instance()
        if (container != null) {
            container.playback.ensureSessionForService()
            startForegroundCompat()
            startNotifier(container)
        } else {
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val container = AppContainer.instance()
        when (intent?.action) {
            ACTION_STOP -> {
                stopForegroundCompat()
                stopSelf()
            }
            ACTION_MEDIA_COMMAND -> {
                container?.playback?.let {
                    when (intent.getIntExtra(EXTRA_COMMAND, 0)) {
                        CMD_TOGGLE -> it.togglePlayPause()
                        CMD_NEXT -> it.next()
                        CMD_PREVIOUS -> it.previous()
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        notifier?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    // ---------- Foreground / notification ----------

    @Suppress("DEPRECATION")
    private fun startForegroundCompat() {
        val container = AppContainer.instance() ?: return
        val notification = buildNotification(container.playback)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIF_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIF_ID, notification)
        }
    }

    @Suppress("DEPRECATION")
    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            stopForeground(true)
        }
    }

    private fun startNotifier(container: AppContainerImpl) {
        notifier?.cancel()
        notifier = serviceScope.launch {
            container.playback.state.collect { st ->
                val session = container.playback.mediaSession
                if (session != null) {
                    // Ensure session is active for lock-screen media controls
                    session.setActive(true)
                }
                if (st.queueSize > 0) {
                    val nm = getSystemService(NotificationManager::class.java)
                    nm.notify(NOTIF_ID, buildNotification(container.playback))
                }
            }
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm: NotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                setShowBadge(false)
            }
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(playback: PlaybackManager): Notification {
        val song = playback.currentSong()

        val openIntent = Intent(this, MainActivity::class.java)
        val openPending = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(song?.title ?: getString(R.string.app_name))
            .setContentText(song?.displayArtist)
            .setContentIntent(openPending)
            .setOngoing(true)
            .setShowWhen(false)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .addAction(0, "السابق", commandPending(CMD_PREVIOUS))
            .addAction(0, "تشغيل/إيقاف", commandPending(CMD_TOGGLE))
            .addAction(0, "التالي", commandPending(CMD_NEXT))
            .build()
    }

    private fun commandPending(cmd: Int): PendingIntent {
        val intent = Intent(this, PlaybackService::class.java).setAction(ACTION_MEDIA_COMMAND)
        intent.putExtra(EXTRA_COMMAND, cmd)
        return PendingIntent.getService(
            this,
            cmd,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        private const val CHANNEL_ID = "rapzone_playback"
        private const val NOTIF_ID = 700
        const val ACTION_STOP = "com.rapzone.app.action.STOP"
        const val ACTION_MEDIA_COMMAND = "com.rapzone.app.action.MEDIA_COMMAND"
        const val EXTRA_COMMAND = "command"
        const val CMD_TOGGLE = 1
        const val CMD_NEXT = 2
        const val CMD_PREVIOUS = 3
    }
}