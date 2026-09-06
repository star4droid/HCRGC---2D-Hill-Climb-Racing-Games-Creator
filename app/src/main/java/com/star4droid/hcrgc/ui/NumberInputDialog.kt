package com.star4droid.hcrgc.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.star4droid.hcrgc.ui.theme.*

@Composable
fun NumberInputDialog(
    title: String,
    initialValue: Float,
    onDismiss: () -> Unit,
    onConfirm: (Float) -> Unit
) {
    val initialText = if (initialValue % 1.0f == 0.0f) {
        initialValue.toInt().toString()
    } else {
        initialValue.toString()
    }

    var valueText by remember { mutableStateOf(initialText) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = StudioSurface,
            border = BorderStroke(1.5.dp, StudioSurfaceBorder),
            shadowElevation = 16.dp,
            modifier = Modifier
                .widthIn(max = 340.dp)
                .fillMaxWidth(0.92f)
                .padding(16.dp)
                .testTag("dialog_number_input")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Title
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = StudioTextPrimary
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Input Display Area: ____ [del icon]
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = StudioSurfaceElevated,
                    border = BorderStroke(1.dp, StudioAccentBlue.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (valueText.isEmpty()) "0" else valueText,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (valueText.isEmpty()) StudioTextSecondary else StudioTextPrimary,
                            maxLines = 1,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("text_number_display")
                        )

                        IconButton(
                            onClick = {
                                if (valueText.isNotEmpty()) {
                                    valueText = valueText.dropLast(1)
                                }
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("btn_keypad_del")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Backspace,
                                contentDescription = "Delete",
                                tint = StudioAccentOrange,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Keypad Rows as requested:
                // Row 1: 1 2 3 C (clear)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    KeypadButton("1", modifier = Modifier.weight(1f)) { valueText += "1" }
                    KeypadButton("2", modifier = Modifier.weight(1f)) { valueText += "2" }
                    KeypadButton("3", modifier = Modifier.weight(1f)) { valueText += "3" }
                    KeypadActionButton(
                        label = "C",
                        contentColor = StudioAccentRed,
                        backgroundColor = StudioAccentRed.copy(alpha = 0.15f),
                        modifier = Modifier.weight(1f)
                    ) { valueText = "" }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Row 2: 4 5 6 X (Exit)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    KeypadButton("4", modifier = Modifier.weight(1f)) { valueText += "4" }
                    KeypadButton("5", modifier = Modifier.weight(1f)) { valueText += "5" }
                    KeypadButton("6", modifier = Modifier.weight(1f)) { valueText += "6" }
                    KeypadActionButton(
                        label = "X",
                        contentColor = StudioTextSecondary,
                        backgroundColor = StudioSurfaceElevated,
                        modifier = Modifier.weight(1f)
                    ) { onDismiss() }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Row 3: 7 8 9 . (decimal)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    KeypadButton("7", modifier = Modifier.weight(1f)) { valueText += "7" }
                    KeypadButton("8", modifier = Modifier.weight(1f)) { valueText += "8" }
                    KeypadButton("9", modifier = Modifier.weight(1f)) { valueText += "9" }
                    KeypadButton("•", modifier = Modifier.weight(1f)) {
                        if (!valueText.contains(".")) {
                            valueText = if (valueText.isEmpty() || valueText == "-") "${valueText}0." else "$valueText."
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Row 4: - 0 OK
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    KeypadButton(
                        label = "−",
                        modifier = Modifier.weight(1f)
                    ) {
                        valueText = if (valueText.startsWith("-")) {
                            valueText.removePrefix("-")
                        } else {
                            "-$valueText"
                        }
                    }

                    KeypadButton("0", modifier = Modifier.weight(1f)) { valueText += "0" }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = StudioAccentGreen,
                        modifier = Modifier
                            .weight(2f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                val parsed = valueText.toFloatOrNull() ?: initialValue
                                onConfirm(parsed)
                            }
                            .testTag("btn_keypad_save")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "OK", tint = Color.Black, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("OK", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = StudioSurfaceElevated,
        border = BorderStroke(1.dp, StudioSurfaceBorder),
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .testTag("btn_keypad_$label")
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = StudioTextPrimary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun KeypadActionButton(
    label: String,
    contentColor: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = backgroundColor,
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.3f)),
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .testTag("btn_keypad_action_$label")
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                textAlign = TextAlign.Center
            )
        }
    }
}
