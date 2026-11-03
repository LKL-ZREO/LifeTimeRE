package com.lifetimere.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TravelerQuestionnaireTest {
    @Test fun neutralAnswersProduceNeutralProfile() {
        val answers = TravelerQuestionnaire.questions.associate { it.id to TravelerQuestionnaire.NEUTRAL_OPTION_ID }

        assertEquals(CharacterAttribute.entries.associateWith { 3 }, TravelerProfileScorer.score(answers))
    }

    @Test fun answersProduceStableBoundedTendencies() {
        val answers = TravelerQuestionnaire.questions.associate { question ->
            question.id to question.options.first().id
        }

        val result = TravelerProfileScorer.score(answers)
        assertEquals(result, TravelerProfileScorer.score(answers))
        assertEquals(CharacterAttribute.entries.toSet(), result.keys)
        assertTrue(result.values.all { it in 1..5 })
    }

    @Test fun unrelatedQuestionDoesNotAffectAttributeAverage() {
        val answers = TravelerQuestionnaire.questions.associate { it.id to TravelerQuestionnaire.NEUTRAL_OPTION_ID }.toMutableMap()
        val planQuestion = TravelerQuestionnaire.questions.first { it.id == "plan_changed" }
        val withoutSkillSignal = TravelerProfileScorer.score(answers)

        assertTrue(planQuestion.options.all { CharacterAttribute.SKILL !in it.scores })
        answers[planQuestion.id] = planQuestion.options.first { it.id == "reorder" }.id
        val withPlanAnswer = TravelerProfileScorer.score(answers)

        assertEquals(withoutSkillSignal[CharacterAttribute.SKILL], withPlanAnswer[CharacterAttribute.SKILL])
    }
}
