package com.example.Sukiverse.domain.anime.service

import com.example.Sukiverse.common.httpClient.CallClient
import com.example.Sukiverse.common.json.JsonUtil
import com.example.Sukiverse.domain.anime.repository.AnimeRepository
import com.example.Sukiverse.exception.CustomException
import com.example.Sukiverse.exception.ErrorCode
import com.example.Sukiverse.types.entity.Anime
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private data class JikanTopResponse(val data: List<JikanAnimeItem>)
private data class JikanAnimeItem(
    val rank: Int,
    val title: String,
    val score: Double?,
    val year: Int?,
    val images: JikanImages,
    val genres: List<JikanGenre>,
)
private data class JikanImages(val jpg: JikanImageUrls)
private data class JikanImageUrls(val image_url: String?)
private data class JikanGenre(val name: String)

data class AnimeCreateRequest(
    val title: String,
    val genre: String,
    val year: Int? = null,
    val rank: Int? = null,
    val score: Double? = null,
    val imageUrl: String? = null,
)

data class AnimeUpdateRequest(
    val title: String? = null,
    val genre: String? = null,
    val year: Int? = null,
    val rank: Int? = null,
    val score: Double? = null,
    val imageUrl: String? = null,
)

@Service
class AnimeService(
    private val animeRepository: AnimeRepository,
    private val callClient: CallClient,
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
        request.title?.let { anime.title = it }
        request.genre?.let { anime.genre = it }
        request.year?.let { anime.year = it }
        request.rank?.let { anime.rank = it }
        request.score?.let { anime.score = it }
        request.imageUrl?.let { anime.imageUrl = it }
        return anime
    }

    @Transactional
    fun delete(id: Long) = animeRepository.deleteById(id)

    @Transactional
    fun syncTop100() {
        for (page in 1..4) {
            val json = callClient.GET(
                "https://api.jikan.moe/v4/top/anime?limit=25&page=$page",
                emptyMap()
            )
            val response = JsonUtil.decodeFromJson<JikanTopResponse>(json)

            response.data.forEach { item ->
                val genre = item.genres.joinToString(", ") { it.name }
                val existing = animeRepository.findByTitle(item.title)
                if (existing != null) {
                    existing.rank = item.rank
                    existing.score = item.score
                    existing.year = item.year
                    existing.imageUrl = item.images.jpg.image_url
                    existing.genre = genre
                } else {
                    animeRepository.save(Anime(
                        title = item.title,
                        genre = genre,
                        year = item.year,
                        rank = item.rank,
                        score = item.score,
                        imageUrl = item.images.jpg.image_url,
                    ))
                }
            }

            if (page < 4) Thread.sleep(500)
        }
    }
}
