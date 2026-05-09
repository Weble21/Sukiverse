package com.example.Sukiverse.types.dto

data class ErrorDto(
    val code: String,
    val message: String,
)

data class ApiResponse<T>(
    val isSuccess: Boolean,
    val responseDto: T?,
    val error: ErrorDto?,
) {
    companion object {
        fun <T> success(data: T): ApiResponse<T> =
            ApiResponse(isSuccess = true, responseDto = data, error = null)

        fun <T> error(code: String, message: String): ApiResponse<T> =
            ApiResponse(isSuccess = false, responseDto = null, error = ErrorDto(code, message))
    }
}
