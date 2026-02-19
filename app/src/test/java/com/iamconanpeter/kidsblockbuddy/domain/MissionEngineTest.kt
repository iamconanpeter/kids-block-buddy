package com.iamconanpeter.kidsblockbuddy.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MissionEngineTest {

    private val engine = MissionEngine()

    @Test
    fun missionIsCompleteWhenRequirementsMet() {
        var world = WorldGrid.empty(4, 2)
        world = world.place(GridPosition(0, 0), BlockType.GRASS)
        world = world.place(GridPosition(1, 0), BlockType.GRASS)
        world = world.place(GridPosition(2, 0), BlockType.GRASS)
        world = world.place(GridPosition(3, 0), BlockType.FLOWER)
        world = world.place(GridPosition(0, 1), BlockType.FLOWER)
        world = world.place(GridPosition(1, 1), BlockType.STONE)
        world = world.place(GridPosition(2, 1), BlockType.STONE)
        world = world.place(GridPosition(3, 1), BlockType.STONE)

        val progress = engine.evaluate(world, Missions.firstMission())
        assertTrue(progress.complete)
    }

    @Test
    fun missionIncompleteWhenNotEnoughFlowers() {
        var world = WorldGrid.empty(4, 2)
        repeat(8) { index ->
            world = world.place(GridPosition(index % 4, index / 4), BlockType.GRASS)
        }

        val progress = engine.evaluate(world, Missions.firstMission())
        assertFalse(progress.complete)
    }

    @Test
    fun hintSuggestsMissingRequirement() {
        val world = WorldGrid.empty(3, 3).place(GridPosition(0, 0), BlockType.GRASS)
        val progress = engine.evaluate(world, Missions.firstMission())

        val hint = engine.nextHint(progress, Missions.firstMission())
        assertTrue(hint.contains("grass") || hint.contains("flower"))
    }
}
