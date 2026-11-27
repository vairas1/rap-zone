package com.rapzone.app.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rapzone.app.di.AppContainer
import com.rapzone.app.model.LocalPlaylistSong
import com.rapzone.app.model.Song
import com.rapzone.app.ui.components.EmptyView
import com.rapzone.app.ui.components.LoadingRow
import com.rapzone.app.ui.components.SongList
import androidx.compose.foundation.layout.Column

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun LocalPlaylistScreen(
    playlistId: String,
    onBack: () -> Unit,
    onPlaySong: (List<Song>, Int) -> Unit
) {
    val container = AppContainer.instance()!!

    var songs by remember { mutableStateOf<List<LocalPlaylistSong>?>(null) }

    LaunchedEffect(Unit) {
        container.repository.playlistSongs(playlistId).collect { songs = it }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("قائمتي") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        val list = songs
        when {
            list == null -> LoadingRow()
            list.isEmpty() -> EmptyView("القائمة فارغة. أضف أغاني من زر قائمة في أي شاشة.")
            else -> SongList(
                songs = list.map { it.toSong() },
                onPlay = onPlaySong,
                showDownloadButton = false,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 100.dp)
            )
        }
    }
}
