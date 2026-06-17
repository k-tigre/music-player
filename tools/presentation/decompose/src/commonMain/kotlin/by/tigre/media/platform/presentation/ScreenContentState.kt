package by.tigre.media.platform.presentation

sealed interface ScreenContentState<out T> {
    data object Loading : ScreenContentState<Nothing>
    data object Error : ScreenContentState<Nothing>
    data class Content<out T>(val value: T) : ScreenContentState<T>
}
