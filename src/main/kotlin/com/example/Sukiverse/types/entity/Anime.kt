package com.example.Sukiverse.types.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table


@Entity
@Table(name = "anime")
class Anime(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, length = 255, unique = true)
    var title: String,

    @Column(length = 100)
    var genre: String,

    var year: Int? = null,

    var rank: Int? = null,

    var score: Double? = null,

    @Column(length = 500)
    var imageUrl: String? = null,

)