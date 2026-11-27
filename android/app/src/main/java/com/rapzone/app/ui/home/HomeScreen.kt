package com.rapzone.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rapzone.app.di.AppContainer
import com.rapzone.app.model.HomeFeed
import com.rapzone.app.model.Song
import com.rapzone.app.ui.components.AlbumCard
import com.rapzone.app.ui.components.ArtistCard
import com.rapzone.app.ui.components.ErrorView
import com.rapzone.app.ui.components.LoadingRow
import com.rapzone.app.ui.components.PlaylistCard
import com.rapzone.app.ui.components.SectionHeader
import com.rapzone.app.ui.components.SongList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun HomeScreen(
    onOpenArtist: (String) -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onPlaySong: (List<Song>, Int) -> Unit
) {
    val repo = AppContainer.instance()?.repository
        ?: throw IllegalStateException("AppContainer not initialized")
    val playback = AppContainer.instance()?.playback

    var feed by remember { mutableStateOf<HomeFeed?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loadId by remember { mutableStateOf(0) }

    fun load() {
        loadId++
    }

    LaunchedEffect(loadId) {
        error = null
        feed = null
        try {
            feed = withContext(Dispatchers.IO) { repo.homeFeed() }
        } catch (e: Exception) {
            error = "تعذر تحميل البيانات: ${e.message}"
        }
    }

    if (error != null || feed == null) {
        if (error != null) {
            ErrorView(message = error!!, onRetry = { load() })
        } else {
            LoadingRow()
        }
        return
    }

    val f = feed!!
    LazyColumn(contentPadding = PaddingValues(bottom = 120.dp)) {
        item {
            Text(
                text = "مرحباً بك",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "استمع لأفضل الأغاني في راب زون",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        if (f.featuredArtists.isNotEmpty()) {
            item { SectionHeader("أبرز الفنانين") }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(f.featuredArtists.size) { i ->
                        Box(modifier = Modifier.width(132.dp)) {
                            ArtistCard(artist = f.featuredArtists[i]) {
                                onOpenArtist(f.featuredArtists[i].id)
                            }
                        }
                    }
                }
            }
        }

        if (f.latestAlbums.isNotEmpty()) {
            item { SectionHeader("أحدث الألبومات") }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(f.latestAlbums.size) { i ->
                        Box(modifier = Modifier.width(160.dp)) {
                            AlbumCard(album = f.latestAlbums[i]) {
                                onOpenAlbum(f.latestAlbums[i].id)
                            }
                        }
                    }
                }
            }
        }

        if (f.featuredPlaylists.isNotEmpty()) {
            item { SectionHeader("قوائم مميزة") }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(f.featuredPlaylists.size) { i ->
                        Box(modifier = Modifier.width(240.dp)) {
                            PlaylistCard(playlist = f.featuredPlaylists[i]) {
                                onOpenPlaylist(f.featuredPlaylists[i].id)
                            }
                        }
                    }
                }
            }
        }

        if (f.popularSongs.isNotEmpty()) {
            item { SectionHeader("الأكثر استماعاً") }
            item {
                SongList(songs = f.popularSongs, onPlay = onPlaySong, showDownloadButton = true)
            }
        }

        if (f.latestSongs.isNotEmpty()) {
            item { SectionHeader("أحدث الأغاني") }
            item {
                SongList(songs = f.latestSongs, onPlay = onPlaySong, showDownloadButton = true)
            }
        }
    }
}
