package com.rapzone.app.download

import android.content.Context
import com.rapzone.app.db.DownloadEntity
import com.rapzone.app.db.RapDatabase
import com.rapzone.app.model.Song
import com.rapzone.app.network.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okio.BufferedSink
import java.io.File
import java.io.IOException

/**
 * Downloads songs to app-internal storage (no storage permission needed).
 */
class DownloadManager(
    private val context: Context,
    private val db: RapDatabase,
    private val apiClient: ApiClient
) {

    private val client = OkHttpClient()

    private val _active = MutableStateFlow<Downloadable?>(null)
    val active: StateFlow<Downloadable?> = _active.asStateFlow()

    data class Downloadable(
        val song: Song,
        val progress: Float,
        val state: String // "downloading", "done", "error"
    )

    private val jobs = mutableMapOf<String, Job?>()
    private val scope = CoroutineScope(Dispatchers.IO)

    fun downloadsDir(): File {
        val dir = File(context.filesDir, "downloads")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun localPathFor(songId: String): String? {
        val f = File(downloadsDir(), "$songId.mp3")
        return if (f.exists() && f.length() > 0) f.absolutePath else null
    }

    fun isDownloaded(songId: String): Boolean = localPathFor(songId) != null

    /**
     * Start downloading [song]. Returns immediately.
     * If already downloaded, just marks it.
     */
    fun download(song: Song) {
        val existing = jobs[song.id]?.takeIf { it.isActive }
        if (existing != null) return

        jobs[song.id] = scope.launch {
            _active.value = Downloadable(song, 0f, "downloading")
            try {
                val response = client.newCall(
                    okhttp3.Request.Builder().url(song.audioUrl).build()
                ).execute()
                if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
                val body: ResponseBody = response.body ?: throw IOException("empty body")

                val target = File(downloadsDir(), "${song.id}.mp3")
                val tmp = File(downloadsDir(), "${song.id}.mp3.part")
                body.byteStream().use { input ->
                    tmp.outputStream().use { out ->
                        val buffer = ByteArray(64 * 1024)
                        var total: Long = 0
                        val contentLength = body.contentLength().takeIf { it > 0 }
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            out.write(buffer, 0, read)
                            total += read
                            if (contentLength != null) {
                                _active.value = Downloadable(
                                    song,
                                    total.toFloat() / contentLength.toFloat(),
                                    "downloading"
                                )
                            }
                        }
                        tmp.renameTo(target)
                    }
                }

                db.downloadDao().upsert(
                    DownloadEntity(
                        songId = song.id,
                        title = song.title,
                        artistName = song.displayArtist,
                        localPath = target.absolutePath,
                        audioUrl = song.audioUrl,
                        addedAt = System.currentTimeMillis()
                    )
                )
                _active.value = Downloadable(song, 1f, "done")
            } catch (e: Exception) {
                _active.value = Downloadable(song, 0f, "error")
            } finally {
                jobs[song.id] = null
                _active.value = null as Downloadable?
            }
        }
    }

    fun cancel(songId: String) {
        jobs[songId]?.cancel()
        jobs[songId] = null
        _active.value = null as Downloadable?
    }

    /** Delete a downloaded file (and its DB record). */
    fun deleteDownload(songId: String) {
        File(downloadsDir(), "$songId.mp3").delete()
        scope.launch {
            db.downloadDao().delete(songId)
        }
    }

    /** Resolve a song's playable URL: local file if downloaded, else remote. */
    fun playableUrlFor(song: Song): String {
        localPathFor(song.id)?.let { return it }
        return song.audioUrl
    }

    /** Map a list of songs to locally-downloadable URLs where available. */
    fun playableSongs(songs: List<Song>): List<Song> =
        songs.map { s ->
            localPathFor(s.id)?.let { s.copy(audioUrl = it) } ?: s
        }
}
