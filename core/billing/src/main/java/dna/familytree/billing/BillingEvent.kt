package dna.familytree.billing

/**
 * Everything the UI needs to know about, delivered on the main thread.
 */
sealed class BillingEvent {
    /** Connected to Play; offers are being loaded. */
    object Connecting : BillingEvent()

    /** Offers resolved from Play, in display order (monthly first, then lifetime). */
    data class OffersLoaded(val offers: List<PlanOffer>) : BillingEvent()

    /** A purchase completed and was acknowledged; Premium is now granted. */
    data class PurchaseGranted(val plan: PremiumPlan) : BillingEvent()

    /** An owned purchase was found on Play (restore / reconcile); Premium is granted. */
    data class Restored(val plan: PremiumPlan) : BillingEvent()

    /** Restore finished and Play reported nothing owned. */
    object NothingToRestore : BillingEvent()

    /** Purchase is pending (e.g. cash payment at a store); Premium not granted yet. */
    object PurchasePending : BillingEvent()

    /** The user dismissed the Play sheet. */
    object Cancelled : BillingEvent()

    /** A previously granted subscription is no longer active on Play; Premium was revoked. */
    object Revoked : BillingEvent()

    /** Play Store app is too old or billing is unavailable on this device. */
    object BillingUnavailable : BillingEvent()

    data class Error(val code: Int, val message: String) : BillingEvent()
}
