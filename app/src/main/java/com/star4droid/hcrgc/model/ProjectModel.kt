package com.star4droid.hcrgc.model

enum class ScreenOrientation {
    PORTRAIT,
    LANDSCAPE
}

enum class BackgroundScaleMode {
    STRETCH,
    FIT,
    FILL_CROP,
    CENTER
}

enum class FollowBehavior {
    SMOOTH_FOLLOW,
    FIXED,
    FREE
}

data class CameraSettings(
    val targetVehicleId: String? = null,
    val followBehavior: FollowBehavior = FollowBehavior.SMOOTH_FOLLOW,
    val followSmoothing: Float = 0.12f,
    val offsetX: Float = 0f,
    val offsetY: Float = -60f,
    val zoom: Float = 1.0f,
    val boundsMinX: Float? = null,
    val boundsMaxX: Float? = null,
    val boundsMinY: Float? = null,
    val boundsMaxY: Float? = null,
    val viewportWidth: Float = 1280f,
    val viewportHeight: Float = 720f
)

data class ProjectConfig(
    val id: String,
    val name: String,
    val orientation: ScreenOrientation = ScreenOrientation.LANDSCAPE,
    val gameWidth: Int = 1280,
    val gameHeight: Int = 720,
    val backgroundColor: Long = 0xFF87CEEB, // Sky blue default
    val backgroundImageAsset: String? = null,
    val backgroundScaleMode: BackgroundScaleMode = BackgroundScaleMode.FILL_CROP,
    val gravityX: Float = 0f,
    val gravityY: Float = 14f,
    val levels: List<String> = listOf("level_1"),
    val activeLevelId: String = "level_1",
    val ambientLightColor: Long = 0xFFFFFFFF,
    val ambientLightIntensity: Float = 1.0f // 1.0 = full daylight, 0.2 = dark night, 0.0 = pitch black
)

data class LevelData(
    val id: String,
    val name: String,
    val objects: List<GameObject> = emptyList(),
    val cameraSettings: CameraSettings = CameraSettings(),
    val ambientLightColor: Long = 0xFFFFFFFF,
    val ambientLightIntensity: Float = 1.0f
)
