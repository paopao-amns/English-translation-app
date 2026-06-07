package com.paopao.englearn.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.paopao.englearn.domain.model.PreloadStrategy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Manages user settings via DataStore Preferences.
 * Mirrors the Chrome extension's chrome.storage.sync functionality.
 */
class SettingsDataStore(private val context: Context) {

    companion object {
        // Keys
        private val KEY_API_ENDPOINT = stringPreferencesKey("api_endpoint")
        private val KEY_API_KEY = stringPreferencesKey("api_key")
        private val KEY_MODEL_NAME = stringPreferencesKey("model_name")
        private val KEY_PROVIDER = stringPreferencesKey("provider")
        private val KEY_PRELOAD_STRATEGY = stringPreferencesKey("preload_strategy")
        private val KEY_DARK_MODE = stringPreferencesKey("dark_mode")

        // Default values (matching Chrome extension)
        const val DEFAULT_ENDPOINT = "https://api.openai.com/v1/chat/completions"
        const val DEFAULT_MODEL = "gpt-4o-mini"
        const val DEFAULT_PROVIDER = "openai"
        const val DEFAULT_PRELOAD_STRATEGY = "hybrid"
        const val DEFAULT_DARK_MODE = "system"  // "system", "light", "dark"
    }

    /**
     * Full settings as a data class for easy consumption.
     */
    data class AppSettings(
        val apiEndpoint: String = DEFAULT_ENDPOINT,
        val apiKey: String = "",
        val modelName: String = DEFAULT_MODEL,
        val provider: String = DEFAULT_PROVIDER,
        val preloadStrategy: PreloadStrategy = PreloadStrategy.HYBRID,
        val darkMode: String = DEFAULT_DARK_MODE
    )

    /**
     * Observe all settings as a Flow.
     */
    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            apiEndpoint = prefs[KEY_API_ENDPOINT] ?: DEFAULT_ENDPOINT,
            apiKey = prefs[KEY_API_KEY] ?: "",
            modelName = prefs[KEY_MODEL_NAME] ?: DEFAULT_MODEL,
            provider = prefs[KEY_PROVIDER] ?: DEFAULT_PROVIDER,
            preloadStrategy = parsePreloadStrategy(prefs[KEY_PRELOAD_STRATEGY] ?: DEFAULT_PRELOAD_STRATEGY),
            darkMode = prefs[KEY_DARK_MODE] ?: DEFAULT_DARK_MODE
        )
    }

    suspend fun saveEndpoint(endpoint: String) {
        context.dataStore.edit { it[KEY_API_ENDPOINT] = endpoint }
    }

    suspend fun saveApiKey(apiKey: String) {
        context.dataStore.edit { it[KEY_API_KEY] = apiKey }
    }

    suspend fun saveModel(model: String) {
        context.dataStore.edit { it[KEY_MODEL_NAME] = model }
    }

    suspend fun saveProvider(provider: String) {
        context.dataStore.edit { it[KEY_PROVIDER] = provider }
    }

    suspend fun savePreloadStrategy(strategy: String) {
        context.dataStore.edit { it[KEY_PRELOAD_STRATEGY] = strategy }
    }

    suspend fun saveDarkMode(mode: String) {
        context.dataStore.edit { it[KEY_DARK_MODE] = mode }
    }

    /**
     * Save all settings at once.
     */
    suspend fun saveAll(settings: AppSettings) {
        context.dataStore.edit { prefs ->
            prefs[KEY_API_ENDPOINT] = settings.apiEndpoint
            prefs[KEY_API_KEY] = settings.apiKey
            prefs[KEY_MODEL_NAME] = settings.modelName
            prefs[KEY_PROVIDER] = settings.provider
            prefs[KEY_PRELOAD_STRATEGY] = settings.preloadStrategy.name.lowercase()
            prefs[KEY_DARK_MODE] = settings.darkMode
        }
    }

    /**
     * Check if API key is configured.
     */
    suspend fun hasApiKey(): Boolean {
        var hasKey = false
        context.dataStore.edit { prefs ->
            hasKey = !prefs[KEY_API_KEY].isNullOrBlank()
        }
        return hasKey
    }

    private fun parsePreloadStrategy(value: String): PreloadStrategy {
        return when (value.lowercase()) {
            "individual" -> PreloadStrategy.INDIVIDUAL
            "batch" -> PreloadStrategy.BATCH
            "hybrid" -> PreloadStrategy.HYBRID
            else -> PreloadStrategy.HYBRID
        }
    }
}
