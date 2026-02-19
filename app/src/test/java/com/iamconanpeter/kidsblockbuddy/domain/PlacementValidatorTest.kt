package com.iamconanpeter.kidsblockbuddy.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlacementValidatorTest {

    private val validator = PlacementValidator()

    @Test
    fun placeBlock_updatesCellWhenInBounds() {
        val world = WorldGrid.empty(3, 3)
        val result = validator.placeBlock(world, GridPosition(1, 1), BlockType.WOOD)

        assertTrue(result is PlacementResult.Success)
        val updated = (result as PlacementResult.Success).updated
        assertEquals(BlockType.WOOD, updated.blockAt(GridPosition(1, 1)))
    }

    @Test
    fun placeBlock_rejectsOutOfBounds() {
        val world = WorldGrid.empty(2, 2)
        val result = validator.placeBlock(world, GridPosition(4, 1), BlockType.GRASS)

        assertTrue(result is PlacementResult.OutOfBounds)
    }

    @Test
    fun undoStack_keepsLast20States() {
        val stack = UndoStack(capacity = 2)
        val s1 = WorldGrid.empty(1, 1)
        val s2 = s1.place(GridPosition(0, 0), BlockType.GRASS)
        val s3 = s2.place(GridPosition(0, 0), BlockType.WOOD)

        stack.push(s1)
        stack.push(s2)
        stack.push(s3)

        assertEquals(2, stack.size())
        assertEquals(s3, stack.popOrNull())
        assertEquals(s2, stack.popOrNull())
    }
}
