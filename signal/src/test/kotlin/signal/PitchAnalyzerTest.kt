package signal

import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.exp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PitchAnalyzerTest {
    private val analyzer = YinPitchAnalyzer()

    @Test fun detectsDeterministicSinesAcrossInitialRange() {
        for (frequency in listOf(100f, 137f, 220f, 440f, 997f, 1500f, 2000f)) {
            val result = analyzer.analyze(sine(frequency, 48_000, 0.18f), 48_000)
            assertTrue(result is PitchResult.Accepted, "$frequency: $result")
            result as PitchResult.Accepted
            assertTrue(kotlin.math.abs(result.frequencyHz - frequency) / frequency < .01f, "$frequency -> ${result.frequencyHz}")
        }
    }

    @Test fun removesDcAndRejectsSilence() {
        val result = analyzer.analyze(FloatArray(9_600) { 0f }, 48_000)
        assertEquals(PitchResult.Rejected(RejectionReason.SILENCE), result)
    }

    @Test fun rejectsTooShortAndInvalidInput() {
        assertEquals(PitchResult.Rejected(RejectionReason.TOO_SHORT), analyzer.analyze(FloatArray(100), 48_000))
        assertEquals(PitchResult.Rejected(RejectionReason.INVALID_INPUT), analyzer.analyze(FloatArray(9_600) { Float.NaN }, 48_000))
    }

    @Test fun rejectsUnpitchedNoise() {
        val noise = FloatArray(9_600)
        var state = 0x12345678
        for (i in noise.indices) {
            state = state * 1664525 + 1013904223
            noise[i] = ((state ushr 8) and 0xffff) / 32768f - 1f
        }
        val result = analyzer.analyze(noise, 48_000)
        assertTrue(result is PitchResult.Rejected)
    }

    @Test fun findsPitchInImpactAndDecayingRing() {
        val sampleRate = 48_000
        val samples = FloatArray((sampleRate * 1.5).toInt()) { i ->
            val t = i.toFloat() / sampleRate
            val ring = (0.28 * exp(-2.8 * t) * sin(2.0 * PI * 220.0 * t)).toFloat()
            val impact = if (i < 180) (0.9f * (1f - i / 180f)) else 0f
            ring + impact
        }
        val result = analyzer.analyze(samples, sampleRate)
        assertTrue(result is PitchResult.Accepted, result.toString())
        result as PitchResult.Accepted
        assertTrue(kotlin.math.abs(result.frequencyHz - 220f) < 4f, result.toString())
    }

    @Test fun ignoresQuietTailAfterShortRing() {
        val sampleRate = 48_000
        val samples = FloatArray(sampleRate * 2) { i ->
            val t = i.toFloat() / sampleRate
            if (t < .35f) (0.25 * exp(-5.0 * t) * sin(2.0 * PI * 330.0 * t)).toFloat() else 0f
        }
        val result = analyzer.analyze(samples, sampleRate)
        assertTrue(result is PitchResult.Accepted, result.toString())
        result as PitchResult.Accepted
        assertTrue(kotlin.math.abs(result.frequencyHz - 330f) < 5f, result.toString())
    }

    private fun sine(frequency: Float, sampleRate: Int, seconds: Float): FloatArray =
        FloatArray((sampleRate * seconds).toInt()) { i -> (0.5 * sin(2.0 * PI * frequency * i / sampleRate)).toFloat() }
}
