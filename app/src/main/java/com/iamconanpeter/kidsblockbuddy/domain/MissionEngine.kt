package com.iamconanpeter.kidsblockbuddy.domain

import kotlinx.serialization.Serializable

@Serializable
data class MissionCard(
    val id: String,
    val title: String,
    val minTotalBlocks: Int,
    val requiredByType: Map<BlockType, Int>,
    val rewardStars: Int
)

data class MissionProgress(
    val totalPlaced: Int,
    val byType: Map<BlockType, Int>,
    val complete: Boolean
)

class MissionEngine {
    fun evaluate(world: WorldGrid, mission: MissionCard): MissionProgress {
        val counts = mutableMapOf<BlockType, Int>().withDefault { 0 }
        for (cell in world.cells) {
            if (cell != BlockType.EMPTY) {
                counts[cell] = counts.getValue(cell) + 1
            }
        }

        val totalPlaced = counts.values.sum()
        val typeGoalsMet = mission.requiredByType.all { (type, required) ->
            counts.getValue(type) >= required
        }

        return MissionProgress(
            totalPlaced = totalPlaced,
            byType = counts,
            complete = totalPlaced >= mission.minTotalBlocks && typeGoalsMet
        )
    }

    fun nextHint(progress: MissionProgress, mission: MissionCard): String {
        mission.requiredByType.forEach { (type, required) ->
            val current = progress.byType[type] ?: 0
            if (current < required) {
                return "Try placing ${required - current} more ${type.name.lowercase()} blocks."
            }
        }

        return if (progress.totalPlaced < mission.minTotalBlocks) {
            "Add ${mission.minTotalBlocks - progress.totalPlaced} more blocks anywhere."
        } else {
            "Great work! Mission complete."
        }
    }
}

object Missions {
    fun firstMission() = MissionCard(
        id = "welcome_park",
        title = "Build a Tiny Park",
        minTotalBlocks = 8,
        requiredByType = mapOf(BlockType.GRASS to 3, BlockType.FLOWER to 2),
        rewardStars = 3
    )
}
