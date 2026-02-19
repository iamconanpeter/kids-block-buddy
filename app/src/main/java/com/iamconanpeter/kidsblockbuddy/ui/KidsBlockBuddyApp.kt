package com.iamconanpeter.kidsblockbuddy.ui

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
        onOpenSettings = { showParentGate = true }
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
            onClearData = vm::clearAllData
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
        Text("Build your Block Buddy Village! First mission takes under 8 minutes.")
        Spacer(Modifier.height(10.dp))
        Text("Safe by design: no chat, no ads, no scary pressure.")
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
    onOpenSettings: () -> Unit
) {
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
                largerControls = state.settings.largerControls
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("⭐ Stars: ${state.stars}", style = MaterialTheme.typography.titleMedium)
            Text("Hint: ${state.hintText}", style = MaterialTheme.typography.bodyMedium)
            Text(state.safetyMessage, style = MaterialTheme.typography.labelLarge)

            BlockPalette(state.selectedBlock, onSelect)

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
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Mission: ${state.mission.title}", fontWeight = FontWeight.Bold)
            Text("Place at least ${state.mission.minTotalBlocks} blocks")
            state.mission.requiredByType.forEach { (type, required) ->
                val current = state.missionProgress.byType[type] ?: 0
                Text("• ${type.name.lowercase().replaceFirstChar { it.uppercase() }}: $current / $required")
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
    largerControls: Boolean
) {
    val cell = if (largerControls) 56.dp else 44.dp
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(worldHeight) { y ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(worldWidth) { x ->
                    val block = cellProvider(x, y)
                    Box(
                        modifier = Modifier
                            .size(cell)
                            .border(1.dp, Color(0xFF4B5563), RoundedCornerShape(6.dp))
                            .background(block.toColor(), RoundedCornerShape(6.dp))
                            .clickable { onTap(GridPosition(x, y)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BlockPalette(selected: BlockType, onSelect: (BlockType) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Blocks")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(BlockType.GRASS, BlockType.WOOD, BlockType.STONE, BlockType.FLOWER).forEach { type ->
                FilterChip(
                    selected = selected == type,
                    onClick = { onSelect(type) },
                    label = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) }
                )
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

                Text("Camera Sensitivity: ${state.settings.cameraSensitivity}")
                Slider(
                    value = state.settings.cameraSensitivity.toFloat(),
                    onValueChange = { onCameraSensitivity(it.toInt()) },
                    valueRange = 1f..10f,
                    steps = 8
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Blueprint Assist", modifier = Modifier.weight(1f))
                    Switch(checked = state.settings.blueprintAssist, onCheckedChange = onBlueprintAssist)
                }

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

private fun BlockType.toColor(): Color = when (this) {
    BlockType.EMPTY -> Color(0xFF111827)
    BlockType.GRASS -> Color(0xFF22C55E)
    BlockType.WOOD -> Color(0xFFB45309)
    BlockType.STONE -> Color(0xFF9CA3AF)
    BlockType.FLOWER -> Color(0xFFF472B6)
}
