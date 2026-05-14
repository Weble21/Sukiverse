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

private const val PROVIDER = "kakao"

@Service(PROVIDER)
class KakaoAuthService(
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

        val json = httpClient.POST(
            "https://kauth.kakao.com/oauth/token",
            mapOf("Content-Type" to "application/x-www-form-urlencoded"),
            body,
        )
        val raw = JsonUtil.decodeFromJson<KakaoRawTokenResponse>(json)

        val token = raw.accessToken
            ?: throw IllegalStateException("Kakao 토큰 발급 실패: ${raw.errorDescription ?: raw.error ?: json}")

        return KakaoTokenResult(token)
    }

    override fun getUserInfo(accessToken: String): OAuth2UserResponse {
        val json = httpClient.GET(
            "https://kapi.kakao.com/v2/user/me",
            mapOf("Authorization" to "Bearer $accessToken"),
        )
        return JsonUtil.decodeFromJson<KakaoUserResponse>(json).toOAuth2UserResponse()
    }
}

data class KakaoRawTokenResponse(
    @JsonProperty("access_token") val accessToken: String? = null,
    @JsonProperty("error") val error: String? = null,
    @JsonProperty("error_description") val errorDescription: String? = null,
)

data class KakaoTokenResult(override val accessToken: String) : OAuth2TokenResponse

data class KakaoUserResponse(
    @JsonProperty("id") val kakaoId: Long,
    @JsonProperty("kakao_account") val kakaoAccount: KakaoAccount? = null,
)  {
    fun toOAuth2UserResponse() : OAuth2UserResponse = object : OAuth2UserResponse {
        override val id: String get() = kakaoId.toString()
        override val email: String? get() = kakaoAccount?.email
        override val name: String? get() = kakaoAccount?.profile?.nickname
        override val profileImage: String? get() = kakaoAccount?.profile?.profileImageUrl
    }
}

data class KakaoAccount(
    val email: String? = null,
    val profile: KakaoProfile? = null,
)

data class KakaoProfile(
    val nickname: String? = null,
    @JsonProperty("profile_image_url") val profileImageUrl: String? = null,
)
