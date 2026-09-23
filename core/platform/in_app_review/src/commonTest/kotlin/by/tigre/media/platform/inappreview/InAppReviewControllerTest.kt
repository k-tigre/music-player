package by.tigre.media.platform.inappreview

import by.tigre.media.platform.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InAppReviewControllerTest {

    private fun controller(store: InAppReviewStore = InAppReviewStore(MemoryPreferences())) =
        InAppReviewController(store)

    @Test
    fun notEligibleOnFirstLaunchEvenWithPlayback() {
        val store = InAppReviewStore(MemoryPreferences())
        val controller = InAppReviewController(store)
        controller.onColdStart()
        controller.onPlayingElapsed(InAppReviewController.DEFAULT_PLAYING_THRESHOLD_MS)
        assertFalse(controller.canRequest(nowMillis = 1_000L, rcEnabled = true))
    }

    @Test
    fun notEligibleWithoutEnoughPlayback() {
        val store = InAppReviewStore(MemoryPreferences())
        val controller = InAppReviewController(store)
        controller.onColdStart()
        controller.onColdStart()
        controller.onPlayingElapsed(InAppReviewController.DEFAULT_PLAYING_THRESHOLD_MS - 1)
        assertFalse(controller.canRequest(nowMillis = 1_000L, rcEnabled = true))
    }

    @Test
    fun eligibleAfterSecondLaunchAndThreshold() {
        val store = InAppReviewStore(MemoryPreferences())
        val controller = InAppReviewController(store)
        controller.onColdStart()
        controller.onColdStart()
        controller.onPlayingElapsed(InAppReviewController.DEFAULT_PLAYING_THRESHOLD_MS)
        assertTrue(controller.canRequest(nowMillis = 1_000L, rcEnabled = true))
    }

    @Test
    fun rcDisabledBlocksRequest() {
        val store = InAppReviewStore(MemoryPreferences())
        val controller = InAppReviewController(store)
        controller.onColdStart()
        controller.onColdStart()
        controller.onPlayingElapsed(InAppReviewController.DEFAULT_PLAYING_THRESHOLD_MS)
        assertFalse(controller.canRequest(nowMillis = 1_000L, rcEnabled = false))
    }

    @Test
    fun secondAttemptRequiresSixtyDays() {
        val store = InAppReviewStore(MemoryPreferences())
        val controller = InAppReviewController(store)
        controller.onColdStart()
        controller.onColdStart()
        controller.onPlayingElapsed(InAppReviewController.DEFAULT_PLAYING_THRESHOLD_MS)

        val firstAttemptAt = 1_000_000L
        controller.recordAttempt(firstAttemptAt)
        assertFalse(
            controller.canRequest(
                nowMillis = firstAttemptAt + InAppReviewController.DEFAULT_RETRY_AFTER_MS - 1,
                rcEnabled = true,
            ),
        )
        assertTrue(
            controller.canRequest(
                nowMillis = firstAttemptAt + InAppReviewController.DEFAULT_RETRY_AFTER_MS,
                rcEnabled = true,
            ),
        )
    }

    @Test
    fun neverAfterTwoAttempts() {
        val store = InAppReviewStore(MemoryPreferences())
        val controller = InAppReviewController(store)
        controller.onColdStart()
        controller.onColdStart()
        controller.onPlayingElapsed(InAppReviewController.DEFAULT_PLAYING_THRESHOLD_MS)

        val first = 1_000_000L
        controller.recordAttempt(first)
        controller.recordAttempt(first + InAppReviewController.DEFAULT_RETRY_AFTER_MS)
        assertFalse(
            controller.canRequest(
                nowMillis = first + InAppReviewController.DEFAULT_RETRY_AFTER_MS * 3,
                rcEnabled = true,
            ),
        )
    }

    private class MemoryPreferences : Preferences {
        private val booleans = mutableMapOf<String, Boolean>()
        private val strings = mutableMapOf<String, String?>()
        private val ints = mutableMapOf<String, Int?>()
        private val longs = mutableMapOf<String, Long?>()

        override fun saveBoolean(key: String, value: Boolean) {
            booleans[key] = value
        }

        override fun saveBooleans(vararg values: Pair<String, Boolean>) {
            values.forEach { (k, v) -> booleans[k] = v }
        }

        override fun loadBoolean(key: String, default: Boolean): Boolean = booleans[key] ?: default

        override fun saveString(key: String, value: String?) {
            strings[key] = value
        }

        override fun loadString(key: String, default: String?): String? = strings[key] ?: default

        override fun saveInt(key: String, value: Int?) {
            ints[key] = value
        }

        override fun loadInt(key: String, default: Int): Int = ints[key] ?: default

        override fun saveLong(key: String, value: Long?) {
            longs[key] = value
        }

        override fun loadLong(key: String, default: Long): Long = longs[key] ?: default
    }
}
