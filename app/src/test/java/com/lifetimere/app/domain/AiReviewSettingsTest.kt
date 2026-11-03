package com.lifetimere.app.domain

import org.junit.Assert.assertNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiReviewSettingsTest {
    @Test fun productionServiceRequiresHttps() {
        val settings = AiReviewSettings(true, "http://example.com", "token", mode = AiConnectionMode.GATEWAY)

        assertTrue(settings.validationError()!!.contains("HTTPS"))
    }

    @Test fun httpsAndEmulatorDevelopmentUrlsAreAccepted() {
        assertNull(AiReviewSettings(true, "https://ai.example.com", "token", mode = AiConnectionMode.GATEWAY).validationError())
        assertNull(AiReviewSettings(true, "http://10.0.2.2:8787", "token", mode = AiConnectionMode.GATEWAY).validationError())
        assertNull(AiReviewSettings(true, "HTTP://10.0.2.2:8787", "token", mode = AiConnectionMode.GATEWAY).validationError())
    }

    @Test fun directDeepSeekRequiresUserKeyWithoutGatewayUrl() {
        assertTrue(AiReviewSettings(enabled = true).validationError()!!.contains("API Key"))
        assertNull(AiReviewSettings(enabled = true, accessToken = "user-key").validationError())
        assertTrue(AiReviewSettings(enabled = true, accessToken = "user-key").isReady)
    }

    @Test fun settlementTimeUsesSafeEveningHalfHourSlots() {
        assertEquals("22:00", AiReviewSettings().settlementTimeLabel)
        assertEquals(12, AiReviewSettings.SUPPORTED_SETTLEMENT_MINUTES.size)
        assertEquals(18 * 60, AiReviewSettings.SUPPORTED_SETTLEMENT_MINUTES.first())
        assertEquals(23 * 60 + 30, AiReviewSettings.SUPPORTED_SETTLEMENT_MINUTES.last())
        assertEquals("21:30", AiReviewSettings(settlementMinuteOfDay = 21 * 60 + 30).settlementTimeLabel)
        assertTrue(AiReviewSettings(settlementMinuteOfDay = 17 * 60).validationError()!!.contains("18:00"))
    }
}
