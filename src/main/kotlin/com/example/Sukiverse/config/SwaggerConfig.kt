package com.example.Sukiverse.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SwaggerConfig {

    @Bean
    fun openAPI(): OpenAPI = OpenAPI().apply {
        info(
            Info().apply {
                title = "Sukiverse API"
                description = "일본 문화 종합 정보 커뮤니티 Sukiverse API 문서"
                version = "v1.0.0"
            }
        )
    }
}