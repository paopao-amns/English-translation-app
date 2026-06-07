package com.paopao.englearn.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.paopao.englearn.EngLearnApp
import com.paopao.englearn.data.preferences.SettingsDataStore
import com.paopao.englearn.data.remote.ChatCompletionRequest
import com.paopao.englearn.data.remote.ChatMessage
import com.paopao.englearn.data.remote.NetworkModule
import com.paopao.englearn.domain.model.PreloadStrategy
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * Provider presets — matching the Chrome extension's options.js PROVIDERS constant.
 */
data class ProviderPreset(
    val id: String,
    val name: String,
    val endpoint: String,
    val defaultModel: String
)

val PROVIDER_PRESETS = listOf(
    ProviderPreset("openai", "OpenAI", "https://api.openai.com/v1/chat/completions", "gpt-4o-mini"),
    ProviderPreset("deepseek", "DeepSeek", "https://api.deepseek.com/v1/chat/completions", "deepseek-chat"),
    ProviderPreset("qwen", "通义千问", "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions", "qwen-plus"),
    ProviderPreset("glm", "智谱 GLM", "https://open.bigmodel.cn/api/paas/v4/chat/completions", "glm-4-flash"),
    ProviderPreset("kimi", "Kimi (月之暗面)", "https://api.moonshot.cn/v1/chat/completions", "moonshot-v1-8k"),
    ProviderPreset("mimo", "小米 MiMo", "https://api.xiaomimimo.com/v1/chat/completions", "MiMo-V2.5"),
    ProviderPreset("siliconflow", "硅基流动", "https://api.siliconflow.cn/v1/chat/completions", "Qwen/Qwen2.5-7B-Instruct"),
    ProviderPreset("custom", "自定义", "", "")
)

data class SettingsUiState(
    val endpoint: String = SettingsDataStore.DEFAULT_ENDPOINT,
    val apiKey: String = "",
    val modelName: String = SettingsDataStore.DEFAULT_MODEL,
    val provider: String = "openai",
    val preloadStrategy: PreloadStrategy = PreloadStrategy.HYBRID,
    val darkMode: String = "system",
    val isTesting: Boolean = false,
    val testResult: String? = null,
    val testSuccess: Boolean = false,
    val isSaved: Boolean = false
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsDataStore = (application as EngLearnApp).settingsDataStore

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsDataStore.settingsFlow.collect { settings ->
                _uiState.update { current ->
                    current.copy(
                        endpoint = settings.apiEndpoint,
                        apiKey = settings.apiKey,
                        modelName = settings.modelName,
                        provider = settings.provider,
                        preloadStrategy = settings.preloadStrategy,
                        darkMode = settings.darkMode,
                        isSaved = false,
                        testResult = null
                    )
                }
            }
        }
    }

    fun updateEndpoint(value: String) {
        _uiState.update { it.copy(endpoint = value, isSaved = false, testResult = null) }
    }

    fun updateApiKey(value: String) {
        _uiState.update { it.copy(apiKey = value, isSaved = false, testResult = null) }
    }

    fun updateModelName(value: String) {
        _uiState.update { it.copy(modelName = value, isSaved = false, testResult = null) }
    }

    fun selectProvider(preset: ProviderPreset) {
        _uiState.update {
            it.copy(
                provider = preset.id,
                endpoint = preset.endpoint.ifBlank { it.endpoint },
                modelName = preset.defaultModel.ifBlank { it.modelName },
                isSaved = false,
                testResult = null
            )
        }
    }

    fun updatePreloadStrategy(strategy: PreloadStrategy) {
        _uiState.update { it.copy(preloadStrategy = strategy, isSaved = false) }
    }

    fun updateDarkMode(mode: String) {
        _uiState.update { it.copy(darkMode = mode, isSaved = false) }
    }

    fun saveSettings() {
        viewModelScope.launch {
            val state = _uiState.value
            settingsDataStore.saveAll(
                SettingsDataStore.AppSettings(
                    apiEndpoint = state.endpoint,
                    apiKey = state.apiKey,
                    modelName = state.modelName,
                    provider = state.provider,
                    preloadStrategy = state.preloadStrategy,
                    darkMode = state.darkMode
                )
            )
            _uiState.update { it.copy(isSaved = true) }
        }
    }

    fun testConnection() {
        viewModelScope.launch {
            _uiState.update { it.copy(isTesting = true, testResult = null) }

            // Save first, then test
            saveSettings()

            try {
                val (apiService, apiConfig) = NetworkModule.createApiService(settingsDataStore)

                val request = ChatCompletionRequest(
                    model = _uiState.value.modelName,
                    messages = listOf(
                        ChatMessage(role = "system", content = "You are a helpful assistant."),
                        ChatMessage(role = "user", content = "Say 'OK' if you can read this.")
                    ),
                    temperature = 0.0
                )

                val response = withTimeout(15_000) {
                    apiService.chatCompletion(apiConfig.path, request)
                }

                if (response.isSuccessful) {
                    val content = response.body()?.choices?.firstOrNull()?.message?.content
                    if (content != null) {
                        _uiState.update {
                            it.copy(
                                isTesting = false,
                                testResult = "连接成功！AI 回复：${content.take(100)}",
                                testSuccess = true
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isTesting = false,
                                testResult = "API 返回了空内容，请检查模型名称是否正确",
                                testSuccess = false
                            )
                        }
                    }
                } else {
                    val errorMsg = when (response.code()) {
                        401, 403 -> "API Key 无效或未授权 (${response.code()})"
                        404 -> "API 端点未找到 (404)，请确认 URL 是否正确"
                        429 -> "请求过于频繁，请稍后重试 (429)"
                        else -> "请求失败 (${response.code()}): ${response.errorBody()?.string()?.take(200) ?: ""}"
                    }
                    _uiState.update {
                        it.copy(isTesting = false, testResult = errorMsg, testSuccess = false)
                    }
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                _uiState.update {
                    it.copy(
                        isTesting = false,
                        testResult = "连接超时（15秒），请检查网络或端点地址",
                        testSuccess = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isTesting = false,
                        testResult = "连接失败：${e.localizedMessage ?: "未知错误"}",
                        testSuccess = false
                    )
                }
            }
        }
    }

    fun clearTestResult() {
        _uiState.update { it.copy(testResult = null) }
    }
}
