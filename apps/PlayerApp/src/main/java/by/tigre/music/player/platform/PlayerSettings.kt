package by.tigre.music.player.platform

interface PlayerSettings {
    fun shouldShowPrompt(): Boolean
    fun markPromptShown()
}
