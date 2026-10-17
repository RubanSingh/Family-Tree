package dna.familytree.billing

/**
 * Persistence boundary. The app implements this (backed by its settings file);
 * the billing module never touches app state directly.
 */
interface EntitlementStore {
    val isPremium: Boolean
    val activePlan: PremiumPlan?

    /** Called after a purchase is acknowledged or an owned purchase is restored. */
    fun grant(plan: PremiumPlan, purchaseToken: String, purchaseTimeMillis: Long)

    /** Called when Play reports no active entitlement for a subscription that was previously granted. */
    fun revoke()
}
