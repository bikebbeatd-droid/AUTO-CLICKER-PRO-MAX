package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ClickMode
import com.example.engine.ClickEvent
import com.example.engine.EngineState
import com.example.ui.components.FloatingControlBar
import com.example.ui.components.TargetOverlayBadge
import com.example.ui.theme.BrightAmber
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.AutoClickerViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class VisualRipple(
    val id: Long,
    val x: Float,
    val y: Float,
    val color: Color
)

@Composable
fun WorkspaceScreen(
    viewModel: AutoClickerViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.navigateBack()
    }

    val selectedMode by viewModel.selectedMode.collectAsState()
    val targets by viewModel.activeTargets.collectAsState()
    val activeHighlight by viewModel.engine.activeTargetHighlight.collectAsState()
    val engineState by viewModel.engine.engineState.collectAsState()
    val liveStats by viewModel.engine.liveStats.collectAsState()
    val themeConfig by viewModel.settingsRepo.themeConfig.collectAsState()
    val isFloatingBarVisible by viewModel.isFloatingBarVisibleInApp.collectAsState()

    val ripples = remember { mutableStateListOf<VisualRipple>() }

    // Listen for real-time click events to trigger visual ripples
    LaunchedEffect(Unit) {
        viewModel.engine.clickEvents.collectLatest { event ->
            val rippleColor = runCatching { Color(android.graphics.Color.parseColor(event.colorHex)) }
                .getOrDefault(CyberCyan)
            val ripple = VisualRipple(System.nanoTime(), event.x, event.y, rippleColor)
            ripples.add(ripple)
            if (ripples.size > 8) {
                ripples.removeAt(0)
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        val maxWidthPx = constraints.maxWidth.toFloat()
        val maxHeightPx = constraints.maxHeight.toFloat()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Workspace Top Toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier.testTag("workspace_btn_back")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Interactive Workspace",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tap canvas or drag targets to test gestures",
                            color = CyberCyan,
                            fontSize = 12.sp
                        )
                    }
                }

                // Status pill
                Surface(
                    color = when (engineState) {
                        EngineState.RUNNING -> NeonEmerald.copy(alpha = 0.2f)
                        EngineState.PAUSED -> BrightAmber.copy(alpha = 0.2f)
                        else -> DarkSurfaceVariant
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = engineState.name,
                        color = when (engineState) {
                            EngineState.RUNNING -> NeonEmerald
                            EngineState.PAUSED -> BrightAmber
                            else -> TextSecondary
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            // Mode Selection Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedMode == ClickMode.SINGLE_TAP,
                    onClick = { viewModel.selectMode(ClickMode.SINGLE_TAP) },
                    label = { Text("Single Tap") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyberCyan,
                        selectedLabelColor = Color.Black
                    )
                )
                FilterChip(
                    selected = selectedMode == ClickMode.MULTI_TAP,
                    onClick = { viewModel.selectMode(ClickMode.MULTI_TAP) },
                    label = { Text("Multi-Target") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyberCyan,
                        selectedLabelColor = Color.Black
                    )
                )
                FilterChip(
                    selected = selectedMode == ClickMode.AUTO_SWIPE,
                    onClick = { viewModel.selectMode(ClickMode.AUTO_SWIPE) },
                    label = { Text("Auto Swipe") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyberCyan,
                        selectedLabelColor = Color.Black
                    )
                )
            }

            // Virtual Canvas Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF070A12))
                    .border(1.5.dp, DarkCardBorder, RoundedCornerShape(20.dp))
                    .pointerInput(selectedMode) {
                        detectTapGestures { offset ->
                            if (selectedMode == ClickMode.SINGLE_TAP) {
                                val current = targets.firstOrNull()
                                if (current != null) {
                                    viewModel.updateTargetPosition(current.id, offset.x, offset.y)
                                }
                            } else if (selectedMode == ClickMode.MULTI_TAP) {
                                viewModel.addTarget(offset.x, offset.y)
                            }
                        }
                    }
                    .testTag("workspace_interactive_canvas")
            ) {
                // Background grid lines
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val step = 60.dp.toPx()
                    for (x in 0..size.width.toInt() step step.toInt()) {
                        drawLine(
                            color = Color.White.copy(alpha = 0.04f),
                            start = Offset(x.toFloat(), 0f),
                            end = Offset(x.toFloat(), size.height),
                            strokeWidth = 1f
                        )
                    }
                    for (y in 0..size.height.toInt() step step.toInt()) {
                        drawLine(
                            color = Color.White.copy(alpha = 0.04f),
                            start = Offset(0f, y.toFloat()),
                            end = Offset(size.width, y.toFloat()),
                            strokeWidth = 1f
                        )
                    }

                    // Draw Ripples
                    for (ripple in ripples) {
                        drawCircle(
                            color = ripple.color.copy(alpha = 0.6f),
                            radius = 28.dp.toPx(),
                            center = Offset(ripple.x, ripple.y),
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }
                }

                // Render Targets
                val displayedTargets = if (selectedMode == ClickMode.SINGLE_TAP) {
                    targets.take(1)
                } else {
                    targets
                }

                displayedTargets.forEach { target ->
                    TargetOverlayBadge(
                        target = target,
                        isActive = activeHighlight == target.number,
                        onPositionChanged = { newX, newY ->
                            viewModel.updateTargetPosition(target.id, newX, newY)
                        }
                    )
                }

                // Render Floating Control Bar directly on canvas
                if (isFloatingBarVisible) {
                    FloatingControlBar(
                        themeConfig = themeConfig,
                        engineState = engineState,
                        clickCount = liveStats.sessionClicks,
                        onPlay = { viewModel.startCurrentAutomation() },
                        onPause = { viewModel.pauseAutomation() },
                        onResume = { viewModel.resumeAutomation() },
                        onStop = { viewModel.stopAutomation() },
                        onAddTarget = { viewModel.addTarget() },
                        onRemoveTarget = { viewModel.removeLastTarget() },
                        onOpenSettings = { viewModel.navigateTo(AppScreen.SETTINGS) },
                        onClose = { viewModel.toggleInAppFloatingBar() }
                    )
                }
            }

            // Bottom Quick Control Tray
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Session Clicks: ${liveStats.sessionClicks}",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Speed: ${"%.1f".format(liveStats.currentCps)} CPS • ${liveStats.sessionDurationSec}s elapsed",
                                color = CyberCyan,
                                fontSize = 12.sp
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (engineState == EngineState.RUNNING || engineState == EngineState.PAUSED) {
                                Button(
                                    onClick = { viewModel.stopAutomation() },
                                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("workspace_btn_stop")
                                ) {
                                    Icon(imageVector = Icons.Default.Stop, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Stop", color = Color.White)
                                }
                            } else {
                                Button(
                                    onClick = { viewModel.startCurrentAutomation() },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald, contentColor = Color.Black),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("workspace_btn_start")
                                ) {
                                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Start", fontWeight = FontWeight.Bold)
                                }
                            }

                            if (selectedMode == ClickMode.MULTI_TAP) {
                                IconButton(
                                    onClick = { viewModel.addTarget() },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(DarkSurfaceVariant)
                                        .testTag("workspace_btn_add_target")
                                ) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Target", tint = CyberCyan)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
