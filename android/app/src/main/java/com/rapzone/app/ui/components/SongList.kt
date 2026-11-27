package com.rapzone.app.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import com.rapzone.app.di.AppContainer
import com.rapzone.app.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * A list of songs with full actions: play in queue, favorite, download,
 * add-to-playlist. Centralizes all per-song action state.
 */
@Composable
fun SongList(
    songs: List<Song>,
    onPlay: (List<Song>, Int) -> Unit,
    showDownloadButton: Boolean = true,
    modifier: Modifier = Modifier
) {
    val container = AppContainer.instance()!!
    val repo = container.repository
    val dm = container.downloadManager
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val favorites by remember { repo.favoritesFlow() }.collectAsState(initial = emptyList())
    val favIds = remember(favorites) { favorites.map { it.songId }.toSet() }

    val downloads by remember { repo.downloadsFlow() }.collectAsState(initial = emptyList())
    val downloadedIds = remember(downloads) { downloads.map { it.songId }.toSet() }

    val playlists by remember { repo.playlistsFlow() }.collectAsState(initial = emptyList())

    var addSong by remember { mutableStateOf<Song?>(null) }

    fun toast(msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    Column(modifier = modifier) {
        songs.forEachIndexed { i, song ->
            SongRow(
                song = song,
                index = i,
                isCurrent = container.playback.isPlayingCurrentSongId(song.id),
                onClick = {
                    repo.reportPlay(song.id)
                    scope.launch(Dispatchers.IO) {
                        repo.addHistory(song)
                    }
                    onPlay(dm.playableSongs(songs), i)
                },
                isFav = song.id in favIds,
                onFavToggle = { s ->
                    scope.launch(Dispatchers.IO) {
                        repo.toggleFavorite(s)
                    }
                },
                showDownload = showDownloadButton,
                isDownloaded = song.id in downloadedIds,
                onDownload = { s ->
                    if (s.id in downloadedIds) {
                        toast("محملة بالفعل")
                    } else {
                        scope.launch {
                            dm.download(s)
                            repo.reportDownload(s.id)
                        }
                    }
                },
                onAddToPlaylist = { s -> addSong = s }
            )
        }

        val s = addSong
        if (s != null) {
            AddToPlaylistDialog(
                songTitle = s.title,
                playlists = playlists,
                onCreateNew = { name ->
                    scope.launch(Dispatchers.IO) {
                        val id = repo.createPlaylist(name)
                        repo.addSongToPlaylist(id, s)
                    }
                    addSong = null
                    toast("تمت الإضافة")
                },
                onPick = { pid ->
                    scope.launch(Dispatchers.IO) {
                        repo.addSongToPlaylist(pid, s)
                    }
                    addSong = null
                    toast("تمت الإضافة")
                },
                onDismiss = { addSong = null }
            )
        }
    }
}
