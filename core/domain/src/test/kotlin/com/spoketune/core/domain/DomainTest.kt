package com.spoketune.core.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DomainTest {
    private val profile = WheelProfile(name = "test", spokeCount = 12)
    private fun m(n: Int, hz: Double) = Measurement(spokeNumber = n, frequencyHz = hz, confidence = 1.0)

    @Test fun `numbering assigns both sides and traversal wraps`() {
        assertEquals(WheelSide.LEFT, profile.spoke(1).side)
        assertEquals(WheelSide.RIGHT, profile.spoke(2).side)
        assertEquals(listOf(1, 12, 11), traversal(profile, 1, TraversalDirection.COUNTER_CLOCKWISE).take(3).map { it.number })
    }

    @Test fun `comparison excludes opposite side`() {
        val result = compareSameSide(profile, listOf(m(1, 100.0), m(3, 110.0), m(5, 120.0), m(2, 1000.0)), 1)
        assertEquals(WheelSide.LEFT, result.side)
        assertEquals(110.0, result.medianHz)
        assertEquals(10.0, result.madHz)
    }

    @Test fun `small sample and mad zero are deterministic`() {
        val tooFew = compareSameSide(profile, listOf(m(1, 100.0), m(3, 100.0)), 1)
        assertEquals(ComparisonStatus.INSUFFICIENT_DATA, tooFew.status)
        val zero = compareSameSide(profile, listOf(m(1, 100.0), m(3, 100.0), m(5, 100.0)), 1)
        assertEquals(ComparisonStatus.CENTER, zero.status)
        assertNull(zero.robustScore)
    }
}
