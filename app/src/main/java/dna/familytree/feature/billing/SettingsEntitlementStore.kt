package dna.familytree.feature.billing

import dna.familytree.Global
import dna.familytree.feature.ads.AdsManager
import dna.familytree.feature.reminders.ReminderScheduler
import dna.familytree.billing.EntitlementStore
import dna.familytree.billing.PremiumPlan

/**
 * Persists the Premium entitlement in the app's `settings.json` through [Global.settings].
 */
object SettingsEntitlementStore : EntitlementStore {

    override val isPremium: Boolean
        get() = Global.settings?.premium == true

    override val activePlan: PremiumPlan?
        get() = PremiumPlan.fromKey(Global.settings?.premiumPlan)
            // Legacy buyers (before plans existed) only ever had the one-time product
            ?: if (isPremium) PremiumPlan.LIFETIME else null

    override fun grant(plan: PremiumPlan, purchaseToken: String, purchaseTimeMillis: Long) {
        val settings = Global.settings ?: return
        settings.premium = true
        settings.premiumPlan = plan.name
        settings.premiumToken = purchaseToken
        settings.premiumSince = purchaseTimeMillis
        settings.save()
        ReminderScheduler.reschedule(Global.context) // Premium reminders start
        AdsManager.onPremiumGranted()
    }

    override fun revoke() {
        val settings = Global.settings ?: return
        settings.premium = false
        settings.premiumPlan = null
        settings.premiumToken = null
        settings.premiumSince = 0L
        settings.save()
        ReminderScheduler.reschedule(Global.context)
        AdsManager.init(Global.context) // Ads come back
    }
}
