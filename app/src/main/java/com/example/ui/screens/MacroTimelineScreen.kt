package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GestureType
import com.example.data.model.Macro
import com.example.data.model.MacroAction
import com.example.ui.theme.BrightAmber
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.RadiantPink
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AutoClickerViewModel
import java.util.UUID

@Composable
fun MacroTimelineScreen(
    viewModel: AutoClickerViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.navigateBack()
    }

    val selectedMacro by viewModel.selectedMacro.collectAsState()
    val recordedActions by viewModel.recordedActions.collectAsState()

    var showJsonDialog by remember { mutableStateOf(false) }
    var jsonContent by remember { mutableStateOf("") }
    var isImportMode by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { viewModel.navigateBack() },
                    modifier = Modifier.testTag("timeline_back")
                ) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = selectedMacro?.name ?: "Macro Timeline",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${recordedActions.size} steps in execution chain",
                        color = CyberCyan,
                        fontSize = 12.sp
                    )
                }
            }

            Row {
                IconButton(
                    onClick = {
                        val currentMacro = selectedMacro ?: Macro(name = "Exported Macro", actions = recordedActions)
                        jsonContent = viewModel.automationRepo.exportMacroToJson(currentMacro)
                        isImportMode = false
                        showJsonDialog = true
                    },
                    modifier = Modifier.testTag("btn_export_macro_json")
                ) {
                    Icon(imageVector = Icons.Default.FileUpload, contentDescription = "Export JSON", tint = CyberCyan)
                }

                IconButton(
                    onClick = {
                        jsonContent = ""
                        isImportMode = true
                        showJsonDialog = true
                    },
                    modifier = Modifier.testTag("btn_import_macro_json")
                ) {
                    Icon(imageVector = Icons.Default.FileDownload, contentDescription = "Import JSON", tint = ElectricViolet)
                }
            }
        }

        // Action steps list
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(recordedActions, key = { index, item -> item.id }) { index, action ->
                TimelineActionCard(
                    stepNumber = index + 1,
                    action = action,
                    onUpdate = { updated ->
                        val list = recordedActions.toMutableList()
                        list[index] = updated
                        viewModel.updateRecordedActions(list)
                    },
                    onMoveUp = if (index > 0) {
                        {
                            val list = recordedActions.toMutableList()
                            val temp = list[index]
                            list[index] = list[index - 1]
                            list[index - 1] = temp
                            viewModel.updateRecordedActions(list)
                        }
                    } else null,
                    onMoveDown = if (index < recordedActions.size - 1) {
                        {
                            val list = recordedActions.toMutableList()
                            val temp = list[index]
                            list[index] = list[index + 1]
                            list[index + 1] = temp
                            viewModel.updateRecordedActions(list)
                        }
                    } else null,
                    onDelete = {
                        val list = recordedActions.toMutableList()
                        list.removeAt(index)
                        viewModel.updateRecordedActions(list)
                    }
                )
            }
        }

        // Bottom Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    val newAction = MacroAction(
                        id = UUID.randomUUID().toString(),
                        type = GestureType.TAP,
                        x = 540f,
                        y = 960f,
                        durationMs = 50L,
                        delayAfterMs = 200L,
                        orderIndex = recordedActions.size
                    )
                    viewModel.updateRecordedActions(recordedActions + newAction)
                },
                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant, contentColor = CyberCyan),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(48.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Step")
            }

            Button(
                onClick = {
                    val macroToPlay = selectedMacro ?: Macro(name = "Playing Macro", actions = recordedActions)
                    viewModel.engine.startMacro(macroToPlay)
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald, contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(48.dp).testTag("btn_replay_macro")
            ) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Replay Macro", fontWeight = FontWeight.Bold)
            }
        }
    }

    // JSON Import/Export Dialog
    if (showJsonDialog) {
        AlertDialog(
            onDismissRequest = { showJsonDialog = false },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = if (isImportMode) "Import Macro JSON" else "Exported Macro JSON",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                OutlinedTextField(
                    value = jsonContent,
                    onValueChange = { if (isImportMode) jsonContent = it },
                    readOnly = !isImportMode,
                    maxLines = 10,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = CyberCyan
                    )
                )
            },
            confirmButton = {
                if (isImportMode) {
                    Button(
                        onClick = {
                            val result = viewModel.automationRepo.importMacroFromJson(jsonContent)
                            result.onSuccess { imported ->
                                viewModel.selectMacroForEditing(imported)
                                showJsonDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black)
                    ) {
                        Text("Import", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = { showJsonDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black)
                    ) {
                        Text("Done", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                Button(
                    onClick = { showJsonDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                ) {
                    Text("Close", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun TimelineActionCard(
    stepNumber: Int,
    action: MacroAction,
    onUpdate: (MacroAction) -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(CyberCyan),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$stepNumber",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = action.type.name,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "(${action.x.toInt()}, ${action.y.toInt()})",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }

                Row {
                    if (onMoveUp != null) {
                        IconButton(onClick = onMoveUp, modifier = Modifier.size(28.dp)) {
                            Icon(imageVector = Icons.Default.ArrowUpward, contentDescription = "Move Up", tint = TextSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                    if (onMoveDown != null) {
                        IconButton(onClick = onMoveDown, modifier = Modifier.size(28.dp)) {
                            Icon(imageVector = Icons.Default.ArrowDownward, contentDescription = "Move Down", tint = TextSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = CrimsonRed, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Delay After ms Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Delay After Action", color = TextSecondary, fontSize = 11.sp)
                Text(text = "${action.delayAfterMs} ms", color = CyberCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
            Slider(
                value = action.delayAfterMs.toFloat(),
                onValueChange = { onUpdate(action.copy(delayAfterMs = it.toLong())) },
                valueRange = 10f..2000f,
                colors = SliderDefaults.colors(thumbColor = CyberCyan, activeTrackColor = CyberCyan)
            )
        }
    }
}
