package com.example.Sukiverse.domain.auth.controller

import com.example.Sukiverse.domain.auth.service.AuthService
import com.example.Sukiverse.domain.auth.service.LoginResponse
import com.example.Sukiverse.exception.CustomException
import com.example.Sukiverse.exception.ErrorCode
import com.example.Sukiverse.types.dto.ApiResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authService: AuthService,
) {
    @PostMapping("/login")
    fun login(@RequestBody request: AuthRequest): ResponseEntity<ApiResponse<LoginResponse>> {
        return try {
            val result = authService.handleAuth(request.provider, request.code)
            ResponseEntity.ok(ApiResponse.success(result))
        } catch (e: CustomException) {
            ResponseEntity.status(e.getHttpStatus())
                .body(ApiResponse.error(e.getErrorCode(), e.message ?: e.getCodeInterface().message))
        } catch (e: Exception) {
            ResponseEntity.badRequest()
                .body(ApiResponse.error(ErrorCode.PROVIDER_NOT_FOUND.errorCode, e.message ?: "로그인 실패"))
        }
    }
}

data class AuthRequest(
    val provider: String,
    val code: String,
)
