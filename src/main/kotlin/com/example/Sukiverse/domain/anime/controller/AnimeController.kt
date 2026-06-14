package com.example.Sukiverse.domain.anime.controller

import com.example.Sukiverse.domain.anime.service.AnimeCreateRequest
import com.example.Sukiverse.domain.anime.service.AnimeService
import com.example.Sukiverse.domain.anime.service.AnimeUpdateRequest
import com.example.Sukiverse.exception.CustomException
import com.example.Sukiverse.types.dto.ApiResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/anime")
class AnimeController(
    private val animeService: AnimeService
) {
    @GetMapping
    fun getAll(
        @RequestParam(defaultValue = "rank") sort: String
    ): ResponseEntity<*> =
        try {
            ResponseEntity.ok(ApiResponse.success(animeService.getAll(sort)))
        } catch (e: CustomException) {
            ResponseEntity.status(e.getHttpStatus())
                .body(ApiResponse.error<Nothing>(e.getErrorCode(), e.message ?: e.getCodeInterface().message))
        }

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): ResponseEntity<*> =
        try {
            ResponseEntity.ok(ApiResponse.success(animeService.getById(id)))
        } catch (e: CustomException) {
            ResponseEntity.status(e.getHttpStatus())
                .body(ApiResponse.error<Nothing>(e.getErrorCode(), e.message ?: e.getCodeInterface().message))
        }

    @PostMapping
    fun create(@RequestBody request: AnimeCreateRequest): ResponseEntity<*> =
        try {
            ResponseEntity.ok(ApiResponse.success(animeService.create(request)))
        } catch (e: CustomException) {
            ResponseEntity.status(e.getHttpStatus())
                .body(ApiResponse.error<Nothing>(e.getErrorCode(), e.message ?: e.getCodeInterface().message))
        }

    @PatchMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @RequestBody request: AnimeUpdateRequest
    ): ResponseEntity<*> =
        try {
            ResponseEntity.ok(ApiResponse.success(animeService.update(id, request)))
        } catch (e: CustomException) {
            ResponseEntity.status(e.getHttpStatus())
                .body(ApiResponse.error<Nothing>(e.getErrorCode(), e.message ?: e.getCodeInterface().message))
        }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<*> =
        try {
            animeService.delete(id)
            ResponseEntity.ok(ApiResponse.success(null))
        } catch (e: CustomException) {
            ResponseEntity.status(e.getHttpStatus())
                .body(ApiResponse.error<Nothing>(e.getErrorCode(), e.message ?: e.getCodeInterface().message))
        }
}
