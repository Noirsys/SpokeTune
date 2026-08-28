package signal

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/** Deterministic, license-free PCM sources used by the analyzer regression lab. */
object SpokeAcousticFixtures {
    const val sampleRateHz = 48_000

    fun ring(
        frequencyHz: Float = 330f,
        seconds: Float = 0.65f,
        amplitude: Float = 0.24f,
        decayPerSecond: Float = 5f,
        harmonics: List<Pair<Float, Float>> = listOf(2f to .16f, 3f to .06f),
        attackSamples: Int = 160,
        noiseAmplitude: Float = 0f,
        seed: Int = 0x13579BDF
    ): FloatArray {
        val count = (seconds * sampleRateHz).toInt()
        val out = FloatArray(count)
        var state = seed
        for (i in out.indices) {
            val t = i.toFloat() / sampleRateHz
            val attack = if (attackSamples <= 0) 1f else (i.toFloat() / attackSamples).coerceAtMost(1f)
            val envelope = attack * exp(-decayPerSecond * t)
            var tone = sin(2 * PI * frequencyHz * t).toFloat()
            for ((multiple, level) in harmonics) tone += level * sin(2 * PI * frequencyHz * multiple * t).toFloat()
            state = state * 1664525 + 1013904223
            val noise = (((state ushr 8) and 0xffff) / 32768f - 1f) * noiseAmplitude
            out[i] = (amplitude * envelope * tone + noise).coerceIn(-1f, 1f)
        }
        return out
    }

    fun doublePluck(frequencyHz: Float = 330f): FloatArray {
        val first = ring(frequencyHz, seconds = .45f, decayPerSecond = 7f)
        val second = ring(frequencyHz * 1.01f, seconds = .45f, decayPerSecond = 7f, seed = 0x2468ACE0)
        return FloatArray(sampleRateHz) { i ->
            val a = first.getOrElse(i) { 0f }
            val b = second.getOrElse(i - sampleRateHz / 12) { 0f }
            a + b
        }
    }

    fun clipped(frequencyHz: Float = 330f): FloatArray = ring(frequencyHz, amplitude = .95f, decayPerSecond = 2f)
    // A realistic room bed is present, but remains below the spoke's early ring.
    fun shopNoise(seconds: Float = .65f): FloatArray = ring(330f, amplitude = .16f, noiseAmplitude = .035f, decayPerSecond = 5f)
    fun silence(seconds: Float = .65f): FloatArray = FloatArray((seconds * sampleRateHz).toInt())
}
