package com.iamconanpeter.kidsblockbuddy.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class DailyMissionPlannerTest {

    private val planner = DailyMissionPlanner()

    @Test
    fun missionOfDay_isDeterministicForSameDayKey() {
        val baseTime = 172800000L // day key = 2

        val missionA = planner.missionOfDay(Missions.phaseTwoPool, epochMs = baseTime, completedCount = 0)
        val missionB = planner.missionOfDay(Missions.phaseTwoPool, epochMs = baseTime + 1_000L, completedCount = 0)

        assertEquals(missionA.id, missionB.id)
    }

    @Test
    fun missionOfDay_shiftsWithCompletedCountForReplayVariety() {
        val baseTime = 172800000L

        val first = planner.missionOfDay(Missions.phaseTwoPool, epochMs = baseTime, completedCount = 0)
        val shifted = planner.missionOfDay(Missions.phaseTwoPool, epochMs = baseTime, completedCount = 1)

        assertNotEquals(first.id, shifted.id)
    }

    @Test
    fun nextMissionLinear_wrapsToFirstMission() {
        val last = Missions.phaseTwoPool.last()
        val next = planner.nextMissionLinear(last, Missions.phaseTwoPool)

        assertEquals(Missions.phaseTwoPool.first().id, next.id)
    }
}
