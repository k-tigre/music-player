package by.tigre.music.player.presentation.base

sealed interface ScreenContentState<out T> {
    object Loading : ScreenContentState<Nothing>
    object Error : ScreenContentState<Nothing>
    data class Content<out T>(val value: T) : ScreenContentState<T>
}
