package com.paopao.englearn.data.remote

import com.paopao.englearn.data.preferences.SettingsDataStore
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Provides Retrofit ApiService instances and the API path.
 *
 * Architecture: Retrofit baseUrl is set to the scheme+host (e.g., "https://api.openai.com/").
 * The path (e.g., "v1/chat/completions") is injected into ApiService calls via @Url.
 * This allows users to configure any OpenAI-compatible endpoint with any path structure.
 */
object NetworkModule {

    private const val TIMEOUT_SECONDS = 60L

    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    /**
     * Holds the base URL and API path, derived from the user's configured endpoint.
     */
    data class ApiConfig(
        val baseUrl: String,
        val path: String
    )

    /**
     * Parse the user-configured endpoint into base URL + path.
     * E.g., "https://api.openai.com/v1/chat/completions"
     *   → baseUrl = "https://api.openai.com/"
     *   → path = "v1/chat/completions"
     */
    fun parseEndpoint(endpoint: String): ApiConfig {
        return try {
            val url = java.net.URL(endpoint)
            val portPart = if (url.port > 0 && url.port != 80 && url.port != 443) ":${url.port}" else ""
            val base = "${url.protocol}://${url.host}$portPart/"
            val path = url.path.trimStart('/')
            ApiConfig(baseUrl = base, path = path)
        } catch (e: Exception) {
            // Fallback for malformed URLs
            ApiConfig(baseUrl = endpoint.trimEnd('/') + "/", path = "v1/chat/completions")
        }
    }

    /**
     * Creates an ApiService configured from DataStore settings.
     */
    suspend fun createApiService(settingsDataStore: SettingsDataStore): Pair<ApiService, ApiConfig> {
        val settings = settingsDataStore.settingsFlow.first()
        val apiConfig = parseEndpoint(settings.apiEndpoint)

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val authInterceptor = okhttp3.Interceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer ${settings.apiKey}")
                .addHeader("Content-Type", "application/json")
                .build()
            chain.proceed(request)
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(apiConfig.baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

        return Pair(retrofit.create(ApiService::class.java), apiConfig)
    }
}
