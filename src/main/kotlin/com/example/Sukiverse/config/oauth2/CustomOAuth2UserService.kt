package com.example.Sukiverse.config.oauth2

import com.example.Sukiverse.domain.user.User
import com.example.Sukiverse.domain.user.UserRepository
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service

@Service
class CustomOAuth2UserService(private val userRepository: UserRepository,) : DefaultOAuth2UserService() {

    override fun loadUser(userRequest: OAuth2UserRequest): OAuth2User {
        val oAuth2User = super.loadUser(userRequest)
        val userInfo = OAuth2UserInfo.of(
            userRequest.clientRegistration.registrationId,
            oAuth2User.attributes,
        )

        val user = userRepository.findByProviderAndProviderId(userInfo.provider, userInfo.providerId)
            ?: userRepository.save(
                User(
                    provider = userInfo.provider,
                    providerId = userInfo.providerId,
                    email = userInfo.email,
                    nickname = userInfo.name,
                    profileImageUrl = userInfo.profileImageUrl,
                )
            )

        if (user.nickname != userInfo.name || user.profileImageUrl != userInfo.profileImageUrl) {
            user.nickname = userInfo.name
            user.profileImageUrl = userInfo.profileImageUrl
            userRepository.save(user)
        }

        return oAuth2User
    }
}
