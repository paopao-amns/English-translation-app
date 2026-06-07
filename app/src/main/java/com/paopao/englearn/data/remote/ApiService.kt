package com.paopao.englearn.data.remote

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

/**
 * Retrofit interface for OpenAI-compatible chat completions API.
 *
 * Uses @Url to support dynamic endpoints — users can configure any
 * OpenAI-compatible provider (OpenAI, DeepSeek, Qwen, GLM, etc.).
 */
interface ApiService {

    /**
     * Generic chat completion call. The path comes from the user's endpoint config.
     */
    @POST
    suspend fun chatCompletion(
        @Url path: String,
        @Body request: ChatCompletionRequest
    ): Response<ChatCompletionResponse>

    /**
     * Raw chat completion that returns ResponseBody — useful for
     * debugging or when the response format doesn't match ChatCompletionResponse.
     */
    @POST
    suspend fun chatCompletionRaw(
        @Url path: String,
        @Body request: ChatCompletionRequest
    ): Response<ResponseBody>
}
