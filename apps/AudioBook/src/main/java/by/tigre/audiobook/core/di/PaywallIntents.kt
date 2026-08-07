package by.tigre.audiobook.core.di

import by.tigre.media.platform.entitlements.Feature

object PaywallIntents {
    const val EXTRA_FEATURE = "by.tigre.audiobook.paywall.feature"
    const val EXTRA_SOURCE = "by.tigre.audiobook.paywall.source"

    fun featureFrom(name: String?): Feature? =
        Feature.entries.firstOrNull { it.name == name }
}
