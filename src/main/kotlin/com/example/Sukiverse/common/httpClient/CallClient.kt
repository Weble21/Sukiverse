package com.example.Sukiverse.common.httpClient

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.springframework.stereotype.Component

@Component
class CallClient(private val httpClient: OkHttpClient) {

    fun POST(url: String, headers: Map<String, String>, body: RequestBody): String {
        val request = Request.Builder()
            .url(url)
            .apply { headers.forEach { (k, v) -> addHeader(k, v) } }
            .post(body)
            .build()

        return httpClient.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: error("Empty response body from $url")
            if (!response.isSuccessful) error("OAuth provider error (${response.code}): $body")
            body
        }
    }

    fun GET(url: String, headers: Map<String, String>): String {
        val request = Request.Builder()
            .url(url)
            .apply { headers.forEach { (k, v) -> addHeader(k, v) } }
            .get()
            .build()

        return httpClient.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: error("Empty response body from $url")
            if (!response.isSuccessful) error("OAuth provider error (${response.code}): $body")
            body
        }
    }
}
