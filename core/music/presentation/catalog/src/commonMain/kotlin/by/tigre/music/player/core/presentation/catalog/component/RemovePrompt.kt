package by.tigre.music.player.core.presentation.catalog.component

data class RemovePrompt(
    val onHide: () -> Unit,
    val onDeleteForever: () -> Unit,
)
