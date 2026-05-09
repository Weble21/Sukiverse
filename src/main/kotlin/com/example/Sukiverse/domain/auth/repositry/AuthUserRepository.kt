package com.example.Sukiverse.domain.auth.repositry

import com.example.Sukiverse.types.entity.User
import org.springframework.data.jpa.repository.JpaRepository

interface AuthUserRepository : JpaRepository<User, String> {
    fun findByProviderAndProviderId(provider: String, providerId: String): User?
}
