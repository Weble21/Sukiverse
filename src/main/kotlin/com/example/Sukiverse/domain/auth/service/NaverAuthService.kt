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

private const val PROVIDER = "naver"

@Service(PROVIDER)
class NaverAuthService(
    private val config: OAuth2Config,
    private val httpClient: CallClient,
) : OAuthServiceInterface {

    private val oAuthInfo by lazy {
        config.providers[PROVIDER] ?: throw CustomException(ErrorCode.AUTH_CONFIG_NOT_FOUND, PROVIDER)
    }

    override val providerName = PROVIDER

    override fun getToken(code: String): OAuth2TokenResponse {
        val body = FormBody.Builder()
            .add("code", code)
            .add("client_id", oAuthInfo.clientId)
            .add("client_secret", oAuthInfo.clientSecret)
            .add("redirect_uri", oAuthInfo.redirectUri)
            .add("grant_type", "authorization_code")
            .build()

        val json = httpClient.POST("https://nid.naver.com/oauth2.0/token", mapOf("Accept" to "application/json"), body)
        val raw = JsonUtil.decodeFromJson<NaverRawTokenResponse>(json)

        val token = raw.accessToken
            ?: throw IllegalStateException("Naver 토큰 발급 실패: ${raw.errorDescription ?: raw.error ?: json}")

        return NaverTokenResult(token)
    }

    override fun getUserInfo(accessToken: String): OAuth2UserResponse {
        val json = httpClient.GET(
            "https://openapi.naver.com/v1/nid/me",
            mapOf("Authorization" to "Bearer $accessToken"),
        )
        val wrapper = JsonUtil.decodeFromJson<NaverUserWrapper>(json)
        return wrapper.response
    }
}

// 카카오/네이버는 에러 시 access_token 없이 응답하므로 nullable로 파싱
data class NaverRawTokenResponse(
    @JsonProperty("access_token") val accessToken: String? = null,
    @JsonProperty("error") val error: String? = null,
    @JsonProperty("error_description") val errorDescription: String? = null,
)

data class NaverTokenResult(override val accessToken: String) : OAuth2TokenResponse

data class NaverUserWrapper(val response: NaverUserResponse)

data class NaverUserResponse(
    override val id: String,
    override val email: String?,
    override val name: String?,
    @JsonProperty("profile_image") override val profileImage: String? = null,
) : OAuth2UserResponse
