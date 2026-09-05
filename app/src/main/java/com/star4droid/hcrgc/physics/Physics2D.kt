package com.star4droid.hcrgc.physics

import kotlin.math.*

data class Vec2(var x: Float = 0f, var y: Float = 0f) {
    operator fun plus(other: Vec2) = Vec2(x + other.x, y + other.y)
    operator fun minus(other: Vec2) = Vec2(x - other.x, y - other.y)
    operator fun times(scalar: Float) = Vec2(x * scalar, y * scalar)
    operator fun div(scalar: Float) = Vec2(x / scalar, y / scalar)

    fun length(): Float = sqrt(x * x + y * y)
    fun lengthSq(): Float = x * x + y * y

    fun normalized(): Vec2 {
        val len = length()
        return if (len > 0.0001f) Vec2(x / len, y / len) else Vec2(0f, -1f)
    }

    fun dot(other: Vec2): Float = x * other.x + y * other.y
    fun cross(other: Vec2): Float = x * other.y - y * other.x
}

class RigidBody(
    val id: String,
    var position: Vec2 = Vec2(),
    var velocity: Vec2 = Vec2(),
    var rotation: Float = 0f, // in degrees
    var angularVelocity: Float = 0f, // degrees per second
    var width: Float = 50f,
    var height: Float = 50f,
    var mass: Float = 1.0f,
    var inertia: Float = 2500f,
    var isStatic: Boolean = false,
    var friction: Float = 0.6f,
    var restitution: Float = 0.2f,
    var isSensor: Boolean = false,
    var fixedRotation: Boolean = false,
    var isVehicleChassis: Boolean = false
) {
    val invMass: Float get() = if (isStatic || mass <= 0f) 0f else 1f / mass
    val invInertia: Float get() = if (isStatic || fixedRotation || inertia <= 0f) 0f else 1f / inertia

    fun applyForce(force: Vec2, dt: Float) {
        if (isStatic) return
        velocity.x += force.x * invMass * dt
        velocity.y += force.y * invMass * dt
    }

    fun applyImpulse(impulse: Vec2) {
        if (isStatic) return
        velocity.x += impulse.x * invMass
        velocity.y += impulse.y * invMass
    }

    fun applyTorque(torque: Float, dt: Float) {
        if (isStatic || fixedRotation) return
        angularVelocity += torque * invInertia * dt
    }

    fun applyTorqueImpulse(torqueImpulse: Float) {
        if (isStatic || fixedRotation) return
        angularVelocity += torqueImpulse * invInertia
    }
}

data class TerrainContact(
    val isContact: Boolean,
    val penetration: Float, // Depth below or inside contact threshold
    val surfacePoint: Vec2,
    val normal: Vec2, // Always pointing UPWARDS / outward into playable space
    val tangent: Vec2, // Tangent along surface pointing forward (right)
    val slopeDegrees: Float
)

object CollisionHelper {

    /**
     * Accurately tests if a point P(px, py) is at or below a continuous terrain chain.
     * Guaranteed upward normal to prevent any falling through.
     */
    fun checkPointAgainstTerrain(
        px: Float, py: Float,
        segments: List<Pair<Vec2, Vec2>>,
        proximityThreshold: Float = 20f
    ): TerrainContact? {
        var bestContact: TerrainContact? = null
        var maxPenetration = -Float.MAX_VALUE

        for (seg in segments) {
            val p1 = seg.first
            val p2 = seg.second

            val minX = min(p1.x, p2.x) - 10f
            val maxX = max(p1.x, p2.x) + 10f

            // Segment direction
            val dx = p2.x - p1.x
            val dy = p2.y - p1.y
            val segLenSq = dx * dx + dy * dy
            if (segLenSq < 0.0001f) continue

            val segLen = sqrt(segLenSq)
            val dir = Vec2(dx / segLen, dy / segLen)

            // Upward normal (perpendicular to segment pointing up, where Y is down)
            var normal = Vec2(-dir.y, dir.x)
            if (normal.y > 0f) {
                normal = Vec2(dir.y, -dir.x)
            }
            if (normal.y > -0.05f && normal.x == 0f) {
                normal = Vec2(0f, -1f)
            }

            var tangent = Vec2(-normal.y, normal.x)
            if (tangent.x < 0f) {
                tangent = Vec2(-tangent.x, -tangent.y)
            }

            // Check projection onto segment
            val t = ((px - p1.x) * dx + (py - p1.y) * dy) / segLenSq
            val clampedT = t.coerceIn(0f, 1f)
            val nearestX = p1.x + clampedT * dx
            val nearestY = p1.y + clampedT * dy

            // Distance along normal from nearest point on segment
            val toPx = px - nearestX
            val toPy = py - nearestY
            val normalDist = toPx * normal.x + toPy * normal.y

            // normalDist < 0 means point is on the underside/inside (penetrating)
            // normalDist >= 0 means point is above the terrain in air
            val penetration = -normalDist

            if (penetration > -proximityThreshold) {
                // If px is within segment X bounds or near endpoint
                if ((px in minX..maxX || hypot(toPx, toPy) < proximityThreshold) && penetration > maxPenetration) {
                    maxPenetration = penetration
                    val slope = atan2(dy, dx) * (180f / Math.PI.toFloat())
                    bestContact = TerrainContact(
                        isContact = true,
                        penetration = penetration,
                        surfacePoint = Vec2(nearestX, nearestY),
                        normal = normal,
                        tangent = tangent,
                        slopeDegrees = slope
                    )
                }
            }
        }

        return bestContact
    }
}

class PhysicsWorld(
    var gravity: Vec2 = Vec2(0f, 980f) // pixels/sec^2
) {
    val bodies = mutableListOf<RigidBody>()

    fun addBody(body: RigidBody) {
        if (!bodies.contains(body)) bodies.add(body)
    }

    fun removeBody(id: String) {
        bodies.removeAll { it.id == id }
    }

    fun clear() {
        bodies.clear()
    }

    fun step(
        dt: Float,
        terrainSegments: List<Pair<Vec2, Vec2>> = emptyList(),
        staticBoxes: List<FloatArray> = emptyList()
    ) {
        for (body in bodies) {
            if (body.isStatic) continue

            // Integrate gravity
            body.velocity.x += gravity.x * dt
            body.velocity.y += gravity.y * dt

            // Air resistance
            body.velocity.x *= 0.998f
            body.velocity.y *= 0.998f
            body.angularVelocity *= 0.985f

            // Integrate position
            body.position.x += body.velocity.x * dt
            body.position.y += body.velocity.y * dt
            if (!body.fixedRotation) {
                body.rotation += body.angularVelocity * dt
            }

            // Collisions for dynamic non-chassis objects (crates, boxes, balls) against terrain
            if (!body.isSensor && !body.isVehicleChassis && terrainSegments.isNotEmpty()) {
                val halfW = body.width / 2f
                val halfH = body.height / 2f
                val bottomY = body.position.y + halfH

                val contact = CollisionHelper.checkPointAgainstTerrain(
                    body.position.x, bottomY, terrainSegments, proximityThreshold = halfH
                )

                if (contact != null && contact.penetration > 0f) {
                    // Push out of terrain
                    body.position.x += contact.normal.x * contact.penetration
                    body.position.y += contact.normal.y * contact.penetration

                    // Cancel normal velocity & apply restitution
                    val normalVel = body.velocity.dot(contact.normal)
                    if (normalVel < 0f) {
                        body.velocity.x -= contact.normal.x * normalVel * (1f + body.restitution)
                        body.velocity.y -= contact.normal.y * normalVel * (1f + body.restitution)
                    }

                    // Surface friction
                    val tangentVel = body.velocity.dot(contact.tangent)
                    body.velocity.x -= contact.tangent.x * tangentVel * body.friction * 0.5f
                    body.velocity.y -= contact.tangent.y * tangentVel * body.friction * 0.5f

                    // Angular friction from roll
                    body.angularVelocity += tangentVel * 0.05f
                }
            }

            // Check against static TileMap boxes
            if (!body.isSensor && staticBoxes.isNotEmpty()) {
                val halfW = body.width / 2f
                val halfH = body.height / 2f

                for (box in staticBoxes) {
                    val bLeft = box[0]
                    val bTop = box[1]
                    val bRight = box[2]
                    val bBottom = box[3]

                    val oLeft = body.position.x - halfW
                    val oTop = body.position.y - halfH
                    val oRight = body.position.x + halfW
                    val oBottom = body.position.y + halfH

                    if (oRight > bLeft && oLeft < bRight && oBottom > bTop && oTop < bBottom) {
                        val overlapX = min(oRight - bLeft, bRight - oLeft)
                        val overlapY = min(oBottom - bTop, bBottom - oTop)

                        if (overlapX < overlapY) {
                            if (body.position.x < (bLeft + bRight) / 2f) {
                                body.position.x -= overlapX
                                if (body.velocity.x > 0) body.velocity.x = 0f
                            } else {
                                body.position.x += overlapX
                                if (body.velocity.x < 0) body.velocity.x = 0f
                            }
                        } else {
                            if (body.position.y < (bTop + bBottom) / 2f) {
                                body.position.y -= overlapY
                                if (body.velocity.y > 0) body.velocity.y = 0f
                            } else {
                                body.position.y += overlapY
                                if (body.velocity.y < 0) body.velocity.y = 0f
                            }
                        }
                    }
                }
            }
        }
    }
}
