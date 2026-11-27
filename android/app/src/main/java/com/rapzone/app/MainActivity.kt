package com.rapzone.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rapzone.app.di.AppContainer
import com.rapzone.app.model.Song
import com.rapzone.app.network.AppConfig
import com.rapzone.app.playback.PlaybackManager
import com.rapzone.app.ui.components.MiniPlayer
import com.rapzone.app.ui.home.HomeScreen
import com.rapzone.app.ui.screens.AlbumScreen
import com.rapzone.app.ui.screens.ArtistScreen
import com.rapzone.app.ui.screens.LibraryScreen
import com.rapzone.app.ui.screens.LocalPlaylistScreen
import com.rapzone.app.ui.screens.PlayerScreen
import com.rapzone.app.ui.screens.PlaylistScreen
import com.rapzone.app.ui.screens.SearchScreen
import com.rapzone.app.ui.screens.SettingsScreen
import com.rapzone.app.ui.theme.RapZoneTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Ask for notification permission (Android 13+) for media notifications
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                NOTIF_PERMISSION_REQ
            )
        }

        setContent {
            val context = LocalContext.current
            val dark = remember { mutableStateOf(AppConfig.isDarkTheme(context)) }

            RapZoneTheme(darkTheme = dark.value) {
                MainApp(onDarkModeChanged = { dark.value = it })
            }
        }
    }

    companion object {
        private const val NOTIF_PERMISSION_REQ = 100
    }
}

// ---------- Root composable ----------

private data class NavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MainApp(onDarkModeChanged: (Boolean) -> Unit) {
    val container = AppContainer.instance()
    if (container == null) {
        Box(modifier = Modifier.fillMaxSize()) {
            Text("جاري التحميل...")
        }
        return
    }
    val playback = container.playback
    val state by playback.state.collectAsState()

    val navController = rememberNavController()
    var showPlayer by remember { mutableStateOf(false) }
    var bottomTab by remember { mutableStateOf(0) }

    val items = listOf(
        NavItem("home", "الرئيسية", Icons.Filled.Home),
        NavItem("search", "بحث", Icons.Filled.Search),
        NavItem("library", "مكتبتي", Icons.Filled.LibraryMusic),
        NavItem("settings", "الإعدادات", Icons.Filled.Settings)
    )

    fun playAt(songs: List<Song>, index: Int) {
        playback.playQueue(songs, index)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                Column {
                    if (state.currentSong != null) {
                        MiniPlayer(onOpenPlayer = { showPlayer = true })
                    }
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        items.forEachIndexed { index, item ->
                            NavigationBarItem(
                                selected = bottomTab == index,
                                onClick = {
                                    bottomTab = index
                                    val route = items[index].route
                                    navController.navigate(route) {
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(item.icon, contentDescription = item.label)
                                },
                                label = { Text(item.label, maxLines = 1) }
                            )
                        }
                    }
                }
            }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                composable("home") {
                    HomeScreen(
                        onOpenArtist = { navController.navigate("artist/$it") },
                        onOpenAlbum = { navController.navigate("album/$it") },
                        onOpenPlaylist = { navController.navigate("playlist/$it") },
                        onPlaySong = { songs, i -> playAt(songs, i) }
                    )
                }
                composable("search") {
                    SearchScreen(
                        onOpenArtist = { navController.navigate("artist/$it") },
                        onOpenAlbum = { navController.navigate("album/$it") },
                        onPlaySong = { songs, i -> playAt(songs, i) }
                    )
                }
                composable("library") {
                    LibraryScreen(
                        onPlaySong = { songs, i -> playAt(songs, i) },
                        onOpenLocalPlaylist = { navController.navigate("local_playlist/$it") }
                    )
                }
                composable("settings") {
                    SettingsScreen(
                        onBack = { navController.popBackStack() },
                        onDarkModeChange = onDarkModeChanged
                    )
                }
                composable("artist/{id}") { backStack ->
                    val id = backStackArgument(backStack)
                    ArtistScreen(
                        artistId = id,
                        onBack = { navController.popBackStack() },
                        onOpenAlbum = { navController.navigate("album/$it") },
                        onPlaySong = { songs, i -> playAt(songs, i) }
                    )
                }
                composable("album/{id}") { backStack ->
                    val id = backStackArgument(backStack)
                    AlbumScreen(
                        albumId = id,
                        onBack = { navController.popBackStack() },
                        onOpenArtist = { navController.navigate("artist/$it") },
                        onPlaySong = { songs, i -> playAt(songs, i) }
                    )
                }
                composable("playlist/{id}") { backStack ->
                    val id = backStackArgument(backStack)
                    PlaylistScreen(
                        playlistId = id,
                        onBack = { navController.popBackStack() },
                        onPlaySong = { songs, i -> playAt(songs, i) }
                    )
                }
                composable("local_playlist/{id}") { backStack ->
                    val id = backStackArgument(backStack)
                    LocalPlaylistScreen(
                        playlistId = id,
                        onBack = { navController.popBackStack() },
                        onPlaySong = { songs, i -> playAt(songs, i) }
                    )
                }
            }
        }

        // Full-screen player overlay
        if (showPlayer) {
            PlayerScreen(onBack = { showPlayer = false })
        }
    }
}

@Composable
fun backStackArgument(backStack: NavBackStackEntry): String {
    return backStack.arguments?.getString("id") ?: ""
}
