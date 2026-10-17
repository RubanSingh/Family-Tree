package dna.familytree.billing

import com.android.billingclient.api.ProductDetails

/**
 * A purchasable offer resolved from Google Play, ready to display and to launch.
 */
data class PlanOffer(
    val plan: PremiumPlan,
    /** Localised price, e.g. "₹149.00" or "$2.99". */
    val formattedPrice: String,
    /** Price in micro-units of [currencyCode], for "save X%" style maths. */
    val priceMicros: Long,
    val currencyCode: String,
    /** ISO-8601 period for subscriptions, e.g. "P1M"; null for one-time products. */
    val billingPeriod: String?,
    /** Free-trial / intro period if the base plan has one, e.g. "P7D". */
    val introPeriod: String?,
    val productDetails: ProductDetails,
    /** Required for subscriptions when launching the billing flow. */
    val offerToken: String?
) {
    val title: String get() = productDetails.name
    val description: String get() = productDetails.description

    companion object {
        /** Builds an offer from Play's [ProductDetails], picking the cheapest base-plan offer for subscriptions. */
        fun from(plan: PremiumPlan, details: ProductDetails): PlanOffer? {
            return if (plan.isSubscription) {
                val offers = details.subscriptionOfferDetails ?: return null
                val best = offers.minByOrNull { offer ->
                    offer.pricingPhases.pricingPhaseList.lastOrNull()?.priceAmountMicros ?: Long.MAX_VALUE
                } ?: return null
                val phases = best.pricingPhases.pricingPhaseList
                val recurring = phases.lastOrNull() ?: return null
                val intro = phases.firstOrNull { it.priceAmountMicros == 0L }
                PlanOffer(
                    plan = plan,
                    formattedPrice = recurring.formattedPrice,
                    priceMicros = recurring.priceAmountMicros,
                    currencyCode = recurring.priceCurrencyCode,
                    billingPeriod = recurring.billingPeriod,
                    introPeriod = intro?.billingPeriod,
                    productDetails = details,
                    offerToken = best.offerToken
                )
            } else {
                val oneTime = details.oneTimePurchaseOfferDetails ?: return null
                PlanOffer(
                    plan = plan,
                    formattedPrice = oneTime.formattedPrice,
                    priceMicros = oneTime.priceAmountMicros,
                    currencyCode = oneTime.priceCurrencyCode,
                    billingPeriod = null,
                    introPeriod = null,
                    productDetails = details,
                    offerToken = null
                )
            }
        }
    }
}
