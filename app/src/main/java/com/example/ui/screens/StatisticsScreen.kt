package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.SessionStatsEntity
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StatisticsScreen(
    viewModel: AutoClickerViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.navigateBack()
    }

    val liveStats by viewModel.engine.liveStats.collectAsState()
    val totalStats by viewModel.totalStats.collectAsState()
    val sessionHistory by viewModel.automationRepo.sessionStats.collectAsState(initial = emptyList())
    val activeTargets by viewModel.activeTargets.collectAsState()
    val macros by viewModel.allMacros.collectAsState()

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
                    modifier = Modifier.testTag("stats_back")
                ) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Analytics & Statistics",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Live telemetry & execution metrics",
                        color = CyberCyan,
                        fontSize = 12.sp
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Live Stats 2x2 Grid
            item {
                Text(
                    text = "Live Session Metrics",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard(
                        title = "Session Clicks",
                        value = "${liveStats.sessionClicks}",
                        icon = Icons.Default.TouchApp,
                        color = CyberCyan,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Speed (CPS)",
                        value = "%.1f".format(liveStats.currentCps),
                        icon = Icons.Default.Speed,
                        color = ElectricViolet,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard(
                        title = "Successful",
                        value = "${liveStats.successfulGestures}",
                        icon = Icons.Default.CheckCircle,
                        color = NeonEmerald,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Duration",
                        value = "${liveStats.sessionDurationSec}s",
                        icon = Icons.Default.HourglassBottom,
                        color = BrightAmber,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Lifetime Overview Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Lifetime Totals",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = "Total Clicks", color = TextSecondary, fontSize = 12.sp)
                                Text(text = "${totalStats.totalClicks}", color = CyberCyan, fontWeight = FontWeight.Black, fontSize = 20.sp)
                            }
                            Column {
                                Text(text = "Total Sessions", color = TextSecondary, fontSize = 12.sp)
                                Text(text = "${totalStats.totalSessions}", color = ElectricViolet, fontWeight = FontWeight.Black, fontSize = 20.sp)
                            }
                            Column {
                                Text(text = "Active Targets", color = TextSecondary, fontSize = 12.sp)
                                Text(text = "${activeTargets.size}", color = NeonEmerald, fontWeight = FontWeight.Black, fontSize = 20.sp)
                            }
                            Column {
                                Text(text = "Saved Macros", color = TextSecondary, fontSize = 12.sp)
                                Text(text = "${macros.size}", color = RadiantPink, fontWeight = FontWeight.Black, fontSize = 20.sp)
                            }
                        }
                    }
                }
            }

            // Interactive Activity Bar Chart
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Recent Session Activity",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Clicks per session",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        val displaySessions = sessionHistory.take(8).reversed()
                        val maxClicks = (displaySessions.maxOfOrNull { it.clicks } ?: 100L).coerceAtLeast(10L).toFloat()

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val barWidth = (size.width / (displaySessions.size.coerceAtLeast(1) * 2)).coerceAtMost(32.dp.toPx())
                                val spacing = size.width / displaySessions.size.coerceAtLeast(1)

                                displaySessions.forEachIndexed { i, session ->
                                    val barHeight = (session.clicks.toFloat() / maxClicks) * (size.height * 0.85f)
                                    val left = i * spacing + (spacing - barWidth) / 2
                                    val top = size.height - barHeight

                                    drawRoundRect(
                                        color = CyberCyan,
                                        topLeft = Offset(left, top),
                                        size = Size(barWidth, barHeight),
                                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Recent Sessions Table
            item {
                Text(
                    text = "Session History Log",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            if (sessionHistory.isEmpty()) {
                item {
                    Text(
                        text = "No sessions completed yet. Run automation from Workspace to populate stats.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            } else {
                items(sessionHistory.take(10)) { session ->
                    SessionRowItem(session = session)
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = title, color = TextSecondary, fontSize = 12.sp)
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = value, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}

@Composable
private fun SessionRowItem(session: SessionStatsEntity) {
    val dateStr = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(session.timestamp))
    Surface(
        color = DarkSurfaceVariant,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = session.mode, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text(text = dateStr, color = TextSecondary, fontSize = 11.sp)
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(text = "${session.clicks} clicks", color = CyberCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(text = "${session.durationSeconds}s • ${session.averageIntervalMs}ms avg", color = TextSecondary, fontSize = 11.sp)
            }
        }
    }
}
