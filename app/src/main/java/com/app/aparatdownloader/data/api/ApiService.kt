package com.app.aparatdownloader.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

data class LoginRequest(
    val phone: String,
    val password: String,
    val device_id: String,
    val model: String? = null
)

data class License(val payload: String, val sig: String)
data class LoginResponse(val token: String, val license: License, val server_time: Long)

data class Playlist(
    val id: String,
    val title: String,
    val cover_url: String?,
    val sort_order: Int?,
    val video_count: Int?
)

data class VideoItem(
    val id: String,
    val title: String,
    val position: Int,
    val duration_sec: Int?,
    val size_bytes: Long?,
    val version: Int,
    val source_type: String?
)

data class PlaylistDetail(val id: String, val videos: List<VideoItem>)
data class VideoSource(val url: String, val type: String?, val aparat_hash: String?, val version: Int)
data class VideoKey(val key: String)
data class MeResponse(
    val full_name: String?,
    val phone: String?,
    val national_id_mask: String?,
    val expires_at: Long?,
    val offline_days: Int?,
    val device_model: String?,
    val device_registered_at: Long?,
    val server_time: Long?
)

interface ApiService {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("license/refresh")
    suspend fun refreshLicense(): Response<LoginResponse>

    @GET("me")
    suspend fun getMe(): Response<MeResponse>

    @GET("playlists")
    suspend fun getPlaylists(): Response<List<Playlist>>

    @GET("playlists/{id}")
    suspend fun getPlaylistVideos(@Path("id") id: String): Response<PlaylistDetail>

    @GET("videos/{id}/source")
    suspend fun getVideoSource(@Path("id") id: String): Response<VideoSource>

    @GET("videos/{id}/key")
    suspend fun getVideoKey(@Path("id") id: String): Response<VideoKey>
}
