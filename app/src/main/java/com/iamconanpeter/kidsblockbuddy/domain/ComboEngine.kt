package com.iamconanpeter.kidsblockbuddy.domain

data class ComboOutcome(
    val streak: Int,
    val bonusStars: Int
)

class ComboEngine(
    private val comboWindowMs: Long = 4_500L,
    private val comboMilestone: Int = 3
) {
    fun registerPlacement(
        nowMs: Long,
        previousPlacementMs: Long?,
        currentStreak: Int
    ): ComboOutcome {
        val nextStreak = if (previousPlacementMs != null && nowMs - previousPlacementMs <= comboWindowMs) {
            currentStreak + 1
        } else {
            1
        }

        val bonus = if (nextStreak > 1 && nextStreak % comboMilestone == 0) 1 else 0
        return ComboOutcome(streak = nextStreak, bonusStars = bonus)
    }
}
