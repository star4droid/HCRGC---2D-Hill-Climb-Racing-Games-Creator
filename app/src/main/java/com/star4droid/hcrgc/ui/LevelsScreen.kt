package com.star4droid.hcrgc.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.star4droid.hcrgc.model.CameraSettings
import com.star4droid.hcrgc.model.LevelData
import com.star4droid.hcrgc.model.ProjectConfig
import com.star4droid.hcrgc.storage.ProjectStorage
import com.star4droid.hcrgc.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LevelsScreen(
    project: ProjectConfig,
    onBackToProjects: () -> Unit,
    onOpenLevel: (String) -> Unit
) {
    val context = LocalContext.current
    val storage = remember { ProjectStorage(context) }
    var levelIds by remember { mutableStateOf(storage.listLevels(project.id)) }
    var showNewLevelDialog by remember { mutableStateOf(false) }
    var newLevelName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = project.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = StudioTextPrimary
                        )
                        Text(
                            text = "Levels (${levelIds.size}) • ${project.orientation.name}",
                            style = MaterialTheme.typography.labelSmall,
                            color = StudioTextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackToProjects,
                        modifier = Modifier.testTag("btn_back_to_projects")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            newLevelName = "Level ${levelIds.size + 1}"
                            showNewLevelDialog = true
                        },
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .testTag("btn_create_new_level"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = StudioAccentBlue)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("New Level")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = StudioSurface)
            )
        },
        containerColor = StudioBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            if (levelIds.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Layers, contentDescription = null, tint = StudioTextTertiary, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No levels created yet", style = MaterialTheme.typography.titleMedium, color = StudioTextSecondary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                newLevelName = "Level 1"
                                showNewLevelDialog = true
                            }
                        ) {
                            Text("Create First Level")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(levelIds) { levelId ->
                        val levelData = remember(levelId) { storage.loadLevel(project.id, levelId) }
                        val displayName = levelData?.name ?: levelId
                        val objCount = levelData?.objects?.size ?: 0

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onOpenLevel(levelId) }
                                .testTag("level_card_$levelId"),
                            shape = RoundedCornerShape(16.dp),
                            color = StudioSurface,
                            shadowElevation = 2.dp,
                            border = BorderStroke(1.dp, StudioSurfaceBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .background(StudioAccentBlue.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Terrain,
                                            contentDescription = null,
                                            tint = StudioAccentBlue,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column {
                                        Text(
                                            text = displayName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = StudioTextPrimary
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "$objCount Objects • ID: $levelId",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = StudioTextSecondary
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Button(
                                        onClick = { onOpenLevel(levelId) },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = StudioAccentGreen),
                                        modifier = Modifier.testTag("btn_edit_level_$levelId")
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Edit")
                                    }

                                    if (levelIds.size > 1) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        IconButton(
                                            onClick = {
                                                storage.deleteLevel(project.id, levelId)
                                                levelIds = storage.listLevels(project.id)
                                            }
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = StudioAccentRed)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNewLevelDialog) {
        AlertDialog(
            onDismissRequest = { showNewLevelDialog = false },
            title = { Text("Create New Level") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newLevelName,
                        onValueChange = { newLevelName = it },
                        label = { Text("Level Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_new_level_name")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = newLevelName.trim().ifEmpty { "Level ${levelIds.size + 1}" }
                        val newId = "level_${System.currentTimeMillis().toString().takeLast(6)}"
                        val newLevel = LevelData(
                            id = newId,
                            name = trimmed,
                            objects = emptyList(),
                            cameraSettings = CameraSettings(
                                viewportWidth = project.gameWidth.toFloat(),
                                viewportHeight = project.gameHeight.toFloat()
                            )
                        )
                        storage.saveLevel(project.id, newLevel)
                        levelIds = storage.listLevels(project.id)
                        showNewLevelDialog = false
                        onOpenLevel(newId)
                    },
                    modifier = Modifier.testTag("btn_confirm_create_level")
                ) {
                    Text("Create & Open")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewLevelDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
