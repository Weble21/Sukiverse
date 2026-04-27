package com.example.Sukiverse.auth

import com.example.Sukiverse.domain.user.User
import com.example.Sukiverse.domain.user.UserRepository
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service

@Service
class CustomOAuth2UserService(
    private val userRepository: UserRepository,
) : DefaultOAuth2UserService() {

    override fun loadUser(request: OAuth2UserRequest): OAuth2User {
        val oAuth2User = super.loadUser(request)
        val provider = request.clientRegistration.registrationId

        val attr = OAuthAttributes.of(provider, oAuth2User.attributes)

        userRepository.findByProviderAndProviderId(attr.provider, attr.providerId)
            ?: userRepository.save(
                User(
                    provider = attr.provider,
                    providerId = attr.providerId,
                    email = attr.email,
                    nickname = attr.name,
                    profileImageUrl = attr.profileImageUrl,
                )
            )

        return oAuth2User
    }
}
