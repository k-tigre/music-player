package by.tigre.media.platform.playback.eq

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EqGainInterpolationTest {

    @Test
    fun alignSameSize_keepsValues() {
        val centers = floatArrayOf(160f, 400f, 800f)
        val saved = listOf(1f, 2f, 3f)
        assertEquals(saved, EqGainInterpolation.alignOrRemapToUiBands(saved, centers))
    }

    @Test
    fun alignFromMusicElevenBands_remapsOntoSpeechSix() {
        val music = UiEqConfig.MusicBandCentersHz
        val speech = AudiobookSpeechEq.bandCentersHz
        val flatMusic = List(music.size) { 0f }
        val remapped = EqGainInterpolation.alignOrRemapToUiBands(flatMusic, speech)
        assertEquals(speech.size, remapped.size)
        assertTrue(remapped.all { it == 0f })
    }

    @Test
    fun shelfAboveHighestBand_usesLastGain() {
        val centers = floatArrayOf(160f, 400f, 12000f)
        val gains = floatArrayOf(0f, 0f, 6f)
        val above = EqGainInterpolation.interpolateGainDb(16000.0, centers, gains)
        assertEquals(6f, above)
    }

    @Test
    fun audiobookSpeechPresets_matchBandCount() {
        val n = AudiobookSpeechEq.bandCentersHz.size
        assertEquals(6, n)
        AudiobookSpeechEq.presets.forEach { preset ->
            assertEquals(n, preset.gainsDb.size, preset.id)
        }
    }
}
