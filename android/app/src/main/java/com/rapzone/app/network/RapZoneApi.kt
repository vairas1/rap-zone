package com.rapzone.app.network

import com.rapzone.app.model.Album
import com.rapzone.app.model.ArtistDetail
import com.rapzone.app.model.AuthRequest
import com.rapzone.app.model.AuthResponse
import com.rapzone.app.model.HomeFeed
import com.rapzone.app.model.Paged
import com.rapzone.app.model.Playlist
import com.rapzone.app.model.SearchResult
import com.rapzone.app.model.Song
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface RapZoneApi {
    @GET("home")
    suspend fun homeFeed(): HomeFeed

    @GET("home/search")
    suspend fun search(@retrofit2.http.Query("q") q: String, @retrofit2.http.Query("limit") limit: Int = 30): SearchResult

    @GET("artists/{id}")
    suspend fun artist(@Path("id") id: String): ArtistDetail

    @GET("albums/{id}")
    suspend fun album(@Path("id") id: String): Album

    @GET("playlists/{id}")
    suspend fun playlist(@Path("id") id: String): Playlist

    @GET("songs/popular")
    suspend fun popularSongs(@retrofit2.http.Query("limit") limit: Int = 20): Paged<Song>

    @GET("songs/latest")
    suspend fun latestSongs(@retrofit2.http.Query("limit") limit: Int = 20): Paged<Song>

    @GET("playlists")
    suspend fun publicPlaylists(): Paged<Playlist>

    @POST("auth/login")
    suspend fun login(@Body body: AuthRequest): AuthResponse

    @POST("auth/register")
    suspend fun register(@Body body: AuthRequest): AuthResponse

    @POST("songs/{id}/play")
    suspend fun recordPlay(@Path("id") id: String, @Body body: Map<String, Any?> = emptyMap())

    @POST("songs/{id}/download")
    suspend fun recordDownload(@Path("id") id: String)
}
