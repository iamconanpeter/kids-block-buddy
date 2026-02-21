package com.iamconanpeter.kidsblockbuddy.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WelcomeBackRewardEngineTest {

    private val engine = WelcomeBackRewardEngine(rewardWindowMs = 86_400_000L, rewardStars = 2)

    @Test
    fun evaluate_grantsRewardAfterWindow() {
        val outcome = engine.evaluate(
            currentStars = 5,
            lastSnapshotUpdatedAtMs = 1_000L,
            nowEpochMs = 86_401_000L
        )

        assertTrue(outcome.granted)
        assertEquals(2, outcome.bonusStars)
        assertEquals(7, outcome.starsAfterReward)
    }

    @Test
    fun evaluate_grantsRewardAtExactBoundary() {
        val outcome = engine.evaluate(
            currentStars = 4,
            lastSnapshotUpdatedAtMs = 5_000L,
            nowEpochMs = 86_405_000L
        )

        assertTrue(outcome.granted)
        assertEquals(6, outcome.starsAfterReward)
    }

    @Test
    fun evaluate_doesNotGrantInsideWindow() {
        val outcome = engine.evaluate(
            currentStars = 5,
            lastSnapshotUpdatedAtMs = 10_000L,
            nowEpochMs = 86_409_999L
        )

        assertFalse(outcome.granted)
        assertEquals(0, outcome.bonusStars)
        assertEquals(5, outcome.starsAfterReward)
        assertNull(outcome.hintText)
    }

    @Test
    fun evaluate_immediateReopenAfterGrantDoesNotGrantAgain() {
        val first = engine.evaluate(
            currentStars = 8,
            lastSnapshotUpdatedAtMs = 2_000L,
            nowEpochMs = 86_402_000L
        )
        val second = engine.evaluate(
            currentStars = first.starsAfterReward,
            lastSnapshotUpdatedAtMs = 86_402_000L,
            nowEpochMs = 86_405_000L
        )

        assertTrue(first.granted)
        assertFalse(second.granted)
        assertEquals(10, second.starsAfterReward)
    }
}
