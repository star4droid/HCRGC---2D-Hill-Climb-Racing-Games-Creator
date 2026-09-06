package com.star4droid.hcrgc.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.star4droid.hcrgc.model.GameObject
import com.star4droid.hcrgc.model.LevelData
import com.star4droid.hcrgc.model.ObjectType
import com.star4droid.hcrgc.model.Point2D
import com.star4droid.hcrgc.model.ProjectConfig

enum class EditorTool {
    SELECT,
    MOVE,
    SCALE,
    ROTATE,
    EDIT_POINTS,
    STAMP_TILES,
    EDIT_IMAGE
}

enum class TransformMode {
    GRID,   // Normal mode with gizmo selection and grid alignment
    MOVE,   // Direct single finger move
    ROTATE, // Direct single finger rotate
    SCALE   // Direct single finger scale width/height/radius
}

class EditorState(
    initialProject: ProjectConfig,
    initialLevel: LevelData,
    val onLevelModified: () -> Unit = {}
) {
    var project by mutableStateOf(initialProject)
    var level by mutableStateOf(initialLevel)
    // Canvas pan and zoom (unlimited zoom range for deep zooming and macro views)
    var zoom by mutableFloatStateOf(0.75f)
    var panX by mutableFloatStateOf(200f)
    var panY by mutableFloatStateOf(100f)

    // User-configurable zoom responsiveness factor
    var zoomFactor by mutableFloatStateOf(1.8f)

    var selectedObjectId by mutableStateOf<String?>(null)
    var activeTool by mutableStateOf(EditorTool.SELECT)
    var transformMode by mutableStateOf(TransformMode.GRID)

    var showGrid by mutableStateOf(true)
    var snapToGrid by mutableStateOf(false)
    val gridSize = 32f

    // Visual Shape Point Editing
    var isEditingShapePoints by mutableStateOf(false)
    var selectedPointIndex by mutableIntStateOf(-1)
    var shapeVertexHandleSize by mutableFloatStateOf(24f) // Large visible handles for easy touch

    // Layer Visibility
    var showUiElementsLayer by mutableStateOf(true)

    // TileMap stamping
    var selectedTileAsset by mutableStateOf<String?>("tile_grass")
    var isErasingTile by mutableStateOf(false)

    // Undo / Redo history
    private val undoStack = mutableListOf<List<GameObject>>()
    private val redoStack = mutableListOf<List<GameObject>>()

    val selectedObject: GameObject?
        get() = level.objects.find { it.id == selectedObjectId }

    fun pushUndoState() {
        undoStack.add(level.objects.map { it.copy() })
        if (undoStack.size > 30) undoStack.removeAt(0)
        redoStack.clear()
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            redoStack.add(level.objects.map { it.copy() })
            val previous = undoStack.removeAt(undoStack.lastIndex)
            level = level.copy(objects = previous)
            onLevelModified()
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            undoStack.add(level.objects.map { it.copy() })
            val next = redoStack.removeAt(redoStack.lastIndex)
            level = level.copy(objects = next)
            onLevelModified()
        }
    }

    fun updateObject(newObj: GameObject) {
        level = level.copy(
            objects = level.objects.map { if (it.id == newObj.id) newObj else it }
        )
        onLevelModified()
    }

    fun addObject(obj: GameObject) {
        pushUndoState()
        level = level.copy(objects = level.objects + obj)
        selectedObjectId = obj.id
        if (obj.type == ObjectType.CUSTOM_SHAPE) {
            isEditingShapePoints = false
            selectedPointIndex = -1
        }
        onLevelModified()
    }

    fun removeObject(id: String) {
        pushUndoState()
        level = level.copy(
            objects = level.objects.filter { it.id != id && it.parentId != id }
        )
        if (selectedObjectId == id) {
            selectedObjectId = null
            isEditingShapePoints = false
            selectedPointIndex = -1
        }
        onLevelModified()
    }

    fun duplicateObject(id: String) {
        val obj = level.objects.find { it.id == id } ?: return
        pushUndoState()
        val copy = obj.copy(
            id = java.util.UUID.randomUUID().toString().take(8),
            name = "${obj.name} (Copy)",
            x = obj.x + 30f,
            y = obj.y + 30f
        )
        level = level.copy(objects = level.objects + copy)
        selectedObjectId = copy.id
        onLevelModified()
    }

    // Z-Order manipulations
    fun bringForward(id: String) {
        pushUndoState()
        val obj = level.objects.find { it.id == id } ?: return
        updateObject(obj.copy(zIndex = obj.zIndex + 1))
    }

    fun sendBackward(id: String) {
        pushUndoState()
        val obj = level.objects.find { it.id == id } ?: return
        updateObject(obj.copy(zIndex = (obj.zIndex - 1).coerceAtLeast(-50)))
    }

    fun bringToFront(id: String) {
        pushUndoState()
        val maxZ = level.objects.maxOfOrNull { it.zIndex } ?: 0
        val obj = level.objects.find { it.id == id } ?: return
        updateObject(obj.copy(zIndex = maxZ + 1))
    }

    fun sendToBack(id: String) {
        pushUndoState()
        val minZ = level.objects.minOfOrNull { it.zIndex } ?: 0
        val obj = level.objects.find { it.id == id } ?: return
        updateObject(obj.copy(zIndex = minZ - 1))
    }

    fun screenToWorld(sx: Float, sy: Float): Pair<Float, Float> {
        val wx = (sx - panX) / zoom
        val wy = (sy - panY) / zoom
        return Pair(wx, wy)
    }

    fun worldToScreen(wx: Float, wy: Float): Pair<Float, Float> {
        val sx = wx * zoom + panX
        val sy = wy * zoom + panY
        return Pair(sx, sy)
    }

    fun applySnap(value: Float): Float {
        return if (snapToGrid) {
            Math.round(value / gridSize) * gridSize
        } else value
    }

    fun panToWorldPoint(wx: Float, wy: Float, screenW: Float, screenH: Float) {
        panX = screenW / 2f - wx * zoom
        panY = screenH / 2f - wy * zoom
    }
}
