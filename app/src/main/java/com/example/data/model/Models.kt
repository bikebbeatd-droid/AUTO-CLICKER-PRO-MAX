package com.example.data.model

import java.util.UUID

enum class GestureType {
    TAP,
    DOUBLE_TAP,
    TRIPLE_TAP,
    LONG_PRESS,
    SWIPE,
    DRAG_DROP
}

enum class ClickMode(val displayName: String, val description: String) {
    SINGLE_TAP("Single Target", "Fast automated tapping on a single point"),
    MULTI_TAP("Multi-Target", "Sequential clicks across multiple customizable points"),
    AUTO_SWIPE("Auto Swipe", "Automated scroll, swipe, or drag gestures"),
    MACRO_RECORDER("Macro Recorder", "Record and replay touch sequences seamlessly"),
    AUTOMATION_BUILDER("Sequence Builder", "Visual block-based scripting logic")
}

data class ClickTarget(
    val id: String = UUID.randomUUID().toString(),
    val number: Int = 1,
    val x: Float = 500f,
    val y: Float = 800f,
    val normX: Float = 0.5f,
    val normY: Float = 0.45f,
    val targetGroupId: String = "default",
    val intervalMs: Long = 100L,
    val durationMs: Long = 50L,
    val delayMs: Long = 0L,
    val randomJitterMs: Long = 0L,
    val enabled: Boolean = true,
    val colorHex: String = "#00F0FF",
    val label: String = "Target 1",
    val orderIndex: Int = 0
)

data class SwipeConfig(
    val startX: Float = 540f,
    val startY: Float = 1400f,
    val endX: Float = 540f,
    val endY: Float = 400f,
    val durationMs: Long = 300L,
    val delayAfterMs: Long = 500L,
    val swipeDirection: String = "UP" // UP, DOWN, LEFT, RIGHT, CUSTOM
)

data class MacroAction(
    val id: String = UUID.randomUUID().toString(),
    val type: GestureType = GestureType.TAP,
    val x: Float = 0f,
    val y: Float = 0f,
    val endX: Float = 0f,
    val endY: Float = 0f,
    val durationMs: Long = 50L,
    val delayAfterMs: Long = 150L,
    val orderIndex: Int = 0
)

data class Macro(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val repeatCount: Int = 1,
    val infiniteLoop: Boolean = false,
    val actions: List<MacroAction> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val folderName: String = "General"
)

enum class SequenceBlockType(val label: String, val iconName: String) {
    TAP("Tap", "TouchApp"),
    DOUBLE_TAP("Double Tap", "TouchApp"),
    LONG_PRESS("Long Press", "PanTool"),
    SWIPE("Swipe", "Swipe"),
    SCROLL("Scroll", "SwapVert"),
    WAIT("Wait / Delay", "HourglassEmpty"),
    REPEAT("Repeat Block", "Loop"),
    STOP("Stop Execution", "Stop"),
    PAUSE("Pause", "Pause"),
    RESUME("Resume", "PlayArrow"),
    RUN_SAVED_MACRO("Run Macro", "PlayCircleFilled")
}

data class SequenceBlock(
    val id: String = UUID.randomUUID().toString(),
    val type: SequenceBlockType,
    val x: Float = 540f,
    val y: Float = 960f,
    val endX: Float = 540f,
    val endY: Float = 400f,
    val durationMs: Long = 100L,
    val delayMs: Long = 300L,
    val repeatCount: Int = 1,
    val macroId: String = "",
    val isEnabled: Boolean = true,
    val orderIndex: Int = 0
)

data class AutomationProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val mode: ClickMode = ClickMode.SINGLE_TAP,
    val targets: List<ClickTarget> = emptyList(),
    val swipeConfig: SwipeConfig? = null,
    val macroId: String? = null,
    val repeatCount: Int = 0, // 0 = infinite
    val intervalMs: Long = 100L,
    val isPreset: Boolean = false
)

data class AutomationStats(
    val totalClicks: Long = 0L,
    val successfulGestures: Long = 0L,
    val failedGestures: Long = 0L,
    val sessionDurationSec: Long = 0L,
    val averageIntervalMs: Long = 100L,
    val clicksPerSecond: Float = 0f,
    val totalMacrosRun: Int = 0,
    val totalSessions: Int = 0,
    val lastSessionTimestamp: Long = 0L
)

data class ScheduledTask(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val profileId: String,
    val scheduleType: String = "ONCE", // ONCE, DAILY, WEEKLY
    val triggerHour: Int = 12,
    val triggerMinute: Int = 0,
    val sessionDurationMinutes: Int = 15,
    val isEnabled: Boolean = true
)

data class FloatingThemeConfig(
    val bgType: String = "GRADIENT", // SOLID, GRADIENT, IMAGE
    val solidColorHex: String = "#161B2E",
    val gradientName: String = "Neon Cyber", // Neon Cyber, Purple Sunset, Emerald Glow, Pink Nebula, Ocean Flow, Dark Stealth
    val skinName: String = "Galaxy", // Sunflowers, Galaxy, Forest, Sunset, CyberGrid
    val iconColorHex: String = "#FFFFFF",
    val alpha: Float = 0.95f,
    val isCompact: Boolean = false,
    val isLocked: Boolean = false,
    val showClickCounter: Boolean = true,
    val clickScreenshotEnabled: Boolean = false,
    val targetDefaultColor: String = "#00F0FF"
)
