package com.lifetimere.app.domain

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReviewResultParserTest {
    private val body = "晨跑了三公里，然后读书二十分钟。"
    private val metadata = ReviewAnalysisMetadata("deepseek/deepseek-flash", "daily_review_v4", "daily_review_output_v1", 100)

    @Test fun directResultKeepsEvidenceAndVersionWithoutCalculatingPoints() {
        val result = parse(validResult())
        assertEquals(metadata, result.metadata)
        assertEquals(1, result.actions.size)
        assertEquals(CharacterAttribute.STRENGTH, result.actions.single().attribute)
        assertEquals("晨跑了三公里", result.actions.single().quote)
        assertEquals(1, result.actions.single().intensity)
    }

    @Test fun emptyActionsAreValid() {
        val result = parse(validResult().put("actions", JSONArray()))
        assertTrue(result.actions.isEmpty())
    }

    @Test fun coercedOrOutOfRangeIntensitiesAreRejected() {
        listOf<Any>("2", 1.5, 0, 4, true, JSONObject.NULL).forEach { value ->
            val root = validResult()
            root.getJSONArray("actions").getJSONObject(0).put("intensity", value)
            assertRejected(root)
        }
    }

    @Test fun fabricatedEvidenceAndUnknownAttributesAreRejected() {
        val fabricated = validResult()
        fabricated.getJSONArray("actions").getJSONObject(0).put("quote", "跑了马拉松")
        assertRejected(fabricated)
        val unknown = validResult()
        unknown.getJSONArray("actions").getJSONObject(0).put("attribute", "LUCK")
        assertRejected(unknown)
    }

    @Test fun ExtraRewardsMissingFieldsAndNonTextAreRejected() {
        assertRejected(validResult().put("points", 99))
        val actionReward = validResult()
        actionReward.getJSONArray("actions").getJSONObject(0).put("points", 99)
        assertRejected(actionReward)
        val missing = validResult()
        missing.getJSONArray("actions").getJSONObject(0).remove("explanation")
        assertRejected(missing)
        assertRejected(validResult().put("summary", 123))
    }

    @Test fun duplicateIdsExcessActionsAndTrailingContentAreRejected() {
        val duplicate = validResult()
        duplicate.getJSONArray("actions").put(duplicate.getJSONArray("actions").getJSONObject(0))
        assertRejected(duplicate)
        val oversized = validResult()
        val rows = JSONArray()
        repeat(9) { index ->
            rows.put(JSONObject(validResult().getJSONArray("actions").getJSONObject(0).toString()).put("id", "action_$index"))
        }
        assertRejected(oversized.put("actions", rows))
        assertFails { ReviewResultParser.parseAnalysis(validResult().toString() + "unexpected", body, metadata) }
    }

    @Test fun gatewayResultStillParsesAndRequiresMetadata() {
        val root = validResult().put("metadata", JSONObject()
            .put("model", metadata.model)
            .put("promptVersion", metadata.promptVersion)
            .put("schemaVersion", metadata.schemaVersion)
            .put("analyzedAtEpochMillis", metadata.analyzedAtEpochMillis))
        assertEquals(metadata, ReviewResultParser.parseGateway(root.toString(), body).metadata)
        assertFails { ReviewResultParser.parseGateway(validResult().toString(), body) }
    }

    private fun validResult() = JSONObject().put("summary", "完成了晨跑。")
        .put("actions", JSONArray().put(JSONObject()
            .put("id", "action_1")
            .put("attribute", "STRENGTH")
            .put("intensity", 1)
            .put("quote", "晨跑了三公里")
            .put("explanation", "明确完成了体能实践。")))

    private fun parse(root: JSONObject) = ReviewResultParser.parseAnalysis(root.toString(), body, metadata)

    private fun assertRejected(root: JSONObject) = assertFails { parse(root) }

    private fun assertFails(block: () -> Unit) {
        try {
            block()
            fail("Malformed AI result should be rejected")
        } catch (_: AiReviewException) {
            // Validation failures must never reach growth settlement.
        }
    }
}
