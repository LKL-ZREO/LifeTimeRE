package com.lifetimere.app.domain

import com.lifetimere.app.data.model.DailyEntry
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class DirectDeepSeekReviewService(
    private val apiKeyProvider: () -> String,
) : ReviewService {
    private val baseUrl = "https://api.deepseek.com"
    private val model = "deepseek-flash"

    override suspend fun analyze(entry: DailyEntry): DetectedReview = withContext(Dispatchers.IO) {
        val apiKey = requireKey(apiKeyProvider())
        val request = JSONObject()
            .put("model", model)
            .put("messages", JSONArray()
                .put(JSONObject().put("role", "system").put("content", DailyReviewPrompt.deepSeekInstructions))
                .put(JSONObject().put("role", "user").put(
                    "content",
                    "旅志日期：${entry.localDate}\n修订号：${entry.revision}\n旅志正文：\n${entry.body}",
                )))
            .put("response_format", JSONObject().put("type", "json_object"))
            .put("temperature", 0)
            .put("reasoning_effort", "none")
            .put("max_tokens", 2000)
            .put("stream", false)
        val response = request(apiKey, "/chat/completions", "POST", request.toString())
        val root = runCatching { JSONObject(response) }.getOrElse {
            throw AiReviewException("DeepSeek 返回了无法读取的响应。")
        }
        val choice = root.optJSONArray("choices")?.optJSONObject(0)
            ?: throw AiReviewException("DeepSeek 响应缺少分析结果。")
        if (choice.optString("finish_reason") == "length") throw AiReviewException("AI 回响输出不完整，请重试。")
        val content = choice.optJSONObject("message")?.optString("content")?.trim().orEmpty()
        if (content.isEmpty()) throw AiReviewException("DeepSeek 没有返回分析内容，请重试。")
        ReviewResultParser.parseAnalysis(
            raw = content,
            body = entry.body,
            metadata = ReviewAnalysisMetadata(
                model = "deepseek/$model",
                promptVersion = DailyReviewPrompt.VERSION,
                schemaVersion = DailyReviewPrompt.SCHEMA_VERSION,
                analyzedAtEpochMillis = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun testConnection(apiKeyInput: String): String = withContext(Dispatchers.IO) {
        val apiKey = requireKey(apiKeyInput)
        val root = runCatching { JSONObject(request(apiKey, "/models", "GET", null)) }.getOrElse {
            throw AiReviewException("DeepSeek 模型列表无法读取。")
        }
        val models = root.optJSONArray("data") ?: throw AiReviewException("DeepSeek 模型列表为空。")
        val found = (0 until models.length()).any { models.optJSONObject(it)?.optString("id") == model }
        if (!found) throw AiReviewException("当前账号不可使用模型 $model。")
        "deepseek/$model"
    }

    private fun request(apiKey: String, path: String, method: String, body: String?): String {
        val connection = (URL(baseUrl + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = CONNECT_TIMEOUT_MILLIS
            readTimeout = READ_TIMEOUT_MILLIS
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Authorization", "Bearer $apiKey")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
        }
        return try {
            if (body != null) connection.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(body) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                val providerMessage = runCatching {
                    JSONObject(response).optJSONObject("error")?.optString("message")
                }.getOrNull()?.takeIf { it.isNotBlank() }
                throw AiReviewException(providerMessage?.replace(apiKey, "[已隐藏]")?.take(160)
                    ?: "DeepSeek 服务返回错误（$status）。")
            }
            response
        } finally {
            connection.disconnect()
        }
    }

    private fun requireKey(value: String): String = value.trim().takeIf { it.isNotEmpty() }
        ?: throw AiReviewException("请先在设置页填写 DeepSeek API Key。")

    private companion object {
        const val CONNECT_TIMEOUT_MILLIS = 10_000
        const val READ_TIMEOUT_MILLIS = 60_000
    }
}
