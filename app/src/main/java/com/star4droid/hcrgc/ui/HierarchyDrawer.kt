package com.star4droid.hcrgc.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.star4droid.hcrgc.editor.EditorState
import com.star4droid.hcrgc.model.GameObject
import com.star4droid.hcrgc.model.ObjectType
import com.star4droid.hcrgc.ui.theme.*

@Composable
fun HierarchyDrawer(
    state: EditorState,
    onClose: () -> Unit,
    onOpenProperties: () -> Unit,
    onSaveToLibrary: (GameObject) -> Unit
) {
    var renamingObj by remember { mutableStateOf<GameObject?>(null) }
    var renameText by remember { mutableStateOf("") }
    var assigningParentObj by remember { mutableStateOf<GameObject?>(null) }

    Surface(
        modifier = Modifier
            .fillMaxHeight()
            .width(320.dp)
            .testTag("drawer_hierarchy"),
        color = StudioSurface,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, StudioSurfaceBorder)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountTree, contentDescription = null, tint = StudioAccentBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Scene Hierarchy",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = StudioTextPrimary
                    )
                }

                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = StudioTextSecondary)
                }
            }

            HorizontalDivider(color = StudioSurfaceBorder)

            // Separate UI Elements vs Scene Objects
            val uiObjects = state.level.objects.filter {
                it.type == ObjectType.UI_BUTTON || it.type == ObjectType.UI_TEXT || it.type == ObjectType.UI_PROGRESS_BAR
            }.sortedByDescending { it.zIndex }

            val worldObjects = state.level.objects.filter {
                it.type != ObjectType.UI_BUTTON && it.type != ObjectType.UI_TEXT && it.type != ObjectType.UI_PROGRESS_BAR
            }.sortedByDescending { it.zIndex }

            var uiLayerExpanded by remember { mutableStateOf(true) }
            var worldLayerExpanded by remember { mutableStateOf(true) }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp)
            ) {
                // --- UI ELEMENTS LAYER SECTION ---
                item {
                    Surface(
                        color = StudioSurfaceElevated,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { uiLayerExpanded = !uiLayerExpanded },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, StudioSurfaceBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (uiLayerExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = StudioAccentBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Default.Layers, contentDescription = null, tint = StudioAccentBlue, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("UI Elements Layer", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = StudioTextPrimary)
                                Spacer(Modifier.width(6.dp))
                                Surface(
                                    color = StudioAccentBlue.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        "${uiObjects.size}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = StudioAccentBlue,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }

                            IconButton(
                                onClick = { state.showUiElementsLayer = !state.showUiElementsLayer },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = if (state.showUiElementsLayer) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle UI Elements Layer",
                                    tint = if (state.showUiElementsLayer) StudioAccentBlue else StudioTextTertiary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }

                if (uiLayerExpanded) {
                    if (uiObjects.isEmpty()) {
                        item {
                            Text(
                                "No UI elements yet (Add button/text from + menu)",
                                fontSize = 11.sp,
                                color = StudioTextTertiary,
                                modifier = Modifier.padding(start = 24.dp, top = 2.dp, bottom = 6.dp)
                            )
                        }
                    } else {
                        items(uiObjects, key = { it.id }) { obj ->
                            HierarchyItemRow(
                                obj = obj,
                                isSelected = (obj.id == state.selectedObjectId),
                                onSelect = { state.selectedObjectId = obj.id },
                                onToggleVisible = { state.updateObject(obj.copy(visible = !obj.visible)) },
                                onOpenProperties = {
                                    state.selectedObjectId = obj.id
                                    onOpenProperties()
                                },
                                onRename = {
                                    renameText = obj.name
                                    renamingObj = obj
                                },
                                onDuplicate = { state.duplicateObject(obj.id) },
                                onBringForward = { state.bringForward(obj.id) },
                                onSendBackward = { state.sendBackward(obj.id) },
                                onBringToFront = { state.bringToFront(obj.id) },
                                onSendToBack = { state.sendToBack(obj.id) },
                                onAssignParent = { assigningParentObj = obj },
                                onSaveToLibrary = { onSaveToLibrary(obj) },
                                onDelete = { state.removeObject(obj.id) }
                            )
                        }
                    }
                }

                // --- WORLD OBJECTS SECTION ---
                item {
                    Surface(
                        color = StudioSurfaceElevated,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { worldLayerExpanded = !worldLayerExpanded },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, StudioSurfaceBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (worldLayerExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = StudioAccentIndigo,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Default.Public, contentDescription = null, tint = StudioAccentIndigo, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Scene Elements", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = StudioTextPrimary)
                                Spacer(Modifier.width(6.dp))
                                Surface(
                                    color = StudioAccentIndigo.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        "${worldObjects.size}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = StudioAccentIndigo,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (worldLayerExpanded) {
                    items(worldObjects, key = { it.id }) { obj ->
                        HierarchyItemRow(
                            obj = obj,
                            isSelected = (obj.id == state.selectedObjectId),
                            onSelect = { state.selectedObjectId = obj.id },
                            onToggleVisible = { state.updateObject(obj.copy(visible = !obj.visible)) },
                            onOpenProperties = {
                                state.selectedObjectId = obj.id
                                onOpenProperties()
                            },
                            onRename = {
                                renameText = obj.name
                                renamingObj = obj
                            },
                            onDuplicate = { state.duplicateObject(obj.id) },
                            onBringForward = { state.bringForward(obj.id) },
                            onSendBackward = { state.sendBackward(obj.id) },
                            onBringToFront = { state.bringToFront(obj.id) },
                            onSendToBack = { state.sendToBack(obj.id) },
                            onAssignParent = { assigningParentObj = obj },
                            onSaveToLibrary = { onSaveToLibrary(obj) },
                            onDelete = { state.removeObject(obj.id) }
                        )
                    }
                }
            }

            // Quick Z-Order Footer Bar when an item is selected
            if (state.selectedObject != null) {
                HorizontalDivider(color = StudioSurfaceBorder)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val selId = state.selectedObject!!.id
                    IconButton(onClick = { state.bringToFront(selId) }) {
                        Icon(Icons.Default.VerticalAlignTop, contentDescription = "To Front", tint = StudioAccentBlue)
                    }
                    IconButton(onClick = { state.bringForward(selId) }) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = "Forward", tint = StudioAccentBlue)
                    }
                    IconButton(onClick = { state.sendBackward(selId) }) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = "Backward", tint = StudioAccentBlue)
                    }
                    IconButton(onClick = { state.sendToBack(selId) }) {
                        Icon(Icons.Default.VerticalAlignBottom, contentDescription = "To Back", tint = StudioAccentBlue)
                    }
                }
            }
        }
    }

    // Rename Dialog
    if (renamingObj != null) {
        AlertDialog(
            onDismissRequest = { renamingObj = null },
            title = { Text("Rename Object") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("Object Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = renameText.trim()
                        if (trimmed.isNotEmpty()) {
                            state.updateObject(renamingObj!!.copy(name = trimmed))
                        }
                        renamingObj = null
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { renamingObj = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Assign Parent Dialog
    if (assigningParentObj != null) {
        val target = assigningParentObj!!
        val potentialParents = state.level.objects.filter { it.id != target.id && it.parentId != target.id }

        AlertDialog(
            onDismissRequest = { assigningParentObj = null },
            title = { Text("Select Parent Object") },
            text = {
                Column {
                    Text(
                        "Attach '${target.name}' to follow another object:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = StudioTextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(
                        onClick = {
                            state.updateObject(target.copy(parentId = null))
                            assigningParentObj = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("None (Detach)")
                    }

                    potentialParents.forEach { p ->
                        TextButton(
                            onClick = {
                                state.updateObject(target.copy(parentId = p.id))
                                assigningParentObj = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("${p.name} (${p.type.name})")
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { assigningParentObj = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun HierarchyItemRow(
    obj: GameObject,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onToggleVisible: () -> Unit,
    onOpenProperties: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onBringForward: () -> Unit,
    onSendBackward: () -> Unit,
    onBringToFront: () -> Unit,
    onSendToBack: () -> Unit,
    onAssignParent: () -> Unit,
    onSaveToLibrary: () -> Unit,
    onDelete: () -> Unit
) {
    val isChild = (obj.parentId != null)
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 1.dp)
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onSelect)
            .testTag("hierarchy_item_${obj.id}"),
        shape = RoundedCornerShape(6.dp),
        color = if (isSelected) StudioAccentBlue.copy(alpha = 0.12f) else StudioSurface,
        border = if (isSelected) BorderStroke(1.dp, StudioAccentBlue) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 36.dp)
                .padding(
                    start = if (isChild) 22.dp else 6.dp,
                    end = 4.dp,
                    top = 2.dp,
                    bottom = 2.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isChild) {
                Icon(
                    imageVector = Icons.Default.SubdirectoryArrowRight,
                    contentDescription = "Child",
                    tint = StudioAccentIndigo,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
            }

            // Compact Type Icon Box
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .background(
                        when (obj.type) {
                            ObjectType.CAR_BODY, ObjectType.WHEEL -> StudioAccentOrange.copy(alpha = 0.15f)
                            ObjectType.COIN, ObjectType.FINISH_FLAG -> StudioAccentAmber.copy(alpha = 0.15f)
                            ObjectType.CUSTOM_SHAPE, ObjectType.TILEMAP -> StudioAccentGreen.copy(alpha = 0.15f)
                            ObjectType.UI_BUTTON, ObjectType.UI_TEXT, ObjectType.UI_PROGRESS_BAR -> StudioAccentBlue.copy(alpha = 0.15f)
                            ObjectType.ELEMENT -> StudioAccentIndigo.copy(alpha = 0.15f)
                            else -> StudioSurfaceBorder
                        },
                        RoundedCornerShape(4.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (obj.type) {
                        ObjectType.CAR_BODY -> Icons.Default.DirectionsCar
                        ObjectType.WHEEL -> Icons.Default.TireRepair
                        ObjectType.COIN -> Icons.Default.MonetizationOn
                        ObjectType.FINISH_FLAG -> Icons.Default.Flag
                        ObjectType.LIGHT -> Icons.Default.Lightbulb
                        ObjectType.TILEMAP -> Icons.Default.GridOn
                        ObjectType.CUSTOM_SHAPE -> Icons.Default.Polyline
                        ObjectType.ELEMENT -> Icons.Default.Widgets
                        ObjectType.UI_BUTTON -> Icons.Default.SmartButton
                        ObjectType.UI_TEXT -> Icons.Default.TextFields
                        ObjectType.UI_PROGRESS_BAR -> Icons.Default.LinearScale
                        else -> Icons.Default.Square
                    },
                    contentDescription = null,
                    tint = StudioTextPrimary,
                    modifier = Modifier.size(13.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Name & Compact Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = obj.name,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) StudioAccentBlue else StudioTextPrimary,
                    maxLines = 1
                )
                Text(
                    text = "${obj.type.name} • Z:${obj.zIndex}",
                    fontSize = 9.sp,
                    color = StudioTextSecondary,
                    maxLines = 1
                )
            }

            // Compact Visibility Button
            IconButton(
                onClick = onToggleVisible,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = if (obj.visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = "Toggle Visibility",
                    tint = if (obj.visible) StudioTextSecondary else StudioTextTertiary,
                    modifier = Modifier.size(14.dp)
                )
            }

            // Action Menu
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Menu",
                        tint = StudioTextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    containerColor = StudioSurface
                ) {
                    DropdownMenuItem(
                        text = { Text("Properties") },
                        leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onOpenProperties()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onRename()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Duplicate") },
                        leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onDuplicate()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Bring Forward") },
                        leadingIcon = { Icon(Icons.Default.ArrowUpward, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onBringForward()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Send Backward") },
                        leadingIcon = { Icon(Icons.Default.ArrowDownward, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onSendBackward()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Bring to Front") },
                        leadingIcon = { Icon(Icons.Default.VerticalAlignTop, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onBringToFront()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Send to Back") },
                        leadingIcon = { Icon(Icons.Default.VerticalAlignBottom, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onSendToBack()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Set Parent Object") },
                        leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onAssignParent()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Save as Reusable Element") },
                        leadingIcon = { Icon(Icons.Default.BookmarkBorder, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onSaveToLibrary()
                        }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Delete", color = StudioAccentRed) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = StudioAccentRed) },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}
