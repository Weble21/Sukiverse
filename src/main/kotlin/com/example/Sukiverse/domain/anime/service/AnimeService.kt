package com.example.Sukiverse.domain.anime.service

import com.example.Sukiverse.common.httpClient.CallClient
import com.example.Sukiverse.common.json.JsonUtil
import com.example.Sukiverse.domain.anime.dto.AnimeResponse
import com.example.Sukiverse.domain.anime.dto.toResponse
import com.example.Sukiverse.domain.anime.repository.AnimeRepository
import com.example.Sukiverse.domain.anime.repository.AnimeSeasonRepository
import com.example.Sukiverse.exception.CustomException
import com.example.Sukiverse.exception.ErrorCode
import com.example.Sukiverse.types.entity.Anime
import com.example.Sukiverse.types.entity.AnimeSeason
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private data class JikanTopResponse(val data: List<JikanAnimeItem>)
private data class JikanAnimeItem(
    val mal_id: Int,
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

data class SeasonRequest(
    val title: String,
    val year: Int? = null,
    val rank: Int? = null,
    val score: Double? = null,
)

data class AnimeCreateRequest(
    val title: String,
    val genre: String,
    val imageUrl: String? = null,
    val seasons: List<SeasonRequest> = emptyList(),
)

data class AnimeUpdateRequest(
    val title: String? = null,
    val genre: String? = null,
    val imageUrl: String? = null,
)

@Service
class AnimeService(
    private val animeRepository: AnimeRepository,
    private val animeSeasonRepository: AnimeSeasonRepository,
    private val callClient: CallClient,
) {
    @Transactional(readOnly = true)
    fun getAll(sort: String): List<AnimeResponse> = when (sort) {
        "score" -> animeRepository.findAllByOrderByScoreDesc()
        else    -> animeRepository.findAllByOrderByRankAsc()
    }.map { it.toResponse() }

    @Transactional(readOnly = true)
    fun getById(id: Long): AnimeResponse =
        animeRepository.findById(id)
            .orElseThrow { CustomException(ErrorCode.ANIME_NOT_FOUND) }
            .toResponse()

    @Transactional(readOnly = true)
    fun getByTitle(title: String): AnimeResponse =
        (animeRepository.findByTitle(title)
            ?: throw CustomException(ErrorCode.ANIME_NOT_FOUND))
            .toResponse()

    @Transactional
    fun create(request: AnimeCreateRequest): AnimeResponse {
        val anime = Anime(
            title = request.title,
            genre = request.genre,
            imageUrl = request.imageUrl,
        )
        request.seasons.forEach { s ->
            anime.seasons.add(
                AnimeSeason(anime = anime, title = s.title, year = s.year, rank = s.rank, score = s.score)
            )
        }
        recalcSeries(anime)
        return animeRepository.save(anime).toResponse()
    }

    @Transactional
    fun update(id: Long, request: AnimeUpdateRequest): AnimeResponse {
        val anime = animeRepository.findById(id)
            .orElseThrow { CustomException(ErrorCode.ANIME_NOT_FOUND) }
        request.title?.let { anime.title = it }
        request.genre?.let { anime.genre = it }
        request.imageUrl?.let { anime.imageUrl = it }
        return anime.toResponse()
    }

    @Transactional
    fun delete(id: Long) = animeRepository.deleteById(id)

    /**
     * Jikan Top100을 시즌 단위로 동기화한다.
     * - 기존 시즌과는 [AnimeSeason.malId]로 매칭하여 rank/score/year를 갱신한다.
     * - 미매칭(신규) 항목은 단일 시즌("본편") 시리즈로 생성한다.
     *   시리즈 그룹핑(병합)은 별도 매핑 규칙으로 추후 처리한다.
     * - 주의: 재구성 이전부터 있던 시즌들은 malId가 비어 있으므로, 1회성 malId 백필 전에는
     *   여기서 매칭되지 않고 신규 시리즈로 생성될 수 있다.
     */
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
                val season = animeSeasonRepository.findByMalId(item.mal_id)
                if (season != null) {
                    season.rank = item.rank
                    season.score = item.score
                    season.year = item.year
                    recalcSeries(season.anime)
                } else {
                    val anime = animeRepository.findByTitle(item.title)
                        ?: animeRepository.save(
                            Anime(title = item.title, genre = genre, imageUrl = item.images.jpg.image_url)
                        )
                    anime.seasons.add(
                        AnimeSeason(
                            anime = anime,
                            malId = item.mal_id,
                            title = "본편",
                            year = item.year,
                            rank = item.rank,
                            score = item.score,
                        )
                    )
                    recalcSeries(anime)
                }
            }

            if (page < 4) Thread.sleep(500)
        }
    }

    /** 시리즈 대표 값을 소속 시즌 기준으로 재계산한다 (rank=최고=MIN, score=최고=MAX). */
    private fun recalcSeries(anime: Anime) {
        anime.rank = anime.seasons.mapNotNull { it.rank }.minOrNull()
        anime.score = anime.seasons.mapNotNull { it.score }.maxOrNull()
    }
}
