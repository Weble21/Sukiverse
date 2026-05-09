package com.example.Sukiverse.exception

class CustomException(
    private val codeInterface: CodeInterface,
    private val detail: String? = null,
) : RuntimeException(
    if (detail == null) codeInterface.message else "${codeInterface.message}: $detail"
) {
    fun getCodeInterface(): CodeInterface = codeInterface
    fun getErrorCode(): String = codeInterface.errorCode
    fun getHttpStatus(): Int = codeInterface.httpStatus
}
