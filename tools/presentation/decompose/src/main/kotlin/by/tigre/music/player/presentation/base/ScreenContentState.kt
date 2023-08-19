package by.tigre.music.player.presentation.base

sealed interface ScreenContentState<out T> {
    data object Loading : ScreenContentState<Nothing>
    data object Error : ScreenContentState<Nothing>
    data class Content<out T>(val value: T) : ScreenContentState<T>
}
