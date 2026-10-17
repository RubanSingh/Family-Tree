package dna.familytree.feature.billing

import android.annotation.SuppressLint
import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import dna.familytree.AppUtils
import dna.familytree.Global
import dna.familytree.R
import dna.familytree.billing.BillingEvent
import dna.familytree.billing.BillingManager
import dna.familytree.billing.PremiumPlan
import dna.familytree.feature.ads.MonetizationPolicy
import dna.familytree.feature.ads.UsageStats
import dna.familytree.util.AnalyticsUtil
import java.text.NumberFormat

/**
 * Invitation to Premium, shown after enough use of the app (docs/specs/upsell-prompt.md): a bottom sheet with the
 * user's own numbers, the most wanted Premium features and the prices. When it appears is up to
 * [MonetizationPolicy.shouldShowUpsell]: at most once a session, then every 3, 7, 14, 30 and 45 days, only to people
 * who keep using the app.
 */
object UpsellPrompt {

    private const val DELAY = 700L

    /** Called by the main screens on resume: shows the invitation if it's time. */
    @JvmStatic
    fun maybeShow(activity: Activity) {
        if (PremiumGate.isPremium() || !isDue()) return
        activity.window.decorView.postDelayed({
            val owner = activity as? LifecycleOwner
            if (activity.isFinishing || activity.isDestroyed || !activity.hasWindowFocus()
                || owner?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) != true
                || PremiumGate.isPremium() || !isDue()) return@postDelayed
            show(activity)
        }, DELAY)
    }

    private fun isDue(): Boolean {
        val usage = UsageStats.usage() ?: return false
        return MonetizationPolicy.shouldShowUpsell(usage, UsageStats.now())
    }

    @SuppressLint("InflateParams")
    @JvmStatic
    fun show(activity: Activity) {
        UsageStats.recordUpsell()
        AnalyticsUtil.logEventUpsell(activity, "shown")
        val dialog = BottomSheetDialog(activity)
        val view = LayoutInflater.from(activity).inflate(R.layout.upsell_sheet, null)
        dialog.setContentView(view)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.behavior.skipCollapsed = true

        bindStats(view)
        bindFeatures(view)
        view.findViewById<TextView>(R.id.upsell_more).apply {
            val more = PremiumFeature.entries.size - PremiumFeature.highlights.size
            text = activity.resources.getQuantityString(R.plurals.upsell_more_features, more, more)
            setOnClickListener {
                AnalyticsUtil.logEventUpsell(activity, "more")
                dialog.dismiss()
                PremiumGate.open(activity, R.string.family_tree_premium)
            }
        }
        view.findViewById<View>(R.id.upsell_cta).setOnClickListener {
            AnalyticsUtil.logEventUpsell(activity, "cta")
            dialog.dismiss()
            PremiumGate.open(activity, R.string.family_tree_premium)
        }
        view.findViewById<View>(R.id.upsell_later).setOnClickListener {
            AnalyticsUtil.logEventUpsell(activity, "later")
            dialog.dismiss()
        }

        // Live prices from Google Play (also restores a purchase made on another device)
        val price = view.findViewById<TextView>(R.id.upsell_price)
        val billing = BillingManager(activity, SettingsEntitlementStore) { event ->
            when (event) {
                is BillingEvent.OffersLoaded -> {
                    val offers = event.offers.associateBy { it.plan }
                    val monthly = offers[PremiumPlan.MONTHLY]
                    val lifetime = offers[PremiumPlan.LIFETIME]
                    val lines = listOfNotNull(
                        monthly?.let { activity.getString(R.string.upsell_price_monthly, it.formattedPrice) },
                        lifetime?.let {
                            activity.getString(if (monthly != null) R.string.upsell_price_lifetime else R.string.upsell_price_lifetime_only,
                                it.formattedPrice)
                        }
                    )
                    price.text = lines.joinToString("\n")
                    price.isVisible = lines.isNotEmpty()
                }
                is BillingEvent.Restored -> {
                    AppUtils.toast(activity, R.string.premium_restored)
                    dialog.dismiss()
                }
                else -> Unit
            }
        }
        dialog.setOnDismissListener { billing.release() }
        dialog.setOnCancelListener { AnalyticsUtil.logEventUpsell(activity, "dismiss") }
        dialog.show()
        billing.connect()
    }

    /** People, trees and generations of the user; the subtitle changes for someone just starting. */
    private fun bindStats(view: View) {
        val settings = Global.settings
        val people = settings?.totalPersons() ?: 0
        val trees = settings?.trees?.size ?: 0
        val generations = settings?.maxGenerations() ?: 0
        val numbers = NumberFormat.getIntegerInstance()
        view.findViewById<View>(R.id.upsell_stats).isVisible = people > 0
        if (people == 0) view.findViewById<TextView>(R.id.upsell_subtitle).setText(R.string.upsell_subtitle_new)
        fun stat(id: Int, value: Int, label: Int) {
            val tile = view.findViewById<View>(id)
            tile.findViewById<TextView>(R.id.upsell_stat_value).text = numbers.format(value)
            tile.findViewById<TextView>(R.id.upsell_stat_label).setText(label)
            tile.isVisible = value > 0
        }
        stat(R.id.upsell_stat_people, people, R.string.upsell_stat_people)
        stat(R.id.upsell_stat_trees, trees, R.string.upsell_stat_trees)
        stat(R.id.upsell_stat_generations, generations, R.string.upsell_stat_generations)
    }

    private fun bindFeatures(view: View) {
        val box = view.findViewById<LinearLayout>(R.id.upsell_features)
        val inflater = LayoutInflater.from(view.context)
        PremiumFeature.highlights.forEach { feature ->
            val row = inflater.inflate(R.layout.upsell_feature, box, false)
            row.findViewById<ImageView>(R.id.upsell_feature_icon).setImageResource(feature.icon)
            row.findViewById<TextView>(R.id.upsell_feature_title).setText(feature.title)
            row.findViewById<TextView>(R.id.upsell_feature_summary).setText(feature.summary)
            box.addView(row)
        }
    }
}
