package com.example.engine

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.data.model.AutomationProfile
import com.example.data.model.ClickMode
import com.example.data.model.ClickTarget
import com.example.data.model.GestureType
import com.example.data.model.Macro
import com.example.data.model.MacroAction
import com.example.data.model.SequenceBlock
import com.example.data.model.SequenceBlockType
import com.example.data.model.SwipeConfig
import com.example.data.repository.AutomationRepository
import com.example.data.repository.SettingsRepository
import com.example.service.AutoClickerAccessibilityService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class EngineState {
    IDLE,
    COUNTDOWN,
    RUNNING,
    PAUSED,
    STOPPED
}

data class ClickEvent(
    val x: Float,
    val y: Float,
    val targetNumber: Int,
    val colorHex: String,
    val gestureType: GestureType,
    val timestamp: Long = System.currentTimeMillis()
)

data class LiveAutomationStats(
    val sessionClicks: Long = 0L,
    val successfulGestures: Long = 0L,
    val failedGestures: Long = 0L,
    val sessionDurationSec: Long = 0L,
    val currentCps: Float = 0f,
    val activeTargetIndex: Int = 0,
    val currentLoop: Int = 0,
    val totalLoops: Int = 0
)

class GestureAutomationEngine(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val automationRepository: AutomationRepository
) {
    private val engineScope = CoroutineScope(Dispatchers.Default)
    private var automationJob: Job? = null
    private var timerJob: Job? = null

    private val _engineState = MutableStateFlow(EngineState.IDLE)
    val engineState: StateFlow<EngineState> = _engineState.asStateFlow()

    private val _liveStats = MutableStateFlow(LiveAutomationStats())
    val liveStats: StateFlow<LiveAutomationStats> = _liveStats.asStateFlow()

    private val _clickEvents = MutableSharedFlow<ClickEvent>(extraBufferCapacity = 64)
    val clickEvents: SharedFlow<ClickEvent> = _clickEvents.asSharedFlow()

    private val _activeTargetHighlight = MutableStateFlow<Int?>(null)
    val activeTargetHighlight: StateFlow<Int?> = _activeTargetHighlight.asStateFlow()

    private val _countdownValue = MutableStateFlow(0)
    val countdownValue: StateFlow<Int> = _countdownValue.asStateFlow()

    private var sessionStartTime = 0L
    private var lastRecordedClickTime = 0L
    private var activeModeName = "SINGLE_TAP"

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun startSingleTarget(target: ClickTarget, repeatCount: Int = 0) {
        startTargets(listOf(target), repeatCount, modeName = "SINGLE_TAP")
    }

    fun startTargets(targets: List<ClickTarget>, repeatCount: Int = 0, modeName: String = "MULTI_TAP") {
        if (targets.isEmpty()) return
        stop()
        activeModeName = modeName

        automationJob = engineScope.launch {
            handleCountdown()
            initSession(totalLoops = repeatCount)

            val enabledTargets = targets.filter { it.enabled }.sortedBy { it.orderIndex }
            if (enabledTargets.isEmpty()) {
                stop()
                return@launch
            }

            var loop = 0
            while (_engineState.value == EngineState.RUNNING || _engineState.value == EngineState.PAUSED) {
                if (_engineState.value == EngineState.PAUSED) {
                    delay(100)
                    continue
                }

                if (repeatCount > 0 && loop >= repeatCount) {
                    break
                }
                loop++
                _liveStats.value = _liveStats.value.copy(currentLoop = loop)

                for ((index, target) in enabledTargets.withIndex()) {
                    while (_engineState.value == EngineState.PAUSED) {
                        delay(100)
                    }
                    if (_engineState.value != EngineState.RUNNING) break

                    _activeTargetHighlight.value = target.number
                    _liveStats.value = _liveStats.value.copy(activeTargetIndex = index)

                    // Execute tap at target coordinates
                    val success = dispatchTap(target.x, target.y, target.durationMs)
                    recordGestureResult(
                        success = success,
                        x = target.x,
                        y = target.y,
                        targetNumber = target.number,
                        colorHex = target.colorHex,
                        gestureType = if (target.durationMs > 400L) GestureType.LONG_PRESS else GestureType.TAP
                    )

                    // Calculate interval with jitter
                    val jitter = target.randomJitterMs.coerceAtLeast(0L)
                    val jitterOffset = if (jitter > 0) Random.nextLong(-jitter, jitter + 1) else 0L
                    val finalInterval = (target.intervalMs + jitterOffset).coerceAtLeast(15L)

                    delay(finalInterval)
                }

                // Check maximum session duration
                val maxMinutes = settingsRepository.maxSessionDurationMinutes.value
                val elapsedMinutes = (System.currentTimeMillis() - sessionStartTime) / 60000
                if (maxMinutes in 1..elapsedMinutes.toInt()) {
                    break
                }
            }
            finishSession()
        }
    }

    fun startSwipe(config: SwipeConfig, repeatCount: Int = 0) {
        stop()
        activeModeName = "AUTO_SWIPE"

        automationJob = engineScope.launch {
            handleCountdown()
            initSession(totalLoops = repeatCount)

            var loop = 0
            while (_engineState.value == EngineState.RUNNING || _engineState.value == EngineState.PAUSED) {
                if (_engineState.value == EngineState.PAUSED) {
                    delay(100)
                    continue
                }

                if (repeatCount > 0 && loop >= repeatCount) {
                    break
                }
                loop++
                _liveStats.value = _liveStats.value.copy(currentLoop = loop)

                val success = dispatchSwipe(config.startX, config.startY, config.endX, config.endY, config.durationMs)
                recordGestureResult(
                    success = success,
                    x = config.startX,
                    y = config.startY,
                    targetNumber = 1,
                    colorHex = "#A855F7",
                    gestureType = GestureType.SWIPE
                )

                delay(config.delayAfterMs.coerceAtLeast(50L))
            }
            finishSession()
        }
    }

    fun startMacro(macro: Macro) {
        if (macro.actions.isEmpty()) return
        stop()
        activeModeName = "MACRO"

        automationJob = engineScope.launch {
            handleCountdown()
            initSession(totalLoops = if (macro.infiniteLoop) 0 else macro.repeatCount)

            var loop = 0
            val sortedActions = macro.actions.sortedBy { it.orderIndex }
            while (_engineState.value == EngineState.RUNNING || _engineState.value == EngineState.PAUSED) {
                if (_engineState.value == EngineState.PAUSED) {
                    delay(100)
                    continue
                }

                if (!macro.infiniteLoop && macro.repeatCount > 0 && loop >= macro.repeatCount) {
                    break
                }
                loop++
                _liveStats.value = _liveStats.value.copy(currentLoop = loop)

                for (action in sortedActions) {
                    while (_engineState.value == EngineState.PAUSED) {
                        delay(100)
                    }
                    if (_engineState.value != EngineState.RUNNING) break

                    val success = when (action.type) {
                        GestureType.TAP -> dispatchTap(action.x, action.y, action.durationMs)
                        GestureType.DOUBLE_TAP -> {
                            val first = dispatchTap(action.x, action.y, 40L)
                            delay(60L)
                            val second = dispatchTap(action.x, action.y, 40L)
                            first && second
                        }
                        GestureType.TRIPLE_TAP -> {
                            dispatchTap(action.x, action.y, 35L)
                            delay(50L)
                            dispatchTap(action.x, action.y, 35L)
                            delay(50L)
                            dispatchTap(action.x, action.y, 35L)
                        }
                        GestureType.LONG_PRESS -> dispatchTap(action.x, action.y, action.durationMs.coerceAtLeast(800L))
                        GestureType.SWIPE, GestureType.DRAG_DROP -> dispatchSwipe(
                            action.x, action.y, action.endX, action.endY, action.durationMs
                        )
                    }

                    recordGestureResult(
                        success = success,
                        x = action.x,
                        y = action.y,
                        targetNumber = 1,
                        colorHex = "#EC4899",
                        gestureType = action.type
                    )

                    delay(action.delayAfterMs.coerceAtLeast(20L))
                }
            }
            finishSession()
        }
    }

    fun startSequence(blocks: List<SequenceBlock>, repeatCount: Int = 1) {
        val enabledBlocks = blocks.filter { it.isEnabled }.sortedBy { it.orderIndex }
        if (enabledBlocks.isEmpty()) return
        stop()
        activeModeName = "BUILDER"

        automationJob = engineScope.launch {
            handleCountdown()
            initSession(totalLoops = repeatCount)

            var loop = 0
            while (_engineState.value == EngineState.RUNNING || _engineState.value == EngineState.PAUSED) {
                if (_engineState.value == EngineState.PAUSED) {
                    delay(100)
                    continue
                }

                if (repeatCount > 0 && loop >= repeatCount) {
                    break
                }
                loop++
                _liveStats.value = _liveStats.value.copy(currentLoop = loop)

                for (block in enabledBlocks) {
                    while (_engineState.value == EngineState.PAUSED) {
                        delay(100)
                    }
                    if (_engineState.value != EngineState.RUNNING) break

                    when (block.type) {
                        SequenceBlockType.TAP -> {
                            val success = dispatchTap(block.x, block.y, block.durationMs)
                            recordGestureResult(success, block.x, block.y, 1, "#00F0FF", GestureType.TAP)
                            delay(block.delayMs.coerceAtLeast(20L))
                        }
                        SequenceBlockType.DOUBLE_TAP -> {
                            dispatchTap(block.x, block.y, 40L)
                            delay(60L)
                            val success = dispatchTap(block.x, block.y, 40L)
                            recordGestureResult(success, block.x, block.y, 1, "#00F0FF", GestureType.DOUBLE_TAP)
                            delay(block.delayMs.coerceAtLeast(20L))
                        }
                        SequenceBlockType.LONG_PRESS -> {
                            val success = dispatchTap(block.x, block.y, block.durationMs.coerceAtLeast(600L))
                            recordGestureResult(success, block.x, block.y, 1, "#F59E0B", GestureType.LONG_PRESS)
                            delay(block.delayMs.coerceAtLeast(20L))
                        }
                        SequenceBlockType.SWIPE, SequenceBlockType.SCROLL -> {
                            val success = dispatchSwipe(block.x, block.y, block.endX, block.endY, block.durationMs)
                            recordGestureResult(success, block.x, block.y, 1, "#A855F7", GestureType.SWIPE)
                            delay(block.delayMs.coerceAtLeast(20L))
                        }
                        SequenceBlockType.WAIT -> {
                            delay(block.delayMs.coerceAtLeast(50L))
                        }
                        SequenceBlockType.STOP -> {
                            stop()
                            return@launch
                        }
                        SequenceBlockType.PAUSE -> {
                            _engineState.value = EngineState.PAUSED
                            delay(block.delayMs.coerceAtLeast(200L))
                            _engineState.value = EngineState.RUNNING
                        }
                        SequenceBlockType.RESUME -> {
                            _engineState.value = EngineState.RUNNING
                        }
                        SequenceBlockType.REPEAT -> {
                            // Sub-repeat pause
                            delay(block.delayMs.coerceAtLeast(50L))
                        }
                        SequenceBlockType.RUN_SAVED_MACRO -> {
                            val macro = automationRepository.getMacro(block.macroId)
                            if (macro != null) {
                                for (action in macro.actions.sortedBy { it.orderIndex }) {
                                    dispatchTap(action.x, action.y, action.durationMs)
                                    delay(action.delayAfterMs)
                                }
                            }
                        }
                    }
                }
            }
            finishSession()
        }
    }

    fun startProfile(profile: AutomationProfile) {
        when (profile.mode) {
            ClickMode.SINGLE_TAP -> {
                val target = profile.targets.firstOrNull() ?: ClickTarget(intervalMs = profile.intervalMs)
                startSingleTarget(target, profile.repeatCount)
            }
            ClickMode.MULTI_TAP -> {
                startTargets(profile.targets, profile.repeatCount, modeName = "MULTI_TAP")
            }
            ClickMode.AUTO_SWIPE -> {
                val swipe = profile.swipeConfig ?: SwipeConfig()
                startSwipe(swipe, profile.repeatCount)
            }
            ClickMode.MACRO_RECORDER -> {
                engineScope.launch {
                    val macro = profile.macroId?.let { automationRepository.getMacro(it) }
                    if (macro != null) {
                        startMacro(macro)
                    }
                }
            }
            ClickMode.AUTOMATION_BUILDER -> {
                // Default builder sample
            }
        }
    }

    fun pause() {
        if (_engineState.value == EngineState.RUNNING) {
            _engineState.value = EngineState.PAUSED
        }
    }

    fun resume() {
        if (_engineState.value == EngineState.PAUSED) {
            _engineState.value = EngineState.RUNNING
        }
    }

    fun stop() {
        automationJob?.cancel()
        automationJob = null
        timerJob?.cancel()
        timerJob = null
        _activeTargetHighlight.value = null
        _countdownValue.value = 0

        if (_engineState.value == EngineState.RUNNING || _engineState.value == EngineState.PAUSED) {
            finishSession()
        } else {
            _engineState.value = EngineState.IDLE
        }
    }

    private suspend fun handleCountdown() {
        val countdownSec = settingsRepository.startCountdownSec.value
        if (countdownSec > 0) {
            _engineState.value = EngineState.COUNTDOWN
            for (i in countdownSec downTo 1) {
                _countdownValue.value = i
                delay(1000)
            }
            _countdownValue.value = 0
        }
    }

    private fun initSession(totalLoops: Int) {
        _engineState.value = EngineState.RUNNING
        sessionStartTime = System.currentTimeMillis()
        lastRecordedClickTime = sessionStartTime
        _liveStats.value = LiveAutomationStats(totalLoops = totalLoops)

        // Start live ticker timer for CPS and duration
        timerJob?.cancel()
        timerJob = engineScope.launch {
            while (_engineState.value == EngineState.RUNNING || _engineState.value == EngineState.PAUSED) {
                delay(1000)
                if (_engineState.value == EngineState.RUNNING) {
                    val durationSec = (System.currentTimeMillis() - sessionStartTime) / 1000
                    val currentClicks = _liveStats.value.sessionClicks
                    val cps = if (durationSec > 0) (currentClicks.toFloat() / durationSec) else 0f
                    _liveStats.value = _liveStats.value.copy(
                        sessionDurationSec = durationSec,
                        currentCps = cps
                    )
                }
            }
        }
    }

    private fun finishSession() {
        _engineState.value = EngineState.STOPPED
        _activeTargetHighlight.value = null
        timerJob?.cancel()

        val stats = _liveStats.value
        val durationSec = if (sessionStartTime > 0) (System.currentTimeMillis() - sessionStartTime) / 1000 else 0L

        engineScope.launch {
            if (stats.sessionClicks > 0 || durationSec > 0) {
                automationRepository.recordSessionStats(
                    clicks = stats.sessionClicks,
                    successful = stats.successfulGestures,
                    failed = stats.failedGestures,
                    durationSeconds = durationSec,
                    averageInterval = if (stats.sessionClicks > 0) (durationSec * 1000 / stats.sessionClicks) else 100L,
                    mode = activeModeName
                )
            }
        }
        _engineState.value = EngineState.IDLE
    }

    private suspend fun dispatchTap(x: Float, y: Float, durationMs: Long): Boolean {
        val service = AutoClickerAccessibilityService.instance
        return if (service != null && AutoClickerAccessibilityService.isServiceActive.value) {
            service.performClick(x, y, durationMs)
        } else {
            // Interactive in-app simulation fallback
            true
        }
    }

    private suspend fun dispatchSwipe(startX: Float, startY: Float, endX: Float, endY: Float, durationMs: Long): Boolean {
        val service = AutoClickerAccessibilityService.instance
        return if (service != null && AutoClickerAccessibilityService.isServiceActive.value) {
            service.performSwipe(startX, startY, endX, endY, durationMs)
        } else {
            // Interactive in-app simulation fallback
            true
        }
    }

    private fun recordGestureResult(
        success: Boolean,
        x: Float,
        y: Float,
        targetNumber: Int,
        colorHex: String,
        gestureType: GestureType
    ) {
        val current = _liveStats.value
        val newClicks = current.sessionClicks + 1
        val newSuccess = current.successfulGestures + (if (success) 1 else 0)
        val newFailed = current.failedGestures + (if (!success) 1 else 0)

        _liveStats.value = current.copy(
            sessionClicks = newClicks,
            successfulGestures = newSuccess,
            failedGestures = newFailed
        )

        // Haptic feedback
        if (settingsRepository.hapticFeedback.value) {
            triggerHaptic()
        }

        // Publish event for ripple animation
        _clickEvents.tryEmit(
            ClickEvent(
                x = x,
                y = y,
                targetNumber = targetNumber,
                colorHex = colorHex,
                gestureType = gestureType
            )
        )
    }

    private fun triggerHaptic() {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(15, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(15)
            }
        }
    }
}
