package com.lifetimere.app.domain

import com.lifetimere.app.data.model.DailyEntry
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class RemoteReviewService(
    private val settingsProvider: () -> AiReviewSettings,
) : ReviewService {
    override suspend fun analyze(entry: DailyEntry): DetectedReview = withContext(Dispatchers.IO) {
        val settings = requireReadySettings()
        val request = JSONObject()
            .put("entryId", entry.id.toString())
            .put("revision", entry.revision)
            .put("localDate", entry.localDate.toString())
            .put("body", entry.body)
        val response = request(settings, "/v1/reviews/analyze", "POST", request.toString())
        ReviewResultParser.parseGateway(response, entry.body)
    }

    suspend fun testConnection(settings: AiReviewSettings): String = withContext(Dispatchers.IO) {
        settings.validationError()?.let { throw AiReviewException(it) }
        val normalized = settings.normalized()
        val response = request(normalized, "/health", "GET", null)
        val json = JSONObject(response)
        if (!json.optBoolean("ok", false)) throw AiReviewException("AI 服务尚未就绪。")
        if (!json.optBoolean("providerConfigured", false)) {
            throw AiReviewException("网关可以访问，但服务端尚未配置模型密钥。")
        }
        json.optString("model").takeIf { it.isNotBlank() } ?: "AI 服务"
    }

    private fun requireReadySettings(): AiReviewSettings {
        val settings = settingsProvider().normalized()
        settings.validationError()?.let { throw AiReviewException(it) }
        if (!settings.isReady) throw AiReviewException("请先在设置中启用并配置 AI 服务。")
        return settings
    }

    private fun request(settings: AiReviewSettings, path: String, method: String, body: String?): String {
        val connection = (URL(settings.serviceUrl + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = CONNECT_TIMEOUT_MILLIS
            readTimeout = READ_TIMEOUT_MILLIS
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Authorization", "Bearer ${settings.accessToken}")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(body) }
            }
        }
        return try {
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                val message = runCatching { JSONObject(response).optString("error") }.getOrNull()
                    ?.takeIf { it.isNotBlank() }
                    ?: "AI 服务返回错误（$status）。"
                throw AiReviewException(message.take(180))
            }
            response
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val CONNECT_TIMEOUT_MILLIS = 10_000
        const val READ_TIMEOUT_MILLIS = 45_000
    }
}

class AiReviewException(message: String) : Exception(message)
