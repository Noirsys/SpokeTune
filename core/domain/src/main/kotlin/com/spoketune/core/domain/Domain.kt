package com.spoketune.core.domain

import java.time.Instant
import java.util.UUID
import kotlin.math.abs

/** The two physical cohorts. They are intentionally never combined by statistics. */
enum class WheelSide { LEFT, RIGHT }

enum class TraversalDirection { CLOCKWISE, COUNTER_CLOCKWISE }

data class Spoke(
    val number: Int,
    val side: WheelSide,
)

/**
 * A profile's spoke numbering is stable: numbers are one-based and clockwise from
 * the chosen reference spoke. Side assignment is explicit and deterministic.
 */
data class WheelProfile(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val spokeCount: Int,
    val firstSide: WheelSide = WheelSide.LEFT,
) {
    init {
        require(spokeCount in 12..48 && spokeCount % 2 == 0) { "spokeCount must be an even number from 12 through 48" }
    }

    val spokes: List<Spoke> = (1..spokeCount).map { number ->
        // Alternating holes are assigned explicitly; no inference from frequency.
        Spoke(number, if (number % 2 == 1) firstSide else firstSide.opposite())
    }

    fun spoke(number: Int): Spoke = spokes.getOrNull(number - 1)
        ?: throw IllegalArgumentException("spoke number must be 1..$spokeCount")
}

fun WheelSide.opposite(): WheelSide = if (this == WheelSide.LEFT) WheelSide.RIGHT else WheelSide.LEFT

/** Stable numbered sequence, wrapping at either end. */
fun traversal(profile: WheelProfile, startSpoke: Int, direction: TraversalDirection): List<Spoke> {
    profile.spoke(startSpoke)
    return (0 until profile.spokeCount).map { offset ->
        val zero = startSpoke - 1
        val index = when (direction) {
            TraversalDirection.CLOCKWISE -> (zero + offset) % profile.spokeCount
            TraversalDirection.COUNTER_CLOCKWISE -> (zero - offset + profile.spokeCount) % profile.spokeCount
        }
        profile.spokes[index]
    }
}

data class Measurement(
    val id: UUID = UUID.randomUUID(),
    val spokeNumber: Int,
    val frequencyHz: Double,
    val confidence: Double,
    val capturedAt: Instant = Instant.now(),
) {
    init {
        require(spokeNumber > 0)
        require(frequencyHz.isFinite() && frequencyHz > 0.0)
        require(confidence.isFinite() && confidence in 0.0..1.0)
    }
}

enum class ComparisonStatus { INSUFFICIENT_DATA, CENTER, LOWER, HIGHER }

data class SideComparison(
    val side: WheelSide,
    val sampleCount: Int,
    val medianHz: Double?,
    val madHz: Double?,
    val deviationHz: Double?,
    val robustScore: Double?,
    val status: ComparisonStatus,
)

/**
 * Median/MAD comparison for one side only. At least three samples are required
 * for a provisional cohort center. MAD==0 uses exact-center classification: values
 * equal to the median are CENTER, values below/above are LOWER/HIGHER.
 */
fun compareSameSide(
    profile: WheelProfile,
    measurements: Iterable<Measurement>,
    targetSpokeNumber: Int,
    minimumSamples: Int = 3,
): SideComparison {
    require(minimumSamples >= 1)
    val target = profile.spoke(targetSpokeNumber)
    val all = measurements.toList()
    val values = all.filter { it.spokeNumber > 0 && it.spokeNumber <= profile.spokeCount }
        .filter { profile.spoke(it.spokeNumber).side == target.side }
        .map { it.frequencyHz }.sorted()
    if (values.size < minimumSamples) return SideComparison(target.side, values.size, null, null, null, null, ComparisonStatus.INSUFFICIENT_DATA)
    val median = median(values)
    val mad = median(values.map { abs(it - median) }.sorted())
    val targetMeasurement = all.lastOrNull { it.spokeNumber == targetSpokeNumber }
    if (targetMeasurement == null) return SideComparison(target.side, values.size, median, mad, null, null, ComparisonStatus.INSUFFICIENT_DATA)
    val deviation = targetMeasurement.frequencyHz - median
    val status = when {
        deviation == 0.0 -> ComparisonStatus.CENTER
        deviation < 0.0 -> ComparisonStatus.LOWER
        else -> ComparisonStatus.HIGHER
    }
    val score = if (mad == 0.0) null else deviation / (1.4826 * mad)
    return SideComparison(target.side, values.size, median, mad, deviation, score, status)
}

private fun median(sorted: List<Double>): Double {
    require(sorted.isNotEmpty())
    val middle = sorted.size / 2
    return if (sorted.size % 2 == 1) sorted[middle] else (sorted[middle - 1] + sorted[middle]) / 2.0
}
