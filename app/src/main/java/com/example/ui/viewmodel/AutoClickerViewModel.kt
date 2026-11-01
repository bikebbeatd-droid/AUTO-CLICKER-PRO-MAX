package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.AutomationProfile
import com.example.data.model.AutomationStats
import com.example.data.model.ClickMode
import com.example.data.model.ClickTarget
import com.example.data.model.FloatingThemeConfig
import com.example.data.model.GestureType
import com.example.data.model.Macro
import com.example.data.model.MacroAction
import com.example.data.model.ScheduledTask
import com.example.data.model.SequenceBlock
import com.example.data.model.SequenceBlockType
import com.example.data.model.SwipeConfig
import com.example.data.repository.AutomationRepository
import com.example.data.repository.MarketplaceRepository
import com.example.data.repository.MarketplaceScript
import com.example.data.repository.SequenceBlockList
import com.example.data.repository.SettingsRepository
import com.example.dsl.AiGeneratedScript
import com.example.dsl.AiScriptAssistant
import com.example.dsl.DslInterpreter
import com.example.dsl.DslParser
import com.example.dsl.DslValidationError
import com.example.dsl.InterpreterDebugState
import com.example.engine.EngineState
import com.example.engine.GestureAutomationEngine
import com.example.service.AutoClickerAccessibilityService
import com.example.service.FloatingOverlayService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

enum class AppScreen(val title: String) {
    SPLASH("Splash"),
    HOME("Home"),
    WORKSPACE("Automation Workspace"),
    SCRIPT_STUDIO("Script Studio Pro"),
    VISUAL_BUILDER("Visual Workflow Builder"),
    AI_ASSISTANT("AI Script Assistant"),
    MARKETPLACE("Script Marketplace"),
    CLOUD_SYNC("Cloud Sync & Devices"),
    WEB_STUDIO("Web Automation Studio"),
    MULTI_TARGET("Multi-Target Editor"),
    MACRO_RECORDER("Macro Recorder Studio"),
    MACRO_TIMELINE("Macro Timeline Editor"),
    AUTOMATION_BUILDER("Sequence Builder"),
    PROFILES("Profile Manager"),
    STATISTICS("Live Statistics"),
    SETTINGS("Settings & Customization")
}

class AutoClickerViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    val automationRepo = AutomationRepository(db)
    val settingsRepo = SettingsRepository(application)
    val marketplaceRepo = MarketplaceRepository()
    val engine = GestureAutomationEngine(application, settingsRepo, automationRepo)

    val dslParser = DslParser()
    val aiAssistant = AiScriptAssistant()
    val dslInterpreter = DslInterpreter(engine) { x, y, gestureType ->
        when (gestureType) {
            GestureType.TAP -> engine.startSingleTarget(ClickTarget(x = x, y = y, intervalMs = 15L), repeatCount = 1)
            GestureType.DOUBLE_TAP -> engine.startSingleTarget(ClickTarget(x = x, y = y, intervalMs = 15L), repeatCount = 2)
            GestureType.LONG_PRESS -> engine.startSingleTarget(ClickTarget(x = x, y = y, durationMs = 1000L), repeatCount = 1)
            GestureType.SWIPE -> engine.startSwipe(SwipeConfig(startX = x, startY = y, endX = x, endY = y - 400f), repeatCount = 1)
            else -> {}
        }
    }

    // Navigation
    private val _currentScreen = MutableStateFlow(AppScreen.HOME)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()
    private val screenBackStack = mutableListOf<AppScreen>()

    fun navigateTo(screen: AppScreen) {
        if (_currentScreen.value != screen) {
            screenBackStack.add(_currentScreen.value)
            _currentScreen.value = screen
        }
    }

    fun navigateBack(): Boolean {
        if (screenBackStack.isNotEmpty()) {
            _currentScreen.value = screenBackStack.removeAt(screenBackStack.size - 1)
            return true
        }
        return false
    }

    // Script Studio state
    private val defaultDslScript = """# Auto Clicker Pro Max - Safe DSL Script
VAR target_x = 540
VAR target_y = 960
VAR cycles = 20
VAR delay_ms = 100

GROUP "Farming Loop" {
    REPEAT cycles {
        TAP target_x target_y 40
        WAIT delay_ms
    }
}

WAIT 500
SWIPE 540 1400 540 500 350
""".trimIndent()

    private val _dslScriptCode = MutableStateFlow(defaultDslScript)
    val dslScriptCode: StateFlow<String> = _dslScriptCode.asStateFlow()

    private val undoStack = mutableListOf<String>()
    private val redoStack = mutableListOf<String>()

    fun updateDslScript(code: String, recordHistory: Boolean = true) {
        if (recordHistory && code != _dslScriptCode.value) {
            undoStack.add(_dslScriptCode.value)
            if (undoStack.size > 30) undoStack.removeAt(0)
            redoStack.clear()
        }
        _dslScriptCode.value = code
    }

    fun undoScript() {
        if (undoStack.isNotEmpty()) {
            redoStack.add(_dslScriptCode.value)
            _dslScriptCode.value = undoStack.removeAt(undoStack.size - 1)
        }
    }

    fun redoScript() {
        if (redoStack.isNotEmpty()) {
            undoStack.add(_dslScriptCode.value)
            _dslScriptCode.value = redoStack.removeAt(redoStack.size - 1)
        }
    }

    fun formatCurrentScript() {
        val formatted = dslParser.format(_dslScriptCode.value)
        updateDslScript(formatted)
    }

    private val _dslErrors = MutableStateFlow<List<DslValidationError>>(emptyList())
    val dslErrors: StateFlow<List<DslValidationError>> = _dslErrors.asStateFlow()

    fun validateCurrentScript(): Boolean {
        val (_, errors) = dslParser.parse(_dslScriptCode.value)
        _dslErrors.value = errors
        return errors.isEmpty()
    }

    fun runDslScript(stepByStep: Boolean = false) {
        val (stmts, errors) = dslParser.parse(_dslScriptCode.value)
        _dslErrors.value = errors
        if (errors.isEmpty()) {
            viewModelScope.launch {
                dslInterpreter.execute(stmts, stepByStep = stepByStep)
            }
        }
    }

    fun stopDslScript() {
        dslInterpreter.cancel()
    }

    // AI Assistant State
    private val _aiResult = MutableStateFlow<AiGeneratedScript?>(null)
    val aiResult: StateFlow<AiGeneratedScript?> = _aiResult.asStateFlow()

    fun generateScriptWithAi(prompt: String) {
        val script = aiAssistant.generateScriptFromPrompt(prompt)
        _aiResult.value = script
    }

    fun applyAiScriptToStudio() {
        _aiResult.value?.let {
            updateDslScript(it.dslCode)
            navigateTo(AppScreen.SCRIPT_STUDIO)
        }
    }

    // Marketplace
    private val _marketplaceCategory = MutableStateFlow("All")
    val marketplaceCategory: StateFlow<String> = _marketplaceCategory.asStateFlow()

    private val _marketplaceSearchQuery = MutableStateFlow("")
    val marketplaceSearchQuery: StateFlow<String> = _marketplaceSearchQuery.asStateFlow()

    fun setMarketplaceCategory(category: String) {
        _marketplaceCategory.value = category
    }

    fun setMarketplaceSearch(query: String) {
        _marketplaceSearchQuery.value = query
    }

    fun importMarketplaceItem(script: MarketplaceScript) {
        val result = marketplaceRepo.validateAndImportScript(script)
        result.onSuccess { code ->
            updateDslScript(code)
            navigateTo(AppScreen.SCRIPT_STUDIO)
        }
    }

    // Targets for Multi-Target Editor & Workspace
    private val _selectedMode = MutableStateFlow(ClickMode.SINGLE_TAP)
    val selectedMode: StateFlow<ClickMode> = _selectedMode.asStateFlow()

    fun selectMode(mode: ClickMode) {
        _selectedMode.value = mode
    }

    private val _activeTargets = MutableStateFlow<List<ClickTarget>>(
        listOf(
            ClickTarget(id = "tgt_1", number = 1, x = 340f, y = 700f, intervalMs = 120L, durationMs = 40L, colorHex = "#00F0FF", label = "Point 1"),
            ClickTarget(id = "tgt_2", number = 2, x = 740f, y = 700f, intervalMs = 150L, durationMs = 40L, colorHex = "#A855F7", label = "Point 2")
        )
    )
    val activeTargets: StateFlow<List<ClickTarget>> = _activeTargets.asStateFlow()

    private val _activeSwipe = MutableStateFlow(
        SwipeConfig(startX = 540f, startY = 1400f, endX = 540f, endY = 500f, durationMs = 350L, delayAfterMs = 500L, swipeDirection = "UP")
    )
    val activeSwipe: StateFlow<SwipeConfig> = _activeSwipe.asStateFlow()

    private val _isRecordingMacro = MutableStateFlow(false)
    val isRecordingMacro: StateFlow<Boolean> = _isRecordingMacro.asStateFlow()

    private val _recordedActions = MutableStateFlow<List<MacroAction>>(emptyList())
    val recordedActions: StateFlow<List<MacroAction>> = _recordedActions.asStateFlow()

    private val _selectedMacro = MutableStateFlow<Macro?>(null)
    val selectedMacro: StateFlow<Macro?> = _selectedMacro.asStateFlow()

    private val _sequenceBlocks = MutableStateFlow<List<SequenceBlock>>(
        listOf(
            SequenceBlock(id = "blk_1", type = SequenceBlockType.TAP, x = 500f, y = 800f, durationMs = 50L, delayMs = 250L, orderIndex = 0),
            SequenceBlock(id = "blk_2", type = SequenceBlockType.WAIT, delayMs = 500L, orderIndex = 1),
            SequenceBlock(id = "blk_3", type = SequenceBlockType.DOUBLE_TAP, x = 600f, y = 900f, delayMs = 300L, orderIndex = 2),
            SequenceBlock(id = "blk_4", type = SequenceBlockType.SWIPE, x = 540f, y = 1200f, endX = 540f, endY = 500f, durationMs = 300L, delayMs = 400L, orderIndex = 3)
        )
    )
    val sequenceBlocks: StateFlow<List<SequenceBlock>> = _sequenceBlocks.asStateFlow()

    // Permissions State
    private val _isAccessibilityEnabled = MutableStateFlow(false)
    val isAccessibilityEnabled: StateFlow<Boolean> = _isAccessibilityEnabled.asStateFlow()

    private val _isOverlayGranted = MutableStateFlow(false)
    val isOverlayGranted: StateFlow<Boolean> = _isOverlayGranted.asStateFlow()

    private val _isFloatingBarVisibleInApp = MutableStateFlow(true)
    val isFloatingBarVisibleInApp: StateFlow<Boolean> = _isFloatingBarVisibleInApp.asStateFlow()

    val allProfiles: StateFlow<List<AutomationProfile>> = automationRepo.allProfiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMacros: StateFlow<List<Macro>> = automationRepo.allMacros
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSequences: StateFlow<List<SequenceBlockList>> = automationRepo.allSequences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalStats: StateFlow<AutomationStats> = automationRepo.totalStatsSummary
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AutomationStats())

    val scheduledTasks: StateFlow<List<ScheduledTask>> = automationRepo.scheduledTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        checkPermissions()

        viewModelScope.launch {
            engine.engineState.collect { state ->
                FloatingOverlayService.overlayEngineState.value = state
            }
        }

        viewModelScope.launch {
            engine.liveStats.collect { stats ->
                FloatingOverlayService.overlayClickCount.value = stats.sessionClicks
            }
        }

        FloatingOverlayService.onOverlayAction = { action ->
            when (action) {
                com.example.service.OverlayAction.PLAY -> startCurrentAutomation()
                com.example.service.OverlayAction.PAUSE -> pauseAutomation()
                com.example.service.OverlayAction.RESUME -> resumeAutomation()
                com.example.service.OverlayAction.STOP -> stopAutomation()
                com.example.service.OverlayAction.ADD_TARGET -> addTarget()
                com.example.service.OverlayAction.REMOVE_TARGET -> removeLastTarget()
                com.example.service.OverlayAction.OPEN_SETTINGS -> navigateTo(AppScreen.SETTINGS)
            }
        }
    }

    fun checkPermissions() {
        val context = getApplication<Application>()
        _isAccessibilityEnabled.value = AutoClickerAccessibilityService.isServiceActive.value
        _isOverlayGranted.value = Settings.canDrawOverlays(context)
    }

    fun toggleInAppFloatingBar() {
        _isFloatingBarVisibleInApp.value = !_isFloatingBarVisibleInApp.value
    }

    fun addTarget(x: Float = 540f, y: Float = 960f) {
        val current = _activeTargets.value
        val nextNumber = (current.maxOfOrNull { it.number } ?: 0) + 1
        val colors = listOf("#00F0FF", "#A855F7", "#EC4899", "#10B981", "#F59E0B", "#3B82F6")
        val color = colors[(nextNumber - 1) % colors.size]
        val newTarget = ClickTarget(
            id = UUID.randomUUID().toString(),
            number = nextNumber,
            x = x,
            y = y,
            intervalMs = settingsRepo.clickIntervalMs.value,
            durationMs = settingsRepo.pressDurationMs.value,
            randomJitterMs = settingsRepo.randomJitterMs.value,
            colorHex = color,
            label = "Target $nextNumber",
            orderIndex = current.size
        )
        _activeTargets.value = current + newTarget
    }

    fun removeTarget(targetId: String) {
        val current = _activeTargets.value
        if (current.size > 1) {
            _activeTargets.value = current.filter { it.id != targetId }.mapIndexed { index, tgt ->
                tgt.copy(number = index + 1, orderIndex = index)
            }
        }
    }

    fun removeLastTarget() {
        val current = _activeTargets.value
        if (current.size > 1) {
            _activeTargets.value = current.dropLast(1)
        }
    }

    fun updateTargetPosition(targetId: String, newX: Float, newY: Float) {
        _activeTargets.value = _activeTargets.value.map {
            if (it.id == targetId) it.copy(x = newX, y = newY) else it
        }
    }

    fun updateTarget(updated: ClickTarget) {
        _activeTargets.value = _activeTargets.value.map {
            if (it.id == updated.id) updated else it
        }
    }

    fun duplicateTarget(target: ClickTarget) {
        val current = _activeTargets.value
        val nextNumber = (current.maxOfOrNull { it.number } ?: 0) + 1
        val duplicated = target.copy(
            id = UUID.randomUUID().toString(),
            number = nextNumber,
            x = target.x + 40f,
            y = target.y + 40f,
            label = "${target.label} (Copy)",
            orderIndex = current.size
        )
        _activeTargets.value = current + duplicated
    }

    fun toggleTargetEnabled(targetId: String) {
        _activeTargets.value = _activeTargets.value.map {
            if (it.id == targetId) it.copy(enabled = !it.enabled) else it
        }
    }

    fun startMacroRecording() {
        _recordedActions.value = emptyList()
        _isRecordingMacro.value = true
    }

    fun recordTouchAction(action: MacroAction) {
        if (_isRecordingMacro.value) {
            val current = _recordedActions.value
            _recordedActions.value = current + action.copy(orderIndex = current.size)
        }
    }

    fun stopMacroRecording() {
        _isRecordingMacro.value = false
    }

    fun saveMacro(name: String, description: String, repeatCount: Int = 1, infinite: Boolean = false) {
        viewModelScope.launch {
            val macro = Macro(
                name = name.ifBlank { "Recorded Macro" },
                description = description,
                repeatCount = repeatCount,
                infiniteLoop = infinite,
                actions = _recordedActions.value
            )
            automationRepo.saveMacro(macro)
            _selectedMacro.value = macro
        }
    }

    fun selectMacroForEditing(macro: Macro) {
        _selectedMacro.value = macro
        _recordedActions.value = macro.actions
    }

    fun updateRecordedActions(actions: List<MacroAction>) {
        _recordedActions.value = actions
        _selectedMacro.value?.let { current ->
            val updated = current.copy(actions = actions)
            _selectedMacro.value = updated
            viewModelScope.launch { automationRepo.saveMacro(updated) }
        }
    }

    fun addSequenceBlock(type: SequenceBlockType) {
        val current = _sequenceBlocks.value
        val newBlock = SequenceBlock(
            id = UUID.randomUUID().toString(),
            type = type,
            orderIndex = current.size
        )
        _sequenceBlocks.value = current + newBlock
    }

    fun removeSequenceBlock(id: String) {
        val current = _sequenceBlocks.value
        _sequenceBlocks.value = current.filter { it.id != id }.mapIndexed { idx, b -> b.copy(orderIndex = idx) }
    }

    fun updateSequenceBlock(block: SequenceBlock) {
        _sequenceBlocks.value = _sequenceBlocks.value.map {
            if (it.id == block.id) block else it
        }
    }

    fun moveBlock(fromIndex: Int, toIndex: Int) {
        val list = _sequenceBlocks.value.toMutableList()
        if (fromIndex in list.indices && toIndex in list.indices) {
            val item = list.removeAt(fromIndex)
            list.add(toIndex, item)
            _sequenceBlocks.value = list.mapIndexed { idx, b -> b.copy(orderIndex = idx) }
        }
    }

    fun convertBlocksToDslCode(): String {
        val sb = StringBuilder("# Workflow Script\n")
        for (b in _sequenceBlocks.value.filter { it.isEnabled }.sortedBy { it.orderIndex }) {
            when (b.type) {
                SequenceBlockType.TAP -> sb.append("TAP ${b.x.toInt()} ${b.y.toInt()} ${b.durationMs}\nWAIT ${b.delayMs}\n")
                SequenceBlockType.DOUBLE_TAP -> sb.append("DOUBLE_TAP ${b.x.toInt()} ${b.y.toInt()}\nWAIT ${b.delayMs}\n")
                SequenceBlockType.LONG_PRESS -> sb.append("LONG_PRESS ${b.x.toInt()} ${b.y.toInt()} ${b.durationMs}\nWAIT ${b.delayMs}\n")
                SequenceBlockType.SWIPE -> sb.append("SWIPE ${b.x.toInt()} ${b.y.toInt()} ${b.endX.toInt()} ${b.endY.toInt()} ${b.durationMs}\nWAIT ${b.delayMs}\n")
                SequenceBlockType.SCROLL -> sb.append("SCROLL UP\nWAIT ${b.delayMs}\n")
                SequenceBlockType.WAIT -> sb.append("WAIT ${b.delayMs}\n")
                SequenceBlockType.STOP -> sb.append("STOP\n")
                SequenceBlockType.PAUSE -> sb.append("PAUSE ${b.delayMs}\n")
                else -> {}
            }
        }
        return sb.toString().trimEnd()
    }

    fun startCurrentAutomation() {
        when (_selectedMode.value) {
            ClickMode.SINGLE_TAP -> {
                val target = _activeTargets.value.firstOrNull() ?: ClickTarget()
                engine.startSingleTarget(target, repeatCount = 0)
            }
            ClickMode.MULTI_TAP -> {
                engine.startTargets(_activeTargets.value, repeatCount = 0)
            }
            ClickMode.AUTO_SWIPE -> {
                engine.startSwipe(_activeSwipe.value, repeatCount = 0)
            }
            ClickMode.MACRO_RECORDER -> {
                val macro = _selectedMacro.value
                if (macro != null) {
                    engine.startMacro(macro)
                } else if (_recordedActions.value.isNotEmpty()) {
                    engine.startMacro(Macro(name = "Temp Macro", actions = _recordedActions.value))
                }
            }
            ClickMode.AUTOMATION_BUILDER -> {
                engine.startSequence(_sequenceBlocks.value, repeatCount = 1)
            }
        }
    }

    fun pauseAutomation() = engine.pause()
    fun resumeAutomation() = engine.resume()
    fun stopAutomation() = engine.stop()

    fun loadProfile(profile: AutomationProfile) {
        _selectedMode.value = profile.mode
        if (profile.targets.isNotEmpty()) {
            _activeTargets.value = profile.targets
        }
        profile.swipeConfig?.let { _activeSwipe.value = it }
        profile.macroId?.let { id ->
            viewModelScope.launch {
                val macro = automationRepo.getMacro(id)
                _selectedMacro.value = macro
            }
        }
    }

    fun saveCurrentAsProfile(name: String, description: String) {
        viewModelScope.launch {
            val profile = AutomationProfile(
                name = name,
                description = description,
                mode = _selectedMode.value,
                targets = _activeTargets.value,
                swipeConfig = _activeSwipe.value,
                macroId = _selectedMacro.value?.id,
                repeatCount = 0,
                intervalMs = settingsRepo.clickIntervalMs.value,
                isPreset = false
            )
            automationRepo.saveProfile(profile)
        }
    }

    fun duplicateProfile(profile: AutomationProfile) {
        viewModelScope.launch {
            automationRepo.duplicateProfile(profile)
        }
    }

    fun deleteProfile(id: String) {
        viewModelScope.launch {
            automationRepo.deleteProfile(id)
        }
    }
}
