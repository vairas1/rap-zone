package com.rapzone.app.playback

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.MediaMetadata
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.audio.AudioAttributes
import com.rapzone.app.model.Song
import java.util.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * App-wide playback engine.
 *
 * - ExoPlayer 2.18.4 streams audio from the API URLs (or local files for downloads).
 * - MediaSessionCompat (androidx.media) drives lock-screen and media-button
 *   controls; transport commands are routed back into this class.
 */
class PlaybackManager(private val context: Context) {

    data class State(
        val currentSong: Song? = null,
        val isPlaying: Boolean = false,
        val positionMs: Long = 0,
        val durationMs: Long = 0,
        val queueIndex: Int = -1,
        val queueSize: Int = 0,
        val isShuffle: Boolean = false,
        val repeatAll: Boolean = false
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val random = Random()

    @Volatile
    var player: ExoPlayer? = null
        private set

    @Volatile
    var mediaSession: MediaSessionCompat? = null
        private set

    @Volatile
    var mediaController: MediaControllerCompat? = null
        private set

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    @Volatile
    var queue: List<Song> = emptyList()
        private set

    @Volatile
    var index: Int = -1
        private set

    @Volatile
    private var isShuffle = false

    @Volatile
    private var repeatAll = false

    private var positionTicker: Job? = null

    // ---------- MediaSession ----------

    private val sessionCallback = object : MediaSessionCompat.Callback() {
        override fun onPlay() = startPlaying()
        override fun onPause() = pause()
        override fun onSkipToNext() = next()
        override fun onSkipToPrevious() = previous()
        override fun onSeekTo(pos: Long) = seekTo(pos)
        override fun onStop() = stop()
    }

    private fun ensureSession() {
        if (mediaSession != null) return
        val session = MediaSessionCompat(context, "RapZone")
        session.setCallback(sessionCallback)
        session.setSessionActivity(openAppPendingIntent())
        mediaSession = session
        mediaController = MediaControllerCompat(context, session.sessionToken)
    }

    private fun openAppPendingIntent(): PendingIntent {
        val intent = Intent(context, com.rapzone.app.MainActivity::class.java)
        return PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    // ---------- Player ----------

    private fun ensurePlayer(): ExoPlayer {
        val existing = player
        if (existing != null) return existing
        val attrs = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.CONTENT_TYPE_MUSIC)
            .build()
        val p = ExoPlayer.Builder(context)
            .setAudioAttributes(attrs, true)
            .build()
        p.addListener(playerListener)
        player = p
        return p
    }

    private val playerListener = object : Player.Listener {
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            val isPlaying = playWhenReady && playbackState == Player.STATE_READY
            _state.value = _state.value.copy(isPlaying = isPlaying)
            updateSessionPlaybackState()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_SEEK) {
                onSongChanged()
            } else {
                // Natural advance: keep our queue index in sync
                syncIndexFromPlayer()
            }
        }
    }

    /** Update [index] to match the position ExoPlayer advanced to. */
    private fun syncIndexFromPlayer() {
        val p = player ?: return
        val pos = p.currentMediaItemIndex
        if (pos in queue.indices) {
            index = pos
            onSongChanged()
        }
    }

    // ---------- Public API ----------

    /** Load a queue, jump to [startIndex] and optionally start playback. */
    fun playQueue(songs: List<Song>, startIndex: Int = 0, autoPlay: Boolean = true) {
        if (songs.isEmpty()) return
        val p = ensurePlayer()
        ensureSession()

        val items = songs.map { song ->
            MediaItem.Builder()
                .setUri(song.audioUrl)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(song.title)
                        .setArtist(song.displayArtist)
                        .setAlbumTitle(song.displayAlbum)
                        .build()
                )
                .build()
        }

        queue = songs
        index = startIndex.coerceIn(0, songs.lastIndex)
        p.setMediaItems(items, index, 0L)
        p.seekTo(index, 0L)

        if (autoPlay) {
            p.playWhenReady = true
            p.play()
            startForegroundService()
        }
        onSongChanged()
    }

    fun startPlaying() {
        if (_state.value.queueSize == 0) return
        ensurePlayer().apply {
            playWhenReady = true
            play()
        }
        startForegroundService()
    }

    fun pause() {
        player?.pause()
    }

    fun togglePlayPause() {
        val s = _state.value
        if (s.queueSize == 0) return
        if (s.isPlaying) pause() else startPlaying()
    }

    fun next() {
        if (queue.isEmpty()) return
        if (isShuffle && queue.size > 1) {
            index = random.nextInt(queue.size)
        } else {
            index += 1
            if (index >= queue.size) index = 0
        }
        jumpToIndex()
    }

    fun previous() {
        if (queue.isEmpty()) return
        if ((player?.currentPosition ?: 0L) > 3000) {
            player?.seekTo(0)
            return
        }
        index -= 1
        if (index < 0) index = queue.lastIndex
        jumpToIndex()
    }

    fun seekTo(ms: Long) {
        player?.seekTo(ms)
        _state.value = _state.value.copy(positionMs = ms)
    }

    fun setShuffle(enabled: Boolean) {
        isShuffle = enabled
        player?.setShuffleModeEnabled(enabled)
        _state.value = _state.value.copy(isShuffle = enabled)
    }

    fun setRepeatAll(enabled: Boolean) {
        repeatAll = enabled
        player?.repeatMode = if (enabled) ExoPlayer.REPEAT_MODE_ALL else ExoPlayer.REPEAT_MODE_OFF
        _state.value = _state.value.copy(repeatAll = enabled)
    }

    fun stop() {
        player?.pause()
        player?.stop()
        player?.clearMediaItems()
        queue = emptyList()
        index = -1
        _state.value = State()
        stopForegroundService()
    }

    fun currentSong(): Song? = if (index in queue.indices) queue[index] else null

    fun currentQueue(): List<Song> = queue

    fun isPlayingCurrentSongId(songId: String): Boolean = currentSong()?.id == songId

    fun release() {
        positionTicker?.cancel()
        player?.release()
        player = null
        mediaSession?.release()
        mediaSession = null
        mediaController = null
        scope.cancel()
    }

    // ---------- Service lifecycle ----------

    fun ensureSessionForService() {
        ensureSession()
    }

    fun setSessionActive(active: Boolean) {
        mediaSession?.setActive(active)
    }

    fun startForegroundService() {
        ContextCompat.startForegroundService(
            context,
            Intent(context, PlaybackService::class.java)
        )
    }

    fun stopForegroundService() {
        ContextCompat.startForegroundService(
            context,
            Intent(context, PlaybackService::class.java)
                .setAction(PlaybackService.ACTION_STOP)
        )
    }

    // ---------- Internals ----------

    private fun jumpToIndex() {
        if (index !in queue.indices) return
        val p = ensurePlayer()
        p.seekTo(index, 0L)
        if (!p.playWhenReady) p.playWhenReady = true
        onSongChanged()
        startForegroundService()
    }

    private fun onSongChanged() {
        val song = currentSong()
        _state.value = _state.value.copy(
            currentSong = song,
            queueIndex = index,
            queueSize = queue.size,
            positionMs = 0
        )
        startTicker()
        updateSessionMetadata()
    }

    private fun startTicker() {
        positionTicker?.cancel()
        positionTicker = scope.launch {
            while (true) {
                delay(500)
                val p = player?.currentPosition ?: 0L
                val d = player?.duration ?: _state.value.durationMs
                val cur = _state.value
                if (cur.positionMs != p || (d > 0 && cur.durationMs != d)) {
                    _state.value = cur.copy(
                        positionMs = p,
                        durationMs = if (d > 0) d else cur.durationMs
                    )
                }
            }
        }
    }

    private fun updateSessionMetadata() {
        val song = _state.value.currentSong ?: return
        ensureSession()
        val builder = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.displayArtist)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, song.displayAlbum)
            .putLong(
                MediaMetadataCompat.METADATA_KEY_DURATION,
                (song.durationSec * 1000).toLong()
            )
        val artUri = song.displayImage
        if (!artUri.isNullOrBlank()) {
            builder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, artUri)
        }
        mediaSession?.setMetadata(builder.build())
        updateSessionPlaybackState()
    }

    private fun updateSessionPlaybackState() {
        val s = _state.value
        if (s.queueSize == 0) return
        var actions =
            PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_STOP or
                PlaybackStateCompat.ACTION_PLAY_PAUSE
        if (s.queueSize > 1) {
            actions = actions or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
        }
        val state = PlaybackStateCompat.Builder()
            .setActions(actions)
            .setState(
                if (s.isPlaying) PlaybackStateCompat.STATE_PLAYING
                else PlaybackStateCompat.STATE_PAUSED,
                s.positionMs,
                1.0f,
                0
            )
            .build()
        mediaSession?.setPlaybackState(state)
    }
}
