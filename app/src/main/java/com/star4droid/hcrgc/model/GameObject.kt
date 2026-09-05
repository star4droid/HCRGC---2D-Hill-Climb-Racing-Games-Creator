package com.star4droid.hcrgc.model

import java.util.UUID

enum class ObjectType {
    // Physical Objects
    BOX,
    CIRCLE,
    CUSTOM_SHAPE,

    // Game Objects
    CAR_BODY,
    WHEEL,
    COIN,
    FINISH_FLAG,
    LIGHT,
    TILEMAP,

    // UI Objects
    UI_TEXT,
    UI_BUTTON,
    UI_PROGRESS_BAR,

    // Non-physical element (no physics body, decorative or attached)
    ELEMENT
}

enum class BodyType {
    DYNAMIC,
    STATIC,
    KINEMATIC,
    NONE // For ELEMENT or non-physical items
}

data class PhysicsProperties(
    val bodyType: BodyType = BodyType.STATIC,
    val density: Float = 1.0f,
    val friction: Float = 0.6f,
    val restitution: Float = 0.2f,
    val isBullet: Boolean = false,
    val fixedRotation: Boolean = false,
    val active: Boolean = true,
    val isSensor: Boolean = false
)

data class GameObject(
    val id: String = UUID.randomUUID().toString().take(8),
    val name: String = "Object",
    val type: ObjectType = ObjectType.BOX,
    val x: Float = 0f,
    val y: Float = 0f,
    val rotation: Float = 0f, // in degrees
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val width: Float = 80f,
    val height: Float = 80f,
    val zIndex: Int = 0,
    val visible: Boolean = true,
    val parentId: String? = null,
    val groupId: String? = null,

    // Visual image & styling
    val imageAsset: String? = null,
    val imageOffsetX: Float = 0f,
    val imageOffsetY: Float = 0f,
    val imageWidth: Float = 0f, // 0 = match object width
    val imageHeight: Float = 0f, // 0 = match object height
    val tintColor: Long = 0xFFFFFFFF,

    // Physics
    val physics: PhysicsProperties = PhysicsProperties(),

    // Specific configurations
    val customShape: CustomShapeConfig? = null,
    val carBody: CarBodyConfig? = null,
    val wheel: WheelConfig? = null,
    val tileMap: TileMapConfig? = null,
    val coin: CoinConfig? = null,
    val finishFlag: FinishFlagConfig? = null,
    val light: LightConfig? = null,
    val uiText: UITextConfig? = null,
    val uiButton: UIButtonConfig? = null,
    val uiProgressBar: UIProgressBarConfig? = null
) {
    val isPhysical: Boolean
        get() = type != ObjectType.ELEMENT &&
                type != ObjectType.UI_TEXT &&
                type != ObjectType.UI_BUTTON &&
                type != ObjectType.UI_PROGRESS_BAR &&
                physics.bodyType != BodyType.NONE

    val effectiveImageWidth: Float
        get() = if (imageWidth > 0f) imageWidth else width

    val effectiveImageHeight: Float
        get() = if (imageHeight > 0f) imageHeight else height
}
