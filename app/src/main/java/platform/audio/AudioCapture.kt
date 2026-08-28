package platform.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** Parameters are deliberately bounded to prevent an accidental open-ended recording. */
data class AudioCaptureConfig(
    val sampleRateHz: Int = 44_100,
    val durationMillis: Long = 1_500,
    val minimumBufferBytes: Int = 4_096
) {
    init {
        require(sampleRateHz > 0)
        require(durationMillis in 1..10_000)
        require(minimumBufferBytes >= 2)
    }
}

sealed interface AudioCaptureResult {
    data class Captured(val pcm: FloatArray, val sampleRateHz: Int) : AudioCaptureResult
    data object Cancelled : AudioCaptureResult
    data class Failed(val reason: FailureReason) : AudioCaptureResult
}

enum class FailureReason { INITIALIZATION, READ, EMPTY }

/** Small seam around AudioRecord, allowing deterministic JVM tests and no permission logic here. */
interface Recorder {
    val stateInitialized: Boolean
    fun start()
    fun read(destination: ShortArray, offset: Int, size: Int): Int
    fun stop()
    fun release()
}

fun interface RecorderFactory {
    fun create(config: AudioCaptureConfig, bufferBytes: Int): Recorder?
}

class AndroidRecorderFactory : RecorderFactory {
    override fun create(config: AudioCaptureConfig, bufferBytes: Int): Recorder? {
        val recorder = AudioRecord(
            MediaRecorder.AudioSource.DEFAULT,
            config.sampleRateHz,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferBytes
        )
        return if (recorder.state == AudioRecord.STATE_INITIALIZED) AndroidRecorder(recorder) else {
            recorder.release()
            null
        }
    }
}

private class AndroidRecorder(private val delegate: AudioRecord) : Recorder {
    override val stateInitialized = delegate.state == AudioRecord.STATE_INITIALIZED
    override fun start() = delegate.startRecording()
    override fun read(destination: ShortArray, offset: Int, size: Int) =
        delegate.read(destination, offset, size, AudioRecord.READ_BLOCKING)
    override fun stop() = runCatching { delegate.stop() }.getOrThrow()
    override fun release() = delegate.release()
}

/** Captures ephemeral, mono, normalized PCM. The returned buffer is owned by the caller. */
class AudioCapture(
    private val factory: RecorderFactory,
    private val config: AudioCaptureConfig = AudioCaptureConfig()
) {
    suspend fun capture(): AudioCaptureResult {
        val bytesPerSecond = config.sampleRateHz * 2
        val bufferBytes = maxOf(config.minimumBufferBytes, bytesPerSecond / 10).let { it and -2 }
        val recorder = factory.create(config, bufferBytes) ?: return AudioCaptureResult.Failed(FailureReason.INITIALIZATION)
        val samples = ShortArray((config.sampleRateHz * config.durationMillis / 1_000).toInt())
        var written = 0
        var started = false
        return try {
            if (!recorder.stateInitialized) return AudioCaptureResult.Failed(FailureReason.INITIALIZATION)
            recorder.start()
            started = true
            while (written < samples.size) {
                currentCoroutineContext().ensureActive()
                val count = recorder.read(samples, written, samples.size - written)
                if (count <= 0) return AudioCaptureResult.Failed(FailureReason.READ)
                written += count
            }
            if (written == 0) return AudioCaptureResult.Failed(FailureReason.EMPTY)
            AudioCaptureResult.Captured(
                FloatArray(written) { samples[it] / 32_768f },
                config.sampleRateHz
            )
        } catch (_: kotlinx.coroutines.CancellationException) {
            AudioCaptureResult.Cancelled
        } finally {
            if (started) runCatching { recorder.stop() }
            recorder.release()
            samples.fill(0)
        }
    }
}
