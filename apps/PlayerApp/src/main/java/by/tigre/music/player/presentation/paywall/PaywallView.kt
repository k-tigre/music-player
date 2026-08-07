package by.tigre.music.player.presentation.paywall

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import by.tigre.media.platform.billing.OfferUi
import by.tigre.media.platform.billing.ProductUi
import by.tigre.media.platform.entitlements.SkuIds
import by.tigre.media.platform.entitlements.Tier
import by.tigre.media.platform.entitlements.parseSubscriptionPeriod
import by.tigre.media.platform.entitlements.SubscriptionPeriod
import by.tigre.media.platform.entitlements.visibleOffersForProduct
import by.tigre.media.platform.tools.platform.compose.ComposableView
import by.tigre.music.player.R
import by.tigre.music.player.core.di.PaywallSection

class PaywallView(
    private val component: PaywallComponent,
    private val initialSection: PaywallSection,
) : ComposableView {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Draw(modifier: Modifier) {
        val plusProduct by component.plusProduct.collectAsState()
        val proProduct by component.proProduct.collectAsState()
        val coffeeTip by component.coffeeTip.collectAsState()
        val pizzaTip by component.pizzaTip.collectAsState()
        val tier by component.tier.collectAsState()
        val ownedBasePlanIds by component.ownedBasePlanIds.collectAsState()
        val thanks by component.thanks.collectAsState()
        val scrollState = rememberScrollState()
        val plansSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val thanksSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var showPlansSheet by remember { mutableStateOf(true) }
        var showThanksSheet by remember { mutableStateOf(false) }

        LaunchedEffect(thanks) {
            if (thanks == null) return@LaunchedEffect
            if (showPlansSheet) {
                runCatching { plansSheetState.hide() }
                showPlansSheet = false
            }
            showThanksSheet = true
        }

        if (showPlansSheet) {
            ModalBottomSheet(
                onDismissRequest = {
                    if (thanks == null) component.dismiss()
                },
                sheetState = plansSheetState,
                modifier = modifier,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                        .padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    PlansContent(
                        plusProduct = plusProduct,
                        proProduct = proProduct,
                        coffeeTip = coffeeTip,
                        pizzaTip = pizzaTip,
                        tier = tier,
                        ownedBasePlanIds = ownedBasePlanIds,
                    )
                }
            }
        }

        if (showThanksSheet && thanks != null) {
            ModalBottomSheet(
                onDismissRequest = component::dismiss,
                sheetState = thanksSheetState,
                modifier = modifier,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                        .padding(bottom = 16.dp),
                ) {
                    ThanksContent(thanks!!)
                }
            }
        }
    }

    @Composable
    private fun PlansContent(
        plusProduct: ProductUi?,
        proProduct: ProductUi?,
        coffeeTip: ProductUi?,
        pizzaTip: ProductUi?,
        tier: Tier,
        ownedBasePlanIds: Map<String, String>,
    ) {
        Text(stringResource(R.string.paywall_title), style = MaterialTheme.typography.headlineSmall)
        Text(
            stringResource(R.string.paywall_benefits),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (initialSection == PaywallSection.Tips) {
            TipsSection(coffeeTip, pizzaTip)
            HorizontalDivider()
        }
        Text(stringResource(R.string.paywall_plans_heading), style = MaterialTheme.typography.titleMedium)
        SubscriptionPlans(plusProduct, proProduct, tier, ownedBasePlanIds)
        if (initialSection != PaywallSection.Tips) {
            HorizontalDivider()
            TipsSection(coffeeTip, pizzaTip)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(onClick = component::restorePurchases, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.paywall_restore))
            }
            OutlinedButton(onClick = component::dismiss, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.paywall_maybe_later))
            }
        }
    }

    @Composable
    private fun ThanksContent(thanks: PaywallThanks) {
        val emoji = when (thanks) {
            PaywallThanks.Coffee -> "☕"
            PaywallThanks.Pizza -> "🍕"
            PaywallThanks.Subscription -> "✨"
        }
        val title = when (thanks) {
            PaywallThanks.Coffee -> stringResource(R.string.paywall_thanks_title_coffee)
            PaywallThanks.Pizza -> stringResource(R.string.paywall_thanks_title_pizza)
            PaywallThanks.Subscription -> stringResource(R.string.paywall_thanks_title_subscription)
        }
        val message = when (thanks) {
            PaywallThanks.Coffee -> stringResource(R.string.paywall_thanks_message_coffee)
            PaywallThanks.Pizza -> stringResource(R.string.paywall_thanks_message_pizza)
            PaywallThanks.Subscription -> stringResource(R.string.paywall_thanks_message_subscription)
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = emoji, fontSize = 56.sp)
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Button(
                onClick = component::dismiss,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.paywall_thanks_done))
            }
        }
    }

    @Composable
    private fun SubscriptionPlans(
        plusProduct: ProductUi?,
        proProduct: ProductUi?,
        tier: Tier,
        ownedBasePlanIds: Map<String, String>,
    ) {
        SubscriptionProduct(
            product = plusProduct,
            tier = stringResource(R.string.paywall_plus),
            benefits = stringResource(R.string.paywall_plus_benefits),
            currentTier = tier,
            ownedBasePlanIds = ownedBasePlanIds,
        )
        SubscriptionProduct(
            product = proProduct,
            tier = stringResource(R.string.paywall_pro),
            benefits = stringResource(R.string.paywall_pro_benefits),
            currentTier = tier,
            ownedBasePlanIds = ownedBasePlanIds,
        )
    }

    @Composable
    private fun SubscriptionProduct(
        product: ProductUi?,
        tier: String,
        benefits: String,
        currentTier: Tier,
        ownedBasePlanIds: Map<String, String>,
    ) {
        if (product == null) {
            Text(
                stringResource(R.string.paywall_price_loading, tier),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return
        }
        val offers = visibleOffersForProduct(
            productId = product.productId,
            offers = product.offers,
            tier = currentTier,
            ownedBasePlanIds = ownedBasePlanIds,
            plusSku = SkuIds.Music.PLUS,
            proSku = SkuIds.Music.PRO,
        ).sortedWith(offerPeriodComparator)
        if (offers.isEmpty()) return
        offers.forEach { offer ->
            val period = periodLabel(offer.basePlanId)
            val recommended = product.productId == SkuIds.Music.PRO &&
                parseSubscriptionPeriod(offer.basePlanId) == SubscriptionPeriod.Yearly
            Card(
                border = if (recommended) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = if (period.isEmpty()) {
                                tier
                            } else {
                                stringResource(R.string.paywall_tier_period, tier, period)
                            },
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                        )
                        if (recommended) {
                            Text(
                                stringResource(R.string.paywall_best_value),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                    Text(benefits, style = MaterialTheme.typography.bodySmall)
                    Button(
                        onClick = {
                            component.purchaseSubscription(
                                productId = product.productId,
                                offerToken = offer.offerToken,
                                basePlanId = offer.basePlanId,
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            if (period.isEmpty()) {
                                stringResource(R.string.paywall_choose_plan, offer.formattedPrice)
                            } else {
                                stringResource(
                                    R.string.paywall_choose_plan_period,
                                    offer.formattedPrice,
                                    period,
                                )
                            },
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun TipsSection(coffeeTip: ProductUi?, pizzaTip: ProductUi?) {
        Text(stringResource(R.string.paywall_tips_heading), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.paywall_tips_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TipButton(coffeeTip, R.string.paywall_tip_coffee)
        TipButton(pizzaTip, R.string.paywall_tip_pizza)
    }

    @Composable
    private fun TipButton(product: ProductUi?, labelRes: Int) {
        val offer: OfferUi? = product?.offers?.firstOrNull()
        OutlinedButton(
            onClick = { product?.productId?.let(component::purchaseTip) },
            enabled = product != null && offer != null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(labelRes, offer?.formattedPrice.orEmpty()))
        }
    }

    @Composable
    private fun periodLabel(basePlanId: String?): String = when (parseSubscriptionPeriod(basePlanId)) {
        SubscriptionPeriod.Yearly -> stringResource(R.string.paywall_period_yearly)
        SubscriptionPeriod.Monthly -> stringResource(R.string.paywall_period_monthly)
        SubscriptionPeriod.Unknown -> ""
    }

    private companion object {
        val offerPeriodComparator = compareBy<OfferUi> { offer ->
            when (parseSubscriptionPeriod(offer.basePlanId)) {
                SubscriptionPeriod.Yearly -> 0
                SubscriptionPeriod.Monthly -> 1
                SubscriptionPeriod.Unknown -> 2
            }
        }
    }
}
