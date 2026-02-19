package com.iamconanpeter.kidsblockbuddy.data

import com.iamconanpeter.kidsblockbuddy.domain.MissionCard
import com.iamconanpeter.kidsblockbuddy.domain.Missions
import com.iamconanpeter.kidsblockbuddy.domain.WorldGrid
import kotlinx.serialization.Serializable

@Serializable
data class WorldSnapshot(
    val world: WorldGrid,
    val stars: Int,
    val activeMission: MissionCard,
    val completedMissionIds: Set<String> = emptySet(),
    val stickerBook: Set<String> = emptySet(),
    val updatedAtEpochMs: Long
) {
    companion object {
        fun default(): WorldSnapshot = WorldSnapshot(
            world = WorldGrid.empty(width = 10, height = 6),
            stars = 0,
            activeMission = Missions.firstMission(),
            completedMissionIds = emptySet(),
            stickerBook = emptySet(),
            updatedAtEpochMs = System.currentTimeMillis()
        )
    }
}
