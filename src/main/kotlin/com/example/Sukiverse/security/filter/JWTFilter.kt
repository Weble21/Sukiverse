package com.example.Sukiverse.security.filter

import com.example.Sukiverse.common.jwt.JwtProvider
import com.example.Sukiverse.exception.CustomException
import com.example.Sukiverse.exception.ErrorCode
import com.example.Sukiverse.types.dto.ApiResponse
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.util.AntPathMatcher
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JWTFilter(
    private val jwtProvider: JwtProvider,
) : OncePerRequestFilter() {

    private val pathMatcher = AntPathMatcher()
    private val mapper = ObjectMapper()

    private val JWT_AUTH_ENDPOINTS = arrayOf(
        "/api/v1/bank/**",
        "/api/v1/history/**",
    )

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        if (!shouldPerformAuthentication(request.requestURI)) {
            filterChain.doFilter(request, response)
            return
        }

        val authHeader = request.getHeader("Authorization")
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            writeError(response, 401, ErrorCode.ACCESS_TOKEN_NEED.errorCode, ErrorCode.ACCESS_TOKEN_NEED.message)
            return
        }

        try {
            jwtProvider.verifyToken(authHeader.substring(7))
        } catch (e: CustomException) {
            writeError(response, e.getHttpStatus(), e.getErrorCode(), e.getCodeInterface().message)
            return
        }

        filterChain.doFilter(request, response)
    }

    private fun writeError(response: HttpServletResponse, status: Int, code: String, message: String) {
        response.status = status
        response.contentType = "application/json;charset=UTF-8"
        response.writer.write(
            mapper.writeValueAsString(ApiResponse.error<Any>(code, message))
        )
        response.writer.flush()
    }

    private fun shouldPerformAuthentication(uri: String): Boolean =
        JWT_AUTH_ENDPOINTS.any { pathMatcher.match(it, uri) }
}
