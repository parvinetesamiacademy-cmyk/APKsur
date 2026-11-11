package com.app.aparatdownloader.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

data class LoginRequest(val phone: String, val password: String, val device_hash: String)
data class LoginResponse(val token: String, val license: String)
data class Playlist(val id: String, val title: String, val cover_url: String?)
data class Video(val id: String, val playlist_id: String, val title: String, val position: Int, val source_url: String, val version: Int)

interface ApiService {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("license/refresh")
    suspend fun refreshLicense(): Response<Map<String, String>>

    @GET("playlists")
    suspend fun getPlaylists(): Response<List<Playlist>>

    @GET("playlists/{id}")
    suspend fun getPlaylistVideos(@Path("id") id: String): Response<List<Video>>
}
