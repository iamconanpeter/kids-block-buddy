package com.iamconanpeter.kidsblockbuddy.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iamconanpeter.kidsblockbuddy.data.AppSettings
import com.iamconanpeter.kidsblockbuddy.data.SettingsRepository
import com.iamconanpeter.kidsblockbuddy.data.WorldSaveRepository
import com.iamconanpeter.kidsblockbuddy.data.WorldSnapshot
import com.iamconanpeter.kidsblockbuddy.domain.BlockType
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

data class GameUiState(
    val loading: Boolean = true,
    val world: WorldGrid = WorldGrid.empty(10, 6),
    val selectedBlock: BlockType = BlockType.GRASS,
    val eraseMode: Boolean = false,
    val stars: Int = 0,
    val mission: MissionCard = Missions.firstMission(),
    val missionProgress: MissionProgress = MissionProgress(0, emptyMap(), false),
    val hintText: String = "Tap blocks to start building!",
    val safetyMessage: String = "No chat. No ads. Offline-friendly by default.",
    val settings: AppSettings = AppSettings(largerControls = false, cameraSensitivity = 5, blueprintAssist = true),
    val parentGatePassed: Boolean = false
)

class GameViewModel(app: Application) : AndroidViewModel(app) {
    private val saveRepo = WorldSaveRepository(app)
    private val settingsRepo = SettingsRepository(app)
    private val validator = PlacementValidator()
    private val missionEngine = MissionEngine()
    private val difficultyAdjuster = DifficultyAdjuster()
    private val undoStack = UndoStack(capacity = 20)

    private val _ui = MutableStateFlow(GameUiState())
    val ui: StateFlow<GameUiState> = _ui.asStateFlow()

    private var failedPlacements = 0
    private var hintUses = 0
    private var idleSeconds = 0
    private var missionRewardClaimed = false
    private var idleJob: Job? = null

    init {
        viewModelScope.launch {
            settingsRepo.settings.collect { settings ->
                _ui.update { it.copy(settings = settings) }
            }
        }
        viewModelScope.launch {
            val snapshot = saveRepo.load()
            val progress = missionEngine.evaluate(snapshot.world, snapshot.activeMission)
            missionRewardClaimed = progress.complete
            _ui.update {
                it.copy(
                    loading = false,
                    world = snapshot.world,
                    stars = snapshot.stars,
                    mission = snapshot.activeMission,
                    missionProgress = progress,
                    hintText = missionEngine.nextHint(progress, snapshot.activeMission)
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
                val progress = missionEngine.evaluate(result.updated, state.mission)
                val bonus = if (progress.complete && !missionRewardClaimed) {
                    missionRewardClaimed = true
                    state.mission.rewardStars
                } else {
                    0
                }
                _ui.update {
                    it.copy(
                        world = result.updated,
                        stars = it.stars + bonus,
                        missionProgress = progress,
                        hintText = missionEngine.nextHint(progress, state.mission)
                    )
                }
                persistSnapshot()
            }

            PlacementResult.OutOfBounds,
            PlacementResult.UnsupportedPlacement -> {
                failedPlacements += 1
                _ui.update { it.copy(hintText = "Try tapping inside the build board.") }
            }
        }
    }

    fun undo() {
        val prev = undoStack.popOrNull() ?: return
        val progress = missionEngine.evaluate(prev, _ui.value.mission)
        _ui.update {
            it.copy(
                world = prev,
                missionProgress = progress,
                hintText = missionEngine.nextHint(progress, it.mission)
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
            " Blueprint assist is ON: focus on filling highlighted goals first."
        } else {
            ""
        }

        _ui.update { it.copy(hintText = base + adaptive) }
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

    fun clearAllData() {
        viewModelScope.launch {
            saveRepo.clearAll()
            undoStack.clear()
            failedPlacements = 0
            hintUses = 0
            idleSeconds = 0
            missionRewardClaimed = false

            val default = WorldSnapshot.default()
            _ui.update {
                it.copy(
                    world = default.world,
                    stars = 0,
                    mission = default.activeMission,
                    missionProgress = missionEngine.evaluate(default.world, default.activeMission),
                    hintText = "Progress reset. Ready for a fresh build!"
                )
            }
        }
    }

    private fun persistSnapshot() {
        viewModelScope.launch {
            val state = _ui.value
            saveRepo.save(
                WorldSnapshot(
                    world = state.world,
                    stars = state.stars,
                    activeMission = state.mission,
                    updatedAtEpochMs = System.currentTimeMillis()
                )
            )
        }
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
