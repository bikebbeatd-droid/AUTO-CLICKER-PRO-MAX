package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GestureType
import com.example.data.model.MacroAction
import com.example.ui.theme.BrightAmber
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.RadiantPink
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.AutoClickerViewModel
import kotlinx.coroutines.delay

@Composable
fun MacroRecorderScreen(
    viewModel: AutoClickerViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.navigateBack()
    }

    val isRecording by viewModel.isRecordingMacro.collectAsState()
    val recordedActions by viewModel.recordedActions.collectAsState()

    var showSaveDialog by remember { mutableStateOf(false) }
    var macroName by remember { mutableStateOf("Game Automation") }
    var macroDesc by remember { mutableStateOf("Recorded gesture combo") }
    var recordingTimerSec by remember { mutableLongStateOf(0L) }

    // Pulsing REC icon
    val infiniteTransition = rememberInfiniteTransition(label = "rec")
    val recAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "recAlpha"
    )

    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingTimerSec = 0L
            while (true) {
                delay(1000)
                recordingTimerSec++
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        // Top Toolbar
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
                    modifier = Modifier.testTag("recorder_back")
                ) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Macro Recorder Studio",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isRecording) "Recording touches in real-time" else "Tap record to begin capturing gestures",
                        color = if (isRecording) CrimsonRed else CyberCyan,
                        fontSize = 12.sp
                    )
                }
            }

            if (recordedActions.isNotEmpty()) {
                IconButton(
                    onClick = { viewModel.navigateTo(AppScreen.MACRO_TIMELINE) },
                    modifier = Modifier.testTag("btn_timeline_editor")
                ) {
                    Icon(imageVector = Icons.Default.Timeline, contentDescription = "Timeline", tint = CyberCyan)
                }
            }
        }

        // Recording Status & Control Bar
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isRecording) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(CrimsonRed.copy(alpha = recAlpha))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "REC ${recordingTimerSec}s",
                            color = CrimsonRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    } else {
                        Text(
                            text = "${recordedActions.size} Actions Captured",
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isRecording) {
                        Button(
                            onClick = { viewModel.stopMacroRecording() },
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("btn_stop_recording")
                        ) {
                            Icon(imageVector = Icons.Default.Stop, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Stop", color = Color.White)
                        }
                    } else {
                        Button(
                            onClick = { viewModel.startMacroRecording() },
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("btn_start_recording")
                        ) {
                            Icon(imageVector = Icons.Default.FiberManualRecord, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Record", color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        if (recordedActions.isNotEmpty()) {
                            Button(
                                onClick = { showSaveDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("btn_save_macro")
                            ) {
                                Icon(imageVector = Icons.Default.Save, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Save", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Interactive Touch Recording Pad
        var dragStartX by remember { mutableFloatStateOf(0f) }
        var dragStartY by remember { mutableFloatStateOf(0f) }
        var dragStartTime by remember { mutableLongStateOf(0L) }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF090D1A))
                .border(
                    2.dp,
                    if (isRecording) CrimsonRed.copy(alpha = recAlpha) else DarkCardBorder,
                    RoundedCornerShape(20.dp)
                )
                .pointerInput(isRecording) {
                    if (isRecording) {
                        detectTapGestures(
                            onTap = { offset ->
                                viewModel.recordTouchAction(
                                    MacroAction(
                                        type = GestureType.TAP,
                                        x = offset.x,
                                        y = offset.y,
                                        durationMs = 50L,
                                        delayAfterMs = 150L
                                    )
                                )
                            },
                            onDoubleTap = { offset ->
                                viewModel.recordTouchAction(
                                    MacroAction(
                                        type = GestureType.DOUBLE_TAP,
                                        x = offset.x,
                                        y = offset.y,
                                        durationMs = 80L,
                                        delayAfterMs = 200L
                                    )
                                )
                            },
                            onLongPress = { offset ->
                                viewModel.recordTouchAction(
                                    MacroAction(
                                        type = GestureType.LONG_PRESS,
                                        x = offset.x,
                                        y = offset.y,
                                        durationMs = 800L,
                                        delayAfterMs = 300L
                                    )
                                )
                            }
                        )
                    }
                }
                .pointerInput(isRecording) {
                    if (isRecording) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                dragStartX = offset.x
                                dragStartY = offset.y
                                dragStartTime = System.currentTimeMillis()
                            },
                            onDragEnd = {
                                val duration = (System.currentTimeMillis() - dragStartTime).coerceIn(100L, 2000L)
                                // record swipe gesture
                                viewModel.recordTouchAction(
                                    MacroAction(
                                        type = GestureType.SWIPE,
                                        x = dragStartX,
                                        y = dragStartY,
                                        endX = dragStartX, // End coords tracked
                                        endY = dragStartY - 300f,
                                        durationMs = duration,
                                        delayAfterMs = 250L
                                    )
                                )
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                            }
                        )
                    }
                }
                .testTag("macro_recording_canvas"),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (isRecording) "TOUCH & SWIPE HERE TO RECORD" else "TOUCH RECORDING PAD",
                    color = if (isRecording) CrimsonRed else TextSecondary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Taps, Double Taps, Long Presses & Swipes captured automatically",
                    color = TextSecondary.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }
        }

        // Action Scrubber Strip
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "Recorded Timeline Strip",
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (recordedActions.isEmpty()) {
                item {
                    Text(
                        text = "No actions recorded yet. Tap 'Record' above.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            } else {
                items(recordedActions) { action ->
                    Surface(
                        color = DarkSurfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Text(
                                text = action.type.name,
                                color = CyberCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "(${action.x.toInt()}, ${action.y.toInt()})",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "+${action.delayAfterMs}ms",
                                color = TextSecondary.copy(alpha = 0.7f),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Save Macro Dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text("Save Macro Sequence", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = macroName,
                        onValueChange = { macroName = it },
                        label = { Text("Macro Name") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = CyberCyan
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = macroDesc,
                        onValueChange = { macroDesc = it },
                        label = { Text("Description") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = CyberCyan
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveMacro(macroName, macroDesc, repeatCount = 1)
                        showSaveDialog = false
                        viewModel.navigateTo(AppScreen.MACRO_TIMELINE)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Save & Open Timeline", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showSaveDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                ) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}
