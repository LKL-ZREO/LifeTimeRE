package com.lifetimere.app.domain

import android.content.Context
import java.net.URI
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AiConnectionMode { DEEPSEEK, GATEWAY }

data class AiReviewSettings(
    val enabled: Boolean = false,
    val serviceUrl: String = "",
    val accessToken: String = "",
    val settlementMinuteOfDay: Int = DEFAULT_SETTLEMENT_MINUTE_OF_DAY,
    val mode: AiConnectionMode = AiConnectionMode.DEEPSEEK,
) {
    val isReady: Boolean
        get() = enabled && accessToken.isNotBlank() && (mode == AiConnectionMode.DEEPSEEK || serviceUrl.isNotBlank())

    val settlementTimeLabel: String
        get() = formatSettlementTime(settlementMinuteOfDay)

    fun normalized(): AiReviewSettings = copy(
        serviceUrl = serviceUrl.trim().trimEnd('/'),
        accessToken = accessToken.trim(),
    )

    fun validationError(): String? {
        if (settlementMinuteOfDay !in SUPPORTED_SETTLEMENT_MINUTES) {
            return "每日汇总时间必须在 18:00–23:30 之间，并以 30 分钟为间隔。"
        }
        if (!enabled) return null
        if (accessToken.isBlank()) return if (mode == AiConnectionMode.DEEPSEEK) "请填写 DeepSeek API Key。" else "请填写访问令牌。"
        if (mode == AiConnectionMode.DEEPSEEK) return null
        if (serviceUrl.isBlank()) return "请填写 AI 服务地址。"
        val uri = runCatching { URI(serviceUrl.trim()) }.getOrNull()
            ?: return "服务地址格式不正确。"
        val localDevelopmentHost = uri.host?.lowercase() in setOf("10.0.2.2", "localhost", "127.0.0.1")
        val isHttps = uri.scheme.equals("https", ignoreCase = true)
        val isLocalHttp = uri.scheme.equals("http", ignoreCase = true) && localDevelopmentHost
        if (!isHttps && !isLocalHttp) {
            return "正式服务必须使用 HTTPS；模拟器本机调试可使用 10.0.2.2。"
        }
        return null
    }

    companion object {
        const val DEFAULT_SETTLEMENT_MINUTE_OF_DAY = 22 * 60
        val SUPPORTED_SETTLEMENT_MINUTES: List<Int> = (18 * 60..23 * 60 + 30 step 30).toList()

        fun formatSettlementTime(minuteOfDay: Int): String =
            "%02d:%02d".format(minuteOfDay / 60, minuteOfDay % 60)
    }
}

class AiReviewSettingsStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val mutableSettings = MutableStateFlow(read())
    val settings: StateFlow<AiReviewSettings> = mutableSettings.asStateFlow()

    init {
        if (preferences.contains(KEY_ACCESS_TOKEN)) save(mutableSettings.value)
    }

    fun save(settings: AiReviewSettings) {
        val normalized = settings.normalized()
        preferences.edit()
            .putBoolean(KEY_ENABLED, normalized.enabled)
            .putString(KEY_SERVICE_URL, normalized.serviceUrl)
            .putString(KEY_ACCESS_TOKEN_ENCRYPTED, LocalSecretCipher.encrypt(normalized.accessToken))
            .remove(KEY_ACCESS_TOKEN)
            .putInt(KEY_SETTLEMENT_MINUTE_OF_DAY, normalized.settlementMinuteOfDay)
            .putString(KEY_MODE, normalized.mode.name)
            .apply()
        mutableSettings.value = normalized
    }

    private fun read() = AiReviewSettings(
        enabled = preferences.getBoolean(KEY_ENABLED, false),
        serviceUrl = preferences.getString(KEY_SERVICE_URL, "").orEmpty(),
        accessToken = preferences.getString(KEY_ACCESS_TOKEN_ENCRYPTED, null)?.let {
            runCatching { LocalSecretCipher.decrypt(it) }.getOrDefault("")
        } ?: preferences.getString(KEY_ACCESS_TOKEN, "").orEmpty(),
        settlementMinuteOfDay = preferences.getInt(
            KEY_SETTLEMENT_MINUTE_OF_DAY,
            AiReviewSettings.DEFAULT_SETTLEMENT_MINUTE_OF_DAY,
        ).takeIf { it in AiReviewSettings.SUPPORTED_SETTLEMENT_MINUTES }
            ?: AiReviewSettings.DEFAULT_SETTLEMENT_MINUTE_OF_DAY,
        mode = runCatching { AiConnectionMode.valueOf(preferences.getString(KEY_MODE, "").orEmpty()) }
            .getOrDefault(if (preferences.getString(KEY_SERVICE_URL, "").isNullOrBlank()) AiConnectionMode.DEEPSEEK else AiConnectionMode.GATEWAY),
    )

    private companion object {
        const val PREFERENCES_NAME = "ai_review_settings"
        const val KEY_ENABLED = "enabled"
        const val KEY_SERVICE_URL = "service_url"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_ACCESS_TOKEN_ENCRYPTED = "access_token_encrypted"
        const val KEY_SETTLEMENT_MINUTE_OF_DAY = "settlement_minute_of_day"
        const val KEY_MODE = "mode"
    }
}
