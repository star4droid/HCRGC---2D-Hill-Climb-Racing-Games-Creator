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

            if (contact != null && contact.penetration > -restLength) {
                wheel.isGrounded = true
                anyWheelGrounded = true
                groundTractionCount++

                val compression = (contact.penetration + restLength).coerceAtLeast(0f)
                wheel.suspensionCompression = compression

                // Spring force
                val springK = wheel.config.suspensionFrequency * 6800f
                val springForceMag = compression * springK

                // Damping along contact normal
                val relVel = chassis.velocity.dot(contact.normal)
                val damping = wheel.config.suspensionDamping * 420f * relVel
                val totalNormalForce = (springForceMag - damping).coerceAtLeast(0f)

                val normalForce = contact.normal * totalNormalForce

                // Apply linear force to chassis
                chassis.applyForce(normalForce, dt)

                // APPLY ROTATIONAL TORQUE TO CHASSIS:
                // r = anchor - chassis.position
                val rx = anchorX - chassis.position.x
                val ry = anchorY - chassis.position.y
                val torque = rx * normalForce.y - ry * normalForce.x
                chassis.applyTorque(torque * 1.35f, dt)

                // Hard anti-penetration resolution if wheel is pushed deep below surface
                if (contact.penetration > 0f) {
                    chassis.position.y += contact.normal.y * contact.penetration * 0.45f
                    val vDotN = chassis.velocity.dot(contact.normal)
                    if (vDotN < -50f) {
                        chassis.velocity.y -= contact.normal.y * vDotN * 0.6f
                    }
                }

                // Propulsion along surface tangent (always driving forward on positive throttle)
                val forwardTangent = if (contact.tangent.x >= 0f) contact.tangent else Vec2(-contact.tangent.x, -contact.tangent.y)
                val driveScale = carConfig.enginePower * 3800f
                if (throttle > 0f) {
                    val driveForce = forwardTangent * (throttle * driveScale)
                    chassis.applyForce(driveForce, dt)
                } else if (throttle < 0f) {
                    val brakeForce = forwardTangent * (throttle * driveScale * 0.85f)
                    chassis.applyForce(brakeForce, dt)
                }

                // Surface traction damping along tangent to prevent unnatural sideways sliding
                val tangentVel = chassis.velocity.dot(forwardTangent)

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

        // 2. Chassis Bumper Collisions (front and rear bumpers without velocity killing)
        val bumperHalfW = 55f
        val frontBumperX = chassis.position.x + bumperHalfW * cosRot
        val frontBumperY = chassis.position.y + bumperHalfW * sinRot + 14f
        val frontBumperContact = CollisionHelper.checkPointAgainstTerrain(frontBumperX, frontBumperY, terrainSegments, 8f)
        if (frontBumperContact != null && frontBumperContact.penetration > 0f) {
            chassis.position.y += frontBumperContact.normal.y * frontBumperContact.penetration * 0.4f
            chassis.applyTorque(-12000f, dt) // Push nose up gently
        }

        val rearBumperX = chassis.position.x - bumperHalfW * cosRot
        val rearBumperY = chassis.position.y - bumperHalfW * sinRot + 14f
        val rearBumperContact = CollisionHelper.checkPointAgainstTerrain(rearBumperX, rearBumperY, terrainSegments, 8f)
        if (rearBumperContact != null && rearBumperContact.penetration > 0f) {
            chassis.position.y += rearBumperContact.normal.y * rearBumperContact.penetration * 0.4f
            chassis.applyTorque(12000f, dt) // Push tail up gently
        }

        // 3. Air Tilt & Attitude Control
        if (!anyWheelGrounded || airTilt != 0f) {
            if (airTilt != 0f) {
                chassis.applyTorque(airTilt * carConfig.airControl * 16000f, dt)
            }
        }

        // 4. Ground stability assistance (smooth natural settling on slopes)
        if (groundTractionCount > 0) {
            // Apply slight rotational damping so vehicle doesn't bounce endlessly
            chassis.angularVelocity *= 0.93f
        }
    }
}
