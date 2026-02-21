package com.iamconanpeter.kidsblockbuddy.domain

data class WelcomeBackRewardOutcome(
    val starsAfterReward: Int,
    val bonusStars: Int,
    val granted: Boolean,
    val hintText: String?
)

class WelcomeBackRewardEngine(
    private val rewardWindowMs: Long = ONE_DAY_MS,
    private val rewardStars: Int = 2
) {
    fun evaluate(
        currentStars: Int,
        lastSnapshotUpdatedAtMs: Long,
        nowEpochMs: Long
    ): WelcomeBackRewardOutcome {
        val elapsedMs = nowEpochMs - lastSnapshotUpdatedAtMs
        if (elapsedMs < rewardWindowMs) {
            return WelcomeBackRewardOutcome(
                starsAfterReward = currentStars,
                bonusStars = 0,
                granted = false,
                hintText = null
            )
        }

        return WelcomeBackRewardOutcome(
            starsAfterReward = currentStars + rewardStars,
            bonusStars = rewardStars,
            granted = true,
            hintText = "Welcome back! You earned +$rewardStars stars for returning."
        )
    }

    companion object {
        private const val ONE_DAY_MS = 24L * 60L * 60L * 1000L
    }
}
