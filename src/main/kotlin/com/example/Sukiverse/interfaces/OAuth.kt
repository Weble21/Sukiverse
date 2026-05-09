package com.example.Sukiverse.interfaces

interface OAuthServiceInterface {
    val providerName: String
    fun getToken(code: String): OAuth2TokenResponse
    fun getUserInfo(accessToken: String): OAuth2UserResponse
}

interface OAuth2TokenResponse {
    val accessToken: String
}

interface OAuth2UserResponse {
    val id: String
    val email: String?
    val name: String?
    val profileImage: String?
}
