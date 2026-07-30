package by.tigre.audiobook.presentation.paywall

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import by.tigre.audiobook.R
import by.tigre.audiobook.core.di.PaywallSection
import by.tigre.media.platform.billing.OfferUi
import by.tigre.media.platform.billing.ProductUi
import by.tigre.media.platform.tools.platform.compose.ComposableView

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

        ModalBottomSheet(
            onDismissRequest = component::dismiss,
            modifier = modifier,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.paywall_title),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = stringResource(R.string.paywall_benefits),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (initialSection == PaywallSection.Tips) {
                    TipsSection(coffeeTip, pizzaTip)
                    HorizontalDivider()
                    Text(
                        text = stringResource(R.string.paywall_plans_heading),
                        style = MaterialTheme.typography.titleMedium,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.paywall_plans_heading),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }

                SubscriptionPlans(plusProduct, proProduct)

                if (initialSection != PaywallSection.Tips) {
                    HorizontalDivider()
                    TipsSection(coffeeTip, pizzaTip)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = component::restorePurchases,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.paywall_restore))
                    }
                    OutlinedButton(
                        onClick = component::dismiss,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.paywall_maybe_later))
                    }
                }
            }
        }
    }

    @Composable
    private fun SubscriptionPlans(
        plusProduct: ProductUi?,
        proProduct: ProductUi?,
    ) {
        SubscriptionProduct(
            product = plusProduct,
            tier = stringResource(R.string.paywall_plus),
            benefits = stringResource(R.string.paywall_plus_benefits),
        )
        SubscriptionProduct(
            product = proProduct,
            tier = stringResource(R.string.paywall_pro),
            benefits = stringResource(R.string.paywall_pro_benefits),
        )
    }

    @Composable
    private fun SubscriptionProduct(
        product: ProductUi?,
        tier: String,
        benefits: String,
    ) {
        val offers = product?.offers.orEmpty()
        if (offers.isEmpty()) {
            Text(
                text = stringResource(R.string.paywall_price_loading, tier),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return
        }
        offers.forEach { offer ->
            val recommended = product?.productId == "audiobook_pro" && offer.basePlanId == "yearly"
            Card(
                border = if (recommended) {
                    BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                } else {
                    null
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(tier, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        if (recommended) {
                            Text(
                                text = stringResource(R.string.paywall_best_value),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                    Text(benefits, style = MaterialTheme.typography.bodySmall)
                    Button(
                        onClick = { component.purchaseSubscription(product!!.productId, offer.offerToken) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.paywall_choose_plan, offer.formattedPrice))
                    }
                }
            }
        }
    }

    @Composable
    private fun TipsSection(
        coffeeTip: ProductUi?,
        pizzaTip: ProductUi?,
    ) {
        Text(
            text = stringResource(R.string.paywall_tips_heading),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.paywall_tips_description),
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
            onClick = { component.purchaseTip(product!!.productId) },
            enabled = product != null && offer != null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            val price = offer?.formattedPrice.orEmpty()
            Text(stringResource(labelRes, price))
        }
    }
}
