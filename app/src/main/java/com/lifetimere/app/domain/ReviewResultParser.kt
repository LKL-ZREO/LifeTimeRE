package com.lifetimere.app.domain

import org.json.JSONObject
import org.json.JSONTokener

object ReviewResultParser {
    fun parseGateway(raw: String, body: String): DetectedReview {
        val root = parseObject(raw)
        val metadataJson = root.optJSONObject("metadata") ?: throw AiReviewException("AI 回响缺少版本信息。")
        val metadata = ReviewAnalysisMetadata(
            model = stringField(metadataJson, "model").trim(),
            promptVersion = stringField(metadataJson, "promptVersion").trim(),
            schemaVersion = stringField(metadataJson, "schemaVersion").trim(),
            analyzedAtEpochMillis = metadataJson.optLong("analyzedAtEpochMillis", 0),
        )
        return parse(root, body, metadata, gateway = true)
    }

    fun parseAnalysis(raw: String, body: String, metadata: ReviewAnalysisMetadata): DetectedReview =
        parse(parseObject(raw), body, metadata)

    private fun parse(root: JSONObject, body: String, metadata: ReviewAnalysisMetadata, gateway: Boolean = false): DetectedReview {
        requireKeys(root, if (gateway) ROOT_FIELDS + "metadata" else ROOT_FIELDS)
        val summary = stringField(root, "summary").trim()
        if (summary.isEmpty() || summary.length > 300) throw AiReviewException("AI 回顾摘要格式不正确。")
        val rows = root.optJSONArray("actions") ?: throw AiReviewException("AI 回响缺少行动列表。")
        if (rows.length() > MAX_ACTIONS) throw AiReviewException("AI 回响包含过多行动。")
        val seenIds = mutableSetOf<String>()
        val actions = (0 until rows.length()).map { index ->
            val row = rows.optJSONObject(index) ?: throw AiReviewException("AI 行动格式不正确。")
            requireKeys(row, ACTION_FIELDS)
            val id = stringField(row, "id")
            val attribute = runCatching { CharacterAttribute.valueOf(stringField(row, "attribute")) }
                .getOrElse { throw AiReviewException("AI 返回了未知属性。") }
            val intensityValue = (row.opt("intensity") as? Number)?.toDouble()
            if (intensityValue == null || intensityValue !in setOf(1.0, 2.0, 3.0)) {
                throw AiReviewException("AI 行动强度必须是 1、2 或 3 的整数。")
            }
            val intensity = intensityValue.toInt()
            val quote = stringField(row, "quote")
            val explanation = stringField(row, "explanation")
            if (!id.matches(ACTION_ID_PATTERN) || !seenIds.add(id)) throw AiReviewException("AI 行动标识无效或重复。")
            if (quote.isBlank() || quote.length > 120 || !body.contains(quote)) {
                throw AiReviewException("AI 引用与旅志原文不一致。")
            }
            if (explanation.isBlank() || explanation.length > 120) throw AiReviewException("AI 行动说明格式不正确。")
            DetectedAction(id, attribute, intensity, quote, explanation)
        }
        if (metadata.model.isEmpty() || metadata.model.length > 100 ||
            !metadata.promptVersion.matches(VERSION_PATTERN) ||
            !metadata.schemaVersion.matches(VERSION_PATTERN) || metadata.analyzedAtEpochMillis <= 0
        ) {
            throw AiReviewException("AI 回响版本信息无效。")
        }
        return DetectedReview(summary, actions, metadata)
    }

    private fun parseObject(raw: String): JSONObject = runCatching {
        val reader = JSONTokener(raw)
        val root = reader.nextValue() as? JSONObject ?: error("Expected object")
        check(reader.nextClean() == '\u0000') { "Unexpected trailing content" }
        root
    }.getOrElse {
        throw AiReviewException("AI 服务返回了无法读取的结果。")
    }

    private fun stringField(row: JSONObject, name: String): String =
        row.opt(name) as? String ?: throw AiReviewException("AI 回响字段 $name 必须是文本。")

    private fun requireKeys(row: JSONObject, expected: Set<String>) {
        if (row.keys().asSequence().toSet() != expected) throw AiReviewException("AI 回响字段缺失或包含未约定字段。")
    }

    private const val MAX_ACTIONS = 8
    private val ROOT_FIELDS = setOf("summary", "actions")
    private val ACTION_FIELDS = setOf("id", "attribute", "intensity", "quote", "explanation")
    private val ACTION_ID_PATTERN = Regex("[a-z][a-z0-9_]{2,39}")
    private val VERSION_PATTERN = Regex("[a-zA-Z0-9._/-]{1,100}")
}
