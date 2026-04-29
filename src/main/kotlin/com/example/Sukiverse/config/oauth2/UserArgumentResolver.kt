package com.example.Sukiverse.config.oauth2

import com.example.Sukiverse.domain.user.Provider

data class UserArgumentResolver(
    val provider: Provider,
    val providerId: String,
    val email: String,
    val name: String,
    val profileImageUrl: String?,
) {
    companion object {
        fun of(provider: String, attributes: Map<String, Any>): UserArgumentResolver {
            return when (provider) {
                "google" -> ofGoogle(attributes)
                "kakao"  -> ofKakao(attributes)
                "naver"  -> ofNaver(attributes)
                else -> throw IllegalArgumentException("지원하지 않는 provider: $provider")
            }
        }

        private fun ofGoogle(attr: Map<String, Any>) = UserArgumentResolver(
            provider = Provider.GOOGLE,
            providerId = attr["sub"] as String,
            email = attr["email"] as String,
            name = attr["name"] as String,
            profileImageUrl = attr["picture"] as? String,
        )

        @Suppress("UNCHECKED_CAST")
        private fun ofKakao(attr: Map<String, Any>): UserArgumentResolver {
            val account = attr["kakao_account"] as Map<String, Any>
            val profile = account["profile"] as Map<String, Any>
            return UserArgumentResolver(
                provider = Provider.KAKAO,
                providerId = attr["id"].toString(),
                email = account["email"] as String,
                name = profile["nickname"] as String,
                profileImageUrl = profile["profile_image_url"] as? String,
            )
        }

        @Suppress("UNCHECKED_CAST")
        private fun ofNaver(attr: Map<String, Any>): UserArgumentResolver {
            val response = attr["response"] as Map<String, Any>
            return UserArgumentResolver(
                provider = Provider.NAVER,
                providerId = response["id"] as String,
                email = response["email"] as String,
                name = response["name"] as String,
                profileImageUrl = response["profile_image"] as? String,
            )
        }
    }
}
