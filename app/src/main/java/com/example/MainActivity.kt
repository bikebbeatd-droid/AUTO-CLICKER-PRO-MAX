package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.screens.AiAssistantScreen
import com.example.ui.screens.AutomationBuilderScreen
import com.example.ui.screens.CloudSyncScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MacroRecorderScreen
import com.example.ui.screens.MacroTimelineScreen
import com.example.ui.screens.MarketplaceScreen
import com.example.ui.screens.MultiTargetEditorScreen
import com.example.ui.screens.ProfilesScreen
import com.example.ui.screens.ScriptStudioScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.StatisticsScreen
import com.example.ui.screens.WebStudioScreen
import com.example.ui.screens.WorkspaceScreen
import com.example.ui.theme.AutoClickerProTheme
import com.example.ui.theme.DarkBg
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.AutoClickerViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: AutoClickerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AutoClickerProTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBg
                ) {
                    MainAppNavigation(viewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkPermissions()
    }
}

@Composable
fun MainAppNavigation(viewModel: AutoClickerViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()

    when (currentScreen) {
        AppScreen.SPLASH -> {
            SplashScreen(onFinished = { viewModel.navigateTo(AppScreen.HOME) })
        }
        AppScreen.HOME -> {
            HomeScreen(viewModel = viewModel)
        }
        AppScreen.WORKSPACE -> {
            WorkspaceScreen(viewModel = viewModel)
        }
        AppScreen.SCRIPT_STUDIO -> {
            ScriptStudioScreen(viewModel = viewModel)
        }
        AppScreen.VISUAL_BUILDER, AppScreen.AUTOMATION_BUILDER -> {
            AutomationBuilderScreen(viewModel = viewModel)
        }
        AppScreen.AI_ASSISTANT -> {
            AiAssistantScreen(viewModel = viewModel)
        }
        AppScreen.MARKETPLACE -> {
            MarketplaceScreen(viewModel = viewModel)
        }
        AppScreen.CLOUD_SYNC -> {
            CloudSyncScreen(viewModel = viewModel)
        }
        AppScreen.WEB_STUDIO -> {
            WebStudioScreen(viewModel = viewModel)
        }
        AppScreen.MULTI_TARGET -> {
            MultiTargetEditorScreen(viewModel = viewModel)
        }
        AppScreen.MACRO_RECORDER -> {
            MacroRecorderScreen(viewModel = viewModel)
        }
        AppScreen.MACRO_TIMELINE -> {
            MacroTimelineScreen(viewModel = viewModel)
        }
        AppScreen.PROFILES -> {
            ProfilesScreen(viewModel = viewModel)
        }
        AppScreen.STATISTICS -> {
            StatisticsScreen(viewModel = viewModel)
        }
        AppScreen.SETTINGS -> {
            SettingsScreen(viewModel = viewModel)
        }
    }
}
