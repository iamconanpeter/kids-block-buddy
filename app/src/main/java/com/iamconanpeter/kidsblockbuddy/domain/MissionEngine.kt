package com.iamconanpeter.kidsblockbuddy.domain

import kotlinx.serialization.Serializable

@Serializable
data class MissionCard(
    val id: String,
    val title: String,
    val minTotalBlocks: Int,
    val requiredByType: Map<BlockType, Int>,
    val rewardStars: Int,
    val stickerReward: String = "starter_tree",
    val celebrationLine: String = "Amazing build, buddy!"
)

data class MissionProgress(
    val totalPlaced: Int,
    val byType: Map<BlockType, Int>,
    val complete: Boolean,
    val completionRatio: Float = 0f
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
            complete = totalPlaced >= mission.minTotalBlocks && typeGoalsMet,
            completionRatio = completionRatio(mission, counts, totalPlaced)
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

    private fun completionRatio(
        mission: MissionCard,
        counts: Map<BlockType, Int>,
        totalPlaced: Int
    ): Float {
        val totalRatio = (totalPlaced.toFloat() / mission.minTotalBlocks.toFloat()).coerceIn(0f, 1f)
        val typeRatios = mission.requiredByType.map { (type, required) ->
            (counts.getOrDefault(type, 0).toFloat() / required.toFloat()).coerceIn(0f, 1f)
        }

        val allRatios = if (typeRatios.isEmpty()) listOf(totalRatio) else listOf(totalRatio) + typeRatios
        return allRatios.average().toFloat()
    }
}

object Missions {
    val phaseTwoPool: List<MissionCard> = listOf(
        MissionCard(
            id = "welcome_park",
            title = "Build a Tiny Park",
            minTotalBlocks = 8,
            requiredByType = mapOf(BlockType.GRASS to 3, BlockType.FLOWER to 2),
            rewardStars = 3,
            stickerReward = "sunny-tree",
            celebrationLine = "Your park looks super cozy!"
        ),
        MissionCard(
            id = "camp_corner",
            title = "Cozy Camp Corner",
            minTotalBlocks = 10,
            requiredByType = mapOf(BlockType.WOOD to 4, BlockType.STONE to 2),
            rewardStars = 4,
            stickerReward = "campfire-badge",
            celebrationLine = "Camp setup complete. High-five!"
        ),
        MissionCard(
            id = "flower_festival",
            title = "Flower Festival",
            minTotalBlocks = 12,
            requiredByType = mapOf(BlockType.FLOWER to 4, BlockType.GRASS to 3),
            rewardStars = 5,
            stickerReward = "flower-crown",
            celebrationLine = "What a colorful festival!"
        ),
        MissionCard(
            id = "stone_bridge",
            title = "Stone Bridge Path",
            minTotalBlocks = 12,
            requiredByType = mapOf(BlockType.STONE to 5, BlockType.WOOD to 3),
            rewardStars = 5,
            stickerReward = "bridge-builder",
            celebrationLine = "Bridge complete. You are a clever builder!"
        )
    )

    fun firstMission() = phaseTwoPool.first()
}
