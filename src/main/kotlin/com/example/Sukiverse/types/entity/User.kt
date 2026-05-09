package com.example.Sukiverse.types.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "auth_users")
class User(
    @Id
    @Column(name = "ulid", length = 36)
    val ulid: String,

    @Column(name = "provider", nullable = false, length = 20)
    val provider: String,

    @Column(name = "provider_id", nullable = false, length = 100)
    val providerId: String,

    @Column(name = "email", length = 200)
    var email: String? = null,

    @Column(name = "nickname", length = 100)
    var nickname: String? = null,

    @Column(name = "profile_image", length = 500)
    var profileImage: String? = null,

    @Column(name = "is_onboarding_completed", nullable = false)
    var isOnboardingCompleted: Boolean = false,

    @Column(name = "access_token", length = 512)
    var accessToken: String? = null,

    @Column(name = "refresh_token", length = 512)
    var refreshToken: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
