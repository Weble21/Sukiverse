package com.example.Sukiverse.config.oauth2

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.stereotype.Component

enum class CustomOAuth2Provider {
    KAKAO {
        override fun getBuilder(registrationId: String) =
            getBuilder(registrationId, ClientAuthenticationMethod.CLIENT_SECRET_POST, DEFAULT_LOGIN_REDIRECT_URL)
                .scope("profile")
                .authorizationUri("https://kauth.kakao.com/oauth/authorize")
                .tokenUri("https://kauth.kakao.com/oauth/token")
                .userInfoUri("https://kapi.kakao.com/v1/user/me")
                .userNameAttributeName("id")
                .clientName("Kakao")
    },
    NAVER {
        override fun getBuilder(registrationId: String) =
            getBuilder(registrationId, ClientAuthenticationMethod.CLIENT_SECRET_POST, DEFAULT_LOGIN_REDIRECT_URL)
                .scope("profile")
                .authorizationUri("https://nid.naver.com/oauth2.0/authorize")
                .tokenUri("https://nid.naver.com/oauth2.0/token")
                .userInfoUri("https://openapi.naver.com/v1/nid/me")
                .userNameAttributeName("response")
                .clientName("Naver")
    };

    companion object {
        const val DEFAULT_LOGIN_REDIRECT_URL = "{baseUrl}/login/oauth2/code/{registrationId}"
    }

    protected fun getBuilder(registrationId: String, method: ClientAuthenticationMethod, redirectUri: String) =
        ClientRegistration.withRegistrationId(registrationId)
            .clientAuthenticationMethod(method)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri(redirectUri)

    abstract fun getBuilder(registrationId: String): ClientRegistration.Builder
}

@Component
@ConfigurationProperties(prefix = "custom.oauth2")
class CustomOAuth2ClientProperties {

    lateinit var registration: Map<String, Registration>

    class Registration {
        lateinit var clientId: String
        var clientSecret: String = "default"
        val jwkSetUri: String = "default"
    }
}
