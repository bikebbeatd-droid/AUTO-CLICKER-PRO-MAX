package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ClickMode
import com.example.engine.EngineState
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
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.RadiantPink
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.AutoClickerViewModel

@Composable
fun HomeScreen(
    viewModel: AutoClickerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val engineState by viewModel.engine.engineState.collectAsState()
    val totalStats by viewModel.totalStats.collectAsState()
    val isAccessibilityActive by viewModel.isAccessibilityEnabled.collectAsState()
    val isOverlayGranted by viewModel.isOverlayGranted.collectAsState()
    val isDisclosureAccepted by viewModel.settingsRepo.hasSeenAccessibilityDisclosure.collectAsState()
    val scope = rememberCoroutineScope()

    var showDisclosureDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .statusBarsPadding()
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Bar Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Auto Clicker",
                            color = TextPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = CyberCyan.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "PRO MAX",
                                color = CyberCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "Cross-Platform Automation Ecosystem",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }

                Row {
                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.CLOUD_SYNC) },
                        modifier = Modifier.testTag("nav_btn_cloud")
                    ) {
                        Icon(imageVector = Icons.Default.CloudSync, contentDescription = "Cloud Sync", tint = CyberCyan)
                    }
                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.STATISTICS) },
                        modifier = Modifier.testTag("nav_btn_stats")
                    ) {
                        Icon(imageVector = Icons.Default.BarChart, contentDescription = "Analytics", tint = ElectricViolet)
                    }
                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.SETTINGS) },
                        modifier = Modifier.testTag("nav_btn_settings")
                    ) {
                        Icon(imageVector = Icons.Default.Palette, contentDescription = "Theme Customizer", tint = RadiantPink)
                    }
                }
            }
        }

        // Permission & Readiness Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "System Readiness",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PermissionStatusPill(
                            title = "Accessibility",
                            isGranted = isAccessibilityActive,
                            onClick = {
                                if (!isDisclosureAccepted) {
                                    showDisclosureDialog = true
                                } else {
                                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                    context.startActivity(intent)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )

                        PermissionStatusPill(
                            title = "Floating Overlay",
                            isGranted = isOverlayGranted,
                            onClick = {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Test Service & Settings Action Buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                scope.launch {
                                    val success = com.example.service.AutoClickerAccessibilityService.testServiceConnection()
                                    android.widget.Toast.makeText(
                                        context,
                                        if (success) "Gesture Dispatch: TEST PASSED!" else "Service Inactive: Enable Accessibility in Settings first",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isAccessibilityActive) NeonEmerald.copy(alpha = 0.2f) else DarkSurfaceVariant
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(38.dp).testTag("btn_home_test_service")
                        ) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = if (isAccessibilityActive) NeonEmerald else TextSecondary, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Test Service", fontSize = 11.sp, color = if (isAccessibilityActive) NeonEmerald else TextSecondary, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(38.dp).testTag("btn_home_open_settings")
                        ) {
                            Icon(imageVector = Icons.Default.Settings, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Open Settings", fontSize = 11.sp, color = CyberCyan, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Engine Running Status
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkSurfaceVariant)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (engineState) {
                                            EngineState.RUNNING -> NeonEmerald
                                            EngineState.PAUSED -> BrightAmber
                                            EngineState.COUNTDOWN -> CyberCyan
                                            else -> TextSecondary
                                        }
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Engine: ${engineState.name}",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (engineState == EngineState.RUNNING || engineState == EngineState.PAUSED) {
                            Button(
                                onClick = { viewModel.stopAutomation() },
                                colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(30.dp).testTag("home_emergency_stop")
                            ) {
                                Icon(imageVector = Icons.Default.Stop, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Stop", fontSize = 11.sp, color = Color.White)
                            }
                        } else {
                            Text(
                                text = "${totalStats.totalClicks} clicks recorded",
                                color = CyberCyan,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // Quick Launch Hero Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, Brush.horizontalGradient(listOf(CyberCyan, ElectricViolet)), RoundedCornerShape(20.dp))
            ) {
                Box(
                    modifier = Modifier
                        .background(Brush.linearGradient(listOf(Color(0xFF0F2B38), Color(0xFF2E1065))))
                        .padding(18.dp)
                ) {
                    Column {
                        Text(
                            text = "Automation Laboratory",
                            color = Color.White,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Interactive touchscreen canvas with movable floating bar",
                            color = Color(0xFFCBD5E1),
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { viewModel.navigateTo(AppScreen.WORKSPACE) },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).height(46.dp).testTag("btn_launch_workspace")
                            ) {
                                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Open Workspace", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            Button(
                                onClick = {
                                    if (isOverlayGranted) {
                                        FloatingOverlayService.start(context)
                                    }
                                    viewModel.navigateTo(AppScreen.WORKSPACE)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet, contentColor = Color.White),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(46.dp).testTag("btn_toggle_floating")
                            ) {
                                Icon(imageVector = Icons.Default.Widgets, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Floating Bar", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }

        // Section 1: Developer & Intelligence Tools
        item {
            Text(
                text = "Pro Max Suite",
                color = TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ModeActionCard(
                        title = "Script Studio",
                        subtitle = "Safe DSL code editor",
                        icon = Icons.Default.Code,
                        iconTint = CyberCyan,
                        onClick = { viewModel.navigateTo(AppScreen.SCRIPT_STUDIO) },
                        modifier = Modifier.weight(1f),
                        testTag = "card_script_studio"
                    )

                    ModeActionCard(
                        title = "AI Assistant",
                        subtitle = "Natural language to DSL",
                        icon = Icons.Default.AutoAwesome,
                        iconTint = ElectricViolet,
                        onClick = { viewModel.navigateTo(AppScreen.AI_ASSISTANT) },
                        modifier = Modifier.weight(1f),
                        testTag = "card_ai_assistant"
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ModeActionCard(
                        title = "Visual Builder",
                        subtitle = "Drag & drop block flow",
                        icon = Icons.Default.Widgets,
                        iconTint = NeonEmerald,
                        onClick = { viewModel.navigateTo(AppScreen.AUTOMATION_BUILDER) },
                        modifier = Modifier.weight(1f),
                        testTag = "card_visual_builder"
                    )

                    ModeActionCard(
                        title = "Marketplace",
                        subtitle = "Community scripts",
                        icon = Icons.Default.Store,
                        iconTint = BrightAmber,
                        onClick = { viewModel.navigateTo(AppScreen.MARKETPLACE) },
                        modifier = Modifier.weight(1f),
                        testTag = "card_marketplace"
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ModeActionCard(
                        title = "Web Automation",
                        subtitle = "Cross-platform studio",
                        icon = Icons.Default.Laptop,
                        iconTint = Color(0xFF38BDF8),
                        onClick = { viewModel.navigateTo(AppScreen.WEB_STUDIO) },
                        modifier = Modifier.weight(1f),
                        testTag = "card_web_studio"
                    )

                    ModeActionCard(
                        title = "Cloud & Devices",
                        subtitle = "Multi-device sync",
                        icon = Icons.Default.CloudSync,
                        iconTint = RadiantPink,
                        onClick = { viewModel.navigateTo(AppScreen.CLOUD_SYNC) },
                        modifier = Modifier.weight(1f),
                        testTag = "card_cloud_sync"
                    )
                }
            }
        }

        // Section 2: Automation Engines
        item {
            Text(
                text = "Gesture Automations",
                color = TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ModeActionCard(
                        title = "Single Tap",
                        subtitle = "High-speed auto clicker",
                        icon = Icons.Default.TouchApp,
                        iconTint = CyberCyan,
                        onClick = {
                            viewModel.selectMode(ClickMode.SINGLE_TAP)
                            viewModel.navigateTo(AppScreen.WORKSPACE)
                        },
                        modifier = Modifier.weight(1f),
                        testTag = "card_single_tap"
                    )

                    ModeActionCard(
                        title = "Multi-Target",
                        subtitle = "Sequenced points",
                        icon = Icons.Default.RadioButtonChecked,
                        iconTint = ElectricViolet,
                        onClick = {
                            viewModel.selectMode(ClickMode.MULTI_TAP)
                            viewModel.navigateTo(AppScreen.MULTI_TARGET)
                        },
                        modifier = Modifier.weight(1f),
                        testTag = "card_multi_tap"
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ModeActionCard(
                        title = "Auto Swipe",
                        subtitle = "Scrolls & gestures",
                        icon = Icons.Default.Swipe,
                        iconTint = RadiantPink,
                        onClick = {
                            viewModel.selectMode(ClickMode.AUTO_SWIPE)
                            viewModel.navigateTo(AppScreen.WORKSPACE)
                        },
                        modifier = Modifier.weight(1f),
                        testTag = "card_auto_swipe"
                    )

                    ModeActionCard(
                        title = "Macro Recorder",
                        subtitle = "Touch sequence capture",
                        icon = Icons.Default.Layers,
                        iconTint = BrightAmber,
                        onClick = {
                            viewModel.selectMode(ClickMode.MACRO_RECORDER)
                            viewModel.navigateTo(AppScreen.MACRO_RECORDER)
                        },
                        modifier = Modifier.weight(1f),
                        testTag = "card_macro_recorder"
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ModeActionCard(
                        title = "Saved Profiles",
                        subtitle = "Presets & custom setups",
                        icon = Icons.Default.FolderSpecial,
                        iconTint = Color(0xFF38BDF8),
                        onClick = { viewModel.navigateTo(AppScreen.PROFILES) },
                        modifier = Modifier.weight(1f),
                        testTag = "card_profiles"
                    )

                    ModeActionCard(
                        title = "Floating Theme",
                        subtitle = "Gradients & icon tints",
                        icon = Icons.Default.Palette,
                        iconTint = NeonEmerald,
                        onClick = { viewModel.navigateTo(AppScreen.SETTINGS) },
                        modifier = Modifier.weight(1f),
                        testTag = "card_themes"
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
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

@Composable
private fun PermissionStatusPill(
    title: String,
    isGranted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (isGranted) NeonEmerald.copy(alpha = 0.15f) else BrightAmber.copy(alpha = 0.15f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isGranted) NeonEmerald.copy(alpha = 0.4f) else BrightAmber.copy(alpha = 0.4f)
        ),
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag("pill_${title.lowercase().replace(' ', '_')}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isGranted) NeonEmerald else BrightAmber,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(text = title, fontSize = 11.sp, color = TextSecondary)
                Text(
                    text = if (isGranted) "Enabled" else "Tap to Enable",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isGranted) NeonEmerald else BrightAmber
                )
            }
        }
    }
}

@Composable
private fun ModeActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 11.sp
            )
        }
    }
}
