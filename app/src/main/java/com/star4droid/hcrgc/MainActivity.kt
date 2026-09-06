package com.star4droid.hcrgc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.star4droid.hcrgc.model.LevelData
import com.star4droid.hcrgc.model.ProjectConfig
import com.star4droid.hcrgc.storage.ProjectStorage
import com.star4droid.hcrgc.ui.EditorScreen
import com.star4droid.hcrgc.ui.LevelsScreen
import com.star4droid.hcrgc.ui.ProjectsScreen
import com.star4droid.hcrgc.ui.theme.HCRGCTheme

sealed interface Screen {
    object Projects : Screen
    data class Levels(val project: ProjectConfig) : Screen
    data class Editor(val project: ProjectConfig, val level: LevelData) : Screen
}

class MainActivity : ComponentActivity() {

    private fun hideSystemBars() {
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode = android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.hide(WindowInsetsCompat.Type.statusBars())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemBars()
        enableEdgeToEdge()

        setContent {
            HCRGCTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val storage = remember { ProjectStorage(applicationContext) }
                    var currentScreen by remember { mutableStateOf<Screen>(Screen.Projects) }

                    when (val screen = currentScreen) {
                        is Screen.Projects -> {
                            ProjectsScreen(
                                onOpenProject = { project ->
                                    val freshProject = storage.loadProject(project.id) ?: project
                                    currentScreen = Screen.Levels(freshProject)
                                }
                            )
                        }
                        is Screen.Levels -> {
                            LevelsScreen(
                                project = screen.project,
                                onBackToProjects = {
                                    currentScreen = Screen.Projects
                                },
                                onOpenLevel = { levelId ->
                                    val freshProject = storage.loadProject(screen.project.id) ?: screen.project
                                    val level = storage.loadLevel(freshProject.id, levelId)
                                    if (level != null) {
                                        currentScreen = Screen.Editor(freshProject, level)
                                    }
                                }
                            )
                        }
                        is Screen.Editor -> {
                            EditorScreen(
                                initialProject = screen.project,
                                initialLevel = screen.level,
                                onBackToLevels = {
                                    val freshProject = storage.loadProject(screen.project.id) ?: screen.project
                                    currentScreen = Screen.Levels(freshProject)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars()
        }
    }
}
