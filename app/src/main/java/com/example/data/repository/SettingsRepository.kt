package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.FloatingThemeConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("autoclicker_prefs", Context.MODE_PRIVATE)

    private val _themeConfig = MutableStateFlow(loadThemeConfig())
    val themeConfig: StateFlow<FloatingThemeConfig> = _themeConfig.asStateFlow()

    private val _clickIntervalMs = MutableStateFlow(prefs.getLong(KEY_CLICK_INTERVAL, 100L))
    val clickIntervalMs: StateFlow<Long> = _clickIntervalMs.asStateFlow()

    private val _pressDurationMs = MutableStateFlow(prefs.getLong(KEY_PRESS_DURATION, 40L))
    val pressDurationMs: StateFlow<Long> = _pressDurationMs.asStateFlow()

    private val _swipeDurationMs = MutableStateFlow(prefs.getLong(KEY_SWIPE_DURATION, 300L))
    val swipeDurationMs: StateFlow<Long> = _swipeDurationMs.asStateFlow()

    private val _randomJitterMs = MutableStateFlow(prefs.getLong(KEY_RANDOM_JITTER, 10L))
    val randomJitterMs: StateFlow<Long> = _randomJitterMs.asStateFlow()

    private val _startCountdownSec = MutableStateFlow(prefs.getInt(KEY_START_COUNTDOWN, 0))
    val startCountdownSec: StateFlow<Int> = _startCountdownSec.asStateFlow()

    private val _maxSessionDurationMinutes = MutableStateFlow(prefs.getInt(KEY_MAX_SESSION_MINUTES, 30))
    val maxSessionDurationMinutes: StateFlow<Int> = _maxSessionDurationMinutes.asStateFlow()

    private val _hapticFeedback = MutableStateFlow(prefs.getBoolean(KEY_HAPTIC, true))
    val hapticFeedback: StateFlow<Boolean> = _hapticFeedback.asStateFlow()

    private val _hasSeenAccessibilityDisclosure = MutableStateFlow(prefs.getBoolean(KEY_SEEN_DISCLOSURE, false))
    val hasSeenAccessibilityDisclosure: StateFlow<Boolean> = _hasSeenAccessibilityDisclosure.asStateFlow()

    private fun loadThemeConfig(): FloatingThemeConfig {
        return FloatingThemeConfig(
            bgType = prefs.getString(KEY_THEME_BG_TYPE, "GRADIENT") ?: "GRADIENT",
            solidColorHex = prefs.getString(KEY_THEME_SOLID_COLOR, "#161B2E") ?: "#161B2E",
            gradientName = prefs.getString(KEY_THEME_GRADIENT, "Neon Cyber") ?: "Neon Cyber",
            skinName = prefs.getString(KEY_THEME_SKIN, "Galaxy") ?: "Galaxy",
            iconColorHex = prefs.getString(KEY_THEME_ICON_COLOR, "#FFFFFF") ?: "#FFFFFF",
            alpha = prefs.getFloat(KEY_THEME_ALPHA, 0.95f),
            isCompact = prefs.getBoolean(KEY_THEME_COMPACT, false),
            isLocked = prefs.getBoolean(KEY_THEME_LOCKED, false),
            showClickCounter = prefs.getBoolean(KEY_THEME_SHOW_COUNTER, true),
            clickScreenshotEnabled = prefs.getBoolean(KEY_THEME_CLICK_SCREENSHOT, false),
            targetDefaultColor = prefs.getString(KEY_TARGET_COLOR, "#00F0FF") ?: "#00F0FF"
        )
    }

    fun updateThemeConfig(config: FloatingThemeConfig) {
        prefs.edit()
            .putString(KEY_THEME_BG_TYPE, config.bgType)
            .putString(KEY_THEME_SOLID_COLOR, config.solidColorHex)
            .putString(KEY_THEME_GRADIENT, config.gradientName)
            .putString(KEY_THEME_SKIN, config.skinName)
            .putString(KEY_THEME_ICON_COLOR, config.iconColorHex)
            .putFloat(KEY_THEME_ALPHA, config.alpha)
            .putBoolean(KEY_THEME_COMPACT, config.isCompact)
            .putBoolean(KEY_THEME_LOCKED, config.isLocked)
            .putBoolean(KEY_THEME_SHOW_COUNTER, config.showClickCounter)
            .putBoolean(KEY_THEME_CLICK_SCREENSHOT, config.clickScreenshotEnabled)
            .putString(KEY_TARGET_COLOR, config.targetDefaultColor)
            .apply()
        _themeConfig.value = config
    }

    fun setClickInterval(ms: Long) {
        val clamped = ms.coerceIn(10L, 60000L)
        prefs.edit().putLong(KEY_CLICK_INTERVAL, clamped).apply()
        _clickIntervalMs.value = clamped
    }

    fun setPressDuration(ms: Long) {
        val clamped = ms.coerceIn(10L, 5000L)
        prefs.edit().putLong(KEY_PRESS_DURATION, clamped).apply()
        _pressDurationMs.value = clamped
    }

    fun setSwipeDuration(ms: Long) {
        val clamped = ms.coerceIn(50L, 5000L)
        prefs.edit().putLong(KEY_SWIPE_DURATION, clamped).apply()
        _swipeDurationMs.value = clamped
    }

    fun setRandomJitter(ms: Long) {
        val clamped = ms.coerceIn(0L, 500L)
        prefs.edit().putLong(KEY_RANDOM_JITTER, clamped).apply()
        _randomJitterMs.value = clamped
    }

    fun setStartCountdown(sec: Int) {
        prefs.edit().putInt(KEY_START_COUNTDOWN, sec).apply()
        _startCountdownSec.value = sec
    }

    fun setMaxSessionDurationMinutes(mins: Int) {
        prefs.edit().putInt(KEY_MAX_SESSION_MINUTES, mins).apply()
        _maxSessionDurationMinutes.value = mins
    }

    fun setHapticFeedback(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HAPTIC, enabled).apply()
        _hapticFeedback.value = enabled
    }

    fun setAccessibilityDisclosureAccepted(accepted: Boolean) {
        prefs.edit().putBoolean(KEY_SEEN_DISCLOSURE, accepted).apply()
        _hasSeenAccessibilityDisclosure.value = accepted
    }

    companion object {
        private const val KEY_THEME_BG_TYPE = "theme_bg_type"
        private const val KEY_THEME_SOLID_COLOR = "theme_solid_color"
        private const val KEY_THEME_GRADIENT = "theme_gradient"
        private const val KEY_THEME_SKIN = "theme_skin"
        private const val KEY_THEME_ICON_COLOR = "theme_icon_color"
        private const val KEY_THEME_ALPHA = "theme_alpha"
        private const val KEY_THEME_COMPACT = "theme_compact"
        private const val KEY_THEME_LOCKED = "theme_locked"
        private const val KEY_THEME_SHOW_COUNTER = "theme_show_counter"
        private const val KEY_THEME_CLICK_SCREENSHOT = "theme_click_screenshot"
        private const val KEY_TARGET_COLOR = "theme_target_color"
        private const val KEY_CLICK_INTERVAL = "click_interval"
        private const val KEY_PRESS_DURATION = "press_duration"
        private const val KEY_SWIPE_DURATION = "swipe_duration"
        private const val KEY_RANDOM_JITTER = "random_jitter"
        private const val KEY_START_COUNTDOWN = "start_countdown"
        private const val KEY_MAX_SESSION_MINUTES = "max_session_minutes"
        private const val KEY_HAPTIC = "haptic_feedback"
        private const val KEY_SEEN_DISCLOSURE = "seen_disclosure"
    }
}
