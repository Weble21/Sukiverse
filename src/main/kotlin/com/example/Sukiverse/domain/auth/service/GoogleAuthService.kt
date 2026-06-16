package com.example.Sukiverse.domain.auth.service

import com.example.Sukiverse.common.httpClient.CallClient
import com.example.Sukiverse.common.json.JsonUtil
import com.example.Sukiverse.config.OAuth2Config
import com.example.Sukiverse.exception.CustomException
import com.example.Sukiverse.exception.ErrorCode
import com.example.Sukiverse.interfaces.OAuth2TokenResponse
import com.example.Sukiverse.interfaces.OAuth2UserResponse
import com.example.Sukiverse.interfaces.OAuthServiceInterface
import com.fasterxml.jackson.annotation.JsonProperty
import okhttp3.FormBody
import org.springframework.stereotype.Service
import java.net.URLDecoder

private const val PROVIDER = "google"

@Service(PROVIDER)
class GoogleAuthService(
    private val config: OAuth2Config,
    private val httpClient: CallClient,
) : OAuthServiceInterface {

    private val oAuthInfo by lazy {
        config.providers[PROVIDER] ?: throw CustomException(ErrorCode.AUTH_CONFIG_NOT_FOUND, PROVIDER)
    }

    override val providerName = PROVIDER

    override fun getToken(code: String): OAuth2TokenResponse {
        val body = FormBody.Builder()
            .add("code", URLDecoder.decode(code, "UTF-8"))
            .add("client_id", oAuthInfo.clientId)
            .add("client_secret", oAuthInfo.clientSecret)
            .add("redirect_uri", oAuthInfo.redirectUri)
            .add("grant_type", "authorization_code")
            .build()

        val json = httpClient.POST("https://oauth2.googleapis.com/token", mapOf("Accept" to "application/json"), body)
        return JsonUtil.decodeFromJson<GoogleTokenResponse>(json)
    }

    override fun getUserInfo(accessToken: String): OAuth2UserResponse {
        val json = httpClient.GET(
            "https://www.googleapis.com/oauth2/v2/userinfo",
            mapOf("Authorization" to "Bearer $accessToken"),
        )
        return JsonUtil.decodeFromJson<GoogleUserResponse>(json)
    }
}

data class GoogleTokenResponse(
    @JsonProperty("access_token") override val accessToken: String,
) : OAuth2TokenResponse

data class GoogleUserResponse(
    override val id: String,
    override val email: String,
    override val name: String,
    @JsonProperty("picture") override val profileImage: String? = null,
) : OAuth2UserResponse
