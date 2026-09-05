package com.star4droid.hcrgc.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.star4droid.hcrgc.model.ProjectConfig
import com.star4droid.hcrgc.model.ScreenOrientation
import com.star4droid.hcrgc.ui.theme.*

@Composable
fun ProjectSettingsDialog(
    project: ProjectConfig,
    onDismiss: () -> Unit,
    onSave: (ProjectConfig) -> Unit
) {
    var name by remember { mutableStateOf(project.name) }
    var orientation by remember { mutableStateOf(project.orientation) }
    var widthText by remember { mutableStateOf(project.gameWidth.toString()) }
    var heightText by remember { mutableStateOf(project.gameHeight.toString()) }
    var gravityText by remember { mutableStateOf(project.gravityY.toString()) }
    var selectedBgColor by remember { mutableLongStateOf(project.backgroundColor) }
    var ambientIntensity by remember { mutableFloatStateOf(project.ambientLightIntensity) }
    var ambientColor by remember { mutableLongStateOf(project.ambientLightColor) }

    val bgColors = listOf(
        0xFF7DD3FC to "Sky",
        0xFF0F172A to "Night",
        0xFF1E293B to "Slate",
        0xFFFDBA74 to "Sunset",
        0xFF86EFAC to "Meadow",
        0xFFD8B4FE to "Twilight"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Project & Display Settings", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Project Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_project_name")
                )

                // Orientation
                Text("Target Orientation:", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = (orientation == ScreenOrientation.LANDSCAPE),
                        onClick = {
                            orientation = ScreenOrientation.LANDSCAPE
                            widthText = "1280"
                            heightText = "720"
                        },
                        label = { Text("Landscape (16:9)") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = (orientation == ScreenOrientation.PORTRAIT),
                        onClick = {
                            orientation = ScreenOrientation.PORTRAIT
                            widthText = "720"
                            heightText = "1280"
                        },
                        label = { Text("Portrait (9:16)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Resolution
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = widthText,
                        onValueChange = { widthText = it },
                        label = { Text("Width (px)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = heightText,
                        onValueChange = { heightText = it },
                        label = { Text("Height (px)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Gravity
                OutlinedTextField(
                    value = gravityText,
                    onValueChange = { gravityText = it },
                    label = { Text("World Gravity Y") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Background Color
                Text("Sky / Background Canvas:", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    bgColors.forEach { (colorVal, _) ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(colorVal))
                                .border(
                                    width = if (selectedBgColor == colorVal) 3.dp else 1.dp,
                                    color = if (selectedBgColor == colorVal) StudioAccentBlue else StudioSurfaceBorder,
                                    shape = CircleShape
                                )
                                .clickable { selectedBgColor = colorVal }
                        )
                    }
                }
                // Ambient Light Settings (box2dlights project-level ambient)
                Text("Ambient World Lighting:", style = MaterialTheme.typography.labelLarge)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        1.0f to ("Day" to 0xFFFFFFFF),
                        0.7f to ("Sunset" to 0xFFFFA07A),
                        0.4f to ("Twilight" to 0xFF6366F1),
                        0.2f to ("Night" to 0xFF1E1B4B),
                        0.05f to ("Pitch Dark" to 0xFF050510)
                    ).forEach { (intensity, pair) ->
                        val (lbl, col) = pair
                        FilterChip(
                            selected = (kotlin.math.abs(ambientIntensity - intensity) < 0.08f),
                            onClick = {
                                ambientIntensity = intensity
                                ambientColor = col
                            },
                            label = { Text(lbl, fontSize = 10.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Ambient Brightness: ${(ambientIntensity * 100).toInt()}%", fontSize = 12.sp)
                    Slider(
                        value = ambientIntensity,
                        onValueChange = { ambientIntensity = it },
                        valueRange = 0.0f..1.0f,
                        modifier = Modifier.weight(1f).padding(start = 12.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val w = widthText.toIntOrNull() ?: project.gameWidth
                    val h = heightText.toIntOrNull() ?: project.gameHeight
                    val g = gravityText.toFloatOrNull() ?: project.gravityY
                    onSave(
                        project.copy(
                            name = name.trim().ifEmpty { project.name },
                            orientation = orientation,
                            gameWidth = w,
                            gameHeight = h,
                            gravityY = g,
                            backgroundColor = selectedBgColor,
                            ambientLightColor = ambientColor,
                            ambientLightIntensity = ambientIntensity
                        )
                    )
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Save Settings")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
