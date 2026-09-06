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
    var isVehicleChassis: Boolean = false,
    var isVehicleWheel: Boolean = false
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

            val minX = if (p1.x < p2.x) p1.x else p2.x
            val maxX = if (p1.x > p2.x) p1.x else p2.x
            if (px < minX - proximityThreshold || px > maxX + proximityThreshold) {
                continue
            }
            val minY = if (p1.y < p2.y) p1.y else p2.y
            val maxY = if (p1.y > p2.y) p1.y else p2.y
            if (py < minY - proximityThreshold || py > maxY + proximityThreshold) {
                continue
            }

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

    fun checkPointAgainstBox(
        px: Float, py: Float,
        bLeft: Float, bTop: Float, bRight: Float, bBottom: Float,
        proximityThreshold: Float = 25f
    ): TerrainContact? {
        if (px < bLeft - proximityThreshold || px > bRight + proximityThreshold) return null
        if (py < bTop - proximityThreshold || py > bBottom + proximityThreshold) return null

        val nearestX = px.coerceIn(bLeft, bRight)
        val nearestY = py.coerceIn(bTop, bBottom)

        val distToTop = abs(py - bTop)
        val distToBottom = abs(py - bBottom)
        val distToLeft = abs(px - bLeft)
        val distToRight = abs(px - bRight)

        val minDist = minOf(distToTop, distToBottom, distToLeft, distToRight)

        val (normal, surfacePt) = when (minDist) {
            distToTop -> Pair(Vec2(0f, -1f), Vec2(nearestX, bTop))
            distToLeft -> Pair(Vec2(-1f, 0f), Vec2(bLeft, nearestY))
            distToRight -> Pair(Vec2(1f, 0f), Vec2(bRight, nearestY))
            else -> Pair(Vec2(0f, 1f), Vec2(nearestX, bBottom))
        }

        val isInside = px in bLeft..bRight && py in bTop..bBottom
        val penetration = if (isInside) (proximityThreshold + minDist) else (proximityThreshold - minDist)

        if (penetration <= -proximityThreshold) return null

        val tangent = Vec2(-normal.y, normal.x)
        val forwardTangent = if (tangent.x >= 0f) tangent else Vec2(-tangent.x, -tangent.y)

        return TerrainContact(
            isContact = true,
            penetration = penetration,
            surfacePoint = surfacePt,
            normal = normal,
            tangent = forwardTangent,
            slopeDegrees = 0f
        )
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
            if (body.isStatic || body.isVehicleWheel) continue

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
                                if (body.velocity.x > 0) body.velocity.x *= -body.restitution
                            } else {
                                body.position.x += overlapX
                                if (body.velocity.x < 0) body.velocity.x *= -body.restitution
                            }
                        } else {
                            if (body.position.y < (bTop + bBottom) / 2f) {
                                body.position.y -= overlapY
                                if (body.velocity.y > 0) body.velocity.y *= -body.restitution
                            } else {
                                body.position.y += overlapY
                                if (body.velocity.y < 0) body.velocity.y *= -body.restitution
                            }
                        }
                    }
                }
            }
        }

        // Dynamic body vs dynamic body collisions (including car chassis vs crates/boxes)
        val nonSensors = bodies.filter { !it.isSensor }
        for (i in nonSensors.indices) {
            val a = nonSensors[i]
            for (j in i + 1 until nonSensors.size) {
                val b = nonSensors[j]
                if ((a.isStatic && b.isStatic) || a.isVehicleWheel || b.isVehicleWheel) continue

                val aHalfW = a.width / 2f
                val aHalfH = a.height / 2f
                val bHalfW = b.width / 2f
                val bHalfH = b.height / 2f

                val dx = b.position.x - a.position.x
                val dy = b.position.y - a.position.y
                val overlapX = (aHalfW + bHalfW) - abs(dx)
                val overlapY = (aHalfH + bHalfH) - abs(dy)

                if (overlapX > 0f && overlapY > 0f) {
                    val totalMass = (if (a.isStatic) 0f else a.mass) + (if (b.isStatic) 0f else b.mass)
                    if (totalMass <= 0.001f) continue

                    val aRatio = if (a.isStatic) 0f else if (b.isStatic) 1f else b.mass / totalMass
                    val bRatio = if (b.isStatic) 0f else if (a.isStatic) 1f else a.mass / totalMass

                    if (overlapX < overlapY) {
                        val sign = if (dx > 0) 1f else -1f
                        if (!a.isStatic) a.position.x -= sign * overlapX * aRatio
                        if (!b.isStatic) b.position.x += sign * overlapX * bRatio

                        val relVelX = b.velocity.x - a.velocity.x
                        val impulse = relVelX * (1f + min(a.restitution, b.restitution))
                        if (!a.isStatic) a.velocity.x += impulse * aRatio * 0.7f
                        if (!b.isStatic) b.velocity.x -= impulse * bRatio * 0.7f
                    } else {
                        val sign = if (dy > 0) 1f else -1f
                        if (!a.isStatic) a.position.y -= sign * overlapY * aRatio
                        if (!b.isStatic) b.position.y += sign * overlapY * bRatio

                        val relVelY = b.velocity.y - a.velocity.y
                        val impulse = relVelY * (1f + min(a.restitution, b.restitution))
                        if (!a.isStatic) a.velocity.y += impulse * aRatio * 0.7f
                        if (!b.isStatic) b.velocity.y -= impulse * bRatio * 0.7f
                    }
                }
            }
        }
    }
}
