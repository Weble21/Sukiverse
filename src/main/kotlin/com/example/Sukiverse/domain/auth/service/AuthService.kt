package com.example.Sukiverse.domain.auth.service

import com.example.Sukiverse.common.jwt.JwtProvider
import com.example.Sukiverse.domain.auth.repositry.AuthUserRepository
import com.example.Sukiverse.exception.CustomException
import com.example.Sukiverse.exception.ErrorCode
import com.example.Sukiverse.interfaces.OAuthServiceInterface
import com.example.Sukiverse.types.entity.User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

data class UserDetailsDto(
    val id: String,
    val profileImage: String?,
    val isOnboardingCompleted: Boolean,
)

data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val userDetails: UserDetailsDto,
)

@Service
class AuthService(
    private val oAuth2Services: Map<String, OAuthServiceInterface>,
    private val authUserRepository: AuthUserRepository,
    private val jwtProvider: JwtProvider,
) {
    @Transactional
    fun handleAuth(provider: String, code: String): LoginResponse {
        val providerKey = provider.lowercase()
        val service = oAuth2Services[providerKey]
            ?: throw CustomException(ErrorCode.PROVIDER_NOT_FOUND)

        val tokenResponse = service.getToken(code)
        val userInfo = service.getUserInfo(tokenResponse.accessToken)

        val user = authUserRepository.findByProviderAndProviderId(providerKey, userInfo.id)
            ?.also { existing ->
                existing.email = userInfo.email
                existing.nickname = userInfo.name
                existing.profileImage = userInfo.profileImage
            }
            ?: authUserRepository.save(
                User(
                    ulid = UUID.randomUUID().toString(),
                    provider = providerKey,
                    providerId = userInfo.id,
                    email = userInfo.email,
                    nickname = userInfo.name,
                    profileImage = userInfo.profileImage,
                )
            )

        val accessToken = jwtProvider.createToken(providerKey, userInfo.email, userInfo.name, user.ulid)
        val refreshToken = jwtProvider.createRefreshToken(user.ulid)
        user.accessToken = accessToken
        user.refreshToken = refreshToken

        return LoginResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            userDetails = UserDetailsDto(
                id = user.ulid,
                profileImage = user.profileImage,
                isOnboardingCompleted = user.isOnboardingCompleted,
            ),
        )
    }

    fun verifyToken(authorization: String) {
        jwtProvider.verifyToken(authorization.removePrefix("Bearer "))
    }
}
