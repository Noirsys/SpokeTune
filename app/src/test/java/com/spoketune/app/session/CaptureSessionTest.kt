package com.spoketune.app.session

import com.spoketune.core.domain.WheelProfile
import kotlin.test.Test
import kotlin.test.assertEquals

class CaptureSessionTest {
    @Test
    fun `accepted readings and skips advance without losing spoke identity`() {
        val session = CaptureSession(WheelProfile(name = "fat tire", spokeCount = 36))

        session.accept(310.0, 0.9)
        session.skip()
        session.accept(325.0, 0.85)

        assertEquals(listOf(1, 3), session.measurements().map { it.spokeNumber })
        assertEquals(setOf(2), session.skippedSpokes())
        assertEquals(4, session.currentSpoke)
    }

    @Test
    fun `same-side comparison excludes alternating opposite side`() {
        val session = CaptureSession(WheelProfile(name = "wheel", spokeCount = 12))
        session.accept(300.0, 0.9) // left 1
        session.accept(900.0, 0.9) // right 2
        session.accept(310.0, 0.9) // left 3
        session.accept(910.0, 0.9) // right 4
        session.accept(320.0, 0.9) // left 5

        assertEquals(310.0, session.comparison(1).medianHz)
    }
}
