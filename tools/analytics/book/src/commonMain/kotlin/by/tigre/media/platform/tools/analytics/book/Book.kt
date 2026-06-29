package by.tigre.media.platform.tools.analytics.book

import by.tigre.media.platform.tools.analytics.common.AnalyticsAction
import by.tigre.media.platform.tools.analytics.common.AnalyticsApp
import by.tigre.media.platform.tools.analytics.common.AnalyticsDoc
import by.tigre.media.platform.tools.analytics.common.AnalyticsScope
import by.tigre.media.platform.tools.analytics.common.AnalyticsScreen

object AudiobookEvents {

    sealed class Action(override val name: String) : AnalyticsAction {

        @AnalyticsScope(AnalyticsApp.AUDIOBOOK)
        @AnalyticsDoc("Open audiobook library catalog")
        data object NavOpenCatalog : Action("book_nav_open_catalog")

        @AnalyticsScope(AnalyticsApp.AUDIOBOOK)
        @AnalyticsDoc("Open night timer settings")
        data object NavOpenNightTimer : Action("book_nav_open_night_timer")

        @AnalyticsScope(AnalyticsApp.AUDIOBOOK)
        @AnalyticsDoc("Select book in library")
        data object CatalogSelectBook : Action("book_catalog_select_book")

        @AnalyticsScope(AnalyticsApp.AUDIOBOOK)
        @AnalyticsDoc("Open folder selection for audiobook library")
        data object CatalogOpenFolderSettings : Action("book_catalog_open_folder_settings")
    }

    sealed class Screen(
        override val name: String,
        override val skip: Boolean = false,
    ) : AnalyticsScreen {

        @AnalyticsScope(AnalyticsApp.AUDIOBOOK)
        @AnalyticsDoc("Audiobook library catalog root")
        data object Catalog : Screen("book_screen_catalog")

        @AnalyticsScope(AnalyticsApp.AUDIOBOOK)
        @AnalyticsDoc("Folder selection for audiobook sources")
        data object FolderSelection : Screen("book_screen_folder_selection")

        @AnalyticsScope(AnalyticsApp.AUDIOBOOK)
        @AnalyticsDoc("Book list in library")
        data object BookList : Screen("book_screen_book_list")

        @AnalyticsScope(AnalyticsApp.AUDIOBOOK)
        @AnalyticsDoc("Night timer settings")
        data object NightTimerSettings : Screen("book_screen_night_timer")

        @AnalyticsScope(AnalyticsApp.AUDIOBOOK)
        @AnalyticsDoc("Playback speed settings")
        data object PlaybackSpeedSettings : Screen("book_screen_playback_speed")
    }
}
