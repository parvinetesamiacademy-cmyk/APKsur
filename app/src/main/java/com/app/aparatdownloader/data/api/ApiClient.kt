package com.app.aparatdownloader.data.api

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class AuthInterceptor(private val getToken: () -> String?) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val token = getToken()
        val request = if (!token.isNullOrEmpty()) {
            original.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            original
        }
        return chain.proceed(request)
    }
}

object ApiClient {
    private const val BASE_URL = "https://vidapp-api.parvinetesamiacademy.workers.dev/"

    fun create(getToken: () -> String?): ApiService {
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(getToken))
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(ApiService::class.java)
    }

    /** پاسخ خطای سرور به شکل {"error":"CODE"} است */
    fun errorCodeOf(body: String?): String? = try {
        if (body.isNullOrBlank()) null else JSONObject(body).optString("error").ifEmpty { null }
    } catch (e: Exception) {
        null
    }
}
