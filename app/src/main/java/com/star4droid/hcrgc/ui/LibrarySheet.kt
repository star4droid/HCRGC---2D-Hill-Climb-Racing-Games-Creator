package com.star4droid.hcrgc.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.star4droid.hcrgc.assets.VectorSprites
import com.star4droid.hcrgc.model.GameObject
import com.star4droid.hcrgc.model.ReusableElement
import com.star4droid.hcrgc.storage.ProjectStorage
import com.star4droid.hcrgc.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibrarySheet(
    projectId: String,
    onDismiss: () -> Unit,
    onInsertElement: (GameObject) -> Unit
) {
    val context = LocalContext.current
    val storage = remember { ProjectStorage(context) }
    var elements by remember { mutableStateOf(storage.listReusableElements(projectId)) }

    val categories = listOf("Vehicles", "Terrain", "Gameplay", "UI", "Decorations")
    val expandedCategories = remember { mutableStateMapOf<String, Boolean>().apply { categories.forEach { put(it, true) } } }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = StudioSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .testTag("sheet_reusable_library")
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.BookmarkBorder, contentDescription = null, tint = StudioAccentBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Reusable Object Library",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = StudioTextPrimary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (elements.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No reusable elements saved yet.\nSelect any object in Hierarchy and tap 'Save as Reusable Element'!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = StudioTextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { category ->
                        val catElements = elements.filter { it.category == category }
                        if (catElements.isNotEmpty()) {
                            item {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            expandedCategories[category] = !(expandedCategories[category] ?: true)
                                        },
                                    color = StudioSurfaceElevated
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "$category (${catElements.size})",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = StudioTextPrimary
                                        )
                                        Icon(
                                            imageVector = if (expandedCategories[category] == true) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = null
                                        )
                                    }
                                }
                            }

                            if (expandedCategories[category] == true) {
                                items(catElements) { elem ->
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 12.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable {
                                                val newObj = elem.gameObject.copy(
                                                    id = java.util.UUID.randomUUID().toString().take(8),
                                                    x = elem.gameObject.x + 30f,
                                                    y = elem.gameObject.y + 30f
                                                )
                                                onInsertElement(newObj)
                                                onDismiss()
                                            },
                                        color = StudioSurface,
                                        border = BorderStroke(1.dp, StudioSurfaceBorder)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .background(StudioSurfaceElevated, RoundedCornerShape(6.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Canvas(modifier = Modifier.size(28.dp)) {
                                                    VectorSprites.drawAsset(this, elem.gameObject.imageAsset, size.width, size.height)
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(elem.name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                                Text(elem.gameObject.type.name, style = MaterialTheme.typography.labelSmall, color = StudioTextSecondary)
                                            }
                                            Icon(Icons.Default.AddCircleOutline, contentDescription = "Insert", tint = StudioAccentBlue)
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
}

@Composable
fun SaveReusableDialog(
    targetObject: GameObject,
    onDismiss: () -> Unit,
    onSave: (name: String, category: String) -> Unit
) {
    var name by remember { mutableStateOf(targetObject.name) }
    var selectedCat by remember { mutableStateOf("Vehicles") }
    val categories = listOf("Vehicles", "Terrain", "Gameplay", "UI", "Decorations")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save as Reusable Element") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Element Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Category:", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.take(3).forEach { cat ->
                        FilterChip(
                            selected = (selectedCat == cat),
                            onClick = { selectedCat = cat },
                            label = { Text(cat, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.drop(3).forEach { cat ->
                        FilterChip(
                            selected = (selectedCat == cat),
                            onClick = { selectedCat = cat },
                            label = { Text(cat, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(name.trim(), selectedCat)
                    }
                }
            ) {
                Text("Save to Library")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
