package com.paopao.englearn.util

/**
 * Generic result wrapper for repository operations.
 * Used to propagate loading/success/error states through the ViewModel layer.
 */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val exception: Throwable? = null) : Result<Nothing>()
    data object Loading : Result<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
    val isLoading: Boolean get() = this is Loading

    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }

    fun errorOrNull(): String? = when (this) {
        is Error -> message
        else -> null
    }

    companion object {
        fun <T> success(data: T): Result<T> = Success(data)
        fun error(message: String, exception: Throwable? = null): Result<Nothing> =
            Error(message, exception)
        fun loading(): Result<Nothing> = Loading
    }
}
