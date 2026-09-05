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

            // UI Elements Layer Collection Header
            val uiObjects = state.level.objects.filter {
                it.type == ObjectType.UI_BUTTON || it.type == ObjectType.UI_TEXT || it.type == ObjectType.UI_PROGRESS_BAR
            }
            Surface(
                color = StudioSurfaceElevated,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, StudioSurfaceBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Layers, contentDescription = null, tint = StudioAccentBlue, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("UI Elements Layer", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = StudioTextPrimary)
                            Text("${uiObjects.size} UI elements in scene", fontSize = 10.sp, color = StudioTextSecondary)
                        }
                    }

                    IconButton(
                        onClick = { state.showUiElementsLayer = !state.showUiElementsLayer },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (state.showUiElementsLayer) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle UI Elements Layer",
                            tint = if (state.showUiElementsLayer) StudioAccentBlue else StudioTextTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Objects List (sorted by Z-order descending)
            val sortedList = state.level.objects.sortedByDescending { it.zIndex }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp)
            ) {
                items(sortedList, key = { it.id }) { obj ->
                    val isSelected = (obj.id == state.selectedObjectId)
                    val isChild = (obj.parentId != null)

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                state.selectedObjectId = obj.id
                            }
                            .testTag("hierarchy_item_${obj.id}"),
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) StudioAccentBlue.copy(alpha = 0.12f) else StudioSurface,
                        border = if (isSelected) BorderStroke(1.5.dp, StudioAccentBlue) else null
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    start = if (isChild) 28.dp else 10.dp,
                                    end = 8.dp,
                                    top = 8.dp,
                                    bottom = 8.dp
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isChild) {
                                Icon(
                                    imageVector = Icons.Default.SubdirectoryArrowRight,
                                    contentDescription = "Child",
                                    tint = StudioAccentIndigo,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }

                            // Object Type Icon
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .background(
                                        when (obj.type) {
                                            ObjectType.CAR_BODY, ObjectType.WHEEL -> StudioAccentOrange.copy(alpha = 0.15f)
                                            ObjectType.COIN, ObjectType.FINISH_FLAG -> StudioAccentAmber.copy(alpha = 0.15f)
                                            ObjectType.CUSTOM_SHAPE, ObjectType.TILEMAP -> StudioAccentGreen.copy(alpha = 0.15f)
                                            ObjectType.ELEMENT -> StudioAccentIndigo.copy(alpha = 0.15f)
                                            else -> StudioAccentBlue.copy(alpha = 0.15f)
                                        },
                                        RoundedCornerShape(6.dp)
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
                                        else -> Icons.Default.Square
                                    },
                                    contentDescription = null,
                                    tint = StudioTextPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            // Name & Type
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = obj.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = StudioTextPrimary,
                                    maxLines = 1
                                )
                                Text(
                                    text = "${obj.type.name} • Z:${obj.zIndex}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = StudioTextSecondary
                                )
                            }

                            // Visibility Toggle
                            IconButton(
                                onClick = {
                                    state.updateObject(obj.copy(visible = !obj.visible))
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (obj.visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle Visibility",
                                    tint = if (obj.visible) StudioTextSecondary else StudioTextTertiary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Action Menu (Rename, Z-Order, Parent, Save as Reusable, Delete)
                            var menuExpanded by remember { mutableStateOf(false) }
                            Box {
                                IconButton(
                                    onClick = { menuExpanded = true },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = StudioTextSecondary, modifier = Modifier.size(18.dp))
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
                                            state.selectedObjectId = obj.id
                                            menuExpanded = false
                                            onOpenProperties()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Rename") },
                                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                        onClick = {
                                            renameText = obj.name
                                            renamingObj = obj
                                            menuExpanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Duplicate") },
                                        leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                                        onClick = {
                                            state.duplicateObject(obj.id)
                                            menuExpanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Bring Forward") },
                                        leadingIcon = { Icon(Icons.Default.ArrowUpward, contentDescription = null) },
                                        onClick = {
                                            state.bringForward(obj.id)
                                            menuExpanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Send Backward") },
                                        leadingIcon = { Icon(Icons.Default.ArrowDownward, contentDescription = null) },
                                        onClick = {
                                            state.sendBackward(obj.id)
                                            menuExpanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Bring to Front") },
                                        leadingIcon = { Icon(Icons.Default.VerticalAlignTop, contentDescription = null) },
                                        onClick = {
                                            state.bringToFront(obj.id)
                                            menuExpanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Send to Back") },
                                        leadingIcon = { Icon(Icons.Default.VerticalAlignBottom, contentDescription = null) },
                                        onClick = {
                                            state.sendToBack(obj.id)
                                            menuExpanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Set Parent Object") },
                                        leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                                        onClick = {
                                            assigningParentObj = obj
                                            menuExpanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Save as Reusable Element") },
                                        leadingIcon = { Icon(Icons.Default.BookmarkBorder, contentDescription = null) },
                                        onClick = {
                                            onSaveToLibrary(obj)
                                            menuExpanded = false
                                        }
                                    )
                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        text = { Text("Delete", color = StudioAccentRed) },
                                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = StudioAccentRed) },
                                        onClick = {
                                            state.removeObject(obj.id)
                                            menuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
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
