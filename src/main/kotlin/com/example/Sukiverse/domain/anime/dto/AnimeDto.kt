package com.example.Sukiverse.domain.anime.dto

import com.example.Sukiverse.types.entity.Anime
import com.example.Sukiverse.types.entity.AnimeSeason

data class SeasonResponse(
    val id: Long,
    val title: String,
    val year: Int?,
    val rank: Int?,
    val score: Double?,
)

data class AnimeResponse(
    val id: Long,
    val title: String,                 // 시리즈명
    val genre: String,
    val rank: Int?,                    // 대표(최고) 순위
    val score: Double?,               // 대표(최고) 점수
    val imageUrl: String?,
    val seasons: List<SeasonResponse>,
)

fun AnimeSeason.toResponse() = SeasonResponse(
    id = id,
    title = title,
    year = year,
    rank = rank,
    score = score,
)

fun Anime.toResponse() = AnimeResponse(
    id = id,
    title = title,
    genre = genre,
    rank = rank,
    score = score,
    imageUrl = imageUrl,
    seasons = seasons
        .sortedWith(compareBy(nullsLast()) { it.rank })
        .map { it.toResponse() },
)
