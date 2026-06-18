package com.example.Sukiverse.types.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table


@Entity
@Table(name = "anime_season")
class AnimeSeason(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "anime_id", nullable = false)
    @JsonIgnore                        // 부모 역참조 — JSON 직렬화 시 순환 방지
    var anime: Anime,

    @Column(nullable = false, length = 255)
    var title: String,                 // 시즌명: "더 파이널 시즌 파트 2"

    var year: Int? = null,

    var rank: Int? = null,

    var score: Double? = null,

    @Column(name = "mal_id", unique = true)
    var malId: Int? = null,            // Jikan/MAL 식별자 — 동기화 매칭 키
)
