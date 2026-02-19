package com.iamconanpeter.kidsblockbuddy.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iamconanpeter.kidsblockbuddy.data.AppSettings
import com.iamconanpeter.kidsblockbuddy.data.SettingsRepository
import com.iamconanpeter.kidsblockbuddy.data.WorldSaveRepository
import com.iamconanpeter.kidsblockbuddy.data.WorldSnapshot
import com.iamconanpeter.kidsblockbuddy.domain.BlockType
import com.iamconanpeter.kidsblockbuddy.domain.ComboEngine
import com.iamconanpeter.kidsblockbuddy.domain.DailyMissionPlanner
import com.iamconanpeter.kidsblockbuddy.domain.DifficultyAdjuster
import com.iamconanpeter.kidsblockbuddy.domain.DifficultySignals
import com.iamconanpeter.kidsblockbuddy.domain.GridPosition
import com.iamconanpeter.kidsblockbuddy.domain.MissionCard
import com.iamconanpeter.kidsblockbuddy.domain.MissionEngine
import com.iamconanpeter.kidsblockbuddy.domain.MissionProgress
import com.iamconanpeter.kidsblockbuddy.domain.Missions
import com.iamconanpeter.kidsblockbuddy.domain.PlacementResult
import com.iamconanpeter.kidsblockbuddy.domain.PlacementValidator
import com.iamconanpeter.kidsblockbuddy.domain.UndoStack
import com.iamconanpeter.kidsblockbuddy.domain.WorldGrid
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class FeedbackType {
    PLACE_SUCCESS,
    PLACE_ERROR,
    COMBO,
    MISSION_COMPLETE
}

data class UiFeedbackEvent(
    val id: Long,
    val type: FeedbackType
)

data class MissionCelebration(
    val missionTitle: String,
    val starsEarned: Int,
    val comboBonus: Int,
    val stickerUnlocked: String?,
    val cheerLine: String
)

data class GameUiState(
    val loading: Boolean = true,
    val world: WorldGrid = WorldGrid.empty(10, 6),
    val selectedBlock: BlockType = BlockType.GRASS,
    val eraseMode: Boolean = false,
    val stars: Int = 0,
    val mission: MissionCard = Missions.firstMission(),
    val missionProgress: MissionProgress = MissionProgress(0, emptyMap(), false, 0f),
    val hintText: String = "Tap blocks to start building!",
    val safetyMessage: String = "No chat. No ads. Offline-friendly by default.",
    val settings: AppSettings = AppSettings(),
    val parentGatePassed: Boolean = false,
    val comboStreak: Int = 0,
    val stickersUnlocked: Set<String> = emptySet(),
    val completedMissionIds: Set<String> = emptySet(),
    val lastChangedCell: GridPosition? = null,
    val celebration: MissionCelebration? = null,
    val feedbackEvent: UiFeedbackEvent? = null,
    val todaysMissionTitle: String = Missions.firstMission().title
)

class GameViewModel(app: Application) : AndroidViewModel(app) {
    private val saveRepo = WorldSaveRepository(app)
    private val settingsRepo = SettingsRepository(app)
    private val validator = PlacementValidator()
    private val missionEngine = MissionEngine()
    private val difficultyAdjuster = DifficultyAdjuster()
    private val undoStack = UndoStack(capacity = 20)
    private val missionPlanner = DailyMissionPlanner()
    private val comboEngine = ComboEngine()

    private val _ui = MutableStateFlow(GameUiState())
    val ui: StateFlow<GameUiState> = _ui.asStateFlow()

    private var failedPlacements = 0
    private var hintUses = 0
    private var idleSeconds = 0
    private var missionRewardClaimed = false
    private var idleJob: Job? = null
    private var lastSuccessPlacementMs: Long? = null
    private var comboStreak = 0
    private var feedbackId = 0L
    private var previousDailyMode = false

    init {
        viewModelScope.launch {
            settingsRepo.settings.collect { settings ->
                val shouldApplyDailyPrompt = !previousDailyMode && settings.dailyChallengeMode && !_ui.value.loading
                previousDailyMode = settings.dailyChallengeMode
                _ui.update { it.copy(settings = settings) }
                if (shouldApplyDailyPrompt) {
                    loadDailyPrompt()
                }
            }
        }

        viewModelScope.launch {
            val snapshot = saveRepo.load()
            val initialMission = if (_ui.value.settings.dailyChallengeMode) {
                missionPlanner.missionOfDay(
                    missions = Missions.phaseTwoPool,
                    completedCount = snapshot.completedMissionIds.size
                )
            } else {
                snapshot.activeMission
            }

            val progress = missionEngine.evaluate(snapshot.world, initialMission)
            missionRewardClaimed = progress.complete

            _ui.update {
                it.copy(
                    loading = false,
                    world = snapshot.world,
                    stars = snapshot.stars,
                    mission = initialMission,
                    missionProgress = progress,
                    hintText = missionEngine.nextHint(progress, initialMission),
                    stickersUnlocked = snapshot.stickerBook,
                    completedMissionIds = snapshot.completedMissionIds,
                    todaysMissionTitle = missionPlanner.missionOfDay(Missions.phaseTwoPool).title
                )
            }
            startIdleTicker()
        }
    }

    fun selectBlock(type: BlockType) {
        _ui.update { it.copy(selectedBlock = type, eraseMode = false) }
        idleSeconds = 0
    }

    fun toggleEraseMode() {
        _ui.update { it.copy(eraseMode = !it.eraseMode) }
        idleSeconds = 0
    }

    fun placeAt(position: GridPosition) {
        val state = _ui.value
        val result = if (state.eraseMode) {
            validator.removeBlock(state.world, position)
        } else {
            validator.placeBlock(state.world, position, state.selectedBlock)
        }

        when (result) {
            is PlacementResult.Success -> {
                undoStack.push(state.world)
                idleSeconds = 0

                val nowMs = System.currentTimeMillis()
                val comboOutcome = comboEngine.registerPlacement(nowMs, lastSuccessPlacementMs, comboStreak)
                comboStreak = comboOutcome.streak
                lastSuccessPlacementMs = nowMs

                val progress = missionEngine.evaluate(result.updated, state.mission)
                var missionBonus = 0
                var unlockedSticker: String? = null
                var completedIds = state.completedMissionIds
                var stickerBook = state.stickersUnlocked
                var celebration: MissionCelebration? = null

                if (progress.complete && !missionRewardClaimed) {
                    missionRewardClaimed = true
                    missionBonus = state.mission.rewardStars
                    completedIds = completedIds + state.mission.id
                    if (!stickerBook.contains(state.mission.stickerReward)) {
                        unlockedSticker = state.mission.stickerReward
                        stickerBook = stickerBook + state.mission.stickerReward
                    }
                    celebration = MissionCelebration(
                        missionTitle = state.mission.title,
                        starsEarned = missionBonus,
                        comboBonus = comboOutcome.bonusStars,
                        stickerUnlocked = unlockedSticker,
                        cheerLine = state.mission.celebrationLine
                    )
                }

                val feedbackType = when {
                    celebration != null -> FeedbackType.MISSION_COMPLETE
                    comboOutcome.bonusStars > 0 -> FeedbackType.COMBO
                    else -> FeedbackType.PLACE_SUCCESS
                }

                val hint = when {
                    comboOutcome.bonusStars > 0 -> "Combo x${comboOutcome.streak}! Bonus ⭐ earned."
                    else -> missionEngine.nextHint(progress, state.mission)
                }

                _ui.update {
                    it.copy(
                        world = result.updated,
                        stars = it.stars + missionBonus + comboOutcome.bonusStars,
                        missionProgress = progress,
                        hintText = hint,
                        comboStreak = comboOutcome.streak,
                        lastChangedCell = position,
                        celebration = celebration ?: it.celebration,
                        stickersUnlocked = stickerBook,
                        completedMissionIds = completedIds,
                        feedbackEvent = nextFeedback(feedbackType)
                    )
                }
                persistSnapshot()
            }

            PlacementResult.OutOfBounds,
            PlacementResult.UnsupportedPlacement -> {
                failedPlacements += 1
                comboStreak = 0
                lastSuccessPlacementMs = null
                _ui.update {
                    it.copy(
                        comboStreak = 0,
                        hintText = "Try tapping inside the build board.",
                        feedbackEvent = nextFeedback(FeedbackType.PLACE_ERROR)
                    )
                }
            }
        }
    }

    fun undo() {
        val prev = undoStack.popOrNull() ?: return
        val progress = missionEngine.evaluate(prev, _ui.value.mission)
        comboStreak = 0
        lastSuccessPlacementMs = null

        _ui.update {
            it.copy(
                world = prev,
                missionProgress = progress,
                hintText = missionEngine.nextHint(progress, it.mission),
                comboStreak = 0,
                lastChangedCell = null
            )
        }
        idleSeconds = 0
        persistSnapshot()
    }

    fun requestHint() {
        hintUses += 1
        val state = _ui.value
        val recommendation = difficultyAdjuster.recommend(
            DifficultySignals(
                failedPlacements = failedPlacements,
                hintUses = hintUses,
                idleSeconds = idleSeconds,
                completionSeconds = null
            )
        )

        val base = missionEngine.nextHint(state.missionProgress, state.mission)
        val adaptive = if (recommendation.highlightBlueprint && state.settings.blueprintAssist) {
            " Blueprint assist is ON: place required block colors first."
        } else {
            ""
        }

        _ui.update { it.copy(hintText = base + adaptive) }
    }

    fun loadDailyPrompt() {
        val state = _ui.value
        val mission = missionPlanner.missionOfDay(
            missions = Missions.phaseTwoPool,
            completedCount = state.completedMissionIds.size
        )
        activateMission(
            mission = mission,
            hint = "Daily build: ${mission.title}. ${missionEngine.nextHint(MissionProgress(0, emptyMap(), false, 0f), mission)}"
        )
    }

    fun nextMission() {
        val state = _ui.value
        val next = if (state.settings.dailyChallengeMode) {
            missionPlanner.missionOfDay(
                missions = Missions.phaseTwoPool,
                completedCount = state.completedMissionIds.size
            )
        } else {
            missionPlanner.nextMissionLinear(state.mission, Missions.phaseTwoPool)
        }
        activateMission(next, "New mission unlocked: ${next.title}")
    }

    fun dismissCelebration() {
        _ui.update { it.copy(celebration = null) }
    }

    fun consumeFeedback(eventId: Long) {
        _ui.update { current ->
            if (current.feedbackEvent?.id == eventId) current.copy(feedbackEvent = null) else current
        }
    }

    fun passParentGate() {
        _ui.update { it.copy(parentGatePassed = true) }
    }

    fun resetParentGate() {
        _ui.update { it.copy(parentGatePassed = false) }
    }

    fun setLargerControls(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setLargerControls(enabled) }
    }

    fun setCameraSensitivity(value: Int) {
        viewModelScope.launch { settingsRepo.setCameraSensitivity(value) }
    }

    fun setBlueprintAssist(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setBlueprintAssist(enabled) }
    }

    fun setDailyChallengeMode(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setDailyChallengeMode(enabled) }
    }

    fun setFeedbackCuesEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setFeedbackCuesEnabled(enabled) }
    }

    fun setSensoryCalmMode(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setSensoryCalmMode(enabled) }
    }

    fun clearAllData() {
        viewModelScope.launch {
            saveRepo.clearAll()
            undoStack.clear()
            failedPlacements = 0
            hintUses = 0
            idleSeconds = 0
            missionRewardClaimed = false
            comboStreak = 0
            lastSuccessPlacementMs = null

            val default = WorldSnapshot.default()
            _ui.update {
                it.copy(
                    world = default.world,
                    stars = 0,
                    mission = default.activeMission,
                    missionProgress = missionEngine.evaluate(default.world, default.activeMission),
                    hintText = "Progress reset. Ready for a fresh build!",
                    stickersUnlocked = emptySet(),
                    completedMissionIds = emptySet(),
                    comboStreak = 0,
                    celebration = null,
                    lastChangedCell = null
                )
            }
        }
    }

    private fun activateMission(mission: MissionCard, hint: String) {
        val state = _ui.value
        missionRewardClaimed = false
        comboStreak = 0
        lastSuccessPlacementMs = null
        undoStack.clear()

        val freshWorld = WorldGrid.empty(state.world.width, state.world.height)
        val progress = missionEngine.evaluate(freshWorld, mission)
        _ui.update {
            it.copy(
                world = freshWorld,
                mission = mission,
                missionProgress = progress,
                hintText = hint,
                comboStreak = 0,
                celebration = null,
                lastChangedCell = null
            )
        }
        persistSnapshot()
    }

    private fun persistSnapshot() {
        viewModelScope.launch {
            val state = _ui.value
            saveRepo.save(
                WorldSnapshot(
                    world = state.world,
                    stars = state.stars,
                    activeMission = state.mission,
                    completedMissionIds = state.completedMissionIds,
                    stickerBook = state.stickersUnlocked,
                    updatedAtEpochMs = System.currentTimeMillis()
                )
            )
        }
    }

    private fun nextFeedback(type: FeedbackType): UiFeedbackEvent {
        feedbackId += 1
        return UiFeedbackEvent(id = feedbackId, type = type)
    }

    private fun startIdleTicker() {
        idleJob?.cancel()
        idleJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                idleSeconds += 1
                if (idleSeconds == 20) {
                    _ui.update { it.copy(hintText = "Need help? Tap Hint for a friendly guide.") }
                }
            }
        }
    }
}
