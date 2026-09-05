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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                                    currentScreen = Screen.Levels(project)
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
                                    val level = storage.loadLevel(screen.project.id, levelId)
                                    if (level != null) {
                                        currentScreen = Screen.Editor(screen.project, level)
                                    }
                                }
                            )
                        }
                        is Screen.Editor -> {
                            EditorScreen(
                                initialProject = screen.project,
                                initialLevel = screen.level,
                                onBackToLevels = {
                                    currentScreen = Screen.Levels(screen.project)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
