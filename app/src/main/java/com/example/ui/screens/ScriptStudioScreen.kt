package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Undo
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dsl.InterpreterDebugState
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

@Composable
fun ScriptStudioScreen(
    viewModel: AutoClickerViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.navigateBack()
    }

    val scriptCode by viewModel.dslScriptCode.collectAsState()
    val errors by viewModel.dslErrors.collectAsState()
    val debugState by viewModel.dslInterpreter.debugState.collectAsState()

    var showDebugPanel by remember { mutableStateOf(false) }

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
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { viewModel.navigateBack() },
                    modifier = Modifier.testTag("studio_btn_back")
                ) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "Script Studio Pro",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Safe DSL Editor & Debugger",
                        color = CyberCyan,
                        fontSize = 12.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.undoScript() }, modifier = Modifier.size(34.dp)) {
                    Icon(imageVector = Icons.Default.Undo, contentDescription = "Undo", tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { viewModel.redoScript() }, modifier = Modifier.size(34.dp)) {
                    Icon(imageVector = Icons.Default.Redo, contentDescription = "Redo", tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { viewModel.formatCurrentScript() }, modifier = Modifier.size(34.dp)) {
                    Icon(imageVector = Icons.Default.FormatAlignLeft, contentDescription = "Format", tint = CyberCyan, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { showDebugPanel = !showDebugPanel }, modifier = Modifier.size(34.dp)) {
                    Icon(imageVector = Icons.Default.BugReport, contentDescription = "Debugger", tint = if (showDebugPanel) BrightAmber else TextSecondary, modifier = Modifier.size(18.dp))
                }
            }
        }

        // Snippet Insertion Bar
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val snippets = listOf(
                "TAP 540 960 40",
                "DOUBLE_TAP 540 960",
                "LONG_PRESS 540 960 1000",
                "SWIPE 540 1400 540 500 350",
                "WAIT 250",
                "REPEAT 10 {\n    \n}",
                "IF 1 {\n    \n}",
                "GROUP \"Farm\" {\n    \n}"
            )
            items(snippets) { snip ->
                Surface(
                    color = DarkSurfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
                ) {
                    Text(
                        text = snip.lines().first(),
                        color = CyberCyan,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .clickable {
                                viewModel.updateDslScript(scriptCode + "\n" + snip)
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Code Editor Canvas (With Line Numbers)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF090D18)),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, if (errors.isEmpty()) DarkCardBorder else CrimsonRed.copy(alpha = 0.5f)),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                // Line Number Column
                val lineCount = scriptCode.lines().size.coerceAtLeast(1)
                Column(
                    modifier = Modifier
                        .padding(end = 8.dp, top = 8.dp)
                        .width(28.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    for (i in 1..lineCount) {
                        val isExecutingThisLine = debugState.isRunning && debugState.currentLine == i
                        Text(
                            text = "$i",
                            color = if (isExecutingThisLine) BrightAmber else TextSecondary.copy(alpha = 0.4f),
                            fontWeight = if (isExecutingThisLine) FontWeight.Bold else FontWeight.Normal,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 20.sp
                        )
                    }
                }

                // Text Input
                OutlinedTextField(
                    value = scriptCode,
                    onValueChange = { viewModel.updateDslScript(it) },
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("dsl_editor_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF67E8F9),
                        unfocusedTextColor = Color(0xFF67E8F9),
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                )
            }
        }

        // Live Debugger & Inspector Drawer
        AnimatedVisibility(visible = showDebugPanel) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BrightAmber.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Debugger: Line ${debugState.currentLine} • ${debugState.totalStepsExecuted} steps",
                            color = BrightAmber,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = debugState.lastAction,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }

                    if (debugState.variables.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Scope: " + debugState.variables.entries.joinToString(", ") { "${it.key}=${it.value}" },
                            color = CyberCyan,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Diagnostic Console
        if (errors.isNotEmpty()) {
            Surface(
                color = CrimsonRed.copy(alpha = 0.15f),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CrimsonRed.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Syntax Error: Line ${errors.first().line}: ${errors.first().message}",
                        color = CrimsonRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Bottom Action Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { viewModel.validateCurrentScript() },
                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant, contentColor = CyberCyan),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(48.dp).testTag("btn_validate_dsl")
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Validate")
            }

            if (debugState.isRunning) {
                Button(
                    onClick = { viewModel.stopDslScript() },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(48.dp).testTag("btn_stop_dsl")
                ) {
                    Icon(imageVector = Icons.Default.Stop, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Stop", color = Color.White)
                }
            } else {
                Button(
                    onClick = { viewModel.runDslScript(stepByStep = showDebugPanel) },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald, contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(48.dp).testTag("btn_run_dsl")
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Execute", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
