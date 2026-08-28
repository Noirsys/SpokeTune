package signal

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

class SpokeAcousticRegressionTest {
    private val analyzer = YinPitchAnalyzer()

    @Test fun attackDecayAndHarmonicsRecoverFundamental() {
        val result = analyzer.analyze(SpokeAcousticFixtures.ring(247f, decayPerSecond = 3f), SpokeAcousticFixtures.sampleRateHz)
        assertNear(result, 247f, 5f)
    }

    @Test fun dampingStillFindsShortRing() {
        val result = analyzer.analyze(SpokeAcousticFixtures.ring(440f, seconds = .28f, decayPerSecond = 18f), SpokeAcousticFixtures.sampleRateHz)
        assertNear(result, 440f, 8f)
    }

    @Test fun shopNoiseDoesNotDisplaceFundamental() {
        val result = analyzer.analyze(SpokeAcousticFixtures.shopNoise(), SpokeAcousticFixtures.sampleRateHz)
        assertNear(result, 330f, 8f)
    }

    @Test fun doublePluckRemainsBoundedOrClearlyRejected() {
        val result = analyzer.analyze(SpokeAcousticFixtures.doublePluck(), SpokeAcousticFixtures.sampleRateHz)
        if (result is PitchResult.Accepted) assertTrue(abs(result.frequencyHz - 330f) < 12f, result.toString())
    }

    @Test fun clippingAndSilenceNeverProduceInvalidAcceptedValues() {
        val clipped = analyzer.analyze(SpokeAcousticFixtures.clipped(), SpokeAcousticFixtures.sampleRateHz)
        if (clipped is PitchResult.Accepted) assertTrue(clipped.frequencyHz in 100f..2_000f)
        assertTrue(analyzer.analyze(SpokeAcousticFixtures.silence(), SpokeAcousticFixtures.sampleRateHz) is PitchResult.Rejected)
    }

    private fun assertNear(result: PitchResult, expected: Float, tolerance: Float) {
        assertTrue(result is PitchResult.Accepted, result.toString())
        result as PitchResult.Accepted
        assertTrue(abs(result.frequencyHz - expected) <= tolerance, "$expected -> $result")
    }
}
