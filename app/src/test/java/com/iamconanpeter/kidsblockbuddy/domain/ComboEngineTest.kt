package com.iamconanpeter.kidsblockbuddy.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ComboEngineTest {

    private val engine = ComboEngine(comboWindowMs = 4_500L, comboMilestone = 3)

    @Test
    fun registerPlacement_incrementsStreakInsideWindow() {
        val first = engine.registerPlacement(nowMs = 10_000L, previousPlacementMs = null, currentStreak = 0)
        val second = engine.registerPlacement(nowMs = 13_000L, previousPlacementMs = 10_000L, currentStreak = first.streak)

        assertEquals(1, first.streak)
        assertEquals(2, second.streak)
        assertEquals(0, second.bonusStars)
    }

    @Test
    fun registerPlacement_awardsBonusAtMilestone() {
        val outcome = engine.registerPlacement(nowMs = 14_000L, previousPlacementMs = 11_000L, currentStreak = 2)

        assertEquals(3, outcome.streak)
        assertEquals(1, outcome.bonusStars)
    }

    @Test
    fun registerPlacement_resetsStreakWhenTooSlow() {
        val outcome = engine.registerPlacement(nowMs = 20_000L, previousPlacementMs = 10_000L, currentStreak = 4)

        assertEquals(1, outcome.streak)
        assertEquals(0, outcome.bonusStars)
    }
}
