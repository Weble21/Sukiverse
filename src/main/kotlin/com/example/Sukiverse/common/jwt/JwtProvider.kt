package com.example.Sukiverse.common.jwt

import com.example.Sukiverse.exception.CustomException
import com.example.Sukiverse.exception.ErrorCode
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Date

@Component
class JwtProvider(
    @Value("\${jwt.secret}") private val secret: String,
    @Value("\${jwt.expiration}") private val expiration: Long,
    @Value("\${jwt.refresh-expiration}") private val refreshExpiration: Long,
) {
    private val key by lazy { Keys.hmacShaKeyFor(secret.toByteArray()) }

    fun createToken(provider: String, email: String?, name: String?, id: String): String {
        return Jwts.builder()
            .subject(id)
            .claim("provider", provider)
            .claim("email", email)
            .claim("name", name)
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + expiration))
            .signWith(key)
            .compact()
    }

    fun createRefreshToken(id: String): String {
        return Jwts.builder()
            .subject(id)
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + refreshExpiration))
            .signWith(key)
            .compact()
    }

    fun verifyToken(token: String) {
        try {
            Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
        } catch (e: JwtException) {
            throw CustomException(ErrorCode.TOKEN_IS_INVALID)
        }
    }
}
