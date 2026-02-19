package com.iamconanpeter.kidsblockbuddy.domain

import kotlinx.serialization.Serializable

@Serializable
enum class BlockType {
    EMPTY,
    GRASS,
    WOOD,
    STONE,
    FLOWER
}

@Serializable
data class GridPosition(val x: Int, val y: Int)

@Serializable
data class WorldGrid(
    val width: Int,
    val height: Int,
    val cells: List<BlockType>
) {
    init {
        require(cells.size == width * height) { "Cells must match width*height" }
    }

    fun indexOf(position: GridPosition): Int = position.y * width + position.x

    fun isInBounds(position: GridPosition): Boolean =
        position.x in 0 until width && position.y in 0 until height

    fun blockAt(position: GridPosition): BlockType {
        if (!isInBounds(position)) return BlockType.EMPTY
        return cells[indexOf(position)]
    }

    fun place(position: GridPosition, type: BlockType): WorldGrid {
        if (!isInBounds(position)) return this
        val updated = cells.toMutableList()
        updated[indexOf(position)] = type
        return copy(cells = updated)
    }

    companion object {
        fun empty(width: Int, height: Int): WorldGrid =
            WorldGrid(width = width, height = height, cells = List(width * height) { BlockType.EMPTY })
    }
}

sealed class PlacementResult {
    data class Success(val updated: WorldGrid, val replaced: BlockType) : PlacementResult()
    data object OutOfBounds : PlacementResult()
    data object UnsupportedPlacement : PlacementResult()
}

class PlacementValidator {
    fun placeBlock(
        world: WorldGrid,
        position: GridPosition,
        type: BlockType
    ): PlacementResult {
        if (!world.isInBounds(position)) return PlacementResult.OutOfBounds
        if (type == BlockType.EMPTY) return PlacementResult.UnsupportedPlacement

        val current = world.blockAt(position)
        return PlacementResult.Success(updated = world.place(position, type), replaced = current)
    }

    fun removeBlock(world: WorldGrid, position: GridPosition): PlacementResult {
        if (!world.isInBounds(position)) return PlacementResult.OutOfBounds
        val current = world.blockAt(position)
        return PlacementResult.Success(updated = world.place(position, BlockType.EMPTY), replaced = current)
    }
}
