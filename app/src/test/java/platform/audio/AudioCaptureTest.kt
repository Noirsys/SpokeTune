package platform.audio

import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AudioCaptureTest {
    @Test
    fun capturesMonoPcmAndAlwaysReleasesRecorder() = runBlocking {
        val fake = FakeRecorder()
        val result = AudioCapture({ _, _ -> fake }, AudioCaptureConfig(1_000, 10, 20)).capture()

        assertTrue(result is AudioCaptureResult.Captured)
        assertEquals(10, (result as AudioCaptureResult.Captured).pcm.size)
        assertEquals(1, fake.starts)
        assertEquals(1, fake.stops)
        assertEquals(1, fake.releases)
    }

    @Test
    fun initializationFailureDoesNotExposeAudio() = runBlocking {
        val result = AudioCapture({ _, _ -> null }, AudioCaptureConfig(1_000, 10, 20)).capture()
        assertEquals(AudioCaptureResult.Failed(FailureReason.INITIALIZATION), result)
    }

    private class FakeRecorder : Recorder {
        override val stateInitialized = true
        var starts = 0
        var stops = 0
        var releases = 0
        override fun start() { starts++ }
        override fun read(destination: ShortArray, offset: Int, size: Int): Int {
            destination[offset] = 16_384
            return 1.coerceAtMost(size)
        }
        override fun stop() { stops++ }
        override fun release() { releases++ }
    }
}
