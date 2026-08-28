package signal

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.Test
import kotlin.test.assertEquals

class Cc0SpokeFixtureTest {
    @Test
    fun `real CC0 brushed spokes are rejected instead of inventing one pitch`() {
        val bytes = requireNotNull(javaClass.getResourceAsStream("/fixtures/cc0/bicycle_spokes_brushed.wav")) {
            "CC0 fixture missing"
        }.use { it.readBytes() }
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val sampleRate = buffer.getInt(24)
        val samples = FloatArray((bytes.size - 44) / 2) { index ->
            buffer.getShort(44 + index * 2) / 32768f
        }

        val result = YinPitchAnalyzer().analyze(samples, sampleRate)

        assertEquals(
            PitchResult.Rejected(RejectionReason.LOW_CONFIDENCE),
            result,
            "A brush across several spokes must not be presented as one trustworthy spoke reading",
        )
    }
}
