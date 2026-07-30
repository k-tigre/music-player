package by.tigre.media.platform.entitlements

object SkuIds {
    object AudioBook {
        const val PLUS = "audiobook_plus"
        const val PRO = "audiobook_pro"
        const val TIP_COFFEE = "audiobook_tip_coffee"
        const val TIP_PIZZA = "audiobook_tip_pizza"
        val subscriptions = listOf(PLUS, PRO)
    }

    object Music {
        const val PLUS = "music_plus"
        const val PRO = "music_pro"
        const val TIP_COFFEE = "music_tip_coffee"
        const val TIP_PIZZA = "music_tip_pizza"
        val subscriptions = listOf(PLUS, PRO)
    }
}
