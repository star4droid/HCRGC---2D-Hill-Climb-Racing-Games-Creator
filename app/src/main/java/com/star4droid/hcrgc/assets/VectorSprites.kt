package com.star4droid.hcrgc.assets

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.cos
import kotlin.math.sin

object VectorSprites {

    const val ASSET_CAR_BUGGY = "asset_car_buggy"
    const val ASSET_CAR_TRUCK = "asset_car_truck"
    const val ASSET_WHEEL_RUGGED = "asset_wheel_rugged"
    const val ASSET_WHEEL_RACING = "asset_wheel_racing"
    const val ASSET_COIN_GOLD = "asset_coin_gold"
    const val ASSET_FINISH_FLAG = "asset_finish_flag"
    const val ASSET_LANTERN = "asset_lantern"
    const val ASSET_TILE_GRASS = "tile_grass"
    const val ASSET_TILE_DIRT = "tile_dirt"
    const val ASSET_TILE_ROCK = "tile_rock"
    const val ASSET_CRATE = "asset_crate"

    val DEFAULT_ASSETS = listOf(
        ASSET_CAR_BUGGY to "Buggy Chassis",
        ASSET_CAR_TRUCK to "Truck Chassis",
        ASSET_WHEEL_RUGGED to "Rugged Wheel",
        ASSET_WHEEL_RACING to "Racing Wheel",
        ASSET_COIN_GOLD to "Gold Coin",
        ASSET_FINISH_FLAG to "Finish Flag",
        ASSET_LANTERN to "Hanging Light",
        ASSET_TILE_GRASS to "Grass Top Tile",
        ASSET_TILE_DIRT to "Dirt Tile",
        ASSET_TILE_ROCK to "Stone Tile",
        ASSET_CRATE to "Wood Crate"
    )

    fun drawAsset(
        drawScope: DrawScope,
        assetId: String?,
        width: Float,
        height: Float,
        tint: Color = Color.White
    ) {
        with(drawScope) {
            when (assetId) {
                ASSET_CAR_BUGGY -> drawBuggyChassis(width, height, tint)
                ASSET_CAR_TRUCK -> drawTruckChassis(width, height, tint)
                ASSET_WHEEL_RUGGED -> drawRuggedWheel(width, height)
                ASSET_WHEEL_RACING -> drawRacingWheel(width, height)
                ASSET_COIN_GOLD -> drawGoldCoin(width, height)
                ASSET_FINISH_FLAG -> drawFinishFlag(width, height)
                ASSET_LANTERN -> drawLantern(width, height)
                ASSET_TILE_GRASS -> drawGrassTile(width, height)
                ASSET_TILE_DIRT -> drawDirtTile(width, height)
                ASSET_TILE_ROCK -> drawRockTile(width, height)
                ASSET_CRATE -> drawWoodCrate(width, height)
                else -> {
                    // Default fallback rect
                    drawRoundRect(
                        color = tint,
                        size = Size(width, height),
                        cornerRadius = CornerRadius(8f, 8f)
                    )
                }
            }
        }
    }

    private fun DrawScope.drawBuggyChassis(w: Float, h: Float, tint: Color) {
        val primaryColor = if (tint != Color.White) tint else Color(0xFFF97316) // Energetic Orange
        val secondaryColor = Color(0xFF0284C7) // Sky blue roll cage
        val darkMetal = Color(0xFF1E293B)

        // Main body shell path
        val bodyPath = Path().apply {
            moveTo(w * 0.05f, h * 0.75f)
            lineTo(w * 0.02f, h * 0.50f)
            lineTo(w * 0.20f, h * 0.40f)
            lineTo(w * 0.40f, h * 0.15f)
            lineTo(w * 0.70f, h * 0.15f)
            lineTo(w * 0.95f, h * 0.55f)
            lineTo(w * 0.98f, h * 0.75f)
            lineTo(w * 0.85f, h * 0.75f)
            // Front wheel cutout arch
            cubicTo(w * 0.85f, h * 0.55f, w * 0.65f, h * 0.55f, w * 0.65f, h * 0.75f)
            lineTo(w * 0.35f, h * 0.75f)
            // Rear wheel cutout arch
            cubicTo(w * 0.35f, h * 0.55f, w * 0.15f, h * 0.55f, w * 0.15f, h * 0.75f)
            close()
        }
        drawPath(bodyPath, primaryColor)

        // Roll cage / Cockpit frame
        val cagePath = Path().apply {
            moveTo(w * 0.25f, h * 0.40f)
            lineTo(w * 0.42f, h * 0.18f)
            lineTo(w * 0.68f, h * 0.18f)
            lineTo(w * 0.78f, h * 0.45f)
            close()
        }
        drawPath(cagePath, secondaryColor.copy(alpha = 0.5f))
        drawPath(cagePath, darkMetal, style = Stroke(width = 3f))

        // Windshield strut
        drawLine(darkMetal, Offset(w * 0.42f, h * 0.18f), Offset(w * 0.48f, h * 0.45f), strokeWidth = 2.5f)

        // Spoiler
        drawRoundRect(
            color = darkMetal,
            topLeft = Offset(w * 0.02f, h * 0.28f),
            size = Size(w * 0.16f, h * 0.08f),
            cornerRadius = CornerRadius(2f, 2f)
        )
        drawLine(darkMetal, Offset(w * 0.08f, h * 0.36f), Offset(w * 0.10f, h * 0.48f), strokeWidth = 3f)

        // Headlight
        drawCircle(
            color = Color(0xFFFDE047),
            radius = h * 0.09f,
            center = Offset(w * 0.94f, h * 0.52f)
        )
    }

    private fun DrawScope.drawTruckChassis(w: Float, h: Float, tint: Color) {
        val primaryColor = if (tint != Color.White) tint else Color(0xFFDC2626) // Crimson Red
        val darkMetal = Color(0xFF1E293B)

        // Truck Cabin & Bed
        val truckPath = Path().apply {
            moveTo(w * 0.05f, h * 0.75f)
            lineTo(w * 0.05f, h * 0.35f)
            lineTo(w * 0.45f, h * 0.35f)
            lineTo(w * 0.50f, h * 0.12f)
            lineTo(w * 0.82f, h * 0.12f)
            lineTo(w * 0.95f, h * 0.45f)
            lineTo(w * 0.95f, h * 0.75f)
            close()
        }
        drawPath(truckPath, primaryColor)

        // Window
        val window = Path().apply {
            moveTo(w * 0.55f, h * 0.38f)
            lineTo(w * 0.55f, h * 0.18f)
            lineTo(w * 0.78f, h * 0.18f)
            lineTo(w * 0.88f, h * 0.38f)
            close()
        }
        drawPath(window, Color(0xFF38BDF8).copy(alpha = 0.8f))
        drawPath(window, darkMetal, style = Stroke(width = 2f))

        // Bumper
        drawRoundRect(
            color = darkMetal,
            topLeft = Offset(w * 0.92f, h * 0.65f),
            size = Size(w * 0.08f, h * 0.14f),
            cornerRadius = CornerRadius(3f, 3f)
        )
    }

    private fun DrawScope.drawRuggedWheel(w: Float, h: Float) {
        val radius = w.coerceAtMost(h) / 2f
        val center = Offset(w / 2f, h / 2f)

        // Outer rubber tire
        drawCircle(color = Color(0xFF1E293B), radius = radius, center = center)

        // Tire treads (notches around perimeter)
        val numTreads = 12
        for (i in 0 until numTreads) {
            val angle = (i * 360f / numTreads) * (Math.PI / 180f).toFloat()
            val tx = center.x + cos(angle) * (radius - 3f)
            val ty = center.y + sin(angle) * (radius - 3f)
            drawCircle(color = Color(0xFF0F172A), radius = radius * 0.15f, center = Offset(tx, ty))
        }

        // Inner rim
        drawCircle(color = Color(0xFFE2E8F0), radius = radius * 0.62f, center = center)
        drawCircle(color = Color(0xFFF97316), radius = radius * 0.50f, center = center)

        // Rim spokes
        for (i in 0 until 5) {
            val angle = (i * 72f) * (Math.PI / 180f).toFloat()
            val sx = center.x + cos(angle) * (radius * 0.48f)
            val sy = center.y + sin(angle) * (radius * 0.48f)
            drawLine(Color(0xFF0F172A), center, Offset(sx, sy), strokeWidth = 3f)
        }

        // Center hub bolt
        drawCircle(color = Color(0xFF1E293B), radius = radius * 0.18f, center = center)
        drawCircle(color = Color(0xFFFFFFFF), radius = radius * 0.08f, center = center)
    }

    private fun DrawScope.drawRacingWheel(w: Float, h: Float) {
        val radius = w.coerceAtMost(h) / 2f
        val center = Offset(w / 2f, h / 2f)

        // Sleek rubber tire
        drawCircle(color = Color(0xFF111827), radius = radius, center = center)
        drawCircle(color = Color(0xFF374151), radius = radius * 0.85f, center = center, style = Stroke(width = 2f))

        // Chrome rim
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFFFFFF), Color(0xFF94A3B8), Color(0xFF475569)),
                center = center,
                radius = radius * 0.65f
            ),
            radius = radius * 0.65f,
            center = center
        )

        // Center lug
        drawCircle(color = Color(0xFFDC2626), radius = radius * 0.20f, center = center)
        drawCircle(color = Color(0xFFFEF2F2), radius = radius * 0.08f, center = center)
    }

    private fun DrawScope.drawGoldCoin(w: Float, h: Float) {
        val radius = w.coerceAtMost(h) / 2f
        val center = Offset(w / 2f, h / 2f)

        // Golden gradient
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFEF08A), Color(0xFFFBBF24), Color(0xFFD97706)),
                center = center,
                radius = radius
            ),
            radius = radius,
            center = center
        )

        // Inner rim border
        drawCircle(
            color = Color(0xFFB45309),
            radius = radius * 0.82f,
            center = center,
            style = Stroke(width = 2.5f)
        )

        // Star symbol in center
        val starPath = Path().apply {
            val innerR = radius * 0.25f
            val outerR = radius * 0.55f
            for (i in 0 until 5) {
                val outerAngle = (i * 72f - 90f) * (Math.PI / 180f).toFloat()
                val innerAngle = (i * 72f + 36f - 90f) * (Math.PI / 180f).toFloat()
                val ox = center.x + cos(outerAngle) * outerR
                val oy = center.y + sin(outerAngle) * outerR
                val ix = center.x + cos(innerAngle) * innerR
                val iy = center.y + sin(innerAngle) * innerR
                if (i == 0) moveTo(ox, oy) else lineTo(ox, oy)
                lineTo(ix, iy)
            }
            close()
        }
        drawPath(starPath, Color(0xFFFFFBEB))
    }

    private fun DrawScope.drawFinishFlag(w: Float, h: Float) {
        val poleX = w * 0.2f
        // Flag Pole
        drawLine(
            color = Color(0xFF334155),
            start = Offset(poleX, h * 0.05f),
            end = Offset(poleX, h * 0.95f),
            strokeWidth = 6f,
            cap = StrokeCap.Round
        )
        // Pole top ball
        drawCircle(color = Color(0xFFF59E0B), radius = 6f, center = Offset(poleX, h * 0.05f))

        // Checkered Flag Waving
        val flagW = w * 0.75f
        val flagH = h * 0.45f
        val rows = 4
        val cols = 6
        val cellW = flagW / cols
        val cellH = flagH / rows

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val isBlack = (r + c) % 2 == 0
                val color = if (isBlack) Color(0xFF0F172A) else Color(0xFFF8FAFC)
                drawRect(
                    color = color,
                    topLeft = Offset(poleX + c * cellW, h * 0.10f + r * cellH),
                    size = Size(cellW, cellH)
                )
            }
        }
    }

    private fun DrawScope.drawLantern(w: Float, h: Float) {
        val center = Offset(w / 2f, h / 2f)
        // Chain/mount
        drawLine(Color(0xFF475569), Offset(w / 2f, 0f), Offset(w / 2f, h * 0.3f), strokeWidth = 3f)
        // Lantern cap
        val capPath = Path().apply {
            moveTo(w * 0.2f, h * 0.35f)
            lineTo(w * 0.8f, h * 0.35f)
            lineTo(w * 0.65f, h * 0.25f)
            lineTo(w * 0.35f, h * 0.25f)
            close()
        }
        drawPath(capPath, Color(0xFF1E293B))

        // Glowing glass
        drawRoundRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFEF08A), Color(0xFFF59E0B)),
                center = center,
                radius = w * 0.3f
            ),
            topLeft = Offset(w * 0.25f, h * 0.35f),
            size = Size(w * 0.5f, h * 0.45f),
            cornerRadius = CornerRadius(4f, 4f)
        )
    }

    fun DrawScope.drawGrassTile(w: Float, h: Float) {
        // Dirt base
        drawRect(Color(0xFF78350F), topLeft = Offset.Zero, size = Size(w, h))
        // Dirt pebble textures
        drawCircle(Color(0xFF5A2508), radius = w * 0.08f, center = Offset(w * 0.3f, h * 0.6f))
        drawCircle(Color(0xFF5A2508), radius = w * 0.06f, center = Offset(w * 0.75f, h * 0.75f))
        // Top lush grass layer
        val grassH = h * 0.35f
        drawRect(Color(0xFF16A34A), topLeft = Offset.Zero, size = Size(w, grassH))
        // Grass jagged blades
        val bladePath = Path().apply {
            moveTo(0f, grassH)
            lineTo(w * 0.2f, grassH + h * 0.12f)
            lineTo(w * 0.4f, grassH)
            lineTo(w * 0.65f, grassH + h * 0.15f)
            lineTo(w * 0.85f, grassH)
            lineTo(w, grassH + h * 0.08f)
            lineTo(w, 0f)
            lineTo(0f, 0f)
            close()
        }
        drawPath(bladePath, Color(0xFF22C55E))
    }

    fun DrawScope.drawDirtTile(w: Float, h: Float) {
        drawRect(Color(0xFF78350F), topLeft = Offset.Zero, size = Size(w, h))
        drawCircle(Color(0xFF92400E), radius = w * 0.12f, center = Offset(w * 0.25f, h * 0.3f))
        drawCircle(Color(0xFF5A2508), radius = w * 0.09f, center = Offset(w * 0.7f, h * 0.4f))
        drawCircle(Color(0xFF92400E), radius = w * 0.14f, center = Offset(w * 0.5f, h * 0.75f))
    }

    fun DrawScope.drawRockTile(w: Float, h: Float) {
        drawRect(Color(0xFF475569), topLeft = Offset.Zero, size = Size(w, h))
        val crack = Path().apply {
            moveTo(w * 0.1f, h * 0.2f)
            lineTo(w * 0.45f, h * 0.4f)
            lineTo(w * 0.6f, h * 0.7f)
            lineTo(w * 0.9f, h * 0.85f)
        }
        drawPath(crack, Color(0xFF1E293B), style = Stroke(width = 2.5f))
        drawCircle(Color(0xFF64748B), radius = w * 0.15f, center = Offset(w * 0.3f, h * 0.75f))
    }

    private fun DrawScope.drawWoodCrate(w: Float, h: Float) {
        drawRoundRect(Color(0xFFB45309), topLeft = Offset.Zero, size = Size(w, h), cornerRadius = CornerRadius(4f, 4f))
        drawRect(
            Color(0xFF78350F),
            topLeft = Offset(4f, 4f),
            size = Size(w - 8f, h - 8f),
            style = Stroke(width = 4f)
        )
        // Cross braces
        drawLine(Color(0xFF78350F), Offset(4f, 4f), Offset(w - 4f, h - 4f), strokeWidth = 3.5f)
        drawLine(Color(0xFF78350F), Offset(w - 4f, 4f), Offset(4f, h - 4f), strokeWidth = 3.5f)
    }
}
