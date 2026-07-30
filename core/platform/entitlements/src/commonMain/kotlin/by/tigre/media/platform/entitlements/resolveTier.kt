package by.tigre.media.platform.entitlements

fun resolveTier(productIds: Set<String>, app: AppSku): Tier {
    val (plusSku, proSku) = when (app) {
        AppSku.AudioBook -> SkuIds.AudioBook.PLUS to SkuIds.AudioBook.PRO
        AppSku.Music -> SkuIds.Music.PLUS to SkuIds.Music.PRO
    }
    return when {
        proSku in productIds -> Tier.Pro
        plusSku in productIds -> Tier.Plus
        else -> Tier.Free
    }
}
