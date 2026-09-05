package com.star4droid.hcrgc.runtime

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
import com.star4droid.hcrgc.physics.*
import com.star4droid.hcrgc.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.*

@Composable
fun GameRuntimeScreen(
    project: ProjectConfig,
    level: LevelData,
    onExitToEditor: () -> Unit
) {
    var isPaused by remember { mutableStateOf(false) }
    var isFinished by remember { mutableStateOf(false) }
    var victoryStars by remember { mutableIntStateOf(3) }
    var gameTime by remember { mutableFloatStateOf(0f) }
    var coinsCollected by remember { mutableIntStateOf(0) }
    var showFps by remember { mutableStateOf(true) }
    var currentFps by remember { mutableIntStateOf(60) }
    var frameCount by remember { mutableIntStateOf(0) }
    var lastFpsTimestamp by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Control inputs
    var isGasPressed by remember { mutableStateOf(false) }
    var isBrakePressed by remember { mutableStateOf(false) }
    var isTiltLeftPressed by remember { mutableStateOf(false) }
    var isTiltRightPressed by remember { mutableStateOf(false) }

    // Camera state
    var camX by remember { mutableFloatStateOf(0f) }
    var camY by remember { mutableFloatStateOf(0f) }

    // Simulation context
    val simContext = remember(level) {
        setupSimulation(project, level)
    }

    val collectedCoinIds = remember { mutableStateListOf<String>() }

    fun resetGame() {
        gameTime = 0f
        coinsCollected = 0
        collectedCoinIds.clear()
        isFinished = false
        isPaused = false

        // Reset car
        val carObj = level.objects.find { it.type == ObjectType.CAR_BODY }
        if (carObj != null && simContext.vehicleController != null) {
            simContext.vehicleController.chassis.position = Vec2(carObj.x, carObj.y)
            simContext.vehicleController.chassis.velocity = Vec2(0f, 0f)
            simContext.vehicleController.chassis.rotation = carObj.rotation
            simContext.vehicleController.chassis.angularVelocity = 0f
        }
    }

    // 60 FPS Game Loop
    LaunchedEffect(isPaused, isFinished) {
        val dt = 1f / 60f
        while (!isPaused && !isFinished) {
            gameTime += dt
            frameCount++
            val now = System.currentTimeMillis()
            if (now - lastFpsTimestamp >= 400) {
                currentFps = ((frameCount * 1000f) / (now - lastFpsTimestamp)).toInt().coerceIn(10, 120)
                frameCount = 0
                lastFpsTimestamp = now
            }

            // Update user input to vehicle controller
            val vc = simContext.vehicleController
            if (vc != null) {
                vc.throttle = when {
                    isGasPressed -> 1.0f
                    isBrakePressed -> -0.8f
                    else -> 0f
                }
                vc.airTilt = when {
                    isTiltLeftPressed -> -1.0f
                    isTiltRightPressed -> 1.0f
                    else -> 0f
                }

                vc.update(dt, simContext.terrainSegments, simContext.staticBoxes)
            }

            // Step physics world (including all dynamic boxes, crates, circles)
            simContext.physicsWorld.step(dt, simContext.terrainSegments, simContext.staticBoxes)

            // Check coin collisions
            val carPos = vc?.chassis?.position ?: Vec2(0f, 0f)
            for (coin in simContext.dynamicCoins) {
                if (!collectedCoinIds.contains(coin.id)) {
                    val dist = hypot(carPos.x - coin.x, carPos.y - coin.y)
                    if (dist < 50f) {
                        collectedCoinIds.add(coin.id)
                        coinsCollected += (coin.coin?.value ?: 10)
                    }
                }
            }

            // Check finish flag collision
            val finishFlagObj = simContext.finishFlag
            if (finishFlagObj != null && !isFinished) {
                val dist = hypot(carPos.x - finishFlagObj.x, carPos.y - finishFlagObj.y)
                if (dist < 75f) {
                    isFinished = true
                    val threeStarTime = finishFlagObj.finishFlag?.timeForThreeStars ?: 35f
                    val twoStarTime = finishFlagObj.finishFlag?.timeForTwoStars ?: 65f
                    victoryStars = when {
                        gameTime <= threeStarTime -> 3
                        gameTime <= twoStarTime -> 2
                        else -> 1
                    }
                }
            }

            // Smooth Camera Follow
            val targetCamX = carPos.x
            val targetCamY = carPos.y
            camX += (targetCamX - camX) * 0.12f
            camY += (targetCamY - camY) * 0.12f

            delay(16)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(project.backgroundColor))
    ) {
        // Main Game World Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val screenW = size.width
            val screenH = size.height

            val zoom = level.cameraSettings.zoom.coerceIn(0.4f, 1.8f)
            val offsetX = screenW * 0.35f - camX * zoom
            val offsetY = screenH * 0.65f - camY * zoom

            // 1. Draw All Custom Shapes (Terrain Roads & Polygons)
            for (trackObj in level.objects.filter { it.type == ObjectType.CUSTOM_SHAPE && it.customShape != null }) {
                val cs = trackObj.customShape ?: continue
                if (cs.isTransparent) {
                    // Transparent track: invisible in game, only acts as collision boundary!
                    continue
                }
                val points = cs.points
                if (points.size < 2) continue

                val path = Path()
                val p0 = points.first()
                path.moveTo((trackObj.x + p0.x) * zoom + offsetX, (trackObj.y + p0.y) * zoom + offsetY)
                for (i in 1 until points.size) {
                    val pt = points[i]
                    path.lineTo((trackObj.x + pt.x) * zoom + offsetX, (trackObj.y + pt.y) * zoom + offsetY)
                }

                if (cs.isClosed) {
                    path.close()
                    drawPath(path, Color(cs.surfaceColor))
                    drawPath(path, Color(0xFF0F172A), style = Stroke(width = 3f * zoom))
                } else {
                    // Deep underground fill using cs.bodyColor
                    val dirtPath = path.copy().apply {
                        val last = points.last()
                        val first = points.first()
                        lineTo((trackObj.x + last.x) * zoom + offsetX, (trackObj.y + 1400f) * zoom + offsetY)
                        lineTo((trackObj.x + first.x) * zoom + offsetX, (trackObj.y + 1400f) * zoom + offsetY)
                        close()
                    }
                    drawPath(dirtPath, Color(cs.bodyColor))

                    // Surface stroke using cs.surfaceColor & cs.strokeWidth
                    drawPath(
                        path,
                        color = Color(cs.surfaceColor),
                        style = Stroke(width = cs.strokeWidth * zoom, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    // Surface top highlight
                    drawPath(
                        path,
                        color = Color(cs.surfaceColor).copy(alpha = 0.75f),
                        style = Stroke(width = (cs.strokeWidth * 0.28f).coerceAtLeast(3f) * zoom, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
            }

            // 2. Draw TileMap Blocks
            for (obj in level.objects.filter { it.type == ObjectType.TILEMAP && it.tileMap != null }) {
                val tm = obj.tileMap ?: continue
                val tw = tm.tileWidth * zoom
                val th = tm.tileHeight * zoom
                for ((key, assetId) in tm.tiles) {
                    val parts = key.split("_")
                    if (parts.size == 2) {
                        val c = parts[0].toIntOrNull() ?: continue
                        val r = parts[1].toIntOrNull() ?: continue
                        val cellX = (obj.x + c * tm.tileWidth) * zoom + offsetX
                        val cellY = (obj.y + r * tm.tileHeight) * zoom + offsetY
                        translate(cellX, cellY) {
                            VectorSprites.drawAsset(this@Canvas, assetId, tw, th)
                        }
                    }
                }
            }

            // 3. Draw All General Objects (Dynamic Boxes, Circles, Custom Sprites, Obstacles)
            val nonSpecialObjects = level.objects.filter {
                it.type != ObjectType.CUSTOM_SHAPE &&
                it.type != ObjectType.TILEMAP &&
                it.type != ObjectType.CAR_BODY &&
                it.type != ObjectType.WHEEL &&
                it.type != ObjectType.COIN &&
                it.type != ObjectType.FINISH_FLAG &&
                it.type != ObjectType.UI_BUTTON &&
                it.type != ObjectType.UI_TEXT
            }.sortedBy { it.zIndex }

            for (obj in nonSpecialObjects) {
                // Check if simulated dynamically in physics world
                val body = simContext.physicsWorld.bodies.find { it.id == obj.id }
                val posX = (body?.position?.x ?: obj.x) * zoom + offsetX
                val posY = (body?.position?.y ?: obj.y) * zoom + offsetY
                val rot = body?.rotation ?: obj.rotation
                val ow = obj.width * zoom
                val oh = obj.height * zoom

                rotate(rot, pivot = Offset(posX, posY)) {
                    if (obj.imageAsset != null) {
                        translate(posX - ow / 2f, posY - oh / 2f) {
                            VectorSprites.drawAsset(this@Canvas, obj.imageAsset, ow, oh, tint = Color(obj.tintColor))
                        }
                    } else if (obj.type == ObjectType.CIRCLE) {
                        val radius = min(ow, oh) / 2f
                        drawCircle(Color(obj.tintColor), radius = radius, center = Offset(posX, posY))
                        drawCircle(Color.White.copy(alpha = 0.4f), radius = radius, center = Offset(posX, posY), style = Stroke(2f))
                    } else {
                        drawRoundRect(
                            color = Color(obj.tintColor),
                            topLeft = Offset(posX - ow / 2f, posY - oh / 2f),
                            size = Size(ow, oh),
                            cornerRadius = CornerRadius(4f, 4f)
                        )
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.35f),
                            topLeft = Offset(posX - ow / 2f, posY - oh / 2f),
                            size = Size(ow, oh),
                            cornerRadius = CornerRadius(4f, 4f),
                            style = Stroke(1.5f)
                        )
                    }
                }
            }

            // 4. Draw Coins
            for (coin in simContext.dynamicCoins) {
                if (!collectedCoinIds.contains(coin.id)) {
                    val cx = coin.x * zoom + offsetX
                    val cy = coin.y * zoom + offsetY
                    val cw = coin.width * zoom
                    val ch = coin.height * zoom
                    translate(cx - cw / 2f, cy - ch / 2f) {
                        VectorSprites.drawAsset(this@Canvas, VectorSprites.ASSET_COIN_GOLD, cw, ch)
                    }
                }
            }

            // 5. Draw Finish Flag
            if (simContext.finishFlag != null) {
                val flag = simContext.finishFlag
                val fx = flag.x * zoom + offsetX
                val fy = flag.y * zoom + offsetY
                val fw = flag.width * zoom
                val fh = flag.height * zoom
                translate(fx - fw / 2f, fy - fh / 2f) {
                    VectorSprites.drawAsset(this@Canvas, VectorSprites.ASSET_FINISH_FLAG, fw, fh)
                }
            }

            // 6. Draw Vehicle (Wheels + Chassis + Attached Elements)
            val vc = simContext.vehicleController
            if (vc != null) {
                // Wheels
                for (wheel in vc.wheels) {
                    val wx = wheel.body.position.x * zoom + offsetX
                    val wy = wheel.body.position.y * zoom + offsetY
                    val wr = wheel.config.radius * 2f * zoom
                    rotate(wheel.body.rotation, pivot = Offset(wx, wy)) {
                        translate(wx - wr / 2f, wy - wr / 2f) {
                            VectorSprites.drawAsset(this@Canvas, VectorSprites.ASSET_WHEEL_RUGGED, wr, wr)
                        }
                    }
                }

                // Chassis (Rotates naturally with terrain slope!)
                val carObj = level.objects.find { it.type == ObjectType.CAR_BODY }
                val chassisPos = vc.chassis.position
                val cx = chassisPos.x * zoom + offsetX
                val cy = chassisPos.y * zoom + offsetY
                val cw = (carObj?.width ?: 130f) * zoom
                val ch = (carObj?.height ?: 55f) * zoom

                rotate(vc.chassis.rotation, pivot = Offset(cx, cy)) {
                    translate(cx - cw / 2f, cy - ch / 2f) {
                        VectorSprites.drawAsset(
                            this@Canvas,
                            carObj?.imageAsset ?: VectorSprites.ASSET_CAR_BUGGY,
                            cw, ch,
                            tint = Color(carObj?.tintColor ?: 0xFFFFFFFF)
                        )
                    }

                    // Attached Elements
                    for (elem in level.objects.filter { it.parentId == carObj?.id && it.type == ObjectType.ELEMENT }) {
                        val relX = (elem.x - (carObj?.x ?: 0f)) * zoom
                        val relY = (elem.y - (carObj?.y ?: 0f)) * zoom
                        val ew = elem.width * zoom
                        val eh = elem.height * zoom

                        drawRoundRect(
                            color = StudioAccentIndigo,
                            topLeft = Offset(cx + relX - ew / 2f, cy + relY - eh / 2f),
                            size = Size(ew, eh),
                            cornerRadius = CornerRadius(4f, 4f)
                        )
                    }
                }
            }

            // 7. Ambient Lighting & Box2D Lights Layer
            val ambientIntensity = project.ambientLightIntensity.coerceIn(0f, 1f)
            val lightObjects = level.objects.filter { it.type == ObjectType.LIGHT && it.light != null }

            if (ambientIntensity < 0.96f || lightObjects.isNotEmpty()) {
                val darknessAlpha = (1f - ambientIntensity).coerceIn(0f, 0.92f)
                val ambientCol = Color(project.ambientLightColor)

                // Render ambient darkness layer across viewport
                if (darknessAlpha > 0.04f) {
                    drawRect(
                        color = ambientCol.copy(alpha = darknessAlpha),
                        topLeft = Offset(0f, 0f),
                        size = size
                    )
                }

                // Render Box2D Lights (Cone, Point, Directional)
                for (lightObj in lightObjects) {
                    val cfg = lightObj.light ?: continue
                    val lightCol = Color(cfg.color)
                    val baseDist = cfg.distance * zoom

                    // Determine light position & direction (handle car headlights attachment)
                    val (lx, ly, lRot) = if (cfg.attachToParent && vc != null) {
                        val cPos = vc.chassis.position
                        Triple(cPos.x * zoom + offsetX, cPos.y * zoom + offsetY, vc.chassis.rotation + cfg.direction)
                    } else {
                        Triple(lightObj.x * zoom + offsetX, lightObj.y * zoom + offsetY, cfg.direction)
                    }

                    when (cfg.lightType) {
                        LightType.CONE -> {
                            rotate(lRot, pivot = Offset(lx, ly)) {
                                val halfAngleRad = Math.toRadians((cfg.coneAngle / 2f).toDouble())
                                val cosA = cos(halfAngleRad).toFloat()
                                val sinA = sin(halfAngleRad).toFloat()
                                val conePath = Path().apply {
                                    moveTo(lx, ly)
                                    lineTo(lx + baseDist * cosA, ly - baseDist * sinA)
                                    lineTo(lx + baseDist, ly)
                                    lineTo(lx + baseDist * cosA, ly + baseDist * sinA)
                                    close()
                                }
                                drawPath(
                                    conePath,
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            lightCol.copy(alpha = (cfg.intensity * 0.75f).coerceIn(0.1f, 0.9f)),
                                            lightCol.copy(alpha = 0.2f),
                                            Color.Transparent
                                        ),
                                        center = Offset(lx, ly),
                                        radius = baseDist
                                    )
                                )
                            }
                        }
                        LightType.DIRECTIONAL -> {
                            rotate(lRot, pivot = Offset(lx, ly)) {
                                drawRect(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            lightCol.copy(alpha = (cfg.intensity * 0.65f).coerceIn(0.1f, 0.85f)),
                                            Color.Transparent
                                        ),
                                        startX = lx,
                                        endX = lx + baseDist * 2f
                                    ),
                                    topLeft = Offset(lx, ly - baseDist * 0.5f),
                                    size = Size(baseDist * 2f, baseDist)
                                )
                            }
                        }
                        else -> {
                            // Point / Omnidirectional light
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        lightCol.copy(alpha = (cfg.intensity * 0.75f).coerceIn(0.1f, 0.9f)),
                                        lightCol.copy(alpha = 0.25f),
                                        Color.Transparent
                                    ),
                                    center = Offset(lx, ly),
                                    radius = baseDist
                                ),
                                radius = baseDist,
                                center = Offset(lx, ly)
                            )
                        }
                    }
                }
            }
        }

        // SCREEN HUD LAYER (Always rendered on top of game world, immune to lighting)
        val hudUiObjects = level.objects.filter {
            it.visible && (it.type == ObjectType.UI_BUTTON || it.type == ObjectType.UI_TEXT || it.type == ObjectType.UI_PROGRESS_BAR)
        }
        RuntimeHUD(
            coins = coinsCollected,
            timeSeconds = gameTime,
            carX = simContext.vehicleController?.chassis?.position?.x ?: 0f,
            finishX = simContext.finishFlag?.x ?: 2000f,
            showFps = showFps,
            fps = currentFps,
            onHideFps = { showFps = false },
            uiObjects = hudUiObjects,
            onPauseClick = { isPaused = !isPaused },
            onGasPressedChange = { isGasPressed = it },
            onBrakePressedChange = { isBrakePressed = it },
            onTiltLeftChange = { isTiltLeftPressed = it },
            onTiltRightChange = { isTiltRightPressed = it }
        )

        // Victory Dialog
        if (isFinished) {
            VictoryDialog(
                stars = victoryStars,
                timeSeconds = gameTime,
                coins = coinsCollected,
                onReplay = { resetGame() },
                onReturnToEditor = onExitToEditor
            )
        }

        // Pause Overlay
        if (isPaused) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = StudioSurface,
                    border = BorderStroke(1.dp, StudioSurfaceBorder),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Game Paused", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = { isPaused = false },
                                colors = ButtonDefaults.buttonColors(containerColor = StudioAccentGreen)
                            ) {
                                Text("Resume")
                            }
                            OutlinedButton(onClick = { resetGame() }) {
                                Text("Restart")
                            }
                            Button(
                                onClick = onExitToEditor,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Exit")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RuntimeHUD(
    coins: Int,
    timeSeconds: Float,
    carX: Float,
    finishX: Float,
    showFps: Boolean,
    fps: Int,
    onHideFps: () -> Unit,
    uiObjects: List<GameObject>,
    onPauseClick: () -> Unit,
    onGasPressedChange: (Boolean) -> Unit,
    onBrakePressedChange: (Boolean) -> Unit,
    onTiltLeftChange: (Boolean) -> Unit,
    onTiltRightChange: (Boolean) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Custom Level UI Elements (Rendered above game, immune to darkness & lights)
        for (ui in uiObjects) {
            when (ui.type) {
                ObjectType.UI_BUTTON -> {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(ui.tintColor),
                        border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.8f)),
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .offset(x = (ui.x * 0.5f).coerceAtLeast(10f).dp, y = (ui.y * 0.5f).coerceAtLeast(10f).dp)
                            .size(width = (ui.width * 0.7f).coerceAtLeast(60f).dp, height = (ui.height * 0.7f).coerceAtLeast(36f).dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = ui.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }
                    }
                }
                ObjectType.UI_PROGRESS_BAR -> {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1E293B),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .offset(x = (ui.x * 0.5f).coerceAtLeast(10f).dp, y = (ui.y * 0.5f).coerceAtLeast(10f).dp)
                            .size(width = (ui.width * 0.8f).coerceAtLeast(100f).dp, height = (ui.height * 0.6f).coerceAtLeast(16f).dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(0.65f)
                                .background(Color(ui.tintColor))
                        )
                    }
                }
                ObjectType.UI_TEXT -> {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xCC0F172A),
                        border = BorderStroke(1.dp, StudioSurfaceBorder),
                        modifier = Modifier
                            .offset(x = (ui.x * 0.5f).coerceAtLeast(10f).dp, y = (ui.y * 0.5f).coerceAtLeast(10f).dp)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = ui.name,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = Color(ui.tintColor)
                        )
                    }
                }
                else -> {}
            }
        }

        // Top HUD Bar: Distance, FPS, Time, Coins, Pause
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Distance Progress & FPS Counter
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                val progress = ((carX / finishX).coerceIn(0f, 1f) * 100).toInt()
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = StudioSurfaceElevated.copy(alpha = 0.9f),
                    border = BorderStroke(1.dp, StudioSurfaceBorder)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Flag, contentDescription = null, tint = StudioAccentOrange, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("$progress%", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = StudioTextPrimary)
                    }
                }

                // FPS Counter with Long Click to Hide
                if (showFps) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = StudioSurfaceElevated.copy(alpha = 0.9f),
                        border = BorderStroke(1.dp, (if (fps >= 50) StudioAccentGreen else StudioAccentAmber).copy(alpha = 0.45f)),
                        modifier = Modifier
                            .testTag("fps_counter")
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onLongPress = {
                                        onHideFps()
                                    }
                                )
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(if (fps >= 50) StudioAccentGreen else StudioAccentAmber, CircleShape)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "$fps FPS",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (fps >= 50) StudioAccentGreen else StudioAccentAmber
                            )
                        }
                    }
                }
            }

            // Live Time & Coins
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = StudioSurfaceElevated.copy(alpha = 0.9f),
                    border = BorderStroke(1.dp, StudioSurfaceBorder)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Timer, contentDescription = null, tint = StudioAccentBlue, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(String.format("%.1fs", timeSeconds), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = StudioTextPrimary)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = StudioSurfaceElevated.copy(alpha = 0.9f),
                    border = BorderStroke(1.dp, StudioSurfaceBorder)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = StudioAccentAmber, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("$coins", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = StudioAccentAmber)
                    }
                }

                IconButton(
                    onClick = onPauseClick,
                    modifier = Modifier
                        .background(StudioSurfaceElevated.copy(alpha = 0.9f), CircleShape)
                        .border(1.dp, StudioSurfaceBorder, CircleShape)
                        .size(36.dp)
                ) {
                    Icon(Icons.Default.Pause, contentDescription = "Pause", tint = StudioTextPrimary, modifier = Modifier.size(18.dp))
                }
            }
        }

        // Bottom Pedals & Controls (Gas, Brake, In-Air Tilt)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            // Left Controls: Brake & Tilt Left
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PedalButton(
                    label = "BRAKE",
                    color = Color(0xFFEF4444),
                    icon = Icons.Default.VerticalAlignBottom,
                    modifier = Modifier.size(width = 110.dp, height = 75.dp).testTag("pedal_brake"),
                    onPressedChange = onBrakePressedChange
                )
                PedalButton(
                    label = "TILT ↶",
                    color = Color(0xFF64748B),
                    icon = Icons.Default.RotateLeft,
                    modifier = Modifier.size(width = 65.dp, height = 75.dp),
                    onPressedChange = onTiltLeftChange
                )
            }

            // Right Controls: Tilt Right & Gas
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PedalButton(
                    label = "↷ TILT",
                    color = Color(0xFF64748B),
                    icon = Icons.Default.RotateRight,
                    modifier = Modifier.size(width = 65.dp, height = 75.dp),
                    onPressedChange = onTiltRightChange
                )
                PedalButton(
                    label = "GAS",
                    color = Color(0xFF22C55E),
                    icon = Icons.Default.Speed,
                    modifier = Modifier.size(width = 110.dp, height = 75.dp).testTag("pedal_gas"),
                    onPressedChange = onGasPressedChange
                )
            }
        }
    }
}

@Composable
private fun PedalButton(
    label: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onPressedChange: (Boolean) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isPressed) color.copy(alpha = 0.85f) else color.copy(alpha = 0.5f),
        border = BorderStroke(2.dp, if (isPressed) Color.White else color),
        shadowElevation = if (isPressed) 2.dp else 6.dp,
        modifier = modifier
            .scale(if (isPressed) 0.95f else 1.0f)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        onPressedChange(true)
                        tryAwaitRelease()
                        isPressed = false
                        onPressedChange(false)
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(2.dp))
            Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

@Composable
private fun VictoryDialog(
    stars: Int,
    timeSeconds: Float,
    coins: Int,
    onReplay: () -> Unit,
    onReturnToEditor: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = StudioSurface,
            border = BorderStroke(2.dp, StudioAccentAmber),
            shadowElevation = 16.dp,
            modifier = Modifier.padding(28.dp).fillMaxWidth(0.65f)
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "LEVEL COMPLETE!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = StudioAccentAmber
                )
                Spacer(Modifier.height(14.dp))

                // Stars
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (i in 1..3) {
                        val earned = i <= stars
                        Icon(
                            imageVector = if (earned) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = null,
                            tint = if (earned) StudioAccentAmber else StudioTextTertiary,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))
                Text("Time: ${String.format("%.1fs", timeSeconds)}   •   Coins: $coins", style = MaterialTheme.typography.titleMedium, color = StudioTextPrimary)
                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onReplay,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Replay")
                    }
                    Button(
                        onClick = onReturnToEditor,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = StudioAccentBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Editor")
                    }
                }
            }
        }
    }
}

private data class SimulationContext(
    val physicsWorld: PhysicsWorld,
    val vehicleController: VehicleController?,
    val terrainSegments: List<Pair<Vec2, Vec2>>,
    val staticBoxes: List<FloatArray>,
    val dynamicCoins: List<GameObject>,
    val finishFlag: GameObject?
)

private fun setupSimulation(project: ProjectConfig, level: LevelData): SimulationContext {
    val world = PhysicsWorld(gravity = Vec2(project.gravityX * 60f, project.gravityY * 60f))

    // 1. Terrain Segments (all CUSTOM_SHAPE objects)
    val segments = mutableListOf<Pair<Vec2, Vec2>>()
    for (obj in level.objects.filter { it.type == ObjectType.CUSTOM_SHAPE && it.customShape != null }) {
        val cs = obj.customShape ?: continue
        val pts = cs.points
        for (i in 0 until pts.size - 1) {
            val p1 = Vec2(obj.x + pts[i].x, obj.y + pts[i].y)
            val p2 = Vec2(obj.x + pts[i + 1].x, obj.y + pts[i + 1].y)
            segments.add(Pair(p1, p2))
        }
        if (cs.isClosed && pts.size > 2) {
            val p1 = Vec2(obj.x + pts.last().x, obj.y + pts.last().y)
            val p2 = Vec2(obj.x + pts.first().x, obj.y + pts.first().y)
            segments.add(Pair(p1, p2))
        }
    }

    // 2. Static TileMap Boxes
    val boxes = mutableListOf<FloatArray>()
    for (obj in level.objects.filter { it.type == ObjectType.TILEMAP && it.tileMap != null }) {
        val tm = obj.tileMap ?: continue
        for ((key, _) in tm.tiles) {
            val parts = key.split("_")
            if (parts.size == 2) {
                val c = parts[0].toIntOrNull() ?: continue
                val r = parts[1].toIntOrNull() ?: continue
                val left = obj.x + c * tm.tileWidth
                val top = obj.y + r * tm.tileHeight
                boxes.add(floatArrayOf(left, top, left + tm.tileWidth, top + tm.tileHeight))
            }
        }
    }

    // 3. Register All Dynamic Non-Car Objects in Physics World (Crates, Boxes, Balls, etc.)
    for (obj in level.objects) {
        if (obj.type != ObjectType.CAR_BODY && obj.type != ObjectType.WHEEL && obj.type != ObjectType.CUSTOM_SHAPE && obj.type != ObjectType.TILEMAP) {
            if (obj.physics.bodyType == BodyType.DYNAMIC) {
                val body = RigidBody(
                    id = obj.id,
                    position = Vec2(obj.x, obj.y),
                    rotation = obj.rotation,
                    width = obj.width,
                    height = obj.height,
                    mass = (obj.physics.density * (obj.width * obj.height / 1000f)).coerceAtLeast(0.5f),
                    friction = obj.physics.friction,
                    restitution = obj.physics.restitution,
                    isSensor = obj.physics.isSensor
                )
                world.addBody(body)
            } else if (obj.physics.bodyType == BodyType.STATIC && !obj.physics.isSensor) {
                val halfW = obj.width / 2f
                val halfH = obj.height / 2f
                boxes.add(floatArrayOf(obj.x - halfW, obj.y - halfH, obj.x + halfW, obj.y + halfH))
            }
        }
    }

    // 4. Vehicle Setup
    val carObj = level.objects.find { it.type == ObjectType.CAR_BODY }
    var vehicleCtrl: VehicleController? = null

    if (carObj != null) {
        val chassisBody = RigidBody(
            id = carObj.id,
            position = Vec2(carObj.x, carObj.y),
            rotation = carObj.rotation,
            width = carObj.width,
            height = carObj.height,
            mass = carObj.physics.density * 14f,
            inertia = 32000f,
            friction = carObj.physics.friction,
            restitution = carObj.physics.restitution,
            isVehicleChassis = true
        )
        world.addBody(chassisBody)

        val wheels = level.objects.filter { it.type == ObjectType.WHEEL && it.wheel?.carBodyId == carObj.id }
        val wheelInstances = wheels.map { wObj ->
            val wCfg = wObj.wheel ?: WheelConfig()
            val initialLocalOffset = Vec2(wObj.x - carObj.x, wObj.y - carObj.y)
            val wBody = RigidBody(
                id = wObj.id,
                position = Vec2(wObj.x, wObj.y),
                rotation = wObj.rotation,
                width = wObj.width,
                height = wObj.height,
                mass = wObj.physics.density * 2.5f,
                friction = wObj.physics.friction
            )
            world.addBody(wBody)
            WheelInstance(
                id = wObj.id,
                config = wCfg,
                initialOffset = initialLocalOffset,
                body = wBody
            )
        }

        vehicleCtrl = VehicleController(
            chassis = chassisBody,
            carConfig = carObj.carBody ?: CarBodyConfig(),
            wheels = wheelInstances
        )
    }

    // 5. Coins & Finish Flag
    val coins = level.objects.filter { it.type == ObjectType.COIN }
    val finish = level.objects.find { it.type == ObjectType.FINISH_FLAG }

    return SimulationContext(
        physicsWorld = world,
        vehicleController = vehicleCtrl,
        terrainSegments = segments,
        staticBoxes = boxes,
        dynamicCoins = coins,
        finishFlag = finish
    )
}
