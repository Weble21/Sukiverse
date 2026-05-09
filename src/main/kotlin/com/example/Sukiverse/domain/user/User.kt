package com.example.Sukiverse.domain.user

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "users")
class User(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Enumerated(EnumType.STRING)
    val provider: Provider,

    val providerId: String,
    val email: String? = null,

    var nickname: String,
    var profileImageUrl: String? = null,

    @CreationTimestamp
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
