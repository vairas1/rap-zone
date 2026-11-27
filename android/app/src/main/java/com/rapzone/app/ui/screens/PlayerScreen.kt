package com.rapzone.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.rapzone.app.R
import com.rapzone.app.di.AppContainer
import com.rapzone.app.ui.components.SectionHeader
import com.rapzone.app.ui.components.SongList

/**
 * Full-screen player: artwork, seek bar, transport controls,
 * shuffle/repeat, lyrics and queue.
 */
@Composable
fun PlayerScreen(onBack: () -> Unit) {
    val container = AppContainer.instance()!!
    val playback = container.playback
    val state by playback.state.collectAsState()

    var showQueue by remember { mutableStateOf(false) }
    var dragValue by remember { mutableStateOf(0f) }

    val song = state.currentSong

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "إغلاق",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                "جاري التشغيل",
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold
            )
            IconButton(onClick = { showQueue = !showQueue }) {
                Icon(
                    Icons.Filled.QueueMusic,
                    contentDescription = "القائمة",
                    tint = if (showQueue) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onBackground
                )
            }
        }

        if (showQueue) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item { SectionHeader("قائمة الانتظار (${state.queueSize})") }
                items(state.queueSize) { i ->
                    val s = playback.currentQueue().getOrNull(i) ?: return@items
                    SongList(
                        songs = listOf(s),
                        onPlay = { _, _ -> playback.playQueue(playback.currentQueue(), i) },
                        showDownloadButton = false
                    )
                }
            }
        } else {
            if (song == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("لم تبدأ تشغيل أي أغنية بعد")
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(24.dp))

                    // Artwork
                    Box(
                        modifier = Modifier
                            .size(260.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        val artUrl = song.displayImage
                        if (!artUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = artUrl,
                                contentDescription = song.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.matchParentSize()
                            )
                        } else {
                            Icon(
                                Icons.Filled.QueueMusic,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(80.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(24.dp))

                    Text(
                        song.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                    Text(
                        song.displayArtist,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                    Spacer(Modifier.height(24.dp))

                    // Seek bar
                    val duration = state.durationMs
                    val progress = if (duration > 0) state.positionMs.toFloat() / duration.toFloat() else 0f
                    val displayProgress = if (dragValue > 0f) dragValue else progress
                    Slider(
                        value = displayProgress.coerceIn(0f, 1f),
                        onValueChange = { v -> dragValue = v },
                        onValueChangeFinished = {
                            playback.seekTo((dragValue * duration).toLong())
                            dragValue = 0f
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 40.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            formatMs(state.positionMs),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            formatMs(duration),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(16.dp))

                    // Transport controls
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(onClick = { playback.setShuffle(!state.isShuffle) }) {
                            Icon(
                                Icons.Filled.Shuffle,
                                contentDescription = "عشوائي",
                                tint = if (state.isShuffle) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        IconButton(onClick = { playback.previous() }) {
                            Icon(
                                Icons.Filled.SkipPrevious,
                                contentDescription = "السابق",
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .clickable { playback.togglePlayPause() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = "تشغيل",
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        IconButton(onClick = { playback.next() }) {
                            Icon(
                                Icons.Filled.SkipNext,
                                contentDescription = "التالي",
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        IconButton(onClick = { playback.setRepeatAll(!state.repeatAll) }) {
                            Icon(
                                Icons.Filled.Repeat,
                                contentDescription = "تكرار",
                                tint = if (state.repeatAll) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(24.dp))

                    // Lyrics (if available)
                    if (!song.lyrics.isNullOrBlank()) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(0.3f),
                            contentPadding = PaddingValues(horizontal = 32.dp, vertical = 8.dp)
                        ) {
                            item {
                                Column {
                                    SectionHeader("الكلمات")
                                    Text(
                                        song.lyrics!!,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        lineHeight = 26.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSec = ms / 1000
    val m = totalSec / 60
    val s = totalSec % 60
    return "%d:%02d".format(m, s)
}
