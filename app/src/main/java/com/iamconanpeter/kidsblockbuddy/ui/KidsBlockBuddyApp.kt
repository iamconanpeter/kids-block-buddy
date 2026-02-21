package com.iamconanpeter.kidsblockbuddy.ui

import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.iamconanpeter.kidsblockbuddy.domain.BlockType
import com.iamconanpeter.kidsblockbuddy.domain.GridPosition
import kotlin.random.Random

@Composable
fun KidsBlockBuddyApp(vm: GameViewModel = viewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    var started by remember { mutableStateOf(false) }
    var showParentGate by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    val view = LocalView.current

    LaunchedEffect(ui.feedbackEvent?.id) {
        val event = ui.feedbackEvent ?: return@LaunchedEffect
        if (ui.settings.feedbackCuesEnabled) {
            when (event.type) {
                FeedbackType.PLACE_SUCCESS -> {
                    val didHaptic = view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    if (!didHaptic) view.playSoundEffect(SoundEffectConstants.CLICK)
                }

                FeedbackType.PLACE_ERROR -> {
                    val didHaptic = view.performHapticFeedback(HapticFeedbackConstants.REJECT)
                    if (!didHaptic) view.playSoundEffect(SoundEffectConstants.NAVIGATION_DOWN)
                }

                FeedbackType.COMBO -> {
                    val didHaptic = view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    if (!didHaptic) view.playSoundEffect(SoundEffectConstants.CLICK)
                }

                FeedbackType.MISSION_COMPLETE -> {
                    val didHaptic = view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    if (!didHaptic) view.playSoundEffect(SoundEffectConstants.CLICK)
                    view.playSoundEffect(SoundEffectConstants.CLICK)
                }
            }
        }
        vm.consumeFeedback(event.id)
    }

    if (!started) {
        OnboardingScreen(onStart = { started = true })
        return
    }

    if (ui.loading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Loading your village...")
        }
        return
    }

    GameScreen(
        state = ui,
        onSelect = vm::selectBlock,
        onTapCell = vm::placeAt,
        onToggleErase = vm::toggleEraseMode,
        onUndo = vm::undo,
        onHint = vm::requestHint,
        onOpenSettings = { showParentGate = true },
        onDailyPrompt = vm::loadDailyPrompt
    )

    if (showParentGate) {
        ParentGateDialog(
            onDismiss = { showParentGate = false },
            onPassed = {
                vm.passParentGate()
                showParentGate = false
                showSettings = true
            }
        )
    }

    if (showSettings) {
        SettingsDialog(
            state = ui,
            onDismiss = {
                showSettings = false
                vm.resetParentGate()
            },
            onLargerControls = vm::setLargerControls,
            onCameraSensitivity = vm::setCameraSensitivity,
            onBlueprintAssist = vm::setBlueprintAssist,
            onDailyChallengeMode = vm::setDailyChallengeMode,
            onFeedbackCuesEnabled = vm::setFeedbackCuesEnabled,
            onSensoryCalmMode = vm::setSensoryCalmMode,
            onClearData = vm::clearAllData
        )
    }

    ui.celebration?.let { celebration ->
        CelebrationDialog(
            celebration = celebration,
            onDismiss = vm::dismissCelebration,
            onNextMission = {
                vm.dismissCelebration()
                vm.nextMission()
            }
        )
    }
}

@Composable
private fun OnboardingScreen(onStart: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🏡 Kids Block Buddy", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text("Welcome, builder! Your first mission takes about 8 minutes.")
        Spacer(Modifier.height(14.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("How to play", fontWeight = FontWeight.Bold)
                Text("1) Pick a block color")
                Text("2) Tap tiles to build your scene")
                Text("3) Complete mission goals to win stars + stickers")
            }
        }
        Spacer(Modifier.height(12.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Safety promise", fontWeight = FontWeight.Bold)
                Text("• No chat")
                Text("• No ads in child mode")
                Text("• Parent controls behind a gate")
            }
        }
        Spacer(Modifier.height(24.dp))
        Button(onClick = onStart) {
            Text("Start First Mission")
        }
    }
}

@Composable
private fun GameScreen(
    state: GameUiState,
    onSelect: (BlockType) -> Unit,
    onTapCell: (GridPosition) -> Unit,
    onToggleErase: () -> Unit,
    onUndo: () -> Unit,
    onHint: () -> Unit,
    onOpenSettings: () -> Unit,
    onDailyPrompt: () -> Unit
) {
    val animatedStars by animateIntAsState(
        targetValue = state.stars,
        animationSpec = tween(durationMillis = if (state.settings.sensoryCalmMode) 0 else 400),
        label = "stars"
    )

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(2f)
                .fillMaxSize()
        ) {
            MissionCard(state)
            Spacer(Modifier.height(8.dp))
            BuildGrid(
                worldWidth = state.world.width,
                worldHeight = state.world.height,
                cellProvider = { x, y -> state.world.blockAt(GridPosition(x, y)) },
                onTap = onTapCell,
                largerControls = state.settings.largerControls,
                missionRequiredTypes = state.mission.requiredByType.keys,
                blueprintAssist = state.settings.blueprintAssist,
                lastChangedCell = state.lastChangedCell,
                sensoryCalmMode = state.settings.sensoryCalmMode
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("⭐ Stars: $animatedStars", style = MaterialTheme.typography.titleMedium)
            AnimatedVisibility(visible = state.comboStreak >= 2) {
                Text(
                    "🔥 Combo x${state.comboStreak}",
                    color = Color(0xFFEF4444),
                    fontWeight = FontWeight.Bold
                )
            }
            Text("Hint: ${state.hintText}", style = MaterialTheme.typography.bodyMedium)
            Text(state.safetyMessage, style = MaterialTheme.typography.labelLarge)

            Text("Today's Build: ${state.todaysMissionTitle}", style = MaterialTheme.typography.labelLarge)
            OutlinedButton(onClick = onDailyPrompt, modifier = Modifier.fillMaxWidth()) {
                Text("Use Daily Prompt")
            }

            BlockPalette(
                selected = state.selectedBlock,
                onSelect = onSelect,
                mission = state.mission,
                progress = state.missionProgress,
                blueprintAssist = state.settings.blueprintAssist
            )

            StickerShelf(state.stickersUnlocked)

            OutlinedButton(onClick = onToggleErase, modifier = Modifier.fillMaxWidth()) {
                Text(if (state.eraseMode) "Erase Mode: ON" else "Erase Mode")
            }
            OutlinedButton(onClick = onUndo, modifier = Modifier.fillMaxWidth()) {
                Text("Undo (20)")
            }
            Button(onClick = onHint, modifier = Modifier.fillMaxWidth()) {
                Text("Hint")
            }
            OutlinedButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                Text("Parent Settings")
            }
        }
    }
}

@Composable
private fun MissionCard(state: GameUiState) {
    val ratio = state.missionProgress.completionRatio.coerceIn(0f, 1f)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Mission: ${state.mission.title}", fontWeight = FontWeight.Bold)
            LinearProgressIndicator(progress = ratio, modifier = Modifier.fillMaxWidth())
            Text("Progress: ${(ratio * 100).toInt()}%")
            Text("Place at least ${state.mission.minTotalBlocks} blocks")
            state.mission.requiredByType.forEach { (type, required) ->
                val current = state.missionProgress.byType[type] ?: 0
                Text("• ${type.displayName()}: $current / $required")
            }
            if (state.missionProgress.complete) {
                Text("🎉 Complete! You earned ${state.mission.rewardStars} stars")
            }
        }
    }
}

@Composable
private fun BuildGrid(
    worldWidth: Int,
    worldHeight: Int,
    cellProvider: (Int, Int) -> BlockType,
    onTap: (GridPosition) -> Unit,
    largerControls: Boolean,
    missionRequiredTypes: Set<BlockType>,
    blueprintAssist: Boolean,
    lastChangedCell: GridPosition?,
    sensoryCalmMode: Boolean
) {
    val baseCell = if (largerControls) 56.dp else 44.dp
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(worldHeight) { y ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(worldWidth) { x ->
                    val block = cellProvider(x, y)
                    val position = GridPosition(x, y)
                    val highlight = blueprintAssist && block != BlockType.EMPTY && missionRequiredTypes.contains(block)
                    val isLastChanged = lastChangedCell == position
                    val scale by animateFloatAsState(
                        targetValue = if (isLastChanged && !sensoryCalmMode) 1.12f else 1f,
                        animationSpec = tween(durationMillis = if (sensoryCalmMode) 0 else 220),
                        label = "cell-scale"
                    )

                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                            .size(baseCell)
                            .border(
                                width = if (highlight) 2.dp else 1.dp,
                                color = if (highlight) Color(0xFFF59E0B) else Color(0xFF4B5563),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .background(block.toColor(), RoundedCornerShape(6.dp))
                            .clickable { onTap(position) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BlockPalette(
    selected: BlockType,
    onSelect: (BlockType) -> Unit,
    mission: com.iamconanpeter.kidsblockbuddy.domain.MissionCard,
    progress: com.iamconanpeter.kidsblockbuddy.domain.MissionProgress,
    blueprintAssist: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Blocks")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(BlockType.GRASS, BlockType.WOOD, BlockType.STONE, BlockType.FLOWER).forEach { type ->
                val required = mission.requiredByType[type] ?: 0
                val current = progress.byType[type] ?: 0
                val label = if (blueprintAssist && required > 0) {
                    "${type.displayName()} $current/$required"
                } else {
                    type.displayName()
                }

                FilterChip(
                    selected = selected == type,
                    onClick = { onSelect(type) },
                    label = {
                        Text(
                            if (blueprintAssist && required > 0) "⭐ $label" else label
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun StickerShelf(stickers: Set<String>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Sticker Shelf", fontWeight = FontWeight.Bold)
            if (stickers.isEmpty()) {
                Text("Finish missions to unlock your first sticker!")
            } else {
                stickers.take(6).forEach { sticker ->
                    Text("🏅 ${sticker.replace('-', ' ').replaceFirstChar { it.uppercase() }}")
                }
            }
        }
    }
}

@Composable
private fun ParentGateDialog(onDismiss: () -> Unit, onPassed: () -> Unit) {
    val a = remember { Random.nextInt(2, 9) }
    val b = remember { Random.nextInt(2, 9) }
    var answer by remember { mutableIntStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Parent Gate") },
        text = {
            Column {
                Text("For grown-ups: solve this quick check")
                Spacer(Modifier.height(8.dp))
                Text("$a + $b = ?")
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(a + b, a + b + 1, a + b - 1).shuffled().forEach { option ->
                        OutlinedButton(onClick = { answer = option }) { Text(option.toString()) }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (answer == a + b) onPassed()
            }) {
                Text("Continue")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun SettingsDialog(
    state: GameUiState,
    onDismiss: () -> Unit,
    onLargerControls: (Boolean) -> Unit,
    onCameraSensitivity: (Int) -> Unit,
    onBlueprintAssist: (Boolean) -> Unit,
    onDailyChallengeMode: (Boolean) -> Unit,
    onFeedbackCuesEnabled: (Boolean) -> Unit,
    onSensoryCalmMode: (Boolean) -> Unit,
    onClearData: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Parent Safety Settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Privacy Summary:")
                Text("• No chat with strangers")
                Text("• No ads in child mode")
                Text("• Local data controls")

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Larger Controls", modifier = Modifier.weight(1f))
                    Switch(checked = state.settings.largerControls, onCheckedChange = onLargerControls)
                }
                Text("Bigger tiles and buttons for easier tapping.", style = MaterialTheme.typography.labelSmall)

                Text("Camera Sensitivity: ${state.settings.cameraSensitivity}")
                Slider(
                    value = state.settings.cameraSensitivity.toFloat(),
                    onValueChange = { onCameraSensitivity(it.toInt()) },
                    valueRange = 1f..10f,
                    steps = 8
                )
                Text("Adjusts look movement speed if camera controls are used.", style = MaterialTheme.typography.labelSmall)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Blueprint Assist", modifier = Modifier.weight(1f))
                    Switch(checked = state.settings.blueprintAssist, onCheckedChange = onBlueprintAssist)
                }
                Text("Highlights mission colors and adds guided block labels.", style = MaterialTheme.typography.labelSmall)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Daily Challenge Mode", modifier = Modifier.weight(1f))
                    Switch(checked = state.settings.dailyChallengeMode, onCheckedChange = onDailyChallengeMode)
                }
                Text("Auto-loads a fresh daily build prompt for replayability.", style = MaterialTheme.typography.labelSmall)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Feedback Cues", modifier = Modifier.weight(1f))
                    Switch(checked = state.settings.feedbackCuesEnabled, onCheckedChange = onFeedbackCuesEnabled)
                }
                Text("Plays gentle click sounds or haptic taps for actions.", style = MaterialTheme.typography.labelSmall)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Sensory Calm Mode", modifier = Modifier.weight(1f))
                    Switch(checked = state.settings.sensoryCalmMode, onCheckedChange = onSensoryCalmMode)
                }
                Text("Reduces motion and quick effects for calmer play.", style = MaterialTheme.typography.labelSmall)

                Button(onClick = onClearData, modifier = Modifier.fillMaxWidth()) {
                    Text("Clear Local Data")
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Done") }
        }
    )
}

@Composable
private fun CelebrationDialog(
    celebration: MissionCelebration,
    onDismiss: () -> Unit,
    onNextMission: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🎊 Mission Complete!") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(celebration.cheerLine)
                Text("Mission: ${celebration.missionTitle}")
                Text("Stars earned: +${celebration.starsEarned}")
                if (celebration.comboBonus > 0) {
                    Text("Combo bonus: +${celebration.comboBonus} ⭐")
                }
                celebration.stickerUnlocked?.let {
                    Text("Sticker unlocked: 🏅 ${it.replace('-', ' ').replaceFirstChar { c -> c.uppercase() }}")
                }
            }
        },
        confirmButton = {
            Button(onClick = onNextMission) { Text("Next Mission") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Keep Building") }
        }
    )
}

private fun BlockType.toColor(): Color = when (this) {
    BlockType.EMPTY -> Color(0xFF111827)
    BlockType.GRASS -> Color(0xFF22C55E)
    BlockType.WOOD -> Color(0xFFB45309)
    BlockType.STONE -> Color(0xFF9CA3AF)
    BlockType.FLOWER -> Color(0xFFF472B6)
}

private fun BlockType.displayName(): String = name.lowercase().replaceFirstChar { it.uppercase() }
