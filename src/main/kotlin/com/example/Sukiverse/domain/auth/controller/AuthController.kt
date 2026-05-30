package com.example.Sukiverse.domain.auth.controller

import com.example.Sukiverse.domain.auth.service.AuthService
import com.example.Sukiverse.domain.auth.service.UserDetailsDto
import com.example.Sukiverse.exception.CustomException
import com.example.Sukiverse.exception.ErrorCode
import com.example.Sukiverse.types.dto.ApiResponse
import io.swagger.v3.oas.annotations.Hidden
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Duration

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authService: AuthService,
    @Value("\${app.cookie.secure:true}") private val secureCookie: Boolean,
) {
    @PostMapping("/login")
    fun login(@RequestBody request: AuthRequest): ResponseEntity<ApiResponse<LoginApiResponse>> {
        return try {
            val result = authService.handleAuth(request.provider, request.code)

            val refreshCookie = ResponseCookie.from("refreshToken", result.refreshToken)
                .httpOnly(true)
                .secure(secureCookie)
                .path("/api/v1/auth/refresh")
                .maxAge(Duration.ofDays(30))
                .sameSite("Strict")
                .build()

            ResponseEntity.ok()
                .header("Set-Cookie", refreshCookie.toString())
                .body(ApiResponse.success(LoginApiResponse(
                    accessToken = result.accessToken,
                    userDetails = result.userDetails,
                )))
        } catch (e: CustomException) {
            ResponseEntity.status(e.getHttpStatus())
                .body(ApiResponse.error(e.getErrorCode(), e.message ?: e.getCodeInterface().message))
        } catch (e: Exception) {
            ResponseEntity.badRequest()
                .body(ApiResponse.error(ErrorCode.PROVIDER_NOT_FOUND.errorCode, e.message ?: "로그인 실패"))
        }
    }

    @GetMapping("/me")
    fun me(
        @RequestHeader("Authorization", required = false) authorization: String?,
    ): ResponseEntity<ApiResponse<UserDetailsDto>> {
        if (authorization == null) {
            return ResponseEntity.status(401)
                .body(ApiResponse.error(ErrorCode.ACCESS_TOKEN_NEED.errorCode, ErrorCode.ACCESS_TOKEN_NEED.message))
        }
        return try {
            val userDetails = authService.getMe(authorization)
            ResponseEntity.ok(ApiResponse.success(userDetails))
        } catch (e: CustomException) {
            ResponseEntity.status(e.getHttpStatus())
                .body(ApiResponse.error(e.getErrorCode(), e.message ?: e.getCodeInterface().message))
        }
    }

    @Hidden
    @PostMapping("/refresh")
    fun refresh(
        @CookieValue(name = "refreshToken", required = false) refreshToken: String?,
    ): ResponseEntity<ApiResponse<AccessTokenResponse>> {
        if (refreshToken == null) {
            return ResponseEntity.status(401)
                .body(ApiResponse.error(ErrorCode.REFRESH_TOKEN_NOT_FOUND.errorCode, ErrorCode.REFRESH_TOKEN_NOT_FOUND.message))
        }
        return try {
            val newAccessToken = authService.refreshAccessToken(refreshToken)
            ResponseEntity.ok(ApiResponse.success(AccessTokenResponse(accessToken = newAccessToken)))
        } catch (e: CustomException) {
            ResponseEntity.status(e.getHttpStatus())
                .body(ApiResponse.error(e.getErrorCode(), e.message ?: e.getCodeInterface().message))
        }
    }
}

data class AuthRequest(
    val provider: String,
    val code: String,
)

data class LoginApiResponse(
    val accessToken: String,
    val userDetails: UserDetailsDto,
)

data class AccessTokenResponse(
    val accessToken: String,
)
