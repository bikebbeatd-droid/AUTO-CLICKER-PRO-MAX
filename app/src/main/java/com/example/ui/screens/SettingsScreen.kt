package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FloatingThemeConfig
import com.example.engine.EngineState
import com.example.service.AutoClickerAccessibilityService
import com.example.service.FloatingBarOverlayContent
import com.example.service.FloatingOverlayService
import com.example.ui.components.SafetyDisclosureDialog
import com.example.ui.theme.BrightAmber
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.GradientBerryFusion
import com.example.ui.theme.GradientDarkStealth
import com.example.ui.theme.GradientEmeraldGlow
import com.example.ui.theme.GradientNeonCyan
import com.example.ui.theme.GradientPurpleSunset
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.RadiantPink
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AutoClickerViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: AutoClickerViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.navigateBack()
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val themeConfig by viewModel.settingsRepo.themeConfig.collectAsState()
    val clickInterval by viewModel.settingsRepo.clickIntervalMs.collectAsState()
    val pressDuration by viewModel.settingsRepo.pressDurationMs.collectAsState()
    val swipeDuration by viewModel.settingsRepo.swipeDurationMs.collectAsState()
    val randomJitter by viewModel.settingsRepo.randomJitterMs.collectAsState()
    val startCountdown by viewModel.settingsRepo.startCountdownSec.collectAsState()
    val maxSessionMins by viewModel.settingsRepo.maxSessionDurationMinutes.collectAsState()
    val hapticEnabled by viewModel.settingsRepo.hapticFeedback.collectAsState()
    val isAccessibilityActive by viewModel.isAccessibilityEnabled.collectAsState()
    val isOverlayGranted by viewModel.isOverlayGranted.collectAsState()

    var showDisclosureDialog by remember { mutableStateOf(false) }
    var selectedCustomizeTab by remember { mutableIntStateOf(0) } // 0 = Control Bar Bg, 1 = Control Bar Icons
    var isTestingService by remember { mutableStateOf(false) }

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
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.navigateBack() },
                modifier = Modifier.testTag("settings_back")
            ) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Custom Settings",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Control bar customization & engine parameters",
                    color = CyberCyan,
                    fontSize = 12.sp
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Main Customization Card (Matching exact user screenshot)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                    modifier = Modifier.fillMaxWidth().testTag("custom_settings_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Customize",
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "You can change control bar icon color and add background of control bar panel with solid color, gradient color and from collection or from gallery photo",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                        )

                        // Tabs: Control Bar Bg | Control Bar Icons
                        TabRow(
                            selectedTabIndex = selectedCustomizeTab,
                            containerColor = DarkSurfaceVariant,
                            contentColor = CyberCyan,
                            indicator = { tabPositions ->
                                TabRowDefaults.Indicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedCustomizeTab]),
                                    color = CyberCyan,
                                    height = 3.dp
                                )
                            },
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .fillMaxWidth()
                        ) {
                            Tab(
                                selected = selectedCustomizeTab == 0,
                                onClick = { selectedCustomizeTab = 0 },
                                text = {
                                    Text(
                                        text = "Control Bar Bg",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (selectedCustomizeTab == 0) CyberCyan else TextSecondary
                                    )
                                }
                            )
                            Tab(
                                selected = selectedCustomizeTab == 1,
                                onClick = { selectedCustomizeTab = 1 },
                                text = {
                                    Text(
                                        text = "Control Bar Icons",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (selectedCustomizeTab == 1) CyberCyan else TextSecondary
                                    )
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // TAB 0: Background Customization
                        if (selectedCustomizeTab == 0) {
                            Text(
                                text = "Control Bar Background",
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // Choice: Solid Color, Gradient Color, Image
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = themeConfig.bgType == "SOLID",
                                    onClick = { viewModel.settingsRepo.updateThemeConfig(themeConfig.copy(bgType = "SOLID")) },
                                    label = { Text("Solid Color", fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CyberCyan,
                                        selectedLabelColor = Color.Black
                                    )
                                )
                                FilterChip(
                                    selected = themeConfig.bgType == "GRADIENT",
                                    onClick = { viewModel.settingsRepo.updateThemeConfig(themeConfig.copy(bgType = "GRADIENT")) },
                                    label = { Text("Gradient Color", fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CyberCyan,
                                        selectedLabelColor = Color.Black
                                    )
                                )
                                FilterChip(
                                    selected = themeConfig.bgType == "IMAGE",
                                    onClick = { viewModel.settingsRepo.updateThemeConfig(themeConfig.copy(bgType = "IMAGE")) },
                                    label = { Text("Image", fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CyberCyan,
                                        selectedLabelColor = Color.Black
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Background Options Rendering
                            when (themeConfig.bgType) {
                                "SOLID" -> {
                                    Text(text = "Color Palette", color = TextSecondary, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    val solidColors = listOf(
                                        "#00F0FF", "#06B6D4", "#3B82F6", "#6366F1",
                                        "#8B5CF6", "#A855F7", "#EC4899", "#F43F5E",
                                        "#EF4444", "#F59E0B", "#10B981", "#14B8A6",
                                        "#161B2E", "#0F172A", "#1E293B", "#000000"
                                    )
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        solidColors.chunked(8).forEach { rowColors ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                rowColors.forEach { hex ->
                                                    val col = runCatching { Color(android.graphics.Color.parseColor(hex)) }
                                                        .getOrDefault(Color.White)
                                                    Box(
                                                        modifier = Modifier
                                                            .size(34.dp)
                                                            .clip(CircleShape)
                                                            .background(col)
                                                            .clickable {
                                                                viewModel.settingsRepo.updateThemeConfig(themeConfig.copy(solidColorHex = hex))
                                                            }
                                                            .then(
                                                                if (themeConfig.solidColorHex.equals(hex, ignoreCase = true)) {
                                                                    Modifier.border(2.5.dp, CyberCyan, CircleShape)
                                                                } else Modifier.border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                                                            ),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        if (themeConfig.solidColorHex.equals(hex, ignoreCase = true)) {
                                                            Icon(
                                                                imageVector = Icons.Default.Check,
                                                                contentDescription = null,
                                                                tint = if (hex == "#00F0FF" || hex == "#10B981" || hex == "#F59E0B") Color.Black else Color.White,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                "GRADIENT" -> {
                                    Text(text = "Gradient Swatches", color = TextSecondary, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    val gradientList = listOf(
                                        "Neon Cyber" to GradientNeonCyan,
                                        "Purple Sunset" to GradientPurpleSunset,
                                        "Emerald Glow" to GradientEmeraldGlow,
                                        "Pink Nebula" to GradientBerryFusion,
                                        "Dark Stealth" to GradientDarkStealth
                                    )
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        gradientList.forEach { (name, brush) ->
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(40.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(brush)
                                                    .clickable {
                                                        viewModel.settingsRepo.updateThemeConfig(themeConfig.copy(gradientName = name))
                                                    }
                                                    .border(
                                                        if (themeConfig.gradientName == name) 2.dp else 1.dp,
                                                        if (themeConfig.gradientName == name) Color.White else Color.White.copy(alpha = 0.2f),
                                                        RoundedCornerShape(10.dp)
                                                    )
                                                    .padding(horizontal = 14.dp),
                                                contentAlignment = Alignment.CenterStart
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(text = name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                    if (themeConfig.gradientName == name) {
                                                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                "IMAGE" -> {
                                    Text(text = "Collection Skins", color = TextSecondary, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    val skins = listOf(
                                        Triple("Sunflowers", "Golden Amber Floral Pattern", Brush.verticalGradient(listOf(Color(0xFFD97706), Color(0xFF78350F)))),
                                        Triple("Galaxy", "Deep Cosmic Starry Nebula", Brush.verticalGradient(listOf(Color(0xFF3B0764), Color(0xFF0F172A)))),
                                        Triple("Forest", "Lush Evergreen Bokeh", Brush.verticalGradient(listOf(Color(0xFF064E3B), Color(0xFF022C22)))),
                                        Triple("Sunset", "Warm Twilight Horizon", Brush.verticalGradient(listOf(Color(0xFFBE123C), Color(0xFF4C0519)))),
                                        Triple("CyberGrid", "Futuristic Neon Digital Grid", Brush.verticalGradient(listOf(Color(0xFF0284C7), Color(0xFF0C4A6E))))
                                    )
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        skins.forEach { (skinName, desc, brush) ->
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(46.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(brush)
                                                    .clickable {
                                                        viewModel.settingsRepo.updateThemeConfig(themeConfig.copy(skinName = skinName))
                                                    }
                                                    .border(
                                                        if (themeConfig.skinName == skinName) 2.dp else 1.dp,
                                                        if (themeConfig.skinName == skinName) CyberCyan else Color.White.copy(alpha = 0.2f),
                                                        RoundedCornerShape(10.dp)
                                                    )
                                                    .padding(horizontal = 14.dp),
                                                contentAlignment = Alignment.CenterStart
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column {
                                                        Text(text = skinName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                        Text(text = desc, color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp)
                                                    }
                                                    if (themeConfig.skinName == skinName) {
                                                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // TAB 1: Icons & Behavior Customization
                        if (selectedCustomizeTab == 1) {
                            Text(text = "Control Bar Icon Color", color = TextSecondary, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            val iconColors = listOf("#FFFFFF", "#00F0FF", "#F59E0B", "#10B981", "#EC4899", "#A855F7", "#FBBF24", "#000000")
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                iconColors.forEach { hex ->
                                    val col = runCatching { Color(android.graphics.Color.parseColor(hex)) }
                                        .getOrDefault(Color.White)
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(col)
                                            .clickable {
                                                viewModel.settingsRepo.updateThemeConfig(themeConfig.copy(iconColorHex = hex))
                                            }
                                            .border(
                                                if (themeConfig.iconColorHex.equals(hex, ignoreCase = true)) 2.5.dp else 1.dp,
                                                if (themeConfig.iconColorHex.equals(hex, ignoreCase = true)) CyberCyan else Color.White.copy(alpha = 0.2f),
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (themeConfig.iconColorHex.equals(hex, ignoreCase = true)) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = if (hex == "#FFFFFF" || hex == "#00F0FF" || hex == "#F59E0B") Color.Black else Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Click Screenshot Toggle (Shown in screenshot)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = "Click Screenshot", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Text(text = "Automatically snapshot target when gesture triggers", color = TextSecondary, fontSize = 11.sp)
                                }
                                Switch(
                                    checked = themeConfig.clickScreenshotEnabled,
                                    onCheckedChange = { viewModel.settingsRepo.updateThemeConfig(themeConfig.copy(clickScreenshotEnabled = it)) },
                                    colors = SwitchDefaults.colors(checkedThumbColor = CyberCyan, checkedTrackColor = CyberCyan.copy(alpha = 0.3f))
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Screenshot Capture Files Row (Shown in screenshot)
                            Surface(
                                color = DarkSurfaceVariant,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        Toast.makeText(context, "Screenshots stored in Pictures/AutoClickerCaptures", Toast.LENGTH_SHORT).show()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Default.Folder, contentDescription = null, tint = BrightAmber, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = "Screenshot Capture Files", color = TextPrimary, fontSize = 13.sp)
                                    }
                                    Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        // Shared Transparency Slider
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Panel Opacity", color = TextSecondary, fontSize = 12.sp)
                            Text(text = "${(themeConfig.alpha * 100).toInt()}%", color = CyberCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Slider(
                            value = themeConfig.alpha,
                            onValueChange = { viewModel.settingsRepo.updateThemeConfig(themeConfig.copy(alpha = it)) },
                            valueRange = 0.3f..1.0f,
                            colors = SliderDefaults.colors(thumbColor = CyberCyan, activeTrackColor = CyberCyan)
                        )

                        // Floating Bar Toggles
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Show Click Counter", color = TextPrimary, fontSize = 13.sp)
                            Switch(
                                checked = themeConfig.showClickCounter,
                                onCheckedChange = { viewModel.settingsRepo.updateThemeConfig(themeConfig.copy(showClickCounter = it)) },
                                colors = SwitchDefaults.colors(checkedThumbColor = CyberCyan, checkedTrackColor = CyberCyan.copy(alpha = 0.3f))
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Lock Panel Position", color = TextPrimary, fontSize = 13.sp)
                            Switch(
                                checked = themeConfig.isLocked,
                                onCheckedChange = { viewModel.settingsRepo.updateThemeConfig(themeConfig.copy(isLocked = it)) },
                                colors = SwitchDefaults.colors(checkedThumbColor = CyberCyan, checkedTrackColor = CyberCyan.copy(alpha = 0.3f))
                            )
                        }

                        // Real-time Preview Pill of Floating Bar
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Live Floating Bar Preview",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF0B0F1A))
                                .border(1.dp, DarkCardBorder, RoundedCornerShape(16.dp))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            FloatingBarOverlayContent(
                                themeConfig = themeConfig,
                                engineState = EngineState.IDLE,
                                clickCount = 42L,
                                onPlay = {},
                                onPause = {},
                                onResume = {},
                                onStop = {},
                                onAddTarget = {},
                                onRemoveTarget = {},
                                onOpenSettings = {},
                                onClose = {}
                            )
                        }
                    }
                }
            }

            // Engine Precision & Timing Parameters Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Tune, contentDescription = null, tint = ElectricViolet, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Engine Parameters",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Default Click Interval
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Click Interval", color = TextSecondary, fontSize = 12.sp)
                            Text(text = "$clickInterval ms", color = CyberCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Slider(
                            value = clickInterval.toFloat(),
                            onValueChange = { viewModel.settingsRepo.setClickInterval(it.toLong()) },
                            valueRange = 20f..2000f,
                            colors = SliderDefaults.colors(thumbColor = CyberCyan, activeTrackColor = CyberCyan)
                        )

                        // Press Duration
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Press Duration", color = TextSecondary, fontSize = 12.sp)
                            Text(text = "$pressDuration ms", color = ElectricViolet, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Slider(
                            value = pressDuration.toFloat(),
                            onValueChange = { viewModel.settingsRepo.setPressDuration(it.toLong()) },
                            valueRange = 10f..1000f,
                            colors = SliderDefaults.colors(thumbColor = ElectricViolet, activeTrackColor = ElectricViolet)
                        )

                        // Random Delay Jitter Range
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Anti-Detection Random Jitter", color = TextSecondary, fontSize = 12.sp)
                            Text(text = "±$randomJitter ms", color = BrightAmber, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Slider(
                            value = randomJitter.toFloat(),
                            onValueChange = { viewModel.settingsRepo.setRandomJitter(it.toLong()) },
                            valueRange = 0f..100f,
                            colors = SliderDefaults.colors(thumbColor = BrightAmber, activeTrackColor = BrightAmber)
                        )

                        // Haptic feedback
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Vibration / Haptic Click", color = TextPrimary, fontSize = 13.sp)
                            Switch(
                                checked = hapticEnabled,
                                onCheckedChange = { viewModel.settingsRepo.setHapticFeedback(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = CyberCyan, checkedTrackColor = CyberCyan.copy(alpha = 0.3f))
                            )
                        }
                    }
                }
            }

            // Permissions & Diagnostics Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = NeonEmerald, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "System Permissions & Diagnostics",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Accessibility Button
                        Button(
                            onClick = {
                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isAccessibilityActive) NeonEmerald.copy(alpha = 0.2f) else CyberCyan
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (isAccessibilityActive) "Accessibility Service: Active & Connected" else "Open Accessibility Settings",
                                color = if (isAccessibilityActive) NeonEmerald else Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Test Service Connection Button (Mandated by PRD)
                        Button(
                            onClick = {
                                scope.launch {
                                    isTestingService = true
                                    val success = AutoClickerAccessibilityService.testServiceConnection()
                                    isTestingService = false
                                    if (success) {
                                        Toast.makeText(context, "Accessibility Service: Gesture dispatch TEST SUCCESSFUL!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Accessibility Service test FAILED: Service is not connected. Please enable in Settings.", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isAccessibilityActive) ElectricViolet else DarkSurfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("btn_test_service")
                        ) {
                            Text(
                                text = if (isTestingService) "Testing Gesture Dispatch..." else "Test Service Connection",
                                color = if (isAccessibilityActive) Color.White else TextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Floating Overlay Permission Button
                        Button(
                            onClick = {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isOverlayGranted) NeonEmerald.copy(alpha = 0.2f) else ElectricViolet
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (isOverlayGranted) "Overlay Permission: Granted" else "Grant Floating Overlay Permission",
                                color = if (isOverlayGranted) NeonEmerald else Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }

    SafetyDisclosureDialog(
        isOpen = showDisclosureDialog,
        onDismiss = { showDisclosureDialog = false },
        onAccept = {
            viewModel.settingsRepo.setAccessibilityDisclosureAccepted(true)
            showDisclosureDialog = false
        }
    )
}
