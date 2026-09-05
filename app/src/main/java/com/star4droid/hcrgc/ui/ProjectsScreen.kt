package com.star4droid.hcrgc.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.star4droid.hcrgc.model.ProjectConfig
import com.star4droid.hcrgc.model.ScreenOrientation
import com.star4droid.hcrgc.storage.ProjectStorage
import com.star4droid.hcrgc.storage.SampleProjectGenerator
import com.star4droid.hcrgc.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(
    onOpenProject: (ProjectConfig) -> Unit
) {
    val context = LocalContext.current
    val storage = remember { ProjectStorage(context) }
    var projects by remember { mutableStateOf(storage.listProjects()) }
    var showNewProjectDialog by remember { mutableStateOf(false) }

    // Ensure sample project is created if empty
    LaunchedEffect(Unit) {
        if (projects.isEmpty()) {
            val (sampleProj, sampleLevel) = SampleProjectGenerator.createSampleProject()
            storage.saveProject(sampleProj)
            storage.saveLevel(sampleProj.id, sampleLevel)
            projects = storage.listProjects()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(StudioAccentOrange, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "HCRGC",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = StudioTextPrimary
                            )
                            Text(
                                text = "Hill Climb Racing Game Creator",
                                style = MaterialTheme.typography.labelSmall,
                                color = StudioTextSecondary
                            )
                        }
                    }
                },
                actions = {
                    Button(
                        onClick = { showNewProjectDialog = true },
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .testTag("btn_new_project"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = StudioAccentBlue)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("New Project")
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
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Text(
                text = "My Games (${projects.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = StudioTextSecondary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(projects, key = { it.id }) { project ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { onOpenProject(project) }
                            .testTag("project_card_${project.id}"),
                        shape = RoundedCornerShape(18.dp),
                        color = StudioSurface,
                        shadowElevation = 3.dp,
                        border = BorderStroke(1.dp, StudioSurfaceBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .background(Color(project.backgroundColor), RoundedCornerShape(14.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (project.orientation == ScreenOrientation.LANDSCAPE) Icons.Default.StayCurrentLandscape else Icons.Default.StayCurrentPortrait,
                                        contentDescription = null,
                                        tint = StudioSurface,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column {
                                    Text(
                                        text = project.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = StudioTextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${project.orientation.name} • ${project.gameWidth}×${project.gameHeight} px",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = StudioTextSecondary
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Button(
                                    onClick = { onOpenProject(project) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = StudioAccentBlue)
                                ) {
                                    Text("Open")
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                IconButton(
                                    onClick = {
                                        storage.deleteProject(project.id)
                                        projects = storage.listProjects()
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

    if (showNewProjectDialog) {
        NewProjectModal(
            onDismiss = { showNewProjectDialog = false },
            onCreate = { newProj ->
                storage.saveProject(newProj)
                // Initialize a default level
                val defaultLevel = com.star4droid.hcrgc.model.LevelData(
                    id = "level_1",
                    name = "Level 1",
                    objects = emptyList(),
                    cameraSettings = com.star4droid.hcrgc.model.CameraSettings(
                        viewportWidth = newProj.gameWidth.toFloat(),
                        viewportHeight = newProj.gameHeight.toFloat()
                    )
                )
                storage.saveLevel(newProj.id, defaultLevel)
                projects = storage.listProjects()
                showNewProjectDialog = false
                onOpenProject(newProj)
            }
        )
    }
}

@Composable
private fun NewProjectModal(
    onDismiss: () -> Unit,
    onCreate: (ProjectConfig) -> Unit
) {
    var name by remember { mutableStateOf("My Hill Climb Game") }
    var orientation by remember { mutableStateOf(ScreenOrientation.LANDSCAPE) }
    var width by remember { mutableStateOf("1280") }
    var height by remember { mutableStateOf("720") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Create New Game Project", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Game Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_new_project_title")
                )

                Text("Screen Orientation:", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = (orientation == ScreenOrientation.LANDSCAPE),
                        onClick = {
                            orientation = ScreenOrientation.LANDSCAPE
                            width = "1280"
                            height = "720"
                        },
                        label = { Text("Landscape (16:9)") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = (orientation == ScreenOrientation.PORTRAIT),
                        onClick = {
                            orientation = ScreenOrientation.PORTRAIT
                            width = "720"
                            height = "1280"
                        },
                        label = { Text("Portrait (9:16)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = width,
                        onValueChange = { width = it },
                        label = { Text("Width") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = height,
                        onValueChange = { height = it },
                        label = { Text("Height") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val w = width.toIntOrNull() ?: 1280
                    val h = height.toIntOrNull() ?: 720
                    val proj = ProjectConfig(
                        id = "proj_${System.currentTimeMillis().toString().takeLast(6)}",
                        name = name.trim().ifEmpty { "New Game" },
                        orientation = orientation,
                        gameWidth = w,
                        gameHeight = h
                    )
                    onCreate(proj)
                },
                modifier = Modifier.testTag("btn_confirm_create_project")
            ) {
                Text("Create Project")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
