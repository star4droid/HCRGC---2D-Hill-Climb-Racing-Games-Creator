package com.star4droid.hcrgc.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.star4droid.hcrgc.assets.VectorSprites
import com.star4droid.hcrgc.editor.EditorState
import com.star4droid.hcrgc.editor.EditorTool
import com.star4droid.hcrgc.editor.TransformMode
import com.star4droid.hcrgc.model.*
import com.star4droid.hcrgc.ui.theme.*

private val LocalNumberKeypadRequester = compositionLocalOf<(String, Float, (Float) -> Unit) -> Unit> { { _, _, _ -> } }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertySheet(
    state: EditorState,
    onDismiss: () -> Unit,
    onOpenAssetManager: () -> Unit
) {
    if (state.selectedObjectId == null) return

    var activeNumberEdit by remember { mutableStateOf<Pair<String, (Float) -> Unit>?>(null) }
    var activeNumberValue by remember { mutableFloatStateOf(0f) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = StudioSurface
    ) {
        val obj = state.selectedObject
        if (obj == null) {
            LaunchedEffect(Unit) { onDismiss() }
            return@ModalBottomSheet
        }

        CompositionLocalProvider(
            LocalNumberKeypadRequester provides { label, currentVal, onConfirm ->
                activeNumberValue = currentVal
                activeNumberEdit = Pair(label, onConfirm)
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
                    .verticalScroll(rememberScrollState())
                    .testTag("sheet_properties")
            ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = obj.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = StudioTextPrimary
                    )
                    Text(
                        text = "Type: ${obj.type.name} • ID: ${obj.id}",
                        style = MaterialTheme.typography.labelSmall,
                        color = StudioTextSecondary
                    )
                }

                Row {
                    IconButton(
                        onClick = {
                            state.removeObject(obj.id)
                            onDismiss()
                        }
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = StudioAccentRed)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // SECTION 1: TRANSFORM
            PropertySectionHeader(title = "Transform", icon = Icons.Default.Transform)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NumericPropertyField(
                    label = "X",
                    value = obj.x,
                    modifier = Modifier.weight(1f),
                    onValueChange = { state.updateObject(obj.copy(x = it)) }
                )
                NumericPropertyField(
                    label = "Y",
                    value = obj.y,
                    modifier = Modifier.weight(1f),
                    onValueChange = { state.updateObject(obj.copy(y = it)) }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NumericPropertyField(
                    label = "Width",
                    value = obj.width,
                    modifier = Modifier.weight(1f),
                    onValueChange = { state.updateObject(obj.copy(width = it.coerceAtLeast(10f))) }
                )
                NumericPropertyField(
                    label = "Height",
                    value = obj.height,
                    modifier = Modifier.weight(1f),
                    onValueChange = { state.updateObject(obj.copy(height = it.coerceAtLeast(10f))) }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NumericPropertyField(
                    label = "Rotation (°)",
                    value = obj.rotation,
                    modifier = Modifier.weight(1f),
                    onValueChange = { state.updateObject(obj.copy(rotation = it % 360f)) }
                )
                NumericPropertyField(
                    label = "Z-Order",
                    value = obj.zIndex.toFloat(),
                    modifier = Modifier.weight(1f),
                    onValueChange = { state.updateObject(obj.copy(zIndex = it.toInt())) }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // SECTION 2: APPEARANCE & IMAGE
            PropertySectionHeader(title = "Appearance & Sprite", icon = Icons.Default.Palette)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Selected Sprite:", style = MaterialTheme.typography.bodyMedium)
                Button(
                    onClick = onOpenAssetManager,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StudioSurfaceElevated, contentColor = StudioTextPrimary)
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(obj.imageAsset ?: "Choose Image")
                }
            }

            if (obj.imageAsset != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NumericPropertyField(
                        label = "Image Offset X",
                        value = obj.imageOffsetX,
                        modifier = Modifier.weight(1f),
                        onValueChange = { state.updateObject(obj.copy(imageOffsetX = it)) }
                    )
                    NumericPropertyField(
                        label = "Image Offset Y",
                        value = obj.imageOffsetY,
                        modifier = Modifier.weight(1f),
                        onValueChange = { state.updateObject(obj.copy(imageOffsetY = it)) }
                    )
                }
            }

            // SECTION 3: PHYSICS (Hidden if ELEMENT)
            if (obj.type != ObjectType.ELEMENT && obj.type != ObjectType.UI_TEXT &&
                obj.type != ObjectType.UI_BUTTON && obj.type != ObjectType.UI_PROGRESS_BAR
            ) {
                Spacer(modifier = Modifier.height(20.dp))
                PropertySectionHeader(title = "Physics Settings", icon = Icons.Default.Speed)

                // Body Type Selector (Dynamic, Static, Kinematic)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BodyType.values().filter { it != BodyType.NONE }.forEach { bt ->
                        FilterChip(
                            selected = (obj.physics.bodyType == bt),
                            onClick = { state.updateObject(obj.copy(physics = obj.physics.copy(bodyType = bt))) },
                            label = { Text(bt.name) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NumericPropertyField(
                        label = "Friction",
                        value = obj.physics.friction,
                        modifier = Modifier.weight(1f),
                        onValueChange = { state.updateObject(obj.copy(physics = obj.physics.copy(friction = it.coerceIn(0f, 1.5f)))) }
                    )
                    NumericPropertyField(
                        label = "Restitution",
                        value = obj.physics.restitution,
                        modifier = Modifier.weight(1f),
                        onValueChange = { state.updateObject(obj.copy(physics = obj.physics.copy(restitution = it.coerceIn(0f, 1.0f)))) }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NumericPropertyField(
                        label = "Density",
                        value = obj.physics.density,
                        modifier = Modifier.weight(1f),
                        onValueChange = { state.updateObject(obj.copy(physics = obj.physics.copy(density = it.coerceAtLeast(0.1f)))) }
                    )
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .background(StudioSurfaceElevated, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Sensor", style = MaterialTheme.typography.labelLarge)
                        Switch(
                            checked = obj.physics.isSensor,
                            onCheckedChange = { state.updateObject(obj.copy(physics = obj.physics.copy(isSensor = it))) }
                        )
                    }
                }
            }

            // SECTION 4: CONTEXTUAL PROPERTIES DEPENDING ON OBJECT TYPE
            when (obj.type) {
                ObjectType.CAR_BODY -> {
                    val cfg = obj.carBody ?: CarBodyConfig()
                    Spacer(modifier = Modifier.height(20.dp))
                    PropertySectionHeader(title = "Car Body Engine & Stability", icon = Icons.Default.DirectionsCar)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        NumericPropertyField(
                            label = "Engine Power",
                            value = cfg.enginePower,
                            modifier = Modifier.weight(1f),
                            onValueChange = { state.updateObject(obj.copy(carBody = cfg.copy(enginePower = it))) }
                        )
                        NumericPropertyField(
                            label = "Max Speed",
                            value = cfg.maxSpeed,
                            modifier = Modifier.weight(1f),
                            onValueChange = { state.updateObject(obj.copy(carBody = cfg.copy(maxSpeed = it))) }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        NumericPropertyField(
                            label = "Air Control",
                            value = cfg.airControl,
                            modifier = Modifier.weight(1f),
                            onValueChange = { state.updateObject(obj.copy(carBody = cfg.copy(airControl = it))) }
                        )
                        NumericPropertyField(
                            label = "Stability",
                            value = cfg.stability,
                            modifier = Modifier.weight(1f),
                            onValueChange = { state.updateObject(obj.copy(carBody = cfg.copy(stability = it))) }
                        )
                    }
                }

                ObjectType.WHEEL -> {
                    val cfg = obj.wheel ?: WheelConfig()
                    Spacer(modifier = Modifier.height(20.dp))
                    PropertySectionHeader(title = "Wheel & Suspension", icon = Icons.Default.TireRepair)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Role:", style = MaterialTheme.typography.bodyMedium)
                        Row {
                            FilterChip(
                                selected = !cfg.isFrontWheel,
                                onClick = { state.updateObject(obj.copy(wheel = cfg.copy(isFrontWheel = false))) },
                                label = { Text("Rear Wheel") }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            FilterChip(
                                selected = cfg.isFrontWheel,
                                onClick = { state.updateObject(obj.copy(wheel = cfg.copy(isFrontWheel = true))) },
                                label = { Text("Front Wheel") }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        NumericPropertyField(
                            label = "Suspension Freq",
                            value = cfg.suspensionFrequency,
                            modifier = Modifier.weight(1f),
                            onValueChange = { state.updateObject(obj.copy(wheel = cfg.copy(suspensionFrequency = it))) }
                        )
                        NumericPropertyField(
                            label = "Suspension Damping",
                            value = cfg.suspensionDamping,
                            modifier = Modifier.weight(1f),
                            onValueChange = { state.updateObject(obj.copy(wheel = cfg.copy(suspensionDamping = it))) }
                        )
                    }
                }

                ObjectType.CUSTOM_SHAPE -> {
                    val cs = obj.customShape ?: CustomShapeConfig()
                    Spacer(modifier = Modifier.height(20.dp))
                    PropertySectionHeader(title = "Custom Shape / Terrain Points", icon = Icons.Default.Polyline)

                    // Point Edit & Handle Size Controls
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (state.isEditingShapePoints) StudioAccentOrange.copy(alpha = 0.12f) else StudioSurfaceElevated,
                        border = BorderStroke(1.dp, if (state.isEditingShapePoints) StudioAccentOrange else StudioSurfaceBorder),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Canvas Vertex Pen Tool", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(
                                        if (state.isEditingShapePoints) "Editing active (Large visible handlers)" else "Tap to edit points visually on canvas",
                                        fontSize = 11.sp,
                                        color = StudioTextSecondary
                                    )
                                }
                                FilledTonalButton(
                                    onClick = {
                                        state.isEditingShapePoints = !state.isEditingShapePoints
                                        if (state.isEditingShapePoints && state.selectedPointIndex == -1) {
                                            state.selectedPointIndex = 0
                                        }
                                    },
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = if (state.isEditingShapePoints) Color(0xFF10B981) else Color(0xFF0284C7)
                                    ),
                                    shape = CircleShape,
                                    modifier = Modifier.size(40.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Points",
                                        modifier = Modifier.size(20.dp),
                                        tint = Color.White
                                    )
                                }
                            }

                            Spacer(Modifier.height(8.dp))
                            Text("Vertex Handler Size (Canvas):", style = MaterialTheme.typography.labelSmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                listOf(16f to "Normal", 24f to "Large", 34f to "Extra Large").forEach { (sz, lbl) ->
                                    FilterChip(
                                        selected = (state.shapeVertexHandleSize == sz),
                                        onClick = { state.shapeVertexHandleSize = sz },
                                        label = { Text(lbl, fontSize = 11.sp) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Track Transparency Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Transparent Track:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text("Invisible in game (guide lines in editor)", fontSize = 11.sp, color = StudioTextSecondary)
                        }
                        Switch(
                            checked = cs.isTransparent,
                            onCheckedChange = { state.updateObject(obj.copy(customShape = cs.copy(isTransparent = it))) }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Closed polygon switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Closed Polygon:", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = cs.isClosed,
                            onCheckedChange = { state.updateObject(obj.copy(customShape = cs.copy(isClosed = it))) }
                        )
                    }

                    // Track Colors (Surface & Body)
                    if (!cs.isTransparent) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Track Surface Color (Top):", style = MaterialTheme.typography.labelMedium)
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                0xFF16A34A to "Grass",
                                0xFFF59E0B to "Sand",
                                0xFFE2E8F0 to "Snow",
                                0xFF475569 to "Rock",
                                0xFF06B6D4 to "Cyan",
                                0xFFA855F7 to "Purple"
                            ).forEach { (colorVal, _) ->
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(colorVal))
                                        .border(
                                            width = if (cs.surfaceColor == colorVal) 3.dp else 1.dp,
                                            color = if (cs.surfaceColor == colorVal) Color.White else StudioSurfaceBorder,
                                            shape = CircleShape
                                        )
                                        .clickable { state.updateObject(obj.copy(customShape = cs.copy(surfaceColor = colorVal))) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Underground Body Color (Dirt/Sub-surface):", style = MaterialTheme.typography.labelMedium)
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                0xFF78350F to "Dirt",
                                0xFF1E293B to "Dark Slate",
                                0xFF334155 to "Stone",
                                0xFF831843 to "Volcanic",
                                0xFF1E1B4B to "Abyss",
                                0xFF365314 to "Moss"
                            ).forEach { (colorVal, _) ->
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(colorVal))
                                        .border(
                                            width = if (cs.bodyColor == colorVal) 3.dp else 1.dp,
                                            color = if (cs.bodyColor == colorVal) Color.White else StudioSurfaceBorder,
                                            shape = CircleShape
                                        )
                                        .clickable { state.updateObject(obj.copy(customShape = cs.copy(bodyColor = colorVal))) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Control Points (${cs.points.size}):", style = MaterialTheme.typography.labelLarge)
                    cs.points.forEachIndexed { idx, pt ->
                        val isSelectedPoint = (idx == state.selectedPointIndex)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .background(
                                    if (isSelectedPoint) StudioAccentOrange.copy(alpha = 0.15f) else StudioSurfaceElevated,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("#${idx + 1}", fontWeight = FontWeight.Bold, modifier = Modifier.width(30.dp))
                            NumericPropertyField(
                                label = "X",
                                value = pt.x,
                                modifier = Modifier.weight(1f),
                                onValueChange = {
                                    val pts = cs.points.toMutableList()
                                    pts[idx] = Point2D(it, pt.y)
                                    state.updateObject(obj.copy(customShape = cs.copy(points = pts)))
                                }
                            )
                            NumericPropertyField(
                                label = "Y",
                                value = pt.y,
                                modifier = Modifier.weight(1f),
                                onValueChange = {
                                    val pts = cs.points.toMutableList()
                                    pts[idx] = Point2D(pt.x, it)
                                    state.updateObject(obj.copy(customShape = cs.copy(points = pts)))
                                }
                            )
                            IconButton(
                                onClick = {
                                    if (cs.points.size > 2) {
                                        val pts = cs.points.toMutableList()
                                        pts.removeAt(idx)
                                        state.updateObject(obj.copy(customShape = cs.copy(points = pts)))
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Point", tint = StudioAccentRed, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    Button(
                        onClick = {
                            val last = cs.points.lastOrNull() ?: Point2D(0f, 0f)
                            val pts = cs.points + Point2D(last.x + 80f, last.y)
                            state.updateObject(obj.copy(customShape = cs.copy(points = pts)))
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Point")
                    }
                }

                ObjectType.TILEMAP -> {
                    val tm = obj.tileMap ?: TileMapConfig()
                    Spacer(modifier = Modifier.height(20.dp))
                    PropertySectionHeader(title = "TileMap (Non-Resizable)", icon = Icons.Default.GridOn)

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = StudioSurfaceElevated,
                        border = BorderStroke(1.dp, StudioSurfaceBorder),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Grid Size: ${tm.cols} columns × ${tm.rows} rows", fontWeight = FontWeight.Bold, color = StudioTextPrimary)
                            Text("Tile Size: ${tm.tileWidth.toInt()}×${tm.tileHeight.toInt()} px • Blocks Placed: ${tm.tiles.size}", fontSize = 12.sp, color = StudioTextSecondary)
                            Text("Stamp tiles by selecting Tile Tool on the top bar.", fontSize = 11.sp, color = StudioAccentBlue)
                        }
                    }
                }

                ObjectType.COIN -> {
                    val cfg = obj.coin ?: CoinConfig()
                    Spacer(modifier = Modifier.height(20.dp))
                    PropertySectionHeader(title = "Coin Value", icon = Icons.Default.MonetizationOn)
                    NumericPropertyField(
                        label = "Score Value",
                        value = cfg.value.toFloat(),
                        modifier = Modifier.fillMaxWidth(),
                        onValueChange = { state.updateObject(obj.copy(coin = cfg.copy(value = it.toInt()))) }
                    )
                }

                ObjectType.LIGHT -> {
                    val cfg = obj.light ?: LightConfig()
                    Spacer(modifier = Modifier.height(20.dp))
                    PropertySectionHeader(title = "Box2D-Lights Properties", icon = Icons.Default.Lightbulb)

                    // Light Type Selector (Point, Cone, Directional, Chain)
                    Text("Light Type (box2dlights):", style = MaterialTheme.typography.labelMedium)
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        LightType.values().forEach { lt ->
                            FilterChip(
                                selected = (cfg.lightType == lt),
                                onClick = { state.updateObject(obj.copy(light = cfg.copy(lightType = lt))) },
                                label = { Text(lt.name, fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Distance & Intensity
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        NumericPropertyField(
                            label = "Distance (px)",
                            value = cfg.distance,
                            modifier = Modifier.weight(1f),
                            onValueChange = { state.updateObject(obj.copy(light = cfg.copy(distance = it.coerceAtLeast(20f)))) }
                        )
                        NumericPropertyField(
                            label = "Intensity",
                            value = cfg.intensity,
                            modifier = Modifier.weight(1f),
                            onValueChange = { state.updateObject(obj.copy(light = cfg.copy(intensity = it.coerceIn(0.05f, 2.5f)))) }
                        )
                    }

                    // Cone Angle & Direction (for Cone / Directional)
                    if (cfg.lightType == LightType.CONE || cfg.lightType == LightType.DIRECTIONAL) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            NumericPropertyField(
                                label = "Direction (°)",
                                value = cfg.direction,
                                modifier = Modifier.weight(1f),
                                onValueChange = { state.updateObject(obj.copy(light = cfg.copy(direction = it % 360f))) }
                            )
                            if (cfg.lightType == LightType.CONE) {
                                NumericPropertyField(
                                    label = "Cone Arc (°)",
                                    value = cfg.coneAngle,
                                    modifier = Modifier.weight(1f),
                                    onValueChange = { state.updateObject(obj.copy(light = cfg.copy(coneAngle = it.coerceIn(5f, 180f)))) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Rays & Softness
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        NumericPropertyField(
                            label = "Rays Count",
                            value = cfg.rays.toFloat(),
                            modifier = Modifier.weight(1f),
                            onValueChange = { state.updateObject(obj.copy(light = cfg.copy(rays = it.toInt().coerceIn(16, 512)))) }
                        )
                        NumericPropertyField(
                            label = "Softness",
                            value = cfg.softnessLength,
                            modifier = Modifier.weight(1f),
                            onValueChange = { state.updateObject(obj.copy(light = cfg.copy(softnessLength = it.coerceIn(0f, 60f)))) }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Light Color
                    Text("Light Color / Temperature:", style = MaterialTheme.typography.labelMedium)
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            0xFFFFEA75 to "Warm Yellow",
                            0xFFFFFFFF to "Bright White",
                            0xFF67E8F9 to "Neon Cyan",
                            0xFFF43F5E to "Red Laser",
                            0xFF10B981 to "Emerald",
                            0xFFA855F7 to "UV Violet"
                        ).forEach { (colVal, _) ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(colVal))
                                    .border(
                                        width = if (cfg.color == colVal) 3.dp else 1.dp,
                                        color = if (cfg.color == colVal) StudioAccentOrange else StudioSurfaceBorder,
                                        shape = CircleShape
                                    )
                                    .clickable { state.updateObject(obj.copy(light = cfg.copy(color = colVal))) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // X-Ray & Attach To Car (Headlights)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("X-Ray Mode (Penetrate):", style = MaterialTheme.typography.bodyMedium)
                            Text("Pass through obstacles without shadow", fontSize = 11.sp, color = StudioTextSecondary)
                        }
                        Switch(
                            checked = cfg.isXray,
                            onCheckedChange = { state.updateObject(obj.copy(light = cfg.copy(isXray = it))) }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Attach to Vehicle (Headlights):", style = MaterialTheme.typography.bodyMedium)
                            Text("Rotates and moves with vehicle", fontSize = 11.sp, color = StudioTextSecondary)
                        }
                        Switch(
                            checked = cfg.attachToParent,
                            onCheckedChange = { state.updateObject(obj.copy(light = cfg.copy(attachToParent = it))) }
                        )
                    }
                }

                ObjectType.UI_TEXT -> {
                    val cfg = obj.uiText ?: UITextConfig()
                    Spacer(modifier = Modifier.height(20.dp))
                    PropertySectionHeader(title = "UI Text & Coin Counter", icon = Icons.Default.TextFields)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Is Coin Counter:", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = cfg.isCoinCounter,
                            onCheckedChange = { state.updateObject(obj.copy(uiText = cfg.copy(isCoinCounter = it))) }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = if (cfg.isCoinCounter) cfg.prefix else cfg.text,
                        onValueChange = {
                            if (cfg.isCoinCounter) {
                                state.updateObject(obj.copy(uiText = cfg.copy(prefix = it)))
                            } else {
                                state.updateObject(obj.copy(uiText = cfg.copy(text = it)))
                            }
                        },
                        label = { Text(if (cfg.isCoinCounter) "Prefix" else "Content") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                ObjectType.UI_BUTTON -> {
                    val cfg = obj.uiButton ?: UIButtonConfig()
                    Spacer(modifier = Modifier.height(20.dp))
                    PropertySectionHeader(title = "UI Button Role", icon = Icons.Default.SmartButton)

                    OutlinedTextField(
                        value = cfg.label,
                        onValueChange = { state.updateObject(obj.copy(uiButton = cfg.copy(label = it))) },
                        label = { Text("Button Label") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Role:", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ButtonRole.values().forEach { role ->
                            FilterChip(
                                selected = (cfg.role == role),
                                onClick = { state.updateObject(obj.copy(uiButton = cfg.copy(role = role))) },
                                label = { Text(role.name.take(5), fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                else -> {}
            }
        }
        }
    }

    if (activeNumberEdit != null) {
        val (fieldLabel, onConfirm) = activeNumberEdit!!
        NumberInputDialog(
            title = "Edit $fieldLabel",
            initialValue = activeNumberValue,
            onDismiss = { activeNumberEdit = null },
            onConfirm = { newVal ->
                onConfirm(newVal)
                activeNumberEdit = null
            }
        )
    }
}

@Composable
private fun PropertySectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = StudioAccentBlue, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = StudioTextPrimary)
    }
}

@Composable
private fun NumericPropertyField(
    label: String,
    value: Float,
    modifier: Modifier = Modifier,
    onValueChange: (Float) -> Unit
) {
    val requester = LocalNumberKeypadRequester.current
    val displayValue = if (value % 1.0f == 0.0f) value.toInt().toString() else String.format(java.util.Locale.US, "%.1f", value)

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = StudioSurfaceElevated,
        border = BorderStroke(1.dp, StudioSurfaceBorder),
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable {
                requester(label, value, onValueChange)
            }
            .testTag("field_$label")
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(
                text = label,
                fontSize = 11.sp,
                color = StudioTextSecondary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = displayValue,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = StudioTextPrimary
            )
        }
    }
}
