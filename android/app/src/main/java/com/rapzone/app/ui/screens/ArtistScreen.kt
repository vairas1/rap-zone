package com.rapzone.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.rapzone.app.R
import com.rapzone.app.di.AppContainer
import com.rapzone.app.model.ArtistDetail
import com.rapzone.app.model.Song
import com.rapzone.app.ui.components.AlbumCard
import com.rapzone.app.ui.components.ErrorView
import com.rapzone.app.ui.components.LoadingRow
import com.rapzone.app.ui.components.SectionHeader
import com.rapzone.app.ui.components.SongList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ArtistScreen(
    artistId: String,
    onBack: () -> Unit,
    onOpenAlbum: (String) -> Unit,
    onPlaySong: (List<Song>, Int) -> Unit
) {
    val container = AppContainer.instance()!!
    var detail by remember { mutableStateOf<ArtistDetail?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loadId by remember { mutableStateOf(0) }

    fun load() {
        loadId++
    }

    LaunchedEffect(loadId) {
        error = null
        detail = null
        try {
            detail = withContext(Dispatchers.IO) { container.repository.artist(artistId) }
        } catch (e: Exception) {
            error = "تعذر تحميل البيانات: ${e.message}"
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("الفنان") },
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
            detail == null -> LoadingRow()
            else -> {
                val artist = detail!!
                LazyColumn(contentPadding = PaddingValues(bottom = 120.dp)) {
                    item {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(140.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!artist.imageUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = artist.imageUrl,
                                        contentDescription = artist.displayName,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(140.dp)
                                            .clip(CircleShape)
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                    )
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = artist.displayName,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            if (artist.bio?.isNotBlank() == true) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = artist.bio!!,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 4,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.Center) {
                                FilledTonalButton(
                                    onClick = {
                                        val songs = container.downloadManager.playableSongs(artist.songs)
                                        if (songs.isNotEmpty()) {
                                            container.repository.reportPlay(artist.songs[0].id)
                                            onPlaySong(songs, 0)
                                        }
                                    },
                                    enabled = artist.songs.isNotEmpty()
                                ) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("تشغيل الكل", fontSize = 14.sp)
                                }
                            }
                        }
                    }

                    if (artist.albums.isNotEmpty()) {
                        item { SectionHeader("الألبومات") }
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(artist.albums.size) { i ->
                                    Box(modifier = Modifier.width(150.dp)) {
                                        AlbumCard(album = artist.albums[i]) {
                                            onOpenAlbum(artist.albums[i].id)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (artist.songs.isNotEmpty()) {
                        item { SectionHeader("الأغاني") }
                        item {
                            SongList(
                                songs = artist.songs,
                                onPlay = onPlaySong,
                                showDownloadButton = true
                            )
                        }
                    }
                }
            }
        }
    }
}
