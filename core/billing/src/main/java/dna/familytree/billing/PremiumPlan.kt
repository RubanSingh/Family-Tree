package dna.familytree.billing

import com.android.billingclient.api.BillingClient

/**
 * The two ways to get Premium. Product ids must match Google Play Console exactly.
 *
 * - [MONTHLY]  → subscription `family_tree_premium_monthly`, base plan id `monthly` (P1M).
 * - [LIFETIME] → one-time, non-consumable in-app product `family_tree_premium` (the original product,
 *                so existing buyers stay entitled).
 */
enum class PremiumPlan(
    val productId: String,
    @BillingClient.ProductType val productType: String
) {
    MONTHLY("family_tree_premium_monthly", BillingClient.ProductType.SUBS),
    LIFETIME("family_tree_premium", BillingClient.ProductType.INAPP);

    val isSubscription: Boolean get() = productType == BillingClient.ProductType.SUBS

    companion object {
        /** Stable key for persistence (`settings.json`). */
        fun fromKey(key: String?): PremiumPlan? = values().firstOrNull { it.name == key }

        fun fromProductId(productId: String): PremiumPlan? = values().firstOrNull { it.productId == productId }
    }
}
