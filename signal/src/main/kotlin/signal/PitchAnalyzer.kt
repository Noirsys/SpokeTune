package signal

/** Android-independent contract for estimating the fundamental of a PCM capture. */
interface PitchAnalyzer {
    fun analyze(samples: FloatArray, sampleRateHz: Int): PitchResult
}

sealed interface PitchResult {
    data class Accepted(
        val frequencyHz: Float,
        val confidence: Float,
        val periodicity: Float,
        val rms: Float
    ) : PitchResult

    data class Rejected(val reason: RejectionReason) : PitchResult
}

enum class RejectionReason { INVALID_INPUT, TOO_SHORT, SILENCE, OUT_OF_RANGE, LOW_CONFIDENCE }

data class YinConfig(
    val minFrequencyHz: Float = 100f,
    val maxFrequencyHz: Float = 2_000f,
    val threshold: Float = 0.18f,
    val minimumConfidence: Float = 0.72f,
    val minimumRms: Float = 0.003f
)

/**
 * YIN difference function and cumulative mean normalized difference (CMNDF).
 * The returned confidence is periodicity (1 - CMNDF at the selected lag),
 * discounted when the candidate is close to a search boundary.
 */
class YinPitchAnalyzer(private val config: YinConfig = YinConfig()) : PitchAnalyzer {
    override fun analyze(samples: FloatArray, sampleRateHz: Int): PitchResult {
        if (sampleRateHz <= 0 || samples.any { !it.isFinite() } ||
            config.minFrequencyHz <= 0f || config.maxFrequencyHz <= config.minFrequencyHz) {
            return PitchResult.Rejected(RejectionReason.INVALID_INPUT)
        }
        val minLag = kotlin.math.floor(sampleRateHz / config.maxFrequencyHz).toInt().coerceAtLeast(2)
        val maxLag = kotlin.math.ceil(sampleRateHz / config.minFrequencyHz).toInt()
        if (samples.size < maxLag * 2 + 2) return PitchResult.Rejected(RejectionReason.TOO_SHORT)

        val mean = samples.average().toFloat()
        val centered = FloatArray(samples.size) { samples[it] - mean }
        val rms = kotlin.math.sqrt(centered.map { it * it }.average()).toFloat()
        if (rms < config.minimumRms) return PitchResult.Rejected(RejectionReason.SILENCE)

        val usableLag = maxLag.coerceAtMost(centered.size / 2)
        val cmndf = FloatArray(usableLag + 1)
        var running = 0.0
        for (lag in 1..usableLag) {
            var difference = 0.0
            for (i in 0 until centered.size - lag) {
                val d = centered[i] - centered[i + lag]
                difference += d * d
            }
            running += difference
            cmndf[lag] = if (running == 0.0) 1f else (difference * lag / running).toFloat()
        }
        var tau = -1
        for (lag in minLag..usableLag) {
            if (cmndf[lag] < config.threshold) {
                tau = lag
                while (tau + 1 <= usableLag && cmndf[tau + 1] < cmndf[tau]) tau++
                break
            }
        }
        if (tau < 0) {
            tau = (minLag..usableLag).minByOrNull { cmndf[it] } ?: return PitchResult.Rejected(RejectionReason.OUT_OF_RANGE)
            if (cmndf[tau] > 0.42f) return PitchResult.Rejected(RejectionReason.LOW_CONFIDENCE)
        }
        val betterTau = if (tau > minLag && tau < usableLag) {
            val a = cmndf[tau - 1]; val b = cmndf[tau]; val c = cmndf[tau + 1]
            val denominator = 2f * (a - 2f * b + c)
            if (denominator != 0f) tau + (a - c) / denominator else tau.toFloat()
        } else tau.toFloat()
        val frequency = sampleRateHz / betterTau
        if (frequency < config.minFrequencyHz || frequency > config.maxFrequencyHz) return PitchResult.Rejected(RejectionReason.OUT_OF_RANGE)
        val periodicity = (1f - cmndf[tau]).coerceIn(0f, 1f)
        val confidence = (periodicity * (1f - 0.15f * (cmndf[tau] / config.threshold).coerceIn(0f, 1f))).coerceIn(0f, 1f)
        return if (confidence >= config.minimumConfidence) PitchResult.Accepted(frequency, confidence, periodicity, rms)
        else PitchResult.Rejected(RejectionReason.LOW_CONFIDENCE)
    }
}
