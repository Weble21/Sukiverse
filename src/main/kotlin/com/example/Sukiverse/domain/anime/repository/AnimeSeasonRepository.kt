package com.example.Sukiverse.domain.anime.repository

import com.example.Sukiverse.types.entity.AnimeSeason
import org.springframework.data.jpa.repository.JpaRepository

interface AnimeSeasonRepository : JpaRepository<AnimeSeason, Long> {
    fun findByMalId(malId: Int): AnimeSeason?
}
