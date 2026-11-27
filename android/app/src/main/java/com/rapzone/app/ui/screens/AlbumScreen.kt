package com.rapzone.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.rapzone.app.R
import com.rapzone.app.di.AppContainer
import com.rapzone.app.model.Album
import com.rapzone.app.model.Song
import com.rapzone.app.ui.components.ErrorView
import com.rapzone.app.ui.components.LoadingRow
import com.rapzone.app.ui.components.SectionHeader
import com.rapzone.app.ui.components.SongList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AlbumScreen(
    albumId: String,
    onBack: () -> Unit,
    onOpenArtist: (String) -> Unit,
    onPlaySong: (List<Song>, Int) -> Unit
) {
    val container = AppContainer.instance()!!
    var album by remember { mutableStateOf<Album?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loadId by remember { mutableStateOf(0) }

    fun load() {
        loadId++
    }

    LaunchedEffect(loadId) {
        error = null
        album = null
        try {
            album = withContext(Dispatchers.IO) { container.repository.album(albumId) }
        } catch (e: Exception) {
            error = "تعذر تحميل البيانات: ${e.message}"
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("الألبوم") },
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
            album == null -> LoadingRow()
            else -> {
                val a = album!!
                val songs = a.songs ?: emptyList()
                LazyColumn(contentPadding = PaddingValues(bottom = 120.dp)) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!a.coverUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = a.coverUrl,
                                        contentDescription = a.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(20.dp))
                                    )
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = a.title,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            if (!a.artistName.isNullOrBlank()) {
                                Text(
                                    text = a.artistName!!,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .padding(top = 4.dp)
                                        .clickable(enabled = !a.artistId.isNullOrBlank()) {
                                            a.artistId?.let(onOpenArtist)
                                        }
                                )
                            }
                            if (a.releaseYear > 0) {
                                Text(
                                    text = "إصدار: ${a.releaseYear}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                            ) {
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
                                    Text("تشغيل الألبوم", fontSize = 14.sp)
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
