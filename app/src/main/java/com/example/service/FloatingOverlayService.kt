package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.MainActivity
import com.example.data.model.ClickTarget
import com.example.data.model.FloatingThemeConfig
import com.example.data.repository.SettingsRepository
import com.example.engine.EngineState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FloatingOverlayService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var windowManager: WindowManager? = null
    private var overlayView: ComposeView? = null
    private val lifecycleOwner = ServiceLifecycleOwner()
    private lateinit var settingsRepo: SettingsRepository

    override fun onCreate() {
        super.onCreate()
        lifecycleOwner.onCreate()
        settingsRepo = SettingsRepository(applicationContext)
        createNotificationChannel()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as? WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        when (action) {
            ACTION_STOP_SERVICE -> {
                removeOverlayView()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                _isOverlayServiceRunning.value = false
                return START_NOT_STICKY
            }
        }

        startForeground(NOTIFICATION_ID, buildNotification("Auto Clicker Pro Active", "Floating control overlay active"))
        _isOverlayServiceRunning.value = true
        lifecycleOwner.onResume()

        if (Settings.canDrawOverlays(this) && overlayView == null) {
            setupFloatingWindow()
        }

        return START_STICKY
    }

    private fun setupFloatingWindow() {
        val wm = windowManager ?: return
        lifecycleOwner.onResume()

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 30
            y = 350
        }

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        val composeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)

            setOnTouchListener { view, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        false
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - initialTouchX).toInt()
                        val dy = (event.rawY - initialTouchY).toInt()
                        if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                            params.x = initialX + dx
                            params.y = initialY + dy
                            try {
                                wm.updateViewLayout(view, params)
                            } catch (_: Exception) {}
                            true
                        } else false
                    }
                    else -> false
                }
            }

            setContent {
                val themeConfig by settingsRepo.themeConfig.collectAsState()
                val engineState by overlayEngineState.collectAsState()
                val clickCount by overlayClickCount.collectAsState()

                FloatingBarOverlayContent(
                    themeConfig = themeConfig,
                    engineState = engineState,
                    clickCount = clickCount,
                    onPlay = { onOverlayAction?.invoke(OverlayAction.PLAY) },
                    onPause = { onOverlayAction?.invoke(OverlayAction.PAUSE) },
                    onResume = { onOverlayAction?.invoke(OverlayAction.RESUME) },
                    onStop = { onOverlayAction?.invoke(OverlayAction.STOP) },
                    onAddTarget = { onOverlayAction?.invoke(OverlayAction.ADD_TARGET) },
                    onRemoveTarget = { onOverlayAction?.invoke(OverlayAction.REMOVE_TARGET) },
                    onOpenSettings = {
                        val appIntent = Intent(this@FloatingOverlayService, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        }
                        startActivity(appIntent)
                    },
                    onClose = {
                        stop(this@FloatingOverlayService)
                    }
                )
            }
        }

        try {
            wm.addView(composeView, params)
            overlayView = composeView
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun removeOverlayView() {
        overlayView?.let { view ->
            try {
                windowManager?.removeView(view)
            } catch (_: Exception) {}
        }
        overlayView = null
    }

    override fun onDestroy() {
        super.onDestroy()
        removeOverlayView()
        lifecycleOwner.onDestroy()
        serviceScope.cancel()
        _isOverlayServiceRunning.value = false
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Auto Clicker Overlay Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows floating automation status and emergency stop control"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(title: String, content: String): Notification {
        val appIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingAppIntent = PendingIntent.getActivity(
            this,
            0,
            appIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, FloatingOverlayService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val pendingStopIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingAppIntent)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Emergency Stop", pendingStopIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "autoclicker_overlay_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP_SERVICE = "com.example.action.STOP_SERVICE"

        private val _isOverlayServiceRunning = MutableStateFlow(false)
        val isOverlayServiceRunning: StateFlow<Boolean> = _isOverlayServiceRunning.asStateFlow()

        val overlayEngineState = MutableStateFlow(EngineState.IDLE)
        val overlayClickCount = MutableStateFlow(0L)

        var onOverlayAction: ((OverlayAction) -> Unit)? = null

        fun start(context: Context) {
            val intent = Intent(context, FloatingOverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, FloatingOverlayService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            context.startService(intent)
        }
    }
}

enum class OverlayAction {
    PLAY,
    PAUSE,
    RESUME,
    STOP,
    ADD_TARGET,
    REMOVE_TARGET,
    OPEN_SETTINGS
}

@Composable
fun FloatingBarOverlayContent(
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
    onClose: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(true) }

    val iconColor = runCatching { Color(android.graphics.Color.parseColor(themeConfig.iconColorHex)) }
        .getOrDefault(Color.White)

    val backgroundModifier = when (themeConfig.bgType) {
        "SOLID" -> {
            val solidColor = runCatching { Color(android.graphics.Color.parseColor(themeConfig.solidColorHex)) }
                .getOrDefault(Color(0xFF161B2E))
            Modifier.background(solidColor.copy(alpha = themeConfig.alpha))
        }
        "IMAGE" -> {
            // Stylized artistic gradient inspired by collection skin
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
            val gradBrush = when (themeConfig.gradientName) {
                "Neon Cyber" -> Brush.verticalGradient(listOf(Color(0xFF00F0FF), Color(0xFF7928CA)))
                "Purple Sunset" -> Brush.verticalGradient(listOf(Color(0xFF6366F1), Color(0xFFEC4899)))
                "Emerald Glow" -> Brush.verticalGradient(listOf(Color(0xFF10B981), Color(0xFF06B6D4)))
                "Pink Nebula" -> Brush.verticalGradient(listOf(Color(0xFFEC4899), Color(0xFF8B5CF6)))
                else -> Brush.verticalGradient(listOf(Color(0xFF1E293B), Color(0xFF090D16)))
            }
            Modifier.background(gradBrush, alpha = themeConfig.alpha)
        }
    }

    Box(
        modifier = Modifier
            .shadow(elevation = 16.dp, shape = RoundedCornerShape(26.dp))
            .clip(RoundedCornerShape(26.dp))
            .then(backgroundModifier)
            .border(1.5.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(26.dp))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 9.dp)
        ) {
            // Handle + Minimize/Expand
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .width(if (isExpanded) 56.dp else 44.dp)
                    .padding(bottom = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Move",
                    tint = iconColor.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle",
                        tint = iconColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Click Counter
            if (themeConfig.showClickCounter) {
                Surface(
                    color = Color.Black.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Text(
                        text = "$clickCount",
                        color = Color(0xFF00F0FF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }

            // Play / Pause / Resume
            when (engineState) {
                EngineState.RUNNING -> {
                    OverlayActionButton(
                        icon = Icons.Default.Pause,
                        tint = Color(0xFFF59E0B),
                        bgColor = Color.Black.copy(alpha = 0.4f),
                        contentDescription = "Pause",
                        onClick = onPause
                    )
                }
                EngineState.PAUSED -> {
                    OverlayActionButton(
                        icon = Icons.Default.PlayArrow,
                        tint = Color(0xFF10B981),
                        bgColor = Color.Black.copy(alpha = 0.4f),
                        contentDescription = "Resume",
                        onClick = onResume
                    )
                }
                else -> {
                    OverlayActionButton(
                        icon = Icons.Default.PlayArrow,
                        tint = Color(0xFF10B981),
                        bgColor = Color.Black.copy(alpha = 0.4f),
                        contentDescription = "Start",
                        onClick = onPlay
                    )
                }
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(6.dp))

                // Add Target
                OverlayActionButton(
                    icon = Icons.Default.Add,
                    tint = iconColor,
                    bgColor = Color.Black.copy(alpha = 0.25f),
                    contentDescription = "Add Target",
                    onClick = onAddTarget
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Remove Target
                OverlayActionButton(
                    icon = Icons.Default.Remove,
                    tint = iconColor,
                    bgColor = Color.Black.copy(alpha = 0.25f),
                    contentDescription = "Remove Target",
                    onClick = onRemoveTarget
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Settings
                OverlayActionButton(
                    icon = Icons.Default.Settings,
                    tint = iconColor,
                    bgColor = Color.Black.copy(alpha = 0.25f),
                    contentDescription = "Settings",
                    onClick = onOpenSettings
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Emergency Stop
                OverlayActionButton(
                    icon = Icons.Default.Stop,
                    tint = Color(0xFFEF4444),
                    bgColor = Color(0xFFEF4444).copy(alpha = 0.2f),
                    contentDescription = "Stop",
                    onClick = onStop
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Close
                OverlayActionButton(
                    icon = Icons.Default.Close,
                    tint = iconColor.copy(alpha = 0.7f),
                    bgColor = Color.Transparent,
                    contentDescription = "Close",
                    onClick = onClose
                )
            }
        }
    }
}

@Composable
private fun OverlayActionButton(
    icon: ImageVector,
    tint: Color,
    bgColor: Color,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(bgColor)
            .clickable(onClick = onClick)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
    }
}
