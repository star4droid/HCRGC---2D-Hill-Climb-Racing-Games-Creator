package com.star4droid.hcrgc.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.star4droid.hcrgc.assets.VectorSprites
import com.star4droid.hcrgc.model.*
import com.star4droid.hcrgc.ui.theme.*
import kotlin.math.*

private enum class DragMode {
    NONE,
    PAN_WORLD,
    MOVE_OBJECT,
    RESIZE_OBJECT,
    ROTATE_OBJECT,
    MOVE_POINT
}

@Composable
fun EditorCanvas(
    state: EditorState,
    modifier: Modifier = Modifier
) {
    var dragMode by remember { mutableStateOf(DragMode.NONE) }
    var resizeHandleIndex by remember { mutableIntStateOf(-1) }
    var initialObjX by remember { mutableFloatStateOf(0f) }
    var initialObjY by remember { mutableFloatStateOf(0f) }
    var initialObjW by remember { mutableFloatStateOf(0f) }
    var initialObjH by remember { mutableFloatStateOf(0f) }
    var initialObjRot by remember { mutableFloatStateOf(0f) }

    var canvasWidth by remember { mutableFloatStateOf(1000f) }
    var canvasHeight by remember { mutableFloatStateOf(600f) }

    // Pulsing animation for selected point
    val infiniteTransition = rememberInfiniteTransition(label = "pointPulse")
    val pointPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(state.project.backgroundColor))
            .pointerInput(
                state.selectedObjectId,
                state.transformMode,
                state.isEditingShapePoints,
                state.selectedPointIndex,
                state.activeTool,
                state.zoomFactor,
                state.shapeVertexHandleSize,
                state.level.objects
            ) {
                awaitEachGesture {
                    val firstDown = awaitFirstDown(requireUnconsumed = false)
                    val downPos = firstDown.position
                    var isMultiTouch = false
                    var dragStarted = false
                    var activeDragMode = DragMode.NONE
                    var activeResizeHandle = -1
                    var prevCentroid = downPos
                    var prevDistance = 0f

                    val (startWx, startWy) = state.screenToWorld(downPos.x, downPos.y)
                    val selObj = state.selectedObject

                    // Check if a point was touched in edit mode
                    var touchedPointIndex = -1
                    if (state.isEditingShapePoints && selObj?.type == ObjectType.CUSTOM_SHAPE && selObj.customShape != null) {
                        val pts = selObj.customShape.points
                        val touchRadius = (state.shapeVertexHandleSize * 1.8f).coerceAtLeast(32f) / state.zoom
                        for (i in pts.indices) {
                            val p = pts[i]
                            val pwx = selObj.x + p.x
                            val pwy = selObj.y + p.y
                            if (hypot(startWx - pwx, startWy - pwy) < touchRadius) {
                                touchedPointIndex = i
                                break
                            }
                        }
                    }

                    // Check if gizmo was touched in GRID mode
                    var touchedGizmoRotate = false
                    var touchedGizmoHandle = -1
                    var touchedInsideObject = false

                    if (selObj != null && selObj.type != ObjectType.CUSTOM_SHAPE && !state.isEditingShapePoints) {
                        val (sx, sy) = state.worldToScreen(selObj.x, selObj.y)
                        val hw = (selObj.width * state.zoom) / 2f
                        val hh = (selObj.height * state.zoom) / 2f
                        val rotHandlePos = Offset(sx, sy - hh - 28f)

                        if ((downPos - rotHandlePos).getDistance() < 28f) {
                            touchedGizmoRotate = true
                        } else {
                            val handles = listOf(
                                Offset(sx - hw, sy - hh),
                                Offset(sx + hw, sy - hh),
                                Offset(sx + hw, sy + hh),
                                Offset(sx - hw, sy + hh)
                            )
                            for (i in handles.indices) {
                                if ((downPos - handles[i]).getDistance() < 26f) {
                                    touchedGizmoHandle = i
                                    break
                                }
                            }
                        }
                        touchedInsideObject = (startWx in (selObj.x - selObj.width / 2f)..(selObj.x + selObj.width / 2f)) &&
                                              (startWy in (selObj.y - selObj.height / 2f)..(selObj.y + selObj.height / 2f))
                    }

                    var lastPointerPos = downPos

                    while (true) {
                        val event = awaitPointerEvent()
                        val activePointers = event.changes.filter { it.pressed }

                        if (activePointers.isEmpty()) {
                            break
                        }

                        if (activePointers.size >= 2) {
                            // MULTI-TOUCH PINCH-TO-ZOOM & PAN (Always Works, Never touches objects!)
                            isMultiTouch = true
                            val p1 = activePointers[0].position
                            val p2 = activePointers[1].position
                            val centroid = (p1 + p2) / 2f
                            val distance = (p1 - p2).getDistance()

                            if (prevDistance > 0f) {
                                val panDelta = centroid - prevCentroid
                                val zoomRatio = distance / prevDistance
                                val effZoomChange = 1f + (zoomRatio - 1f) * state.zoomFactor
                                state.zoom = (state.zoom * effZoomChange).coerceIn(0.005f, 250f)
                                state.panX += panDelta.x
                                state.panY += panDelta.y
                            }
                            prevCentroid = centroid
                            prevDistance = distance
                            activePointers.forEach { it.consume() }
                        } else if (activePointers.size == 1 && !isMultiTouch) {
                            val pointer = activePointers[0]
                            val currPos = pointer.position
                            val distFromDown = (currPos - downPos).getDistance()

                            if (!dragStarted && distFromDown > viewConfiguration.touchSlop) {
                                dragStarted = true
                                lastPointerPos = currPos
                                // Initialize active drag mode
                                if (state.isEditingShapePoints && selObj?.type == ObjectType.CUSTOM_SHAPE && selObj.customShape != null) {
                                    if (touchedPointIndex != -1) {
                                        state.selectedPointIndex = touchedPointIndex
                                        activeDragMode = DragMode.MOVE_POINT
                                        state.pushUndoState()
                                    } else if (state.selectedPointIndex in selObj.customShape.points.indices) {
                                        val selP = selObj.customShape.points[state.selectedPointIndex]
                                        val (screenPx, screenPy) = state.worldToScreen(selObj.x + selP.x, selObj.y + selP.y)
                                        if (hypot(downPos.x - screenPx, downPos.y - screenPy) < 50f) {
                                            activeDragMode = DragMode.MOVE_POINT
                                            state.pushUndoState()
                                        } else {
                                            activeDragMode = DragMode.PAN_WORLD
                                        }
                                    } else {
                                        activeDragMode = DragMode.PAN_WORLD
                                    }
                                } else if (selObj != null) {
                                    when (state.transformMode) {
                                        TransformMode.GRID -> {
                                            if (touchedGizmoRotate) {
                                                activeDragMode = DragMode.ROTATE_OBJECT
                                                state.pushUndoState()
                                            } else if (touchedGizmoHandle != -1) {
                                                activeDragMode = DragMode.RESIZE_OBJECT
                                                activeResizeHandle = touchedGizmoHandle
                                                state.pushUndoState()
                                            } else if (touchedInsideObject) {
                                                activeDragMode = DragMode.MOVE_OBJECT
                                                state.pushUndoState()
                                            } else {
                                                activeDragMode = DragMode.PAN_WORLD
                                            }
                                        }
                                        TransformMode.MOVE -> {
                                            // MOVE mode explicitly drags the selected element
                                            activeDragMode = DragMode.MOVE_OBJECT
                                            state.pushUndoState()
                                        }
                                        TransformMode.ROTATE -> {
                                            activeDragMode = DragMode.ROTATE_OBJECT
                                            state.pushUndoState()
                                        }
                                        TransformMode.SCALE -> {
                                            activeDragMode = DragMode.RESIZE_OBJECT
                                            activeResizeHandle = 2
                                            state.pushUndoState()
                                        }
                                    }
                                } else {
                                    activeDragMode = DragMode.PAN_WORLD
                                }
                            }

                            if (dragStarted) {
                                val dx = (currPos.x - lastPointerPos.x) / state.zoom
                                val dy = (currPos.y - lastPointerPos.y) / state.zoom
                                val screenDx = currPos.x - lastPointerPos.x
                                val screenDy = currPos.y - lastPointerPos.y
                                lastPointerPos = currPos

                                when (activeDragMode) {
                                    DragMode.MOVE_POINT -> {
                                        val curObj = state.selectedObject
                                        if (curObj?.type == ObjectType.CUSTOM_SHAPE && curObj.customShape != null) {
                                            val pts = curObj.customShape.points.toMutableList()
                                            val idx = state.selectedPointIndex
                                            if (idx in pts.indices) {
                                                val oldP = pts[idx]
                                                val newRelX = oldP.x + dx
                                                val newRelY = oldP.y + dy
                                                pts[idx] = Point2D(newRelX, newRelY)
                                                state.updateObject(curObj.copy(customShape = curObj.customShape.copy(points = pts)))
                                            }
                                        }
                                    }
                                    DragMode.MOVE_OBJECT -> {
                                        val curObj = state.selectedObject
                                        if (curObj != null) {
                                            val newX = curObj.x + dx
                                            val newY = curObj.y + dy
                                            state.updateObject(curObj.copy(x = newX, y = newY))
                                        }
                                    }
                                    DragMode.ROTATE_OBJECT -> {
                                        val curObj = state.selectedObject
                                        if (curObj != null) {
                                            val dRot = screenDx * 0.45f
                                            var newRot = (curObj.rotation + dRot) % 360f
                                            if (newRot < 0) newRot += 360f
                                            state.updateObject(curObj.copy(rotation = newRot))
                                        }
                                    }
                                    DragMode.RESIZE_OBJECT -> {
                                        val curObj = state.selectedObject
                                        if (curObj != null && curObj.type != ObjectType.CUSTOM_SHAPE) {
                                            val dw = dx * 2f
                                            val dh = dy * 2f
                                            val newW = (curObj.width + dw).coerceAtLeast(16f)
                                            val newH = (curObj.height + dh).coerceAtLeast(16f)
                                            state.updateObject(curObj.copy(width = newW, height = newH))
                                        }
                                    }
                                    DragMode.PAN_WORLD -> {
                                        state.panX += screenDx
                                        state.panY += screenDy
                                    }
                                    else -> {}
                                }
                                pointer.consume()
                            }
                        }
                    }

                    // Single-finger tap resolution
                    if (!isMultiTouch && !dragStarted) {
                        if (state.isEditingShapePoints && selObj?.type == ObjectType.CUSTOM_SHAPE && selObj.customShape != null) {
                            if (touchedPointIndex != -1) {
                                if (state.selectedPointIndex == touchedPointIndex) {
                                    // User clicked the same point -> deselect it so user can move freely!
                                    state.selectedPointIndex = -1
                                } else {
                                    state.selectedPointIndex = touchedPointIndex
                                }
                            } else {
                                // Tapped empty canvas in edit mode -> deselect point
                                state.selectedPointIndex = -1
                            }
                        } else {
                            // TileMap stamping
                            if (selObj?.type == ObjectType.TILEMAP && state.activeTool == EditorTool.STAMP_TILES) {
                                val tm = selObj.tileMap
                                if (tm != null) {
                                    val localX = startWx - selObj.x
                                    val localY = startWy - selObj.y
                                    val col = (localX / tm.tileWidth).toInt()
                                    val row = (localY / tm.tileHeight).toInt()
                                    if (col in 0 until tm.cols && row in 0 until tm.rows) {
                                        val key = "${col}_${row}"
                                        val newTiles = tm.tiles.toMutableMap()
                                        if (state.isErasingTile) {
                                            newTiles.remove(key)
                                        } else {
                                            state.selectedTileAsset?.let { newTiles[key] = it }
                                        }
                                        state.pushUndoState()
                                        state.updateObject(selObj.copy(tileMap = tm.copy(tiles = newTiles)))
                                    }
                                }
                            } else {
                                // Hit-test objects
                                val hit = state.level.objects
                                    .sortedByDescending { it.zIndex }
                                    .find { obj ->
                                        if (!obj.visible) return@find false
                                        if (!state.showUiElementsLayer && (obj.type == ObjectType.UI_BUTTON || obj.type == ObjectType.UI_TEXT || obj.type == ObjectType.UI_PROGRESS_BAR)) {
                                            return@find false
                                        }
                                        if (obj.type == ObjectType.CUSTOM_SHAPE && obj.customShape != null) {
                                            val pts = obj.customShape.points
                                            pts.any { p -> hypot(startWx - (obj.x + p.x), startWy - (obj.y + p.y)) < 40f / state.zoom }
                                        } else {
                                            val halfW = obj.width / 2f
                                            val halfH = obj.height / 2f
                                            startWx in (obj.x - halfW)..(obj.x + halfW) &&
                                            startWy in (obj.y - halfH)..(obj.y + halfH)
                                        }
                                    }
                                state.selectedObjectId = hit?.id
                                if (hit?.type != ObjectType.CUSTOM_SHAPE) {
                                    state.isEditingShapePoints = false
                                    state.selectedPointIndex = -1
                                }
                            }
                        }
                    }
                }
            }
    ) {
        // Main Rendering Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            canvasWidth = size.width
            canvasHeight = size.height

            // 1. Grid
            if (state.showGrid) {
                drawEditorGrid(state)
            }

            // 2. Camera Viewport Boundary
            drawCameraViewport(state)

            // 3. Render Game Objects sorted by Z-Index
            val sortedObjects = state.level.objects.sortedBy { it.zIndex }
            for (obj in sortedObjects) {
                if (!obj.visible) continue
                val (sx, sy) = state.worldToScreen(obj.x, obj.y)

                when (obj.type) {
                    ObjectType.CUSTOM_SHAPE -> drawCustomShapeObject(state, obj, sx, sy)
                    ObjectType.TILEMAP -> drawTileMapObject(state, obj, sx, sy)
                    else -> drawStandardObject(state, obj, sx, sy)
                }
            }

            // 4. Selection Gizmos & Visual Point Markers
            val selected = state.selectedObject
            if (selected != null) {
                drawSelectionGizmos(state, selected, pointPulseAlpha)
            }
        }

        // FLOATING PEN OVERLAY BUTTON (for CustomShape / ChainShape)
        val selectedObj = state.selectedObject
        if (selectedObj?.type == ObjectType.CUSTOM_SHAPE && selectedObj.customShape != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = if (state.isEditingShapePoints) StudioAccentOrange else StudioSurfaceElevated,
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                    border = BorderStroke(2.dp, if (state.isEditingShapePoints) Color.White else StudioAccentOrange),
                    modifier = Modifier
                        .testTag("shape_pen_edit_button")
                        .clickable {
                            state.isEditingShapePoints = !state.isEditingShapePoints
                            if (state.isEditingShapePoints && state.selectedPointIndex == -1) {
                                state.selectedPointIndex = 0
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (state.isEditingShapePoints) Icons.Default.Close else Icons.Default.Edit,
                            contentDescription = if (state.isEditingShapePoints) "Exit Shape Edit" else "Edit Shape Points",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = if (state.isEditingShapePoints) "Exit Edit (X)" else "Pen Tool (Edit Points)",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // HORIZONTALLY SCROLLABLE POINT NUMBER GRID OVERLAY (shown when editing shape points)
        AnimatedVisibility(
            visible = state.isEditingShapePoints && selectedObj?.type == ObjectType.CUSTOM_SHAPE,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp, start = 12.dp, end = 12.dp)
        ) {
            val cs = selectedObj?.customShape
            if (cs != null) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = StudioSurfaceElevated.copy(alpha = 0.95f),
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                    border = BorderStroke(1.dp, StudioSurfaceBorder),
                    modifier = Modifier.fillMaxWidth().testTag("shape_points_bar")
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Column {
                                Text(
                                    text = "Chain Points (${cs.points.size})",
                                    color = StudioTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Tap point to select • Tap again to deselect & pan",
                                    color = StudioTextSecondary,
                                    fontSize = 10.sp
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                // Handle size toggles
                                Row(
                                    modifier = Modifier
                                        .background(StudioSurface, RoundedCornerShape(8.dp))
                                        .padding(2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    listOf(18f to "S", 28f to "M", 38f to "L").forEach { (sz, label) ->
                                        val isCurrent = state.shapeVertexHandleSize == sz
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    if (isCurrent) StudioAccentOrange else Color.Transparent,
                                                    RoundedCornerShape(6.dp)
                                                )
                                                .clickable { state.shapeVertexHandleSize = sz }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = label,
                                                color = if (isCurrent) Color.White else StudioTextSecondary,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                // Add Point Button
                                FilledTonalButton(
                                    onClick = {
                                        val pts = cs.points.toMutableList()
                                        val lastP = pts.lastOrNull() ?: Point2D(0f, 0f)
                                        pts.add(Point2D(lastP.x + 120f, lastP.y))
                                        state.pushUndoState()
                                        state.updateObject(selectedObj.copy(customShape = cs.copy(points = pts)))
                                        state.selectedPointIndex = pts.lastIndex
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Add", fontSize = 11.sp)
                                }

                                // Delete Point Button
                                if (cs.points.size > 2 && state.selectedPointIndex in cs.points.indices) {
                                    Button(
                                        onClick = {
                                            val pts = cs.points.toMutableList()
                                            pts.removeAt(state.selectedPointIndex)
                                            state.pushUndoState()
                                            state.updateObject(selectedObj.copy(customShape = cs.copy(points = pts)))
                                            state.selectedPointIndex = (state.selectedPointIndex - 1).coerceAtLeast(0)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(4.dp))

                        // Scrollable row of point numbers (1), (2), (3)...
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(vertical = 4.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            cs.points.forEachIndexed { index, point ->
                                val isSelected = (index == state.selectedPointIndex)
                                Surface(
                                    shape = CircleShape,
                                    color = if (isSelected) StudioAccentOrange else StudioSurface,
                                    border = BorderStroke(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) Color.White else StudioSurfaceBorder
                                    ),
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clickable {
                                            if (state.selectedPointIndex == index) {
                                                // Deselect on second click so user can move freely
                                                state.selectedPointIndex = -1
                                            } else {
                                                state.selectedPointIndex = index
                                                state.panToWorldPoint(
                                                    selectedObj.x + point.x,
                                                    selectedObj.y + point.y,
                                                    canvasWidth,
                                                    canvasHeight
                                                )
                                            }
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "(${index + 1})",
                                            color = if (isSelected) Color.White else StudioTextPrimary,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 11.sp
                                        )
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

private fun DrawScope.drawEditorGrid(state: EditorState) {
    val step = state.gridSize * state.zoom
    if (step < 6f) return

    val startX = (state.panX % step + step) % step
    val startY = (state.panY % step + step) % step

    var x = startX
    while (x < size.width) {
        drawLine(CanvasGridLine, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
        x += step
    }

    var y = startY
    while (y < size.height) {
        drawLine(CanvasGridLine, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        y += step
    }

    // World axes
    val (origX, origY) = state.worldToScreen(0f, 0f)
    if (origX in 0f..size.width) {
        drawLine(Color(0xFF3B82F6).copy(alpha = 0.55f), Offset(origX, 0f), Offset(origX, size.height), strokeWidth = 1.5f)
    }
    if (origY in 0f..size.height) {
        drawLine(Color(0xFFEF4444).copy(alpha = 0.55f), Offset(0f, origY), Offset(size.width, origY), strokeWidth = 1.5f)
    }
}

private fun DrawScope.drawCameraViewport(state: EditorState) {
    val gw = state.project.gameWidth.toFloat()
    val gh = state.project.gameHeight.toFloat()
    val (sx, sy) = state.worldToScreen(0f, 0f)
    val sw = gw * state.zoom
    val sh = gh * state.zoom

    drawRect(
        color = CanvasCameraBorder,
        topLeft = Offset(sx, sy),
        size = Size(sw, sh),
        style = Stroke(width = 2.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 10f), 0f))
    )
}

private fun DrawScope.drawStandardObject(state: EditorState, obj: GameObject, sx: Float, sy: Float) {
    val sw = obj.width * state.zoom
    val sh = obj.height * state.zoom

    rotate(obj.rotation, pivot = Offset(sx, sy)) {
        if (obj.imageAsset != null) {
            translate(sx - sw / 2f, sy - sh / 2f) {
                VectorSprites.drawAsset(this@drawStandardObject, obj.imageAsset, sw, sh)
            }
        } else {
            when (obj.type) {
                ObjectType.CIRCLE -> {
                    val radius = min(sw, sh) / 2f
                    drawCircle(Color(obj.tintColor), radius = radius, center = Offset(sx, sy))
                    drawCircle(Color.White.copy(alpha = 0.4f), radius = radius, center = Offset(sx, sy), style = Stroke(2f))
                }
                ObjectType.CAR_BODY -> {
                    translate(sx - sw / 2f, sy - sh / 2f) {
                        VectorSprites.drawAsset(this@drawStandardObject, VectorSprites.ASSET_CAR_BUGGY, sw, sh)
                    }
                }
                ObjectType.WHEEL -> {
                    translate(sx - sw / 2f, sy - sh / 2f) {
                        VectorSprites.drawAsset(this@drawStandardObject, VectorSprites.ASSET_WHEEL_RUGGED, sw, sh)
                    }
                }
                ObjectType.COIN -> {
                    translate(sx - sw / 2f, sy - sh / 2f) {
                        VectorSprites.drawAsset(this@drawStandardObject, VectorSprites.ASSET_COIN_GOLD, sw, sh)
                    }
                }
                ObjectType.FINISH_FLAG -> {
                    translate(sx - sw / 2f, sy - sh / 2f) {
                        VectorSprites.drawAsset(this@drawStandardObject, VectorSprites.ASSET_FINISH_FLAG, sw, sh)
                    }
                }
                ObjectType.LIGHT -> {
                    val lp = obj.light ?: LightConfig()
                    val lightDist = (lp.distance * state.zoom).coerceAtLeast(40f)
                    val lightAlpha = (lp.intensity * 0.45f).coerceIn(0f, 1f)
                    val lightCol = Color(lp.color).copy(alpha = lightAlpha)
                    when (lp.lightType) {
                        LightType.POINT, LightType.CONE -> {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    listOf(lightCol, Color.Transparent),
                                    center = Offset(sx, sy),
                                    radius = lightDist
                                ),
                                radius = lightDist,
                                center = Offset(sx, sy)
                            )
                        }
                        LightType.DIRECTIONAL, LightType.CHAIN -> {
                            val startY = sy - (lightDist * 0.5f)
                            val endY = sy + (lightDist * 0.5f)
                            drawRect(
                                brush = Brush.verticalGradient(
                                    listOf(lightCol, Color.Transparent),
                                    startY = startY,
                                    endY = endY
                                ),
                                topLeft = Offset(sx - lightDist, startY),
                                size = Size(lightDist * 2f, lightDist)
                            )
                        }
                    }
                    // Light icon center
                    drawCircle(Color(lp.color), radius = 10f, center = Offset(sx, sy))
                    drawCircle(Color.White, radius = 5f, center = Offset(sx, sy))
                }
                ObjectType.UI_BUTTON -> {
                    // Visual match with game runtime button
                    drawRoundRect(
                        color = Color(obj.tintColor),
                        topLeft = Offset(sx - sw / 2f, sy - sh / 2f),
                        size = Size(sw, sh),
                        cornerRadius = CornerRadius(10f, 10f)
                    )
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.85f),
                        topLeft = Offset(sx - sw / 2f, sy - sh / 2f),
                        size = Size(sw, sh),
                        cornerRadius = CornerRadius(10f, 10f),
                        style = Stroke(2f)
                    )
                }
                ObjectType.UI_PROGRESS_BAR -> {
                    // Visual match with game runtime progress bar
                    drawRoundRect(
                        color = Color(0xFF1E293B),
                        topLeft = Offset(sx - sw / 2f, sy - sh / 2f),
                        size = Size(sw, sh),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                    drawRoundRect(
                        color = Color(obj.tintColor),
                        topLeft = Offset(sx - sw / 2f, sy - sh / 2f),
                        size = Size(sw * 0.65f, sh),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.5f),
                        topLeft = Offset(sx - sw / 2f, sy - sh / 2f),
                        size = Size(sw, sh),
                        cornerRadius = CornerRadius(6f, 6f),
                        style = Stroke(1.5f)
                    )
                }
                ObjectType.UI_TEXT -> {
                    // Visual match with game runtime UI text
                    drawRoundRect(
                        color = Color(0xCC0F172A),
                        topLeft = Offset(sx - sw / 2f, sy - sh / 2f),
                        size = Size(sw, sh),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                    drawRoundRect(
                        color = Color(obj.tintColor),
                        topLeft = Offset(sx - sw / 2f + 4f, sy - 2f),
                        size = Size((sw - 8f).coerceAtLeast(4f), 4f),
                        cornerRadius = CornerRadius(2f, 2f)
                    )
                    drawRoundRect(
                        color = StudioSurfaceBorder,
                        topLeft = Offset(sx - sw / 2f, sy - sh / 2f),
                        size = Size(sw, sh),
                        cornerRadius = CornerRadius(6f, 6f),
                        style = Stroke(1.5f)
                    )
                }
                else -> {
                    drawRoundRect(
                        color = Color(obj.tintColor),
                        topLeft = Offset(sx - sw / 2f, sy - sh / 2f),
                        size = Size(sw, sh),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.35f),
                        topLeft = Offset(sx - sw / 2f, sy - sh / 2f),
                        size = Size(sw, sh),
                        cornerRadius = CornerRadius(6f, 6f),
                        style = Stroke(1.5f)
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawCustomShapeObject(state: EditorState, obj: GameObject, sx: Float, sy: Float) {
    val cs = obj.customShape ?: return
    val points = cs.points
    if (points.size < 2) return

    val path = Path()
    val firstScreenX = sx + (points[0].x * state.zoom)
    val firstScreenY = sy + (points[0].y * state.zoom)
    path.moveTo(firstScreenX, firstScreenY)

    for (i in 1 until points.size) {
        val px = sx + (points[i].x * state.zoom)
        val py = sy + (points[i].y * state.zoom)
        path.lineTo(px, py)
    }

    if (cs.isTransparent) {
        // Transparent collision-only track: shown as lines in the editor only
        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 8f), 0f)
        drawPath(
            path,
            color = Color(0xFF38BDF8),
            style = Stroke(width = 3.5f * state.zoom, cap = StrokeCap.Round, pathEffect = dashEffect)
        )
        drawPath(
            path,
            color = Color(0xFF0284C7).copy(alpha = 0.35f),
            style = Stroke(width = 12f * state.zoom, cap = StrokeCap.Round)
        )
        return
    }

    val bodyColor = Color(cs.bodyColor)
    val surfaceColor = Color(cs.surfaceColor)
    val surfaceWidth = (cs.strokeWidth * state.zoom).coerceAtLeast(2f)

    if (cs.isClosed) {
        path.close()
        drawPath(path, bodyColor)
        drawPath(path, surfaceColor, style = Stroke(width = surfaceWidth))
    } else {
        // Open terrain path - draw deep underground body & surface stroke
        val terrainBottom = path.copy().apply {
            val lastP = points.last()
            val firstP = points.first()
            lineTo(sx + (lastP.x * state.zoom), sy + 800f * state.zoom)
            lineTo(sx + (firstP.x * state.zoom), sy + 800f * state.zoom)
            close()
        }
        // Dirt body
        drawPath(terrainBottom, bodyColor)

        // Surface stroke
        drawPath(
            path,
            color = surfaceColor,
            style = Stroke(width = surfaceWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        // Top highlight line
        drawPath(
            path,
            color = Color.White.copy(alpha = 0.25f),
            style = Stroke(width = (surfaceWidth * 0.25f).coerceAtLeast(1.5f), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

private fun DrawScope.drawTileMapObject(state: EditorState, obj: GameObject, sx: Float, sy: Float) {
    val tm = obj.tileMap ?: return
    val tw = tm.tileWidth * state.zoom
    val th = tm.tileHeight * state.zoom

    for ((key, assetId) in tm.tiles) {
        val parts = key.split("_")
        if (parts.size == 2) {
            val c = parts[0].toIntOrNull() ?: continue
            val r = parts[1].toIntOrNull() ?: continue
            val cellX = sx + (c * tw)
            val cellY = sy + (r * th)

            translate(cellX, cellY) {
                VectorSprites.drawAsset(this@drawTileMapObject, assetId, tw, th)
            }
        }
    }

    if (state.selectedObjectId == obj.id) {
        for (c in 0..tm.cols) {
            val lx = sx + c * tw
            drawLine(CanvasSelection.copy(alpha = 0.4f), Offset(lx, sy), Offset(lx, sy + tm.rows * th), strokeWidth = 1f)
        }
        for (r in 0..tm.rows) {
            val ly = sy + r * th
            drawLine(CanvasSelection.copy(alpha = 0.4f), Offset(sx, ly), Offset(sx + tm.cols * tw, ly), strokeWidth = 1f)
        }
    }
}

private fun DrawScope.drawSelectionGizmos(state: EditorState, obj: GameObject, pointPulseAlpha: Float) {
    val (sx, sy) = state.worldToScreen(obj.x, obj.y)
    val sw = obj.width * state.zoom
    val sh = obj.height * state.zoom
    val hw = sw / 2f
    val hh = sh / 2f

    // 1. Standard Object Gizmo (when not editing shape points)
    if (obj.type != ObjectType.CUSTOM_SHAPE) {
        rotate(obj.rotation, pivot = Offset(sx, sy)) {
            drawRoundRect(
                color = CanvasSelection,
                topLeft = Offset(sx - hw, sy - hh),
                size = Size(sw, sh),
                cornerRadius = CornerRadius(4f, 4f),
                style = Stroke(width = 2.5f)
            )

            // Corner Resize Handles
            val handleR = 8f
            val handles = listOf(
                Offset(sx - hw, sy - hh),
                Offset(sx + hw, sy - hh),
                Offset(sx + hw, sy + hh),
                Offset(sx - hw, sy + hh)
            )
            for (h in handles) {
                drawCircle(Color.White, radius = handleR, center = h)
                drawCircle(CanvasSelection, radius = handleR, center = h, style = Stroke(width = 2.5f))
            }

            // Top Rotation Handle
            val rotHandlePos = Offset(sx, sy - hh - 28f)
            drawLine(CanvasSelection, Offset(sx, sy - hh), rotHandlePos, strokeWidth = 2f)
            drawCircle(StudioAccentOrange, radius = handleR + 2f, center = rotHandlePos)
            drawCircle(Color.White, radius = handleR - 2f, center = rotHandlePos)
        }
    }

    // 2. Custom Shape Control Points Gizmo
    if (obj.type == ObjectType.CUSTOM_SHAPE && obj.customShape != null) {
        val points = obj.customShape.points
        val isEditing = state.isEditingShapePoints
        val baseHandleSize = state.shapeVertexHandleSize

        for (i in points.indices) {
            val p = points[i]
            val pwx = sx + (p.x * state.zoom)
            val pwy = sy + (p.y * state.zoom)
            val isSelected = (i == state.selectedPointIndex)

            // Make handles extra large, clear, and visible when editing mode is active
            val radius = if (isEditing) (if (isSelected) baseHandleSize * 1.35f else baseHandleSize) else (if (isSelected) 14f else 9f)

            if (isSelected) {
                // Pulsing highlight ring around the selected point
                drawCircle(
                    color = StudioAccentOrange.copy(alpha = pointPulseAlpha * 0.5f),
                    radius = radius * 2.2f,
                    center = Offset(pwx, pwy)
                )
            }

            // High contrast dark outline
            drawCircle(
                color = Color(0xFF0F172A),
                radius = radius + 3f,
                center = Offset(pwx, pwy)
            )

            // Outer vivid ring
            drawCircle(
                color = if (isSelected) StudioAccentOrange else CanvasSelection,
                radius = radius,
                center = Offset(pwx, pwy)
            )

            // Inner center pip
            drawCircle(
                color = if (isSelected) Color.White.copy(alpha = pointPulseAlpha) else Color.White,
                radius = radius * 0.45f,
                center = Offset(pwx, pwy)
            )
        }
    }
}
