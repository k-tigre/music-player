package by.tigre.music.player.core.data.playback.impl.dsp

import java.io.IOException
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream

/**
 * 16-bit PCM peaking EQ; applies [DesktopEqualizerPresets] bands per channel while decoding for [javax.sound.sampled.Clip].
 */
internal class EqualizingPcmAudioInputStream(
    private val backing: AudioInputStream,
    channels: Int,
    sampleRate: Float,
    gainsDb: FloatArray,
    centersHz: FloatArray,
) : AudioInputStream(backing, backing.format, backing.frameLength) {

    private val processor =
        Pcm16EqualizerProcessor.forGains(channels, sampleRate, gainsDb, centersHz)

    private val bigEndian: Boolean = backing.format.isBigEndian

    override fun read(): Int {
        throw UnsupportedOperationException()
    }

    @Throws(IOException::class)
    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val n = backing.read(b, off, len)
        if (n <= 0) return n
        val fmt = format
        if (fmt.encoding != AudioFormat.Encoding.PCM_SIGNED || fmt.sampleSizeInBits != 16) {
            return n
        }
        processor.processInterleavedPcmS16(b, off, n, bigEndian)
        return n
    }

    override fun available(): Int = backing.available()

    override fun skip(n: Long): Long = backing.skip(n)
}
