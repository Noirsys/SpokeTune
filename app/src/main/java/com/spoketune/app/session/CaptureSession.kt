package com.spoketune.app.session

import com.spoketune.core.domain.*

/** In-memory capture boundary; replace the store behind this API when persistence arrives. */
class CaptureSession(val profile: WheelProfile) {
    private val readings = linkedMapOf<Int, Measurement>()
    private val skipped = linkedSetOf<Int>()
    var currentSpoke: Int = 1
        private set
    fun accept(frequencyHz: Double, confidence: Double): Measurement {
        val m = Measurement(spokeNumber = currentSpoke, frequencyHz = frequencyHz, confidence = confidence)
        readings[currentSpoke] = m
        skipped.remove(currentSpoke)
        advance(); return m
    }
    fun skip() { skipped += currentSpoke; advance() }
    private fun advance() { currentSpoke = if (currentSpoke == profile.spokeCount) 1 else currentSpoke + 1 }
    fun measurements(): List<Measurement> = readings.values.toList()
    fun skippedSpokes(): Set<Int> = skipped.toSet()
    fun comparison(spokeNumber: Int): SideComparison = compareSameSide(profile, readings.values, spokeNumber)
    fun reset() { readings.clear(); skipped.clear(); currentSpoke = 1 }
}
