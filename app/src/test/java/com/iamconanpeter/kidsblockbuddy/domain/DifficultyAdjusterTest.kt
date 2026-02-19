package com.iamconanpeter.kidsblockbuddy.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DifficultyAdjusterTest {

    private val adjuster = DifficultyAdjuster()

    @Test
    fun strugglingSignalsEnableAssists() {
        val recommendation = adjuster.recommend(
            DifficultySignals(
                failedPlacements = 6,
                hintUses = 2,
                idleSeconds = 30,
                completionSeconds = null
            )
        )

        assertTrue(recommendation.reducedObjectiveCount)
        assertTrue(recommendation.highlightBlueprint)
        assertTrue(recommendation.increasedResourceDrops)
    }

    @Test
    fun normalSignalsKeepDifficultySteady() {
        val recommendation = adjuster.recommend(
            DifficultySignals(
                failedPlacements = 0,
                hintUses = 0,
                idleSeconds = 5,
                completionSeconds = 90
            )
        )

        assertFalse(recommendation.reducedObjectiveCount)
        assertFalse(recommendation.highlightBlueprint)
        assertFalse(recommendation.increasedResourceDrops)
    }
}
