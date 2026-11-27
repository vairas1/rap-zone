package com.rapzone.app.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Insert
import androidx.room.OnConflictStrategy

/**
 * Local Room database: favorites, user playlists, downloads, play history.
 *
 * NOTE: column names default to the Kotlin property names (camelCase).
 */

@Entity(tableName = "local_favorites", indices = [Index("songId", unique = true)])
data class FavoriteEntity(
    @PrimaryKey val songId: String,
    val title: String,
    val artistName: String? = null,
    val albumTitle: String? = null,
    val imageUrl: String? = null,
    val audioUrl: String,
    val durationSec: Int = 0,
    val lyrics: String? = null,
    val addedAt: Long = 0
)

@Entity(tableName = "local_playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String? = null,
    val createdAt: Long = 0
)

@Entity(
    tableName = "local_playlist_songs",
    indices = [Index(value = ["playlistId", "songId"], unique = true)]
)
data class PlaylistSongEntity(
    @PrimaryKey(autoGenerate = true) val pk: Long? = null,
    val playlistId: String,
    val songId: String,
    val position: Int,
    val title: String,
    val artistName: String? = null,
    val albumTitle: String? = null,
    val audioUrl: String,
    val imageUrl: String? = null,
    val durationSec: Int = 0,
    val lyrics: String? = null
)

@Entity(tableName = "downloads", indices = [Index("songId", unique = true)])
data class DownloadEntity(
    @PrimaryKey val songId: String,
    val title: String,
    val artistName: String? = null,
    val localPath: String,
    val audioUrl: String,
    val addedAt: Long = 0
)

@Entity(tableName = "play_history", indices = [Index("songId")])
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val pk: Long? = null,
    val songId: String,
    val title: String,
    val artistName: String? = null,
    val imageUrl: String? = null,
    val playedAt: Long = 0
)

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM local_favorites ORDER BY addedAt DESC")
    fun observeAll(): kotlinx.coroutines.flow.Flow<List<FavoriteEntity>>

    @Query("SELECT COUNT(*) FROM local_favorites WHERE songId = :songId")
    suspend fun exists(songId: String): Int

    @Query("SELECT * FROM local_favorites WHERE songId = :songId")
    suspend fun find(songId: String): FavoriteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: FavoriteEntity)

    @Query("DELETE FROM local_favorites WHERE songId = :songId")
    suspend fun delete(songId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<FavoriteEntity>)
}

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM local_playlists ORDER BY createdAt DESC")
    fun observeAll(): kotlinx.coroutines.flow.Flow<List<PlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PlaylistEntity)

    @Query("DELETE FROM local_playlists WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface PlaylistSongDao {
    @Query("SELECT * FROM local_playlist_songs WHERE playlistId = :playlistId ORDER BY position ASC")
    fun songsFor(playlistId: String): kotlinx.coroutines.flow.Flow<List<PlaylistSongEntity>>

    @Query("SELECT COUNT(*) FROM local_playlist_songs WHERE playlistId = :playlistId")
    suspend fun countFor(playlistId: String): Int

    @Query("SELECT COUNT(*) FROM local_playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun exists(playlistId: String, songId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PlaylistSongEntity)

    @Query("DELETE FROM local_playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun remove(playlistId: String, songId: String)

    @Query("DELETE FROM local_playlist_songs WHERE playlistId = :playlistId")
    suspend fun removeForPlaylist(playlistId: String)

    @Query("SELECT MAX(position) FROM local_playlist_songs WHERE playlistId = :playlistId")
    suspend fun maxPosition(playlistId: String): Int?
}

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY addedAt DESC")
    fun observeAll(): kotlinx.coroutines.flow.Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE songId = :songId")
    suspend fun find(songId: String): DownloadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DownloadEntity)

    @Query("DELETE FROM downloads WHERE songId = :songId")
    suspend fun delete(songId: String)
}

@Dao
interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: HistoryEntity)

    @Query("SELECT * FROM play_history ORDER BY playedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): kotlinx.coroutines.flow.Flow<List<HistoryEntity>>
}

@Database(
    entities = [
        FavoriteEntity::class,
        PlaylistEntity::class,
        PlaylistSongEntity::class,
        DownloadEntity::class,
        HistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class RapDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun playlistSongDao(): PlaylistSongDao
    abstract fun downloadDao(): DownloadDao
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile
        private var INSTANCE: RapDatabase? = null

        fun getInstance(androidContext: android.content.Context): RapDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: androidx.room.Room.databaseBuilder(
                    androidContext.applicationContext,
                    RapDatabase::class.java,
                    "rapzone.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
    }
}
