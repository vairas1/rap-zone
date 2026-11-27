package com.rapzone.app.model

/**
 * Data models matching the Rap Zone REST API.
 */

data class Artist(
    val id: String,
    val name: String,
    val nameEn: String? = null,
    val bio: String? = null,
    val imageUrl: String? = null,
    val playCount: Int = 0,
    val albums: List<Album> = emptyList()
) {
    val displayName: String get() = nameEn?.takeIf { it.isNotBlank() } ?: name
}

data class ArtistDetail(
    val id: String,
    val name: String,
    val nameEn: String? = null,
    val bio: String? = null,
    val imageUrl: String? = null,
    val playCount: Int = 0,
    val totalPlays: Int = 0,
    val songs: List<Song> = emptyList(),
    val albums: List<Album> = emptyList()
) {
    val displayName: String get() = nameEn?.takeIf { it.isNotBlank() } ?: name
}

data class Album(
    val id: String,
    val title: String,
    val description: String? = null,
    val coverUrl: String? = null,
    val releaseYear: Int = 0,
    val artistId: String? = null,
    val artistName: String? = null,
    val songs: List<Song>? = null
) {
    val trackCount: Int get() = songs?.size ?: 0
}

data class Song(
    val id: String,
    val title: String,
    val lyrics: String? = null,
    val durationSec: Int = 0,
    val audioUrl: String,
    val imageUrl: String? = null,
    val playCount: Int = 0,
    val downloadCount: Int = 0,
    val position: Int = 0,
    val artistId: String? = null,
    val artistName: String? = null,
    val albumId: String? = null,
    val albumTitle: String? = null,
    val artistImageUrl: String? = null,
    val releasedAt: String? = null
) {
    val displayArtist: String get() = artistName?.takeIf { it.isNotBlank() } ?: "فنان مجهول"
    val displayAlbum: String get() = albumTitle?.takeIf { it.isNotBlank() } ?: "ألبوم مجهول"
    val displayImage: String? get() = imageUrl?.takeIf { it.isNotBlank() } ?: artistImageUrl

    fun formattedDuration(): String {
        val mins = durationSec / 60
        val secs = durationSec % 60
        return "%d:%02d".format(mins, secs)
    }
}

data class Playlist(
    val id: String,
    val name: String,
    val nameEn: String? = null,
    val description: String? = null,
    val imageUrl: String? = null,
    val songs: List<Song>? = null
) {
    val displayName: String get() = nameEn?.takeIf { it.isNotBlank() } ?: name
    val songCount: Int get() = songs?.size ?: 0
}

data class HomeFeed(
    val featuredArtists: List<Artist> = emptyList(),
    val latestAlbums: List<Album> = emptyList(),
    val latestSongs: List<Song> = emptyList(),
    val popularSongs: List<Song> = emptyList(),
    val featuredPlaylists: List<Playlist> = emptyList()
)

data class SearchResult(
    val query: String = "",
    val artists: List<Artist> = emptyList(),
    val albums: List<Album> = emptyList(),
    val songs: List<Song> = emptyList()
)

data class Paged<T>(
    val items: List<T> = emptyList(),
    val total: Int = 0
)

// ---------- Auth ----------

data class AuthRequest(
    val email: String,
    val password: String,
    val name: String? = null
)

data class AuthResponse(
    val accessToken: String,
    val user: UserInfo? = null
)

data class UserInfo(
    val id: String? = null,
    val email: String? = null,
    val role: String? = null
)

// ---------- Local (Room mirrors) ----------

data class LocalFavorite(
    val songId: String,
    val title: String,
    val artistName: String? = null,
    val albumTitle: String? = null,
    val imageUrl: String? = null,
    val audioUrl: String,
    val durationSec: Int = 0,
    val lyrics: String? = null,
    val addedAt: Long = 0
) {
    fun toSong(): Song = Song(
        id = songId,
        title = title,
        lyrics = lyrics,
        durationSec = durationSec,
        audioUrl = audioUrl,
        imageUrl = imageUrl,
        artistName = artistName,
        albumTitle = albumTitle
    )
}

data class LocalPlaylist(
    val id: String,
    val name: String,
    val description: String? = null,
    val createdAt: Long = 0
)

data class LocalPlaylistSong(
    val playlistId: String,
    val songId: String,
    val position: Int,
    val title: String,
    val artistName: String? = null,
    val albumTitle: String? = null,
    val audioUrl: String,
    val imageUrl: String? = null,
    val durationSec: Int = 0,
    val lyrics: String? = null
) {
    fun toSong(): Song = Song(
        id = songId,
        title = title,
        lyrics = lyrics,
        durationSec = durationSec,
        audioUrl = audioUrl,
        imageUrl = imageUrl,
        artistName = artistName,
        albumTitle = albumTitle
    )
}

data class DownloadedSong(
    val songId: String,
    val title: String,
    val artistName: String? = null,
    val localPath: String,
    val audioUrl: String,
    val addedAt: Long = 0
)

/** Alias used by the repository layer. */
typealias DownloadableInfo = DownloadedSong

data class PlayHistory(
    val songId: String,
    val title: String,
    val artistName: String? = null,
    val imageUrl: String? = null,
    val playedAt: Long = 0
)
