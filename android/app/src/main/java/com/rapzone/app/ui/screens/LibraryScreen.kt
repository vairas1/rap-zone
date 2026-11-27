package com.rapzone.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rapzone.app.di.AppContainer
import com.rapzone.app.model.Song
import com.rapzone.app.ui.components.EmptyView
import com.rapzone.app.ui.components.SongList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Library: favorites / my playlists / downloads / history.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onPlaySong: (List<Song>, Int) -> Unit,
    onOpenLocalPlaylist: (String) -> Unit
) {
    val container = AppContainer.instance()!!
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val tabs = listOf("المفضلة", "قوائمي", "التحميلات", "السجل")
    var selectedTab by remember { mutableStateOf(0) }

    val favorites by remember { container.repository.favoritesFlow() }.collectAsState(initial = emptyList())
    val playlists by remember { container.repository.playlistsFlow() }.collectAsState(initial = emptyList())
    val downloads by remember { container.repository.downloadsFlow() }.collectAsState(initial = emptyList())
    val history by remember { container.repository.historyFlow() }.collectAsState(initial = emptyList())

    var newPlaylistVisible by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    LazyColumn(contentPadding = PaddingValues(bottom = 120.dp)) {
        item {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { i, t ->
                    Tab(
                        selected = selectedTab == i,
                        onClick = { selectedTab = i },
                        text = { Text(t) }
                    )
                }
            }
        }

        when (selectedTab) {
            0 -> {
                if (favorites.isEmpty()) {
                    item { EmptyView("لم تضف أي أغاني للمفضلة بعد") }
                } else {
                    item {
                        SongList(
                            songs = favorites.map { it.toSong() },
                            onPlay = onPlaySong,
                            showDownloadButton = false
                        )
                    }
                }
            }

            1 -> {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "قوائمك",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { newPlaylistVisible = true }) {
                            Icon(Icons.Default.Add, contentDescription = "قائمة جديدة")
                        }
                    }
                }
                if (playlists.isEmpty()) {
                    item { EmptyView("أنشئ قائمة جديدة وابدأ الترتيب") }
                } else {
                    items(playlists.size) { i ->
                        val p = playlists[i]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenLocalPlaylist(p.id) }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.PlaylistAdd,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                p.name,
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = {
                                scope.launch(Dispatchers.IO) {
                                    container.repository.deletePlaylist(p.id)
                                }
                                Toast.makeText(context, "تم حذف القائمة", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "حذف",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            2 -> {
                if (downloads.isEmpty()) {
                    item { EmptyView("لا توجد ملفات محملة بعد") }
                } else {
                    item {
                        Text(
                            "الملفات المحملة (متاحة دون إنترنت)",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    items(downloads.size) { i ->
                        val d = downloads[i]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Download,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(d.title, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    d.artistName ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = {
                                container.downloadManager.deleteDownload(d.songId)
                            }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "حذف الملف",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            3 -> {
                if (history.isEmpty()) {
                    item { EmptyView("لا يوجد سجل استماع بعد") }
                } else {
                    items(history.size) { i ->
                        val h = history[i]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(h.title, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    h.artistName ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                formatTime(h.playedAt),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    if (newPlaylistVisible) {
        AlertDialog(
            onDismissRequest = { newPlaylistVisible = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            scope.launch(Dispatchers.IO) {
                                container.repository.createPlaylist(newPlaylistName.trim())
                            }
                        }
                        newPlaylistVisible = false
                        newPlaylistName = ""
                    }
                ) {
                    Text("إنشاء")
                }
            },
            title = { Text("قائمة تشغيل جديدة") },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    label = { Text("اسم القائمة") },
                    singleLine = true
                )
            }
        )
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0) return ""
    return java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.getDefault())
        .format(java.util.Date(ms))
}
