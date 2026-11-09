package com.lifetimere.app.domain

import java.io.IOException
import java.net.SocketTimeoutException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewFailureTest {
    @Test fun mapsFailuresToMessagesThatCanBeSavedAndShown() {
        assertEquals("TIMEOUT", ReviewFailure.from(SocketTimeoutException()).kind)
        assertEquals("NETWORK", ReviewFailure.from(IOException()).kind)
        assertEquals("INVALID_RESPONSE", ReviewFailure.from(IllegalArgumentException()).kind)
        val service = ReviewFailure.from(AiReviewException("服务正在维护"))
        assertEquals("SERVICE", service.kind)
        assertTrue(service.message.contains("维护"))
    }
}
