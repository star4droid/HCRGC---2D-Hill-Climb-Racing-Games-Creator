package com.star4droid.hcrgc.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.star4droid.hcrgc.assets.VectorSprites
import com.star4droid.hcrgc.editor.EditorCanvas
import com.star4droid.hcrgc.editor.EditorState
import com.star4droid.hcrgc.editor.EditorTool
import com.star4droid.hcrgc.editor.TransformMode
import com.star4droid.hcrgc.model.GameObject
import com.star4droid.hcrgc.model.LevelData
import com.star4droid.hcrgc.model.ObjectType
import com.star4droid.hcrgc.model.ProjectConfig
import com.star4droid.hcrgc.model.ReusableElement
import com.star4droid.hcrgc.model.ScreenOrientation
import com.star4droid.hcrgc.runtime.GameRuntimeScreen
import com.star4droid.hcrgc.storage.ProjectStorage
import com.star4droid.hcrgc.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    initialProject: ProjectConfig,
    initialLevel: LevelData,
    onBackToLevels: () -> Unit
) {
    val context = LocalContext.current
    val storage = remember { ProjectStorage(context) }

    var currentProject by remember { mutableStateOf(initialProject) }
    var isPlayMode by remember { mutableStateOf(false) }

    // Physical Device Orientation Sync
    val activity = context as? Activity
    LaunchedEffect(currentProject.orientation) {
        activity?.requestedOrientation = if (currentProject.orientation == ScreenOrientation.LANDSCAPE) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Editor State
    var editorStateHolder by remember { mutableStateOf<EditorState?>(null) }
    val editorState = remember {
        EditorState(
            initialProject = currentProject,
            initialLevel = initialLevel,
            onLevelModified = {
                editorStateHolder?.let { storage.saveLevel(currentProject.id, it.level) }
            }
        ).also { editorStateHolder = it }
    }

    // Auto-save when level changes
    LaunchedEffect(editorState.level) {
        storage.saveLevel(currentProject.id, editorState.level)
    }

    // Dialogs & Drawers states
    var showAddObjectDialog by remember { mutableStateOf(false) }
    var showHierarchyDrawer by remember { mutableStateOf(false) }
    var showPropertySheet by remember { mutableStateOf(false) }
    var showAssetManagerSheet by remember { mutableStateOf(false) }
    var showLibrarySheet by remember { mutableStateOf(false) }
    var showProjectSettings by remember { mutableStateOf(false) }
    var objectToSaveToLibrary by remember { mutableStateOf<GameObject?>(null) }

    if (isPlayMode) {
        // Fullscreen Play Runtime Mode
        GameRuntimeScreen(
            project = currentProject,
            level = editorState.level,
            onExitToEditor = { isPlayMode = false }
        )
    } else {
        Scaffold(
            topBar = {
                Surface(
                    color = StudioSurface,
                    shadowElevation = 4.dp,
                    border = BorderStroke(1.dp, StudioSurfaceBorder)
                ) {
                    // Single horizontally scrollable top bar with all tools & prominent play button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Back button
                        IconButton(
                            onClick = onBackToLevels,
                            modifier = Modifier.size(34.dp).testTag("btn_back_levels")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = StudioTextPrimary, modifier = Modifier.size(20.dp))
                        }

                        // Compact Level Title badge
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = StudioSurfaceElevated,
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Text(
                                text = editorState.level.name,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = StudioTextPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }

                        // PROMINENT PLAY BUTTON - Placed right upfront so it's always easy to click!
                        Button(
                            onClick = { isPlayMode = true },
                            modifier = Modifier.height(34.dp).testTag("btn_play_game"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = StudioAccentGreen),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Play", modifier = Modifier.size(18.dp), tint = Color.Black)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("PLAY", fontWeight = FontWeight.Black, fontSize = 12.sp, color = Color.Black)
                        }

                        VerticalDivider(modifier = Modifier.height(20.dp), color = StudioSurfaceBorder)

                        // Snap to grid
                        IconButton(
                            onClick = { editorState.snapToGrid = !editorState.snapToGrid },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Grid4x4,
                                contentDescription = "Snap Grid",
                                tint = if (editorState.snapToGrid) StudioAccentBlue else StudioTextTertiary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Hierarchy Drawer toggle
                        IconButton(
                            onClick = { showHierarchyDrawer = !showHierarchyDrawer },
                            modifier = Modifier.size(34.dp).testTag("btn_hierarchy")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountTree,
                                contentDescription = "Hierarchy",
                                tint = if (showHierarchyDrawer) StudioAccentBlue else StudioTextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Assets
                        IconButton(
                            onClick = { showAssetManagerSheet = true },
                            modifier = Modifier.size(34.dp).testTag("btn_assets")
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = "Assets", tint = StudioTextPrimary, modifier = Modifier.size(18.dp))
                        }

                        // Library
                        IconButton(
                            onClick = { showLibrarySheet = true },
                            modifier = Modifier.size(34.dp).testTag("btn_library")
                        ) {
                            Icon(Icons.Default.BookmarkBorder, contentDescription = "Library", tint = StudioTextPrimary, modifier = Modifier.size(18.dp))
                        }

                        // Project Settings
                        IconButton(
                            onClick = { showProjectSettings = true },
                            modifier = Modifier.size(34.dp).testTag("btn_settings")
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = StudioTextPrimary, modifier = Modifier.size(18.dp))
                        }

                        VerticalDivider(modifier = Modifier.height(20.dp), color = StudioSurfaceBorder)

                        // Undo
                        IconButton(
                            onClick = { editorState.undo() },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo", tint = StudioTextPrimary, modifier = Modifier.size(18.dp))
                        }

                        // Redo
                        IconButton(
                            onClick = { editorState.redo() },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo", tint = StudioTextPrimary, modifier = Modifier.size(18.dp))
                        }

                        VerticalDivider(modifier = Modifier.height(20.dp), color = StudioSurfaceBorder)

                        // Zoom info & Reset
                        Text(
                            "Zoom ${(editorState.zoom * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = StudioTextSecondary,
                            fontSize = 11.sp
                        )
                        FilledTonalButton(
                            onClick = { editorState.zoom = 1.0f },
                            modifier = Modifier.height(28.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("100%", fontSize = 10.5.sp)
                        }
                    }
                }
            },
            bottomBar = {
                // Bottom Quick Action Bar (Horizontally scrollable, icon-only buttons)
                Surface(
                    color = StudioSurface,
                    shadowElevation = 8.dp,
                    border = BorderStroke(1.dp, StudioSurfaceBorder)
                ) {
                    val sel = editorState.selectedObject
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Fixed width object name container with auto-text-size to fit
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = StudioSurfaceElevated,
                            border = BorderStroke(1.dp, StudioSurfaceBorder),
                            modifier = Modifier
                                .width(120.dp)
                                .height(38.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                val nameText = sel?.name ?: "No Selection"
                                val autoFontSize = when {
                                    nameText.length > 14 -> 9.sp
                                    nameText.length > 10 -> 10.5.sp
                                    else -> 12.sp
                                }
                                Text(
                                    text = nameText,
                                    fontSize = autoFontSize,
                                    fontWeight = FontWeight.Bold,
                                    color = if (sel != null) StudioTextPrimary else StudioTextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Properties Button: Icon only
                        IconButton(
                            onClick = { showPropertySheet = true },
                            enabled = (sel != null),
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (sel != null) StudioAccentBlue else StudioSurfaceElevated)
                                .testTag("btn_inspect_properties")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Properties",
                                tint = if (sel != null) Color.White else StudioTextTertiary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Add Object Button: Icon only
                        IconButton(
                            onClick = { showAddObjectDialog = true },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(StudioAccentOrange)
                                .testTag("btn_add_object")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Object",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Coordinates summary badge
                        if (sel != null) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = StudioSurfaceElevated,
                                modifier = Modifier.padding(start = 2.dp)
                            ) {
                                Text(
                                    text = "X: ${sel.x.toInt()}  Y: ${sel.y.toInt()}  W: ${sel.width.toInt()}  H: ${sel.height.toInt()}  ${sel.rotation.toInt()}°",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = StudioTextSecondary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        } else {
                            Text(
                                text = "Drag canvas to pan • Tap object to select",
                                style = MaterialTheme.typography.labelSmall,
                                color = StudioTextSecondary,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                }
            },
            containerColor = StudioBackground
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Interactive Editor Canvas
                EditorCanvas(
                    state = editorState,
                    modifier = Modifier.fillMaxSize()
                )

                // Contextual TileMap Stamping Palette (visible when TileMap is selected)
                val selObj = editorState.selectedObject
                if (selObj?.type == ObjectType.TILEMAP) {
                    TileMapPaletteBar(
                        state = editorState,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 12.dp)
                    )
                }

                // Scene Hierarchy Drawer (slides from right)
                AnimatedVisibility(
                    visible = showHierarchyDrawer,
                    enter = slideInHorizontally(initialOffsetX = { it }),
                    exit = slideOutHorizontally(targetOffsetX = { it }),
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    HierarchyDrawer(
                        state = editorState,
                        onClose = { showHierarchyDrawer = false },
                        onOpenProperties = { showPropertySheet = true },
                        onSaveToLibrary = { obj -> objectToSaveToLibrary = obj }
                    )
                }
            }
        }
    }

    // Modal Dialogs & Sheets
    if (showAddObjectDialog) {
        AddObjectDialog(
            onDismiss = { showAddObjectDialog = false },
            onAddObject = { obj ->
                // Center new object in view
                val (cx, cy) = editorState.screenToWorld(500f, 400f)
                editorState.addObject(obj.copy(x = cx, y = cy))
            }
        )
    }

    if (showPropertySheet && editorState.selectedObject != null) {
        PropertySheet(
            state = editorState,
            onDismiss = { showPropertySheet = false },
            onOpenAssetManager = { showAssetManagerSheet = true }
        )
    }

    if (showAssetManagerSheet) {
        AssetManagerSheet(
            projectName = currentProject.id,
            onDismiss = { showAssetManagerSheet = false },
            onSelectAsset = { assetId ->
                editorState.selectedObject?.let { sel ->
                    editorState.updateObject(sel.copy(imageAsset = assetId))
                }
            }
        )
    }

    if (showLibrarySheet) {
        LibrarySheet(
            projectId = currentProject.id,
            onDismiss = { showLibrarySheet = false },
            onInsertElement = { obj ->
                val (cx, cy) = editorState.screenToWorld(500f, 400f)
                editorState.addObject(obj.copy(x = cx, y = cy))
            }
        )
    }

    if (showProjectSettings) {
        ProjectSettingsDialog(
            project = currentProject,
            onDismiss = { showProjectSettings = false },
            onSave = { updatedProj ->
                currentProject = updatedProj
                editorState.project = updatedProj
                storage.saveProject(updatedProj)
                showProjectSettings = false
            }
        )
    }

    if (objectToSaveToLibrary != null) {
        SaveReusableDialog(
            targetObject = objectToSaveToLibrary!!,
            onDismiss = { objectToSaveToLibrary = null },
            onSave = { name, category ->
                val elem = ReusableElement(
                    id = "elem_${System.currentTimeMillis().toString().takeLast(6)}",
                    name = name,
                    category = category,
                    gameObject = objectToSaveToLibrary!!
                )
                storage.saveReusableElement(currentProject.id, elem)
                objectToSaveToLibrary = null
            }
        )
    }
}

@Composable
private fun TileMapPaletteBar(
    state: EditorState,
    modifier: Modifier = Modifier
) {
    val tiles = listOf(
        "tile_grass" to "Grass",
        "tile_dirt" to "Dirt",
        "tile_rock" to "Rock",
        "tile_stone" to "Stone"
    )

    Surface(
        modifier = modifier.testTag("bar_tilemap_palette"),
        shape = RoundedCornerShape(16.dp),
        color = StudioSurface.copy(alpha = 0.95f),
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, StudioSurfaceBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Stamp:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)

            tiles.forEach { (assetId, label) ->
                val isSelected = (state.selectedTileAsset == assetId && !state.isErasingTile)
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            state.selectedTileAsset = assetId
                            state.isErasingTile = false
                            state.activeTool = EditorTool.STAMP_TILES
                        },
                    color = if (isSelected) StudioAccentBlue.copy(alpha = 0.2f) else Color.Transparent,
                    border = if (isSelected) BorderStroke(1.5.dp, StudioAccentBlue) else null
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(
                                    when (assetId) {
                                        "tile_grass" -> Color(0xFF16A34A)
                                        "tile_dirt" -> Color(0xFF78350F)
                                        "tile_rock" -> Color(0xFF475569)
                                        else -> Color(0xFF334155)
                                    },
                                    RoundedCornerShape(4.dp)
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(label, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // Eraser
            IconButton(
                onClick = {
                    state.isErasingTile = true
                    state.activeTool = EditorTool.STAMP_TILES
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoFixNormal,
                    contentDescription = "Erase Tile",
                    tint = if (state.isErasingTile) StudioAccentRed else StudioTextSecondary
                )
            }
        }
    }
}
