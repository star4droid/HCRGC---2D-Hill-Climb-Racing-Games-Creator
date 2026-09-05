package com.star4droid.hcrgc.physics

import com.star4droid.hcrgc.model.CarBodyConfig
import com.star4droid.hcrgc.model.WheelConfig
import kotlin.math.*

class WheelInstance(
    val id: String,
    val config: WheelConfig,
    val initialOffset: Vec2, // Offset from car center in local coordinates
    val body: RigidBody,
    var isGrounded: Boolean = false,
    var suspensionCompression: Float = 0f
)

class VehicleController(
    val chassis: RigidBody,
    val carConfig: CarBodyConfig,
    val wheels: List<WheelInstance>
) {
    var throttle: Float = 0f // -1 (brake/reverse) to +1 (accelerate)
    var airTilt: Float = 0f // -1 (tilt left) to +1 (tilt right)

    fun update(
        dt: Float,
        terrainSegments: List<Pair<Vec2, Vec2>>,
        staticBoxes: List<FloatArray> // [left, top, right, bottom]
    ) {
        val rad = chassis.rotation * (Math.PI / 180f).toFloat()
        val cosRot = cos(rad)
        val sinRot = sin(rad)

        var anyWheelGrounded = false
        var groundTractionCount = 0

        for (wheel in wheels) {
            // Anchor point in world space
            val anchorX = chassis.position.x + (wheel.initialOffset.x * cosRot - wheel.initialOffset.y * sinRot)
            val anchorY = chassis.position.y + (wheel.initialOffset.x * sinRot + wheel.initialOffset.y * cosRot)

            val wheelRadius = wheel.config.radius
            val restLength = wheelRadius * 0.95f

            // 1. Check terrain contact
            val terrainContact = CollisionHelper.checkPointAgainstTerrain(
                anchorX, anchorY, terrainSegments, proximityThreshold = restLength * 1.8f
            )

            // 2. Check static box contact
            var boxContact: TerrainContact? = null
            for (box in staticBoxes) {
                val bLeft = box[0]
                val bTop = box[1]
                val bRight = box[2]
                val bBottom = box[3]

                val nearestX = anchorX.coerceIn(bLeft, bRight)
                val nearestY = anchorY.coerceIn(bTop, bBottom)
                val dist = hypot(anchorX - nearestX, anchorY - nearestY)

                if (dist < restLength * 1.5f) {
                    val diffX = anchorX - nearestX
                    val diffY = anchorY - nearestY
                    val norm = if (dist > 0.001f) Vec2(diffX / dist, diffY / dist) else Vec2(0f, -1f)
                    val penetration = (restLength - dist)
                    boxContact = TerrainContact(
                        isContact = true,
                        penetration = penetration,
                        surfacePoint = Vec2(nearestX, nearestY),
                        normal = norm,
                        tangent = Vec2(-norm.y, norm.x),
                        slopeDegrees = 0f
                    )
                    break
                }
            }

            val contact = terrainContact ?: boxContact

            if (contact != null && contact.penetration > -restLength * 1.35f) {
                wheel.isGrounded = true
                anyWheelGrounded = true
                groundTractionCount++

                val compression = (contact.penetration + restLength).coerceAtLeast(0f)
                wheel.suspensionCompression = compression

                // Smooth spring force (no violent bouncing)
                val springK = wheel.config.suspensionFrequency * 2200f
                val springForceMag = compression * springK

                // Damping along contact normal
                val relVel = chassis.velocity.dot(contact.normal)
                val damping = wheel.config.suspensionDamping * 160f * relVel
                val totalNormalForce = (springForceMag - damping).coerceAtLeast(0f)

                val normalForce = contact.normal * totalNormalForce

                // Apply linear force to chassis
                chassis.applyForce(normalForce, dt)

                // Rotational torque to chassis from wheel position
                val rx = anchorX - chassis.position.x
                val ry = anchorY - chassis.position.y
                val torque = rx * normalForce.y - ry * normalForce.x
                chassis.applyTorque(torque * 0.85f, dt)

                // Smooth anti-penetration resolution if wheel is deep inside terrain
                if (contact.penetration > 2f) {
                    val pushAmount = min(contact.penetration * 0.25f, 6f)
                    chassis.position.x += contact.normal.x * pushAmount
                    chassis.position.y += contact.normal.y * pushAmount
                }

                // Surface tangent & propulsion
                val forwardTangent = if (contact.tangent.x >= 0f) contact.tangent else Vec2(-contact.tangent.x, -contact.tangent.y)
                val tangentVel = chassis.velocity.dot(forwardTangent)
                val driveScale = carConfig.enginePower * 2800f

                if (throttle > 0f) {
                    // GAS: Continuous forward acceleration up to max speed
                    if (tangentVel < 1000f) {
                        // Uphill torque boost: if climbing, provide extra torque so car doesn't get stuck
                        val uphillFactor = if (forwardTangent.y < -0.1f) 1.5f else 1.0f
                        val driveForce = forwardTangent * (throttle * driveScale * uphillFactor)
                        chassis.applyForce(driveForce, dt)
                    }
                } else if (throttle < 0f) {
                    // BRAKE / REVERSE
                    if (tangentVel > 20f) {
                        // Strong active braking when moving forward
                        val brakeForce = forwardTangent * (throttle * driveScale * 2.2f)
                        chassis.applyForce(brakeForce, dt)
                    } else if (tangentVel > -400f) {
                        // Reverse drive when stopped or moving backward
                        val reverseForce = forwardTangent * (throttle * driveScale * 0.75f)
                        chassis.applyForce(reverseForce, dt)
                    }
                } else {
                    // Neutral / Coasting: slight rolling friction
                    if (abs(tangentVel) > 5f) {
                        val rollFriction = forwardTangent * (-sign(tangentVel) * 150f)
                        chassis.applyForce(rollFriction, dt)
                    }
                }

                // WHEEL PHYSICAL ROLLING ROTATION:
                wheel.body.angularVelocity = (tangentVel / wheelRadius) * (180f / Math.PI.toFloat())

                // Position wheel visually along suspension travel
                val extension = (restLength - compression).coerceAtLeast(0f)
                wheel.body.position.x = anchorX - contact.normal.x * extension
                wheel.body.position.y = anchorY - contact.normal.y * extension
            } else {
                wheel.isGrounded = false
                wheel.suspensionCompression = 0f

                // Wheel follows suspension extended in air
                val uncompressedX = anchorX - (restLength * sinRot)
                val uncompressedY = anchorY + (restLength * cosRot)
                wheel.body.position.x = uncompressedX
                wheel.body.position.y = uncompressedY
                wheel.body.velocity = chassis.velocity

                // Spin wheel freely with throttle input
                if (throttle != 0f) {
                    wheel.body.angularVelocity += throttle * 550f * dt
                }
            }

            // Integrate wheel rotation
            wheel.body.rotation += wheel.body.angularVelocity * dt
        }

        // 2. Chassis Bumper Collisions with terrain
        val bumperHalfW = 45f
        val frontBumperX = chassis.position.x + bumperHalfW * cosRot
        val frontBumperY = chassis.position.y + bumperHalfW * sinRot + 8f
        val frontBumperContact = CollisionHelper.checkPointAgainstTerrain(frontBumperX, frontBumperY, terrainSegments, 12f)
        if (frontBumperContact != null && frontBumperContact.penetration > 0f) {
            chassis.position.y += frontBumperContact.normal.y * min(frontBumperContact.penetration * 0.3f, 5f)
            chassis.applyTorque(-6000f, dt)
        }

        val rearBumperX = chassis.position.x - bumperHalfW * cosRot
        val rearBumperY = chassis.position.y - bumperHalfW * sinRot + 8f
        val rearBumperContact = CollisionHelper.checkPointAgainstTerrain(rearBumperX, rearBumperY, terrainSegments, 12f)
        if (rearBumperContact != null && rearBumperContact.penetration > 0f) {
            chassis.position.y += rearBumperContact.normal.y * min(rearBumperContact.penetration * 0.3f, 5f)
            chassis.applyTorque(6000f, dt)
        }

        // 3. Chassis Collisions with Static Boxes (Walls, hurdles, platforms)
        val cHalfW = chassis.width / 2f
        val cHalfH = chassis.height / 2f
        for (box in staticBoxes) {
            val bLeft = box[0]
            val bTop = box[1]
            val bRight = box[2]
            val bBottom = box[3]

            val oLeft = chassis.position.x - cHalfW
            val oTop = chassis.position.y - cHalfH
            val oRight = chassis.position.x + cHalfW
            val oBottom = chassis.position.y + cHalfH

            if (oRight > bLeft && oLeft < bRight && oBottom > bTop && oTop < bBottom) {
                val overlapX = min(oRight - bLeft, bRight - oLeft)
                val overlapY = min(oBottom - bTop, bBottom - oTop)

                if (overlapX < overlapY) {
                    if (chassis.position.x < (bLeft + bRight) / 2f) {
                        chassis.position.x -= overlapX
                        if (chassis.velocity.x > 0f) chassis.velocity.x = -chassis.velocity.x * 0.2f
                    } else {
                        chassis.position.x += overlapX
                        if (chassis.velocity.x < 0f) chassis.velocity.x = -chassis.velocity.x * 0.2f
                    }
                } else {
                    if (chassis.position.y < (bTop + bBottom) / 2f) {
                        chassis.position.y -= overlapY
                        if (chassis.velocity.y > 0f) chassis.velocity.y = 0f
                    } else {
                        chassis.position.y += overlapY
                        if (chassis.velocity.y < 0f) chassis.velocity.y = 0f
                    }
                }
            }
        }

        // 4. In-Air Controls (Gas tilts back, Brake tilts forward, plus manual tilt buttons)
        if (!anyWheelGrounded) {
            val airTorque = when {
                airTilt != 0f -> airTilt * carConfig.airControl * 14000f
                throttle > 0f -> -throttle * carConfig.airControl * 9000f // Gas pitches up/back
                throttle < 0f -> -throttle * carConfig.airControl * 9000f // Brake pitches down/forward
                else -> 0f
            }
            if (airTorque != 0f) {
                chassis.applyTorque(airTorque, dt)
            }
        } else if (airTilt != 0f) {
            chassis.applyTorque(airTilt * carConfig.airControl * 8000f, dt)
        }

        // 5. Ground stability assistance (smooth natural settling on slopes)
        if (groundTractionCount > 0) {
            chassis.angularVelocity *= 0.94f
        }
    }
}
