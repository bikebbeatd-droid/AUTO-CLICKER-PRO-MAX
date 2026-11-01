package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ClickTarget
import kotlin.math.roundToInt

@Composable
fun TargetOverlayBadge(
    target: ClickTarget,
    isActive: Boolean,
    onPositionChanged: (Float, Float) -> Unit,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var posX by remember(target.x) { mutableFloatStateOf(target.x) }
    var posY by remember(target.y) { mutableFloatStateOf(target.y) }

    val targetColor = runCatching { Color(android.graphics.Color.parseColor(target.colorHex)) }
        .getOrDefault(Color(0xFF00F0FF))

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isActive) 1.45f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = modifier
            .offset { IntOffset((posX - 32.dp.value).roundToInt(), (posY - 32.dp.value).roundToInt()) }
            .pointerInput(target.id) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    posX += dragAmount.x
                    posY += dragAmount.y
                    onPositionChanged(posX, posY)
                }
            }
            .size(64.dp)
            .testTag("target_badge_${target.number}"),
        contentAlignment = Alignment.Center
    ) {
        // Crosshair canvas
        Canvas(modifier = Modifier.size(64.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            val strokeWidth = 2.dp.toPx()

            // Pulse ring if active
            if (isActive) {
                drawCircle(
                    color = targetColor.copy(alpha = 0.35f),
                    radius = (size.width / 2) * pulseScale,
                    center = center
                )
            }

            // Crosshair lines
            drawLine(
                color = targetColor.copy(alpha = 0.7f),
                start = Offset(center.x, 0f),
                end = Offset(center.x, size.height),
                strokeWidth = strokeWidth
            )
            drawLine(
                color = targetColor.copy(alpha = 0.7f),
                start = Offset(0f, center.y),
                end = Offset(size.width, center.y),
                strokeWidth = strokeWidth
            )
        }

        // Center badge
        Box(
            modifier = Modifier
                .size(38.dp)
                .shadow(8.dp, CircleShape)
                .clip(CircleShape)
                .background(targetColor)
                .border(2.dp, Color.White, CircleShape)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${target.number}",
                color = Color.Black,
                fontWeight = FontWeight.Black,
                fontSize = 15.sp
            )
        }
    }
}
