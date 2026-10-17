package dna.familytree.feature.billing

import android.content.Context
import android.content.Intent
import androidx.annotation.StringRes
import dna.familytree.Global
import dna.familytree.constant.Extra
import dna.familytree.feature.ads.UsageStats

/**
 * Single entry point for every "this needs Premium" check in the app.
 *
 * ```java
 * if (PremiumGate.isPremium()) { ... } else PremiumGate.open(this, R.string.theme);
 * ```
 * The catalogue of Premium features is [PremiumFeature]; docs/specs/premium-features.md lists every gate.
 */
object PremiumGate {

    @JvmStatic
    fun isPremium(): Boolean = Global.settings?.premium == true

    /**
     * For features that were free before version 1.6 and are Premium for new installs: people who had the app
     * before keep them (see Settings.legacyUser).
     */
    @JvmStatic
    fun isPremiumOrLegacy(): Boolean = isPremium() || Global.settings?.legacyUser == true

    /** Opens the paywall. [reason] is the feature title shown in the toolbar (0 = generic). */
    @JvmStatic
    @JvmOverloads
    fun open(context: Context, @StringRes reason: Int = 0) {
        val intent = Intent(context, PaywallController::class.java)
        if (reason != 0) intent.putExtra(Extra.STRING, reason)
        UsageStats.recordPremiumPrompt()
        context.startActivity(intent)
    }

    /** Runs [action] if premium, otherwise opens the paywall. Returns true when the action ran. */
    @JvmStatic
    fun require(context: Context, @StringRes reason: Int, action: Runnable): Boolean {
        return if (isPremium()) {
            action.run(); true
        } else {
            open(context, reason); false
        }
    }
}
