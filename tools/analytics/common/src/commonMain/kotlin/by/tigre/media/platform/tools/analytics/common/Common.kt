package by.tigre.media.platform.tools.analytics.common

object CommonEvents {

    sealed class Action(override val name: String) : AnalyticsAction {

        @AnalyticsScope(AnalyticsApp.PLAYER, AnalyticsApp.AUDIOBOOK, AnalyticsApp.DESKTOP)
        @AnalyticsDoc("Play / resume playback")
        data object PlayerPlay : Action("common_player_play")

        @AnalyticsScope(AnalyticsApp.PLAYER, AnalyticsApp.AUDIOBOOK, AnalyticsApp.DESKTOP)
        @AnalyticsDoc("Pause playback")
        data object PlayerPause : Action("common_player_pause")

        @AnalyticsScope(AnalyticsApp.PLAYER, AnalyticsApp.AUDIOBOOK, AnalyticsApp.DESKTOP)
        @AnalyticsDoc("Skip to next track or chapter")
        data object PlayerNext : Action("common_player_next")

        @AnalyticsScope(AnalyticsApp.PLAYER, AnalyticsApp.AUDIOBOOK, AnalyticsApp.DESKTOP)
        @AnalyticsDoc("Skip to previous track or chapter")
        data object PlayerPrev : Action("common_player_prev")

        @AnalyticsScope(AnalyticsApp.PLAYER, AnalyticsApp.AUDIOBOOK, AnalyticsApp.DESKTOP)
        @AnalyticsDoc("Seek backward 15 seconds")
        data object PlayerSeekBack15 : Action("common_player_seek_back_15")

        @AnalyticsScope(AnalyticsApp.PLAYER, AnalyticsApp.AUDIOBOOK, AnalyticsApp.DESKTOP)
        @AnalyticsDoc("Seek forward 15 seconds")
        data object PlayerSeekForward15 : Action("common_player_seek_forward_15")

        @AnalyticsScope(AnalyticsApp.PLAYER, AnalyticsApp.AUDIOBOOK, AnalyticsApp.DESKTOP)
        @AnalyticsDoc("Seek backward 60 seconds")
        data object PlayerSeekBack60 : Action("common_player_seek_back_60")

        @AnalyticsScope(AnalyticsApp.PLAYER, AnalyticsApp.AUDIOBOOK, AnalyticsApp.DESKTOP)
        @AnalyticsDoc("Seek forward 60 seconds")
        data object PlayerSeekForward60 : Action("common_player_seek_forward_60")

        @AnalyticsScope(AnalyticsApp.PLAYER, AnalyticsApp.DESKTOP)
        @AnalyticsDoc("Toggle shuffle / repeat order mode")
        data object PlayerShuffleToggle : Action("common_player_shuffle_toggle")

        @AnalyticsScope(AnalyticsApp.PLAYER, AnalyticsApp.AUDIOBOOK, AnalyticsApp.DESKTOP)
        @AnalyticsDoc("Open full player screen")
        data object NavOpenPlayer : Action("common_nav_open_player")

        @AnalyticsScope(AnalyticsApp.PLAYER, AnalyticsApp.AUDIOBOOK, AnalyticsApp.DESKTOP)
        @AnalyticsDoc("Open equalizer screen")
        data object NavOpenEqualizer : Action("common_nav_open_equalizer")
    }

    sealed class Screen(
        override val name: String,
        override val skip: Boolean = false,
    ) : AnalyticsScreen {

        @AnalyticsScope(AnalyticsApp.PLAYER, AnalyticsApp.AUDIOBOOK, AnalyticsApp.DESKTOP)
        @AnalyticsDoc("Full player screen")
        data object Player : Screen("common_screen_player")

        @AnalyticsScope(AnalyticsApp.PLAYER, AnalyticsApp.AUDIOBOOK, AnalyticsApp.DESKTOP)
        @AnalyticsDoc("Equalizer screen")
        data object Equalizer : Screen("common_screen_equalizer")

        @AnalyticsScope(AnalyticsApp.PLAYER, AnalyticsApp.DESKTOP)
        @AnalyticsDoc("Root overlay without a dedicated screen (not sent to analytics)")
        data object RootOverlay : Screen("common_screen_root_overlay", skip = true)
    }
}
