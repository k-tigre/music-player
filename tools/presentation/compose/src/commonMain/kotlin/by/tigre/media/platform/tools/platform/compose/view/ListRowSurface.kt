package by.tigre.media.platform.tools.platform.compose.view

enum class ListRowSurface {
    /** Inset card inside list horizontal padding (legacy). */
    CardInset,

    /** Card spans list width; list should use horizontal padding 0. */
    CardFullWidth,

    /** No card — flat row with divider. */
    Plain,
}
