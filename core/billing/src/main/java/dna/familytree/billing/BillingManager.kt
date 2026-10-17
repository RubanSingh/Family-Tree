package dna.familytree.billing

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams

/**
 * Google Play Billing for Family Tree Premium: Monthly subscription + Lifetime one-time purchase.
 *
 * Lifecycle: create in `onCreate`, call [connect], release in `onDestroy`.
 * All callbacks arrive on the main thread.
 *
 * Flow:
 *  1. [connect]  → on success, [restore] runs: owned purchases are acknowledged + granted,
 *                  otherwise product details are queried and [BillingEvent.OffersLoaded] is emitted.
 *  2. [purchase] → Play sheet → [PurchasesUpdatedListener] → acknowledge → [EntitlementStore.grant].
 *  3. [reconcile] (headless, on app start) → revokes a stored subscription Play no longer reports.
 */
class BillingManager(
    context: Context,
    private val store: EntitlementStore,
    private val listener: (BillingEvent) -> Unit
) {
    private val appContext = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private var client: BillingClient? = null
    private var offers: Map<PremiumPlan, PlanOffer> = emptyMap()
    private var pendingAction: (() -> Unit)? = null
    private var released = false

    private val purchasesUpdated = PurchasesUpdatedListener { result, purchases ->
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> purchases?.forEach(::handlePurchase)
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> restore()
            BillingClient.BillingResponseCode.USER_CANCELED -> emit(BillingEvent.Cancelled)
            else -> emit(BillingEvent.Error(result.responseCode, result.debugMessage))
        }
    }

    // ------------------------------------------------------------------ connection

    fun connect() {
        if (released) return
        val existing = client
        if (existing != null && existing.connectionState == BillingClient.ConnectionState.CONNECTED) {
            restore()
            return
        }
        // Still connecting: the pending action runs when the setup finishes
        if (existing != null && existing.connectionState == BillingClient.ConnectionState.CONNECTING) return
        existing?.endConnection() // A disconnected client is replaced, not leaked
        emit(BillingEvent.Connecting)
        val c = BillingClient.newBuilder(appContext)
            .setListener(purchasesUpdated)
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .enableAutoServiceReconnection() // Play Billing reconnects by itself when the service drops
            .build()
        client = c
        c.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                Log.d(TAG, "setup finished: ${result.responseCode} ${result.debugMessage}")
                when (result.responseCode) {
                    BillingClient.BillingResponseCode.OK -> {
                        val action = pendingAction
                        pendingAction = null
                        if (action != null) main.post(action) else restore()
                    }
                    BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> emit(BillingEvent.BillingUnavailable)
                    else -> emit(BillingEvent.Error(result.responseCode, result.debugMessage))
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.d(TAG, "service disconnected")
            }
        })
    }

    fun release() {
        released = true
        client?.endConnection()
        client = null
    }

    private fun whenConnected(action: () -> Unit) {
        val c = client
        if (c != null && c.connectionState == BillingClient.ConnectionState.CONNECTED) {
            action()
        } else {
            pendingAction = action
            connect()
        }
    }

    // ------------------------------------------------------------------ restore / reconcile

    /**
     * Looks for owned purchases of either plan. Grants the first active one found,
     * otherwise loads offers so the paywall can show prices.
     */
    fun restore() = whenConnected {
        queryOwned { owned ->
            val active = owned.firstOrNull { it.purchaseState == Purchase.PurchaseState.PURCHASED }
            if (active != null) {
                val plan = active.products.firstNotNullOfOrNull(PremiumPlan::fromProductId) ?: PremiumPlan.LIFETIME
                acknowledgeIfNeeded(active) { grant(plan, active, restored = true) }
            } else {
                if (owned.any { it.purchaseState == Purchase.PurchaseState.PENDING }) emit(BillingEvent.PurchasePending)
                loadOffers()
            }
        }
    }

    /**
     * Headless check used at app start. Only a *subscription* can lapse, so this only
     * revokes when the stored plan is [PremiumPlan.MONTHLY] and Play returns no active sub.
     * Network failures leave the entitlement untouched.
     */
    fun reconcile() = whenConnected {
        if (store.activePlan != PremiumPlan.MONTHLY) {
            release(); return@whenConnected
        }
        queryOwned { owned ->
            val stillActive = owned.any { p ->
                p.purchaseState == Purchase.PurchaseState.PURCHASED &&
                    p.products.contains(PremiumPlan.MONTHLY.productId)
            }
            val lifetime = owned.firstOrNull { p ->
                p.purchaseState == Purchase.PurchaseState.PURCHASED &&
                    p.products.contains(PremiumPlan.LIFETIME.productId)
            }
            when {
                stillActive -> Unit
                lifetime != null -> grant(PremiumPlan.LIFETIME, lifetime, restored = true)
                else -> {
                    Log.i(TAG, "Monthly subscription no longer active → revoking premium")
                    store.revoke()
                    emit(BillingEvent.Revoked)
                }
            }
            release()
        }
    }

    private fun queryOwned(done: (List<Purchase>) -> Unit) {
        val c = client ?: return
        val all = mutableListOf<Purchase>()
        var remaining = 2
        var failed = false
        fun finish() {
            remaining--
            if (remaining == 0) main.post { if (failed) emit(BillingEvent.Error(-1, "query failed")) else done(all) }
        }
        for (type in listOf(BillingClient.ProductType.SUBS, BillingClient.ProductType.INAPP)) {
            c.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(type).build()) { result, purchases ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    synchronized(all) { all += purchases }
                } else if (result.responseCode != BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED) {
                    // SUBS may be unsupported on very old Play versions; treat that as "none owned"
                    failed = true
                }
                finish()
            }
        }
    }

    // ------------------------------------------------------------------ offers

    private fun loadOffers() {
        val c = client ?: return
        if (c.isFeatureSupported(BillingClient.FeatureType.PRODUCT_DETAILS).responseCode ==
            BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED
        ) {
            emit(BillingEvent.BillingUnavailable); return
        }
        val resolved = mutableMapOf<PremiumPlan, PlanOffer>()
        var remaining = PremiumPlan.values().size
        fun finish() {
            remaining--
            if (remaining == 0) main.post {
                offers = resolved
                val ordered = PremiumPlan.values().mapNotNull { resolved[it] }
                if (ordered.isEmpty()) emit(BillingEvent.Error(-2, "no products"))
                else emit(BillingEvent.OffersLoaded(ordered))
            }
        }
        // Subscriptions and in-app products must be queried separately.
        for (plan in PremiumPlan.values()) {
            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(
                    listOf(
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(plan.productId)
                            .setProductType(plan.productType)
                            .build()
                    )
                ).build()
            c.queryProductDetailsAsync(params) { result, queryResult ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryResult.productDetailsList.firstOrNull()?.let { details: ProductDetails ->
                        PlanOffer.from(plan, details)?.let { synchronized(resolved) { resolved[plan] = it } }
                    }
                } else {
                    Log.w(TAG, "product details for ${plan.productId}: ${result.responseCode} ${result.debugMessage}")
                }
                finish()
            }
        }
    }

    fun offer(plan: PremiumPlan): PlanOffer? = offers[plan]

    // ------------------------------------------------------------------ purchase

    /** Opens the Play purchase sheet for [plan]. Requires [BillingEvent.OffersLoaded] first. */
    fun purchase(activity: Activity, plan: PremiumPlan) = whenConnected {
        val offer = offers[plan]
        if (offer == null) {
            emit(BillingEvent.Error(-3, "offer not loaded")); return@whenConnected
        }
        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(offer.productDetails)
            .apply { offer.offerToken?.let { setOfferToken(it) } }
            .build()
        val flow = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams))
            .build()
        val result = client?.launchBillingFlow(activity, flow)
        Log.d(TAG, "launch flow: ${result?.responseCode} ${result?.debugMessage}")
        if (result != null && result.responseCode != BillingClient.BillingResponseCode.OK) {
            emit(BillingEvent.Error(result.responseCode, result.debugMessage))
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        when (purchase.purchaseState) {
            Purchase.PurchaseState.PURCHASED -> {
                val plan = purchase.products.firstNotNullOfOrNull(PremiumPlan::fromProductId) ?: PremiumPlan.LIFETIME
                acknowledgeIfNeeded(purchase) { grant(plan, purchase, restored = false) }
            }
            Purchase.PurchaseState.PENDING -> emit(BillingEvent.PurchasePending)
            else -> Unit
        }
    }

    /** Play refunds un-acknowledged purchases after 3 days, so this must succeed before granting. */
    private fun acknowledgeIfNeeded(purchase: Purchase, then: () -> Unit) {
        if (purchase.isAcknowledged) {
            then(); return
        }
        val c = client ?: return
        val params = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
        c.acknowledgePurchase(params) { result ->
            main.post {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) then()
                else emit(BillingEvent.Error(result.responseCode, result.debugMessage))
            }
        }
    }

    private fun grant(plan: PremiumPlan, purchase: Purchase, restored: Boolean) {
        store.grant(plan, purchase.purchaseToken, purchase.purchaseTime)
        emit(if (restored) BillingEvent.Restored(plan) else BillingEvent.PurchaseGranted(plan))
    }

    private fun emit(event: BillingEvent) {
        if (released && event !is BillingEvent.Restored && event !is BillingEvent.Revoked) return
        if (Looper.myLooper() == Looper.getMainLooper()) listener(event) else main.post { listener(event) }
    }

    companion object {
        private const val TAG = "BillingManager"

        /** Deep link to manage/cancel a subscription in the Play Store app. */
        fun manageSubscriptionUrl(packageName: String, plan: PremiumPlan = PremiumPlan.MONTHLY): String =
            "https://play.google.com/store/account/subscriptions?sku=${plan.productId}&package=$packageName"
    }
}
