package com.example.Sukiverse.domain.anime.service

import com.example.Sukiverse.domain.anime.repository.AnimeRepository
import com.example.Sukiverse.exception.CustomException
import com.example.Sukiverse.exception.ErrorCode
import com.example.Sukiverse.types.entity.Anime
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class AnimeCreateRequest(
    val title: String,
    val genre: String,
    val year: Int? = null,
    val rank: Int? = null,
    val score: Double? = null,
    val imageUrl: String? = null,
)

data class AnimeUpdateRequest(
    val genre: String? = null,
    val year: Int? = null,
    val rank: Int? = null,
    val score: Double? = null,
    val imageUrl: String? = null,
)

@Service
class AnimeService(
    private val animeRepository: AnimeRepository
) {
    fun getAll(sort: String): List<Anime> = when (sort) {
        "year"  -> animeRepository.findAllByOrderByYearDesc()
        "score" -> animeRepository.findAllByOrderByScoreDesc()
        else    -> animeRepository.findAllByOrderByRankAsc()
    }

    fun getById(id: Long): Anime =
        animeRepository.findById(id)
            .orElseThrow { CustomException(ErrorCode.ANIME_NOT_FOUND) }

    fun getByTitle(title: String): Anime =
        animeRepository.findByTitle(title)
            ?: throw CustomException(ErrorCode.ANIME_NOT_FOUND)

    @Transactional
    fun create(request: AnimeCreateRequest): Anime =
        animeRepository.save(
            Anime(
                title = request.title,
                genre = request.genre,
                year = request.year,
                rank = request.rank,
                score = request.score,
                imageUrl = request.imageUrl,
            )
        )

    @Transactional
    fun update(id: Long, request: AnimeUpdateRequest): Anime {
        val anime = getById(id)
        request.genre?.let { anime.genre = it }
        request.year?.let { anime.year = it }
        request.rank?.let { anime.rank = it }
        request.score?.let { anime.score = it }
        request.imageUrl?.let { anime.imageUrl = it }
        return anime
    }

    @Transactional
    fun delete(id: Long) = animeRepository.deleteById(id)
}
