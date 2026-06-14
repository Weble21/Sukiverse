package com.example.Sukiverse.domain.anime.repository

import com.example.Sukiverse.types.entity.Anime
import org.springframework.data.jpa.repository.JpaRepository

interface AnimeRepository : JpaRepository<Anime, Long> {
    fun findByTitle(title: String): Anime?
    fun findByGenre(genre: String): List<Anime>
    fun findAllByOrderByRankAsc(): List<Anime>
    fun findAllByOrderByYearDesc(): List<Anime>
    fun findAllByOrderByScoreDesc(): List<Anime>
}
