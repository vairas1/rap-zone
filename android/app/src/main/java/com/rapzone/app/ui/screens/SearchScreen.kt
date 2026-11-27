package com.rapzone.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.rapzone.app.di.AppContainer
import com.rapzone.app.model.SearchResult
import com.rapzone.app.model.Song
import com.rapzone.app.ui.components.AlbumCard
import com.rapzone.app.ui.components.ArtistCard
import com.rapzone.app.ui.components.EmptyView
import com.rapzone.app.ui.components.ErrorView
import com.rapzone.app.ui.components.LoadingRow
import com.rapzone.app.ui.components.SectionHeader
import com.rapzone.app.ui.components.SongList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SearchScreen(
    onOpenArtist: (String) -> Unit,
    onOpenAlbum: (String) -> Unit,
    onPlaySong: (List<Song>, Int) -> Unit
) {
    val container = AppContainer.instance()!!
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<SearchResult?>(null) }
    var searching by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    var searchJob: Job? = null

    fun runSearch(q: String) {
        searchJob?.cancel()
        searchJob = scope.launch {
            searching = true
            error = null
            results = null
            try {
                results = withContext(Dispatchers.IO) { container.repository.search(q) }
            } catch (e: Exception) {
                error = "تعذر البحث: ${e.message}"
            } finally {
                searching = false
            }
        }
    }

    LazyColumn(contentPadding = PaddingValues(bottom = 120.dp)) {
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { q ->
                    query = q
                    runSearch(q)
                },
                placeholder = { Text("ابحث عن فنانين، ألبومات، أو أغاني...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "بحث")
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }

        when {
            searching -> item { LoadingRow() }
            error != null -> item {
                ErrorView(error!!, onRetry = { runSearch(query) })
            }
            results != null -> {
                val r = results!!
                if (r.artists.isEmpty() && r.albums.isEmpty() && r.songs.isEmpty()) {
                    item { EmptyView("لا توجد نتائج لـ \"$r.query\"") }
                } else {
                    if (r.artists.isNotEmpty()) {
                        item { SectionHeader("الفنانون") }
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(r.artists.size) { i ->
                                    Box(modifier = Modifier.width(132.dp)) {
                                        ArtistCard(artist = r.artists[i]) {
                                            onOpenArtist(r.artists[i].id)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (r.albums.isNotEmpty()) {
                        item { SectionHeader("الألبومات") }
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(r.albums.size) { i ->
                                    Box(modifier = Modifier.width(150.dp)) {
                                        AlbumCard(album = r.albums[i]) {
                                            onOpenAlbum(r.albums[i].id)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (r.songs.isNotEmpty()) {
                        item { SectionHeader("الأغاني") }
                        item {
                            SongList(songs = r.songs, onPlay = onPlaySong, showDownloadButton = true)
                        }
                    }
                }
            }
            query.isBlank() -> item {
                EmptyView("ابدأ البحث للعثور على الموسيقى")
            }
        }
    }
}
