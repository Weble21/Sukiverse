package com.example.Sukiverse.types.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table


@Entity
@Table(name = "anime")
class Anime(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, length = 255, unique = true)
    var title: String,                 // 시리즈명: "진격의 거인"

    @Column(length = 100)
    var genre: String,

    var rank: Int? = null,             // 시리즈 대표 순위 = 소속 시즌 중 최고(MIN)

    var score: Double? = null,         // 시리즈 대표 점수 = 소속 시즌 중 최고(MAX)

    @Column(length = 500)
    var imageUrl: String? = null,

    @OneToMany(mappedBy = "anime", cascade = [CascadeType.ALL], orphanRemoval = true)
    var seasons: MutableList<AnimeSeason> = mutableListOf(),
)
