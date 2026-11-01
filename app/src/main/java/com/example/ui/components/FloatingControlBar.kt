package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FloatingThemeConfig
import com.example.engine.EngineState
import com.example.ui.theme.BrightAmber
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.GradientBerryFusion
import com.example.ui.theme.GradientDarkStealth
import com.example.ui.theme.GradientEmeraldGlow
import com.example.ui.theme.GradientNeonCyan
import com.example.ui.theme.GradientPurpleSunset
import com.example.ui.theme.NeonEmerald
import kotlin.math.roundToInt

@Composable
fun FloatingControlBar(
    themeConfig: FloatingThemeConfig,
    engineState: EngineState,
    clickCount: Long,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onAddTarget: () -> Unit,
    onRemoveTarget: () -> Unit,
    onOpenSettings: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var offsetX by remember { mutableFloatStateOf(40f) }
    var offsetY by remember { mutableFloatStateOf(300f) }
    var isExpanded by remember { mutableStateOf(true) }

    val iconColor = runCatching { Color(android.graphics.Color.parseColor(themeConfig.iconColorHex)) }
        .getOrDefault(Color.White)

    // Compute background brush or solid color
    val backgroundModifier = when (themeConfig.bgType) {
        "SOLID" -> {
            val solidColor = runCatching { Color(android.graphics.Color.parseColor(themeConfig.solidColorHex)) }
                .getOrDefault(Color(0xFF161B2E))
            Modifier.background(solidColor.copy(alpha = themeConfig.alpha))
        }
        "IMAGE" -> {
            val skinBrush = when (themeConfig.skinName) {
                "Sunflowers" -> Brush.verticalGradient(listOf(Color(0xFFD97706), Color(0xFF78350F)))
                "Galaxy" -> Brush.verticalGradient(listOf(Color(0xFF3B0764), Color(0xFF0F172A)))
                "Forest" -> Brush.verticalGradient(listOf(Color(0xFF064E3B), Color(0xFF022C22)))
                "Sunset" -> Brush.verticalGradient(listOf(Color(0xFFBE123C), Color(0xFF4C0519)))
                else -> Brush.verticalGradient(listOf(Color(0xFF0284C7), Color(0xFF0C4A6E)))
            }
            Modifier.background(skinBrush, alpha = themeConfig.alpha)
        }
        else -> {
            val brush = when (themeConfig.gradientName) {
                "Neon Cyber" -> GradientNeonCyan
                "Purple Sunset" -> GradientPurpleSunset
                "Emerald Glow" -> GradientEmeraldGlow
                "Pink Nebula" -> GradientBerryFusion
                else -> GradientDarkStealth
            }
            Modifier.background(brush, alpha = themeConfig.alpha)
        }
    }

    Box(
        modifier = modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .pointerInput(themeConfig.isLocked) {
                if (!themeConfig.isLocked) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        offsetX = (offsetX + dragAmount.x).coerceIn(0f, 800f)
                        offsetY = (offsetY + dragAmount.y).coerceIn(100f, 1800f)
                    }
                }
            }
            .shadow(elevation = 16.dp, shape = RoundedCornerShape(28.dp))
            .clip(RoundedCornerShape(28.dp))
            .then(backgroundModifier)
            .border(1.5.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(28.dp))
            .animateContentSize()
            .testTag("floating_control_bar")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp)
        ) {
            // Drag handle / Title header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .width(if (isExpanded) 62.dp else 48.dp)
                    .padding(bottom = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Drag Handle",
                    tint = iconColor.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Minimize" else "Expand",
                        tint = iconColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Click Counter Pill
            if (themeConfig.showClickCounter) {
                Surface(
                    color = Color.Black.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .testTag("floating_counter")
                ) {
                    Text(
                        text = "$clickCount",
                        color = CyberCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Main Action Buttons
            // Play / Pause / Resume
            when (engineState) {
                EngineState.RUNNING -> {
                    FloatingActionButton(
                        icon = Icons.Default.Pause,
                        tint = BrightAmber,
                        bgColor = Color.Black.copy(alpha = 0.35f),
                        contentDescription = "Pause",
                        onClick = onPause
                    )
                }
                EngineState.PAUSED -> {
                    FloatingActionButton(
                        icon = Icons.Default.PlayArrow,
                        tint = NeonEmerald,
                        bgColor = Color.Black.copy(alpha = 0.35f),
                        contentDescription = "Resume",
                        onClick = onResume
                    )
                }
                else -> {
                    FloatingActionButton(
                        icon = Icons.Default.PlayArrow,
                        tint = NeonEmerald,
                        bgColor = Color.Black.copy(alpha = 0.35f),
                        contentDescription = "Start",
                        onClick = onPlay
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(modifier = Modifier.height(6.dp))

                    // Add Target Button
                    FloatingActionButton(
                        icon = Icons.Default.Add,
                        tint = iconColor,
                        bgColor = Color.Black.copy(alpha = 0.25f),
                        contentDescription = "Add Target",
                        onClick = onAddTarget
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Remove Target Button
                    FloatingActionButton(
                        icon = Icons.Default.Remove,
                        tint = iconColor,
                        bgColor = Color.Black.copy(alpha = 0.25f),
                        contentDescription = "Remove Target",
                        onClick = onRemoveTarget
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Settings Button
                    FloatingActionButton(
                        icon = Icons.Default.Settings,
                        tint = iconColor,
                        bgColor = Color.Black.copy(alpha = 0.25f),
                        contentDescription = "Settings",
                        onClick = onOpenSettings
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Emergency Stop Button
                    FloatingActionButton(
                        icon = Icons.Default.Stop,
                        tint = CrimsonRed,
                        bgColor = CrimsonRed.copy(alpha = 0.2f),
                        contentDescription = "Emergency Stop",
                        onClick = onStop
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Close Button
                    FloatingActionButton(
                        icon = Icons.Default.Close,
                        tint = iconColor.copy(alpha = 0.7f),
                        bgColor = Color.Transparent,
                        contentDescription = "Close Floating Bar",
                        onClick = onClose
                    )
                }
            }
        }
    }
}

@Composable
private fun FloatingActionButton(
    icon: ImageVector,
    tint: Color,
    bgColor: Color,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(bgColor)
            .clickable(onClick = onClick)
            .testTag("action_${contentDescription.lowercase().replace(' ', '_')}")
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
    }
}
