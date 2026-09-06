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
        obstacleBoxes: List<FloatArray> // [left, top, right, bottom]
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

            val wheelRadius = wheel.config.radius.coerceAtLeast(16f)
            val restLength = wheelRadius * 0.95f

            // 1. Check terrain segment contact
            val terrainContact = if (terrainSegments.isNotEmpty()) {
                CollisionHelper.checkPointAgainstTerrain(
                    anchorX, anchorY, terrainSegments, proximityThreshold = restLength * 1.75f
                )
            } else null

            // 2. Check static/dynamic box contacts (Boxes, Crates, Platforms)
            var bestBoxContact: TerrainContact? = null
            for (box in obstacleBoxes) {
                val bContact = CollisionHelper.checkPointAgainstBox(
                    anchorX, anchorY,
                    bLeft = box[0], bTop = box[1], bRight = box[2], bBottom = box[3],
                    proximityThreshold = restLength * 1.75f
                )
                if (bContact != null) {
                    if (bestBoxContact == null || bContact.surfacePoint.y < bestBoxContact.surfacePoint.y) {
                        bestBoxContact = bContact
                    }
                }
            }

            // Pick the best contact: prioritize the higher surface (smaller Y in screen coords)
            val contact = when {
                terrainContact != null && bestBoxContact != null -> {
                    if (bestBoxContact.surfacePoint.y < terrainContact.surfacePoint.y) bestBoxContact else terrainContact
                }
                bestBoxContact != null -> bestBoxContact
                else -> terrainContact
            }

            if (contact != null && contact.penetration > -restLength * 1.35f) {
                wheel.isGrounded = true
                anyWheelGrounded = true
                groundTractionCount++

                val compression = (contact.penetration + restLength).coerceIn(0f, restLength * 1.6f)
                wheel.suspensionCompression = compression

                // Spring & damper force along contact normal
                val springK = wheel.config.suspensionFrequency * 3200f
                val springForceMag = compression * springK

                val relVel = chassis.velocity.dot(contact.normal)
                val damping = wheel.config.suspensionDamping * 220f * relVel
                val totalNormalForce = (springForceMag - damping).coerceAtLeast(0f)

                val normalForce = contact.normal * totalNormalForce

                // Apply normal force to chassis
                chassis.applyForce(normalForce, dt)

                // Rotational torque from suspension push
                val rx = anchorX - chassis.position.x
                val ry = anchorY - chassis.position.y
                val torque = rx * normalForce.y - ry * normalForce.x
                chassis.applyTorque(torque * 0.75f, dt)

                // Anti-penetration position correction
                if (contact.penetration > 1.5f) {
                    val pushAmount = min(contact.penetration * 0.35f, 5f)
                    chassis.position.x += contact.normal.x * pushAmount
                    chassis.position.y += contact.normal.y * pushAmount
                }

                // Surface tangent & vehicle drive force
                val forwardTangent = if (contact.tangent.x >= 0f) contact.tangent else Vec2(-contact.tangent.x, -contact.tangent.y)
                val tangentVel = chassis.velocity.dot(forwardTangent)
                val driveScale = (carConfig.enginePower.coerceAtLeast(0.6f)) * 9000f

                if (throttle > 0f) {
                    // Gas: continuous responsive forward acceleration
                    if (tangentVel < 1200f) {
                        val uphillFactor = 1.0f + (-forwardTangent.y * 1.4f).coerceAtLeast(0f)
                        val driveForce = forwardTangent * (throttle * driveScale * uphillFactor)
                        chassis.applyForce(driveForce, dt)
                    }
                } else if (throttle < 0f) {
                    // Brake / Reverse
                    if (tangentVel > 25f) {
                        val brakeForce = forwardTangent * (-min(tangentVel * 22f, 26000f))
                        chassis.applyForce(brakeForce, dt)
                    } else if (tangentVel > -450f) {
                        val revForce = forwardTangent * (throttle * driveScale * 0.7f)
                        chassis.applyForce(revForce, dt)
                    }
                } else {
                    // Natural coasting friction
                    if (abs(tangentVel) > 4f) {
                        val rollFriction = forwardTangent * (-sign(tangentVel) * 120f)
                        chassis.applyForce(rollFriction, dt)
                    }
                }

                // Wheel physical rolling rotation
                wheel.body.angularVelocity = (tangentVel / wheelRadius) * (180f / Math.PI.toFloat())

                // Wheel visual placement along suspension compression
                val extension = (restLength - compression).coerceAtLeast(0f)
                wheel.body.position.x = anchorX - contact.normal.x * extension
                wheel.body.position.y = anchorY - contact.normal.y * extension
            } else {
                wheel.isGrounded = false
                wheel.suspensionCompression = 0f

                // In air: wheel extends naturally from car chassis
                val uncompressedX = anchorX - (restLength * sinRot)
                val uncompressedY = anchorY + (restLength * cosRot)
                wheel.body.position.x = uncompressedX
                wheel.body.position.y = uncompressedY
                wheel.body.velocity = chassis.velocity

                // Spin wheel freely in air with throttle input
                if (throttle != 0f) {
                    wheel.body.angularVelocity += throttle * 600f * dt
                }
            }

            // Integrate wheel rotation
            wheel.body.rotation += wheel.body.angularVelocity * dt
        }

        // 2. Chassis Bumper Collisions with terrain
        val bumperHalfW = (chassis.width * 0.45f).coerceAtLeast(35f)
        val frontBumperX = chassis.position.x + bumperHalfW * cosRot
        val frontBumperY = chassis.position.y + bumperHalfW * sinRot + 6f
        val frontBumperContact = if (terrainSegments.isNotEmpty()) {
            CollisionHelper.checkPointAgainstTerrain(frontBumperX, frontBumperY, terrainSegments, 14f)
        } else null
        if (frontBumperContact != null && frontBumperContact.penetration > 2f) {
            val push = min(frontBumperContact.penetration * 0.4f, 6f)
            chassis.position.x += frontBumperContact.normal.x * push
            chassis.position.y += frontBumperContact.normal.y * push
            chassis.velocity.x *= 0.94f
        }

        val rearBumperX = chassis.position.x - bumperHalfW * cosRot
        val rearBumperY = chassis.position.y - bumperHalfW * sinRot + 6f
        val rearBumperContact = if (terrainSegments.isNotEmpty()) {
            CollisionHelper.checkPointAgainstTerrain(rearBumperX, rearBumperY, terrainSegments, 14f)
        } else null
        if (rearBumperContact != null && rearBumperContact.penetration > 2f) {
            val push = min(rearBumperContact.penetration * 0.4f, 6f)
            chassis.position.x += rearBumperContact.normal.x * push
            chassis.position.y += rearBumperContact.normal.y * push
            chassis.velocity.x *= 0.94f
        }

        // 3. Chassis Collisions with Obstacle Boxes (Walls, hurdles, crates, platforms)
        val cHalfW = chassis.width / 2f
        val cHalfH = chassis.height / 2f
        for (box in obstacleBoxes) {
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
                    val sign = if (chassis.position.x < (bLeft + bRight) / 2f) -1f else 1f
                    chassis.position.x += sign * overlapX
                    if (chassis.velocity.x * sign < 0f) {
                        chassis.velocity.x = 0f
                    }
                } else {
                    val sign = if (chassis.position.y < (bTop + bBottom) / 2f) -1f else 1f
                    chassis.position.y += sign * overlapY
                    if (chassis.velocity.y * sign < 0f) {
                        chassis.velocity.y = 0f
                    }
                }
            }
        }

        // 4. In-Air Tilt Controls
        if (!anyWheelGrounded) {
            val airControlRate = carConfig.airControl.coerceAtLeast(0.5f)
            val airTorque = when {
                airTilt != 0f -> airTilt * airControlRate * 16000f
                throttle > 0f -> -throttle * airControlRate * 10500f // Gas pitches up/back
                throttle < 0f -> -throttle * airControlRate * 10500f // Brake pitches down/forward
                else -> 0f
            }
            if (airTorque != 0f) {
                chassis.applyTorque(airTorque, dt)
            }
        } else if (airTilt != 0f) {
            chassis.applyTorque(airTilt * carConfig.airControl * 9000f, dt)
        }

        // 5. Ground stability assistance
        if (groundTractionCount > 0) {
            chassis.angularVelocity *= 0.94f
        }
    }
}
