package com.iamconanpeter.kidsblockbuddy.domain

class DailyMissionPlanner {
    fun dayKey(epochMs: Long = System.currentTimeMillis()): Long = epochMs / DAY_MS

    fun missionOfDay(
        missions: List<MissionCard>,
        epochMs: Long = System.currentTimeMillis(),
        completedCount: Int = 0
    ): MissionCard {
        require(missions.isNotEmpty()) { "Mission list cannot be empty" }
        val key = dayKey(epochMs)
        val raw = (key + completedCount) % missions.size.toLong()
        val index = if (raw < 0) (raw + missions.size).toInt() else raw.toInt()
        return missions[index]
    }

    fun nextMissionLinear(current: MissionCard, missions: List<MissionCard>): MissionCard {
        require(missions.isNotEmpty()) { "Mission list cannot be empty" }
        val currentIndex = missions.indexOfFirst { it.id == current.id }
        if (currentIndex == -1) return missions.first()
        return missions[(currentIndex + 1) % missions.size]
    }

    companion object {
        private const val DAY_MS = 24L * 60L * 60L * 1000L
    }
}
