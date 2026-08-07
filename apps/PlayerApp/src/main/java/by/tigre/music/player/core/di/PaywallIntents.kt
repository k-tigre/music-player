package by.tigre.music.player.core.di

import by.tigre.media.platform.entitlements.Feature

object PaywallIntents {
    const val EXTRA_FEATURE = "by.tigre.music.player.paywall.feature"
    const val EXTRA_SOURCE = "by.tigre.music.player.paywall.source"

    fun featureFrom(name: String?): Feature? =
        Feature.entries.firstOrNull { it.name == name }
}
