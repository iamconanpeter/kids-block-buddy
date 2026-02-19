package com.iamconanpeter.kidsblockbuddy.domain

data class DifficultySignals(
    val failedPlacements: Int,
    val hintUses: Int,
    val idleSeconds: Int,
    val completionSeconds: Int?
)

data class DifficultyRecommendation(
    val reducedObjectiveCount: Boolean,
    val highlightBlueprint: Boolean,
    val increasedResourceDrops: Boolean
)

class DifficultyAdjuster {
    fun recommend(signals: DifficultySignals): DifficultyRecommendation {
        val struggling = signals.failedPlacements >= 5 || signals.hintUses >= 2 || signals.idleSeconds >= 25
        return DifficultyRecommendation(
            reducedObjectiveCount = struggling,
            highlightBlueprint = struggling,
            increasedResourceDrops = struggling && (signals.completionSeconds == null || signals.completionSeconds > 240)
        )
    }
}
