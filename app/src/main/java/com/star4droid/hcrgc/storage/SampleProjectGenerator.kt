package com.star4droid.hcrgc.storage

import com.star4droid.hcrgc.assets.VectorSprites
import com.star4droid.hcrgc.model.*

object SampleProjectGenerator {

    fun createSampleProject(): Pair<ProjectConfig, LevelData> {
        val projectId = "project_mountain_hills"
        val project = ProjectConfig(
            id = projectId,
            name = "Mountain Hills",
            orientation = ScreenOrientation.LANDSCAPE,
            gameWidth = 1280,
            gameHeight = 720,
            backgroundColor = 0xFF7DD3FC, // Crisp Sky Blue
            gravityX = 0f,
            gravityY = 15f,
            levels = listOf("level_1"),
            activeLevelId = "level_1"
        )

        val terrainPoints = listOf(
            Point2D(-200f, 480f),
            Point2D(0f, 480f),
            Point2D(250f, 480f),
            Point2D(450f, 420f),
            Point2D(650f, 320f), // Hill crest 1
            Point2D(820f, 440f),
            Point2D(1000f, 480f),
            Point2D(1250f, 350f), // Ramp 2
            Point2D(1450f, 220f), // High peak
            Point2D(1650f, 390f),
            Point2D(1850f, 460f),
            Point2D(2050f, 380f), // Finish summit
            Point2D(2250f, 380f),
            Point2D(2500f, 500f)
        )

        val terrainObject = GameObject(
            id = "terrain_main",
            name = "Mountain Track",
            type = ObjectType.CUSTOM_SHAPE,
            x = 0f,
            y = 0f,
            width = 2700f,
            height = 600f,
            zIndex = 1,
            physics = PhysicsProperties(
                bodyType = BodyType.STATIC,
                friction = 0.85f,
                restitution = 0.05f
            ),
            customShape = CustomShapeConfig(
                points = terrainPoints,
                isClosed = false
            ),
            tintColor = 0xFF16A34A // Lush green
        )

        // Car Buggy Chassis
        val carBody = GameObject(
            id = "buggy_chassis",
            name = "Hill Buggy",
            type = ObjectType.CAR_BODY,
            x = 130f,
            y = 390f,
            width = 130f,
            height = 55f,
            zIndex = 10,
            imageAsset = VectorSprites.ASSET_CAR_BUGGY,
            physics = PhysicsProperties(
                bodyType = BodyType.DYNAMIC,
                density = 1.3f,
                friction = 0.4f,
                restitution = 0.15f
            ),
            carBody = CarBodyConfig(
                maxSpeed = 38f,
                acceleration = 28f,
                enginePower = 48f,
                airControl = 9f,
                stability = 6.5f,
                vehicleId = "buggy_chassis"
            )
        )

        // Rear Wheel
        val rearWheel = GameObject(
            id = "wheel_rear",
            name = "Rear Wheel",
            type = ObjectType.WHEEL,
            x = 90f,
            y = 430f,
            width = 46f,
            height = 46f,
            zIndex = 11,
            imageAsset = VectorSprites.ASSET_WHEEL_RUGGED,
            physics = PhysicsProperties(
                bodyType = BodyType.DYNAMIC,
                density = 1.6f,
                friction = 1.0f,
                restitution = 0.1f
            ),
            wheel = WheelConfig(
                carBodyId = "buggy_chassis",
                isFrontWheel = false,
                radius = 23f,
                suspensionFrequency = 4.5f,
                suspensionDamping = 0.75f,
                motorTorque = 42f
            )
        )

        // Front Wheel
        val frontWheel = GameObject(
            id = "wheel_front",
            name = "Front Wheel",
            type = ObjectType.WHEEL,
            x = 170f,
            y = 430f,
            width = 46f,
            height = 46f,
            zIndex = 11,
            imageAsset = VectorSprites.ASSET_WHEEL_RUGGED,
            physics = PhysicsProperties(
                bodyType = BodyType.DYNAMIC,
                density = 1.6f,
                friction = 1.0f,
                restitution = 0.1f
            ),
            wheel = WheelConfig(
                carBodyId = "buggy_chassis",
                isFrontWheel = true,
                radius = 23f,
                suspensionFrequency = 4.5f,
                suspensionDamping = 0.75f,
                motorTorque = 38f
            )
        )

        // Non-physical Element attached to vehicle
        val vehicleLabel = GameObject(
            id = "elem_car_label",
            name = "Vehicle Badge",
            type = ObjectType.ELEMENT,
            x = 130f,
            y = 350f,
            width = 60f,
            height = 20f,
            zIndex = 12,
            parentId = "buggy_chassis",
            physics = PhysicsProperties(bodyType = BodyType.NONE),
            uiText = UITextConfig(
                text = "HCR-1",
                fontSize = 13f,
                color = 0xFFFFFFFF
            )
        )

        // Coins along the trail
        val coinPositions = listOf(
            Pair(400f, 380f),
            Pair(650f, 250f),
            Pair(900f, 390f),
            Pair(1300f, 280f),
            Pair(1450f, 150f),
            Pair(1750f, 380f)
        )

        val coins = coinPositions.mapIndexed { idx, pos ->
            GameObject(
                id = "coin_${idx + 1}",
                name = "Coin ${idx + 1}",
                type = ObjectType.COIN,
                x = pos.first,
                y = pos.second,
                width = 34f,
                height = 34f,
                zIndex = 5,
                imageAsset = VectorSprites.ASSET_COIN_GOLD,
                physics = PhysicsProperties(
                    bodyType = BodyType.STATIC,
                    isSensor = true
                ),
                coin = CoinConfig(value = 10)
            )
        }

        // Lantern Light
        val lightObj = GameObject(
            id = "lantern_ramp",
            name = "Ramp Lantern",
            type = ObjectType.LIGHT,
            x = 1250f,
            y = 260f,
            width = 40f,
            height = 50f,
            zIndex = 4,
            imageAsset = VectorSprites.ASSET_LANTERN,
            physics = PhysicsProperties(bodyType = BodyType.STATIC, isSensor = true),
            light = LightConfig(
                lightType = LightType.POINT,
                distance = 240f,
                intensity = 0.9f,
                color = 0xFFFDE047
            )
        )

        // Finish Flag
        val finishFlag = GameObject(
            id = "finish_flag",
            name = "Finish Line",
            type = ObjectType.FINISH_FLAG,
            x = 2150f,
            y = 310f,
            width = 70f,
            height = 110f,
            zIndex = 6,
            imageAsset = VectorSprites.ASSET_FINISH_FLAG,
            physics = PhysicsProperties(
                bodyType = BodyType.STATIC,
                isSensor = true
            ),
            finishFlag = FinishFlagConfig(targetStars = 3)
        )

        // TileMap Platform at start
        val startTileMap = GameObject(
            id = "tilemap_depot",
            name = "Start Depot",
            type = ObjectType.TILEMAP,
            x = -150f,
            y = 480f,
            width = 240f,
            height = 96f,
            zIndex = 2,
            physics = PhysicsProperties(bodyType = BodyType.STATIC),
            tileMap = TileMapConfig(
                tileWidth = 48f,
                tileHeight = 48f,
                cols = 5,
                rows = 2,
                tiles = mapOf(
                    "0_0" to "tile_grass", "1_0" to "tile_grass", "2_0" to "tile_grass", "3_0" to "tile_grass", "4_0" to "tile_grass",
                    "0_1" to "tile_dirt", "1_1" to "tile_dirt", "2_1" to "tile_dirt", "3_1" to "tile_dirt", "4_1" to "tile_dirt"
                )
            )
        )

        // UI Objects: On-Screen Controls & HUD
        val gasBtn = GameObject(
            id = "ui_btn_gas",
            name = "Gas Pedal",
            type = ObjectType.UI_BUTTON,
            x = 1150f,
            y = 610f,
            width = 110f,
            height = 70f,
            zIndex = 100,
            uiButton = UIButtonConfig(
                label = "GAS",
                role = ButtonRole.ACCELERATE,
                targetVehicleId = "buggy_chassis",
                pressAnimation = ButtonAnimation.SCALE_DOWN
            )
        )

        val brakeBtn = GameObject(
            id = "ui_btn_brake",
            name = "Brake Pedal",
            type = ObjectType.UI_BUTTON,
            x = 130f,
            y = 610f,
            width = 110f,
            height = 70f,
            zIndex = 100,
            uiButton = UIButtonConfig(
                label = "BRAKE",
                role = ButtonRole.BRAKE,
                targetVehicleId = "buggy_chassis",
                pressAnimation = ButtonAnimation.SCALE_DOWN
            )
        )

        val tiltLeftBtn = GameObject(
            id = "ui_btn_tilt_left",
            name = "Tilt Left",
            type = ObjectType.UI_BUTTON,
            x = 260f,
            y = 620f,
            width = 75f,
            height = 55f,
            zIndex = 100,
            uiButton = UIButtonConfig(
                label = "⟲ TILT",
                role = ButtonRole.TILT_LEFT,
                targetVehicleId = "buggy_chassis"
            )
        )

        val tiltRightBtn = GameObject(
            id = "ui_btn_tilt_right",
            name = "Tilt Right",
            type = ObjectType.UI_BUTTON,
            x = 1020f,
            y = 620f,
            width = 75f,
            height = 55f,
            zIndex = 100,
            uiButton = UIButtonConfig(
                label = "TILT ⟳",
                role = ButtonRole.TILT_RIGHT,
                targetVehicleId = "buggy_chassis"
            )
        )

        val coinCounter = GameObject(
            id = "ui_coin_counter",
            name = "Coin Counter",
            type = ObjectType.UI_TEXT,
            x = 100f,
            y = 50f,
            width = 140f,
            height = 40f,
            zIndex = 100,
            uiText = UITextConfig(
                text = "0",
                fontSize = 20f,
                color = 0xFFFBBF24,
                isCoinCounter = true,
                prefix = "Coins: "
            )
        )

        val progressBar = GameObject(
            id = "ui_progress_bar",
            name = "Track Progress",
            type = ObjectType.UI_PROGRESS_BAR,
            x = 640f,
            y = 40f,
            width = 320f,
            height = 18f,
            zIndex = 100,
            uiProgressBar = UIProgressBarConfig(
                barType = ProgressBarType.DISTANCE_TO_FINISH,
                color = 0xFF22C55E
            )
        )

        val level = LevelData(
            id = "level_1",
            name = "Alpine Summit",
            objects = listOf(
                terrainObject,
                startTileMap,
                carBody,
                rearWheel,
                frontWheel,
                vehicleLabel,
                lightObj,
                finishFlag,
                gasBtn,
                brakeBtn,
                tiltLeftBtn,
                tiltRightBtn,
                coinCounter,
                progressBar
            ) + coins,
            cameraSettings = CameraSettings(
                targetVehicleId = "buggy_chassis",
                followBehavior = FollowBehavior.SMOOTH_FOLLOW,
                followSmoothing = 0.14f,
                offsetX = 120f,
                offsetY = -50f,
                zoom = 1.0f,
                boundsMinX = -250f,
                boundsMaxX = 2600f,
                viewportWidth = 1280f,
                viewportHeight = 720f
            ),
            ambientLightColor = 0xFFFFFFFF,
            ambientLightIntensity = 1.0f
        )

        return Pair(project, level)
    }
}
