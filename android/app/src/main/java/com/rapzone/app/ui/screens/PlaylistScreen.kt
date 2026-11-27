package com.rapzone.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rapzone.app.di.AppContainer
import com.rapzone.app.model.Playlist
import com.rapzone.app.model.Song
import com.rapzone.app.ui.components.CoverImage
import com.rapzone.app.ui.components.ErrorView
import com.rapzone.app.ui.components.LoadingRow
import com.rapzone.app.ui.components.SectionHeader
import com.rapzone.app.ui.components.SongList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreen(
    playlistId: String,
    onBack: () -> Unit,
    onPlaySong: (List<Song>, Int) -> Unit
) {
    val container = AppContainer.instance()!!
    var playlist by remember { mutableStateOf<Playlist?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loadId by remember { mutableStateOf(0) }

    fun load() {
        loadId++
    }

    LaunchedEffect(loadId) {
        error = null
        playlist = null
        try {
            playlist = withContext(Dispatchers.IO) { container.repository.playlist(playlistId) }
        } catch (e: Exception) {
            error = "تعذر تحميل البيانات: ${e.message}"
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("قائمة التشغيل") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        when {
            error != null -> ErrorView(message = error!!, onRetry = { load() })
            playlist == null -> LoadingRow()
            else -> {
                val p = playlist!!
                val songs = p.songs ?: emptyList()
                LazyColumn(contentPadding = PaddingValues(bottom = 120.dp)) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CoverImage(url = p.imageUrl, sizeDp = 88)
                                Spacer(Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = p.displayName,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Text(
                                        text = p.description ?: "",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (songs.isNotEmpty()) {
                                        Text(
                                            text = "${songs.size} أغنية",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Row {
                                FilledTonalButton(
                                    onClick = {
                                        val playable = container.downloadManager.playableSongs(songs)
                                        if (playable.isNotEmpty()) {
                                            container.repository.reportPlay(songs[0].id)
                                            onPlaySong(playable, 0)
                                        }
                                    },
                                    enabled = songs.isNotEmpty()
                                ) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("تشغيل القائمة", fontSize = 14.sp)
                                }
                            }
                        }
                    }
                    if (songs.isNotEmpty()) {
                        item { SectionHeader("الأغاني") }
                        item {
                            SongList(songs = songs, onPlay = onPlaySong, showDownloadButton = true)
                        }
                    }
                }
            }
        }
    }
}
