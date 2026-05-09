package com.example.Sukiverse.exception

interface CodeInterface {
    val httpStatus: Int
    val errorCode: String
    var message: String
}

enum class ErrorCode(
    override val httpStatus: Int,
    override val errorCode: String,
    override var message: String,
) : CodeInterface {
    AUTH_CONFIG_NOT_FOUND(400, "AUTH_100", "auth config not found"),
    PROVIDER_NOT_FOUND(400, "AUTH_401", "Bad Request: Unsupported OAuth Provider"),
    TOKEN_IS_INVALID(401, "AUTH_104", "token invalid"),
    TOKEN_IS_EXPIRED(401, "AUTH_105", "token expired"),
    ACCESS_TOKEN_NEED(401, "AUTH_118", "access token needed"),
}
