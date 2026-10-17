package dna.familytree.feature.billing

import android.content.Context
import android.util.Log
import dna.familytree.billing.BillingEvent
import dna.familytree.billing.BillingManager
import dna.familytree.billing.PremiumPlan

/**
 * Keeps the stored entitlement honest with Google Play.
 *
 * Runs once per process on the home screen. Only a Monthly subscription can lapse, so it is a
 * no-op for Lifetime buyers and for free users; network failures never revoke anything.
 */
object EntitlementSync {

    private const val TAG = "EntitlementSync"
    private var done = false

    @JvmStatic
    fun reconcileOnce(context: Context, onRevoked: Runnable? = null) {
        if (done) return
        done = true
        if (SettingsEntitlementStore.activePlan != PremiumPlan.MONTHLY) return
        var manager: BillingManager? = null
        manager = BillingManager(context, SettingsEntitlementStore) { event ->
            when (event) {
                BillingEvent.Revoked -> onRevoked?.run()
                is BillingEvent.Error, BillingEvent.BillingUnavailable -> {
                    Log.w(TAG, "reconcile skipped: $event")
                    manager?.release()
                }
                else -> Unit
            }
        }
        manager.reconcile()
    }
}
