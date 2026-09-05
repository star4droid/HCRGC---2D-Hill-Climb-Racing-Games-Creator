package com.star4droid.hcrgc.model

data class Point2D(
    val x: Float,
    val y: Float
)

data class CustomShapeConfig(
    val points: List<Point2D> = listOf(
        Point2D(-80f, 0f),
        Point2D(-20f, -40f),
        Point2D(40f, -30f),
        Point2D(100f, 0f)
    ),
    val isClosed: Boolean = false, // false for open terrain path/road, true for closed polygon
    val surfaceColor: Long = 0xFF16A34A, // Green grass top
    val bodyColor: Long = 0xFF78350F, // Brown dirt underground
    val strokeWidth: Float = 16f,
    val isTransparent: Boolean = false // When true: invisible in gameplay, shown as dashed guide lines in editor
)

data class CarBodyConfig(
    val maxSpeed: Float = 35f,
    val acceleration: Float = 25f,
    val enginePower: Float = 45f,
    val airControl: Float = 8f,
    val stability: Float = 6f,
    val vehicleId: String = "car_1"
)

data class WheelConfig(
    val carBodyId: String? = null,
    val isFrontWheel: Boolean = false,
    val radius: Float = 24f,
    val suspensionFrequency: Float = 4.0f,
    val suspensionDamping: Float = 0.7f,
    val motorTorque: Float = 35f
)

data class TileMapConfig(
    val tileWidth: Float = 48f,
    val tileHeight: Float = 48f,
    val cols: Int = 30,
    val rows: Int = 12,
    val tiles: Map<String, String> = emptyMap(), // Key: "col_row", Value: tile asset ID
    val palette: List<String> = listOf("tile_grass", "tile_dirt", "tile_rock")
)

data class CoinConfig(
    val value: Int = 10,
    val collectedSound: String = "coin_pickup"
)

data class FinishFlagConfig(
    val targetStars: Int = 3,
    val timeForThreeStars: Float = 30f,
    val timeForTwoStars: Float = 60f
)

enum class LightType {
    POINT,
    CONE,
    DIRECTIONAL,
    CHAIN
}

data class LightConfig(
    val lightType: LightType = LightType.POINT,
    val distance: Float = 280f, // Radius or reach of light (px)
    val intensity: Float = 1.0f,
    val color: Long = 0xFFFFEA75,
    val direction: Float = 0f, // Direction angle in degrees (for CONE & DIRECTIONAL)
    val coneAngle: Float = 55f, // Spread arc in degrees (for CONE)
    val softnessLength: Float = 16f, // Soft edge factor
    val rays: Int = 64, // Quality / rays count (Box2D-lights)
    val isXray: Boolean = false, // If true, shines through obstacles without casting shadows
    val isStatic: Boolean = false,
    val attachToParent: Boolean = false // E.g. vehicle headlights
)

data class UITextConfig(
    val text: String = "Text",
    val fontSize: Float = 24f,
    val color: Long = 0xFFFFFFFF,
    val isCoinCounter: Boolean = false,
    val prefix: String = "Coins: ",
    val suffix: String = ""
)

enum class ButtonRole {
    ACCELERATE,
    BRAKE,
    TILT_LEFT,
    TILT_RIGHT,
    RESET
}

enum class ButtonAnimation {
    SCALE_DOWN,
    SCALE_UP,
    ALPHA
}

data class UIButtonConfig(
    val label: String = "GAS",
    val role: ButtonRole = ButtonRole.ACCELERATE,
    val targetVehicleId: String? = null,
    val pressAnimation: ButtonAnimation = ButtonAnimation.SCALE_DOWN
)

enum class ProgressBarType {
    DISTANCE_TO_FINISH,
    SPEED,
    FUEL
}

data class UIProgressBarConfig(
    val barType: ProgressBarType = ProgressBarType.DISTANCE_TO_FINISH,
    val color: Long = 0xFF22C55E,
    val backgroundColor: Long = 0x66000000
)

data class ReusableElement(
    val id: String,
    val name: String,
    val category: String, // "Vehicles", "Terrain", "Gameplay", "UI", "Decorations"
    val gameObject: GameObject,
    val children: List<GameObject> = emptyList()
)
