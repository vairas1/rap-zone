package com.rapzone.app.data

import android.content.Context
import com.rapzone.app.db.DownloadEntity
import com.rapzone.app.db.FavoriteEntity
import com.rapzone.app.db.HistoryEntity
import com.rapzone.app.db.PlaylistEntity
import com.rapzone.app.db.PlaylistSongEntity
import com.rapzone.app.db.RapDatabase
import com.rapzone.app.model.Album
import com.rapzone.app.model.ArtistDetail
import com.rapzone.app.model.AuthRequest
import com.rapzone.app.model.AuthResponse
import com.rapzone.app.model.DownloadableInfo
import com.rapzone.app.model.HomeFeed
import com.rapzone.app.model.LocalFavorite
import com.rapzone.app.model.LocalPlaylist
import com.rapzone.app.model.LocalPlaylistSong
import com.rapzone.app.model.PlayHistory
import com.rapzone.app.model.Playlist
import com.rapzone.app.model.SearchResult
import com.rapzone.app.model.Song
import com.rapzone.app.network.ApiClient
import com.rapzone.app.network.AppConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Single data source for the UI. Wraps:
 * - Remote API (Retrofit) for streaming content
 * - Room DB for local favorites / playlists / downloads / history
 */
class RapZoneRepository(
    private val context: Context,
    private val db: RapDatabase,
    private val apiClient: ApiClient
) {

    private val api get() = apiClient.get(context)
    private val background = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ---------- Auth ----------

    fun isLogged(): Boolean = AppConfig.token(context) != null

    suspend fun login(email: String, password: String): AuthResponse {
        val resp = api.login(AuthRequest(email = email, password = password))
        AppConfig.setToken(context, resp.accessToken)
        return resp
    }

    suspend fun register(name: String, email: String, password: String): AuthResponse {
        val resp = api.register(AuthRequest(name = name, email = email, password = password))
        AppConfig.setToken(context, resp.accessToken)
        return resp
    }

    fun logout() {
        AppConfig.setToken(context, null)
    }

    // ---------- Remote content ----------

    suspend fun homeFeed(): HomeFeed = api.homeFeed()

    suspend fun search(q: String): SearchResult =
        if (q.isBlank()) SearchResult() else api.search(q.trim())

    suspend fun artist(id: String): ArtistDetail = api.artist(id)

    suspend fun album(id: String): Album = api.album(id)

    suspend fun playlist(id: String): Playlist = api.playlist(id)

    /** Report a play event to the server (best-effort, non-blocking). */
    fun reportPlay(songId: String) {
        val s = api
        background.launch {
            try {
                s.recordPlay(songId)
            } catch (_: Exception) {
            }
        }
    }

    fun reportDownload(songId: String) {
        val s = api
        background.launch {
            try {
                s.recordDownload(songId)
            } catch (_: Exception) {
            }
        }
    }

    // ---------- Local favorites ----------

    fun favoritesFlow(): Flow<List<LocalFavorite>> =
        db.favoriteDao().observeAll().map { list ->
            list.map { it.toModel() }
        }

    suspend fun isFavorite(songId: String): Boolean =
        db.favoriteDao().exists(songId) > 0

    suspend fun toggleFavorite(song: Song) {
        val exists = db.favoriteDao().exists(song.id) > 0
        if (exists) {
            db.favoriteDao().delete(song.id)
        } else {
            db.favoriteDao().upsert(toFavoriteEntity(song))
        }
    }

    // ---------- Local playlists ----------

    fun playlistsFlow(): Flow<List<LocalPlaylist>> =
        db.playlistDao().observeAll().map { list ->
            list.map { it.toModel() }
        }

    suspend fun createPlaylist(name: String, description: String? = null): String {
        val id = UUID.randomUUID().toString()
        db.playlistDao().insert(
            PlaylistEntity(
                id = id,
                name = name,
                description = description,
                createdAt = System.currentTimeMillis()
            )
        )
        return id
    }

    suspend fun deletePlaylist(id: String) {
        db.playlistSongDao().removeForPlaylist(id)
        db.playlistDao().delete(id)
    }

    fun playlistSongs(id: String): Flow<List<LocalPlaylistSong>> =
        db.playlistSongDao().songsFor(id).map { list ->
            list.map { it.toModel() }
        }

    suspend fun playlistSongCount(id: String): Int = db.playlistSongDao().countFor(id)

    /** Returns false if the song was already in the playlist. */
    suspend fun addSongToPlaylist(playlistId: String, song: Song): Boolean {
        if (db.playlistSongDao().exists(playlistId, song.id) > 0) return false
        val maxPos = db.playlistSongDao().maxPosition(playlistId) ?: 0
        db.playlistSongDao().insert(
            PlaylistSongEntity(
                playlistId = playlistId,
                songId = song.id,
                position = maxPos + 1,
                title = song.title,
                artistName = song.displayArtist,
                albumTitle = song.displayAlbum,
                audioUrl = song.audioUrl,
                imageUrl = song.displayImage,
                durationSec = song.durationSec,
                lyrics = song.lyrics
            )
        )
        return true
    }

    suspend fun removeSongFromPlaylist(playlistId: String, songId: String) {
        db.playlistSongDao().remove(playlistId, songId)
    }

    // ---------- Downloads ----------

    fun downloadsFlow(): Flow<List<DownloadableInfo>> =
        db.downloadDao().observeAll().map { list ->
            list.map { it.toModel() }
        }

    suspend fun isDownloaded(songId: String): Boolean =
        db.downloadDao().find(songId) != null

    // ---------- Play history ----------

    suspend fun addHistory(song: Song) {
        db.historyDao().insert(
            HistoryEntity(
                songId = song.id,
                title = song.title,
                artistName = song.displayArtist,
                imageUrl = song.displayImage,
                playedAt = System.currentTimeMillis()
            )
        )
    }

    fun historyFlow(): Flow<List<PlayHistory>> =
        db.historyDao().observeRecent(50).map { list ->
            list.map { it.toModel() }
        }
}

// ---------- Entity <-> Model mapping ----------

fun toFavoriteEntity(song: Song) = FavoriteEntity(
    songId = song.id,
    title = song.title,
    artistName = song.displayArtist,
    albumTitle = song.displayAlbum,
    imageUrl = song.displayImage,
    audioUrl = song.audioUrl,
    durationSec = song.durationSec,
    lyrics = song.lyrics,
    addedAt = System.currentTimeMillis()
)

private fun FavoriteEntity.toModel() = LocalFavorite(
    songId = songId,
    title = title,
    artistName = artistName,
    albumTitle = albumTitle,
    imageUrl = imageUrl,
    audioUrl = audioUrl,
    durationSec = durationSec,
    lyrics = lyrics,
    addedAt = addedAt
)

private fun PlaylistEntity.toModel() = LocalPlaylist(
    id = id,
    name = name,
    description = description,
    createdAt = createdAt
)

private fun PlaylistSongEntity.toModel() = LocalPlaylistSong(
    playlistId = playlistId,
    songId = songId,
    position = position,
    title = title,
    artistName = artistName,
    albumTitle = albumTitle,
    audioUrl = audioUrl,
    imageUrl = imageUrl,
    durationSec = durationSec,
    lyrics = lyrics
)

private fun DownloadEntity.toModel() = DownloadableInfo(
    songId = songId,
    title = title,
    artistName = artistName,
    localPath = localPath,
    audioUrl = audioUrl,
    addedAt = addedAt
)

private fun HistoryEntity.toModel() = PlayHistory(
    songId = songId,
    title = title,
    artistName = artistName,
    imageUrl = imageUrl,
    playedAt = playedAt
)
