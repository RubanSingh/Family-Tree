package dna.familytree.feature.billing

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import com.google.android.material.divider.MaterialDivider
import dna.familytree.AppUtils
import dna.familytree.BaseController
import dna.familytree.R
import dna.familytree.billing.BillingEvent
import dna.familytree.billing.BillingManager
import dna.familytree.billing.PlanOffer
import dna.familytree.billing.PremiumPlan
import dna.familytree.constant.Extra
import dna.familytree.util.AnalyticsUtil
import dna.familytree.util.InsetsUtil
import dna.familytree.util.LoggerUtils

/**
 * Premium paywall (docs/specs/paywall.md): Monthly subscription or Lifetime one-time purchase, with everything
 * Premium includes. Feature cards, comparison rows and FAQ are built from [PremiumFeature].
 */
class PaywallController : BaseController(), InsetsUtil.SelfInsetting {

    private lateinit var billing: BillingManager
    private var selected = PremiumPlan.LIFETIME
    private var offers: Map<PremiumPlan, PlanOffer> = emptyMap()
    private var reasonFeature: PremiumFeature? = null

    private lateinit var monthlyCard: MaterialCardView
    private lateinit var lifetimeCard: MaterialCardView
    private lateinit var cta: MaterialButton
    private lateinit var summary: TextView
    private lateinit var loading: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.paywall_activity)
        applyInsets()

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val reason = intent.getIntExtra(Extra.STRING, 0)
        toolbar.title = if (reason != 0) getString(reason) else getString(R.string.family_tree_premium)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        val specificReason = reason != 0 && reason != R.string.family_tree_premium
        findViewById<TextView>(R.id.paywall_reason).apply {
            isVisible = specificReason
            if (specificReason) text = getString(R.string.paywall_reason, getString(reason))
        }
        reasonFeature = PremiumFeature.forReason(reason)
        if (savedInstanceState == null)
            AnalyticsUtil.logEventPaywall(this, "open", if (reason != 0) resources.getResourceEntryName(reason) else "generic")

        monthlyCard = findViewById(R.id.paywall_plan_monthly)
        lifetimeCard = findViewById(R.id.paywall_plan_lifetime)
        cta = findViewById(R.id.paywall_cta)
        summary = findViewById(R.id.paywall_summary)
        loading = findViewById(R.id.paywall_loading)

        monthlyCard.setOnClickListener { select(PremiumPlan.MONTHLY) }
        lifetimeCard.setOnClickListener { select(PremiumPlan.LIFETIME) }
        cta.setOnClickListener {
            AnalyticsUtil.logEventPremiumAction(firebaseAnalytics)
            AnalyticsUtil.logEventPaywall(this, "purchase_" + selected.name.lowercase(), null)
            billing.purchase(this, selected)
        }
        findViewById<View>(R.id.paywall_restore).setOnClickListener {
            loading.isVisible = true
            billing.restore()
        }
        findViewById<View>(R.id.paywall_manage).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(BillingManager.manageSubscriptionUrl(packageName))))
        }

        buildComparison()
        buildFeatures()
        buildFaq()

        select(selected)
        billing = BillingManager(this, SettingsEntitlementStore, ::onBillingEvent)
        billing.connect()
    }

    /** Edge-to-edge: the toolbar under the status bar, the purchase bar above the navigation bar, the page between. */
    private fun applyInsets() {
        val appBar = findViewById<View>(R.id.paywall_appbar)
        val bar = findViewById<View>(R.id.paywall_bar)
        val scroll = findViewById<View>(R.id.paywall_scroll)
        val barBottom = bar.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.paywall_root)) { root, insets ->
            val bars: Insets = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            root.updatePadding(left = bars.left, right = bars.right)
            appBar.updatePadding(top = bars.top)
            bar.updatePadding(bottom = barBottom + bars.bottom)
            WindowInsetsCompat.CONSUMED
        }
        // The page ends above the purchase bar
        bar.addOnLayoutChangeListener { _, _, top, _, bottom, _, _, _, _ ->
            if (scroll.paddingBottom != bottom - top) scroll.updatePadding(bottom = bottom - top)
        }
    }

    // ------------------------------------------------------------------ content

    private fun buildComparison() {
        val table = findViewById<LinearLayout>(R.id.paywall_compare)
        val inflater = LayoutInflater.from(this)
        PremiumFeature.entries.forEach { feature ->
            table.addView(MaterialDivider(this).apply {
                dividerColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOutlineVariant)
            })
            val row = inflater.inflate(R.layout.paywall_compare_row, table, false)
            row.findViewById<TextView>(R.id.compare_feature).setText(feature.title)
            val free = freeText(feature)
            row.findViewById<TextView>(R.id.compare_free).apply {
                setText(free)
                if (free == R.string.compare_none) contentDescription = getString(R.string.paywall_compare_free) + ": ✕"
            }
            row.findViewById<TextView>(R.id.compare_premium).setText(feature.premium)
            table.addView(row)
        }
    }

    /** What free users get; PDF export stays free for people who had the app before 1.6. */
    private fun freeText(feature: PremiumFeature): Int =
        if (feature == PremiumFeature.EXPORTS && PremiumGate.isPremiumOrLegacy()) R.string.pf_exports_free_legacy else feature.free

    private fun buildFeatures() {
        val box = findViewById<LinearLayout>(R.id.paywall_features)
        val inflater = LayoutInflater.from(this)
        PremiumFeature.Group.entries.forEach { group ->
            val header = inflater.inflate(R.layout.paywall_feature_group, box, false)
            header.findViewById<TextView>(R.id.feature_group_title).setText(group.title)
            header.findViewById<TextView>(R.id.feature_group_desc).setText(group.subtitle)
            box.addView(header)
            PremiumFeature.entries.filter { it.group == group }.forEach { feature ->
                box.addView(featureCard(inflater, box, feature))
            }
        }
    }

    private fun featureCard(inflater: LayoutInflater, parent: ViewGroup, feature: PremiumFeature): View {
        val card = inflater.inflate(R.layout.paywall_feature_card, parent, false) as MaterialCardView
        card.findViewById<ImageView>(R.id.feature_icon).setImageResource(feature.icon)
        card.findViewById<TextView>(R.id.feature_title).setText(feature.title)
        card.findViewById<TextView>(R.id.feature_summary).setText(feature.summary)
        val free = freeText(feature)
        card.findViewById<TextView>(R.id.feature_free).apply {
            isVisible = free != R.string.compare_none
            text = getString(R.string.paywall_free_label, getString(free))
        }
        card.findViewById<TextView>(R.id.feature_premium).text =
            if (free == R.string.compare_none && feature.premium == R.string.compare_included) getString(R.string.paywall_premium_only)
            else getString(R.string.paywall_premium_label, getString(feature.premium))
        val details = card.findViewById<LinearLayout>(R.id.feature_details)
        feature.details.forEach { line ->
            val text = inflater.inflate(R.layout.paywall_detail_line, details, false) as TextView
            text.setText(line)
            details.addView(text)
        }
        val chevron = card.findViewById<ImageView>(R.id.feature_chevron)
        fun expand(open: Boolean) {
            details.isVisible = open
            chevron.rotation = if (open) 180f else 0f
            card.contentDescription = getString(feature.title) + ", " +
                    getString(if (open) R.string.paywall_details_hide else R.string.paywall_details_show)
        }
        expand(false)
        card.setOnClickListener {
            TransitionManager.beginDelayedTransition(findViewById(R.id.paywall_features), AutoTransition().setDuration(180))
            expand(!details.isVisible)
        }
        if (feature == reasonFeature) { // The feature the user was trying to use: highlighted and open
            card.strokeWidth = resources.getDimensionPixelSize(R.dimen.paywall_stroke_selected)
            expand(true)
        }
        return card
    }

    private fun buildFaq() {
        val box = findViewById<LinearLayout>(R.id.paywall_faq)
        val inflater = LayoutInflater.from(this)
        val faq = listOf(
            R.string.paywall_faq_plans_q to R.string.paywall_faq_plans_a,
            R.string.paywall_faq_cancel_q to R.string.paywall_faq_cancel_a,
            R.string.paywall_faq_device_q to R.string.paywall_faq_device_a,
            R.string.paywall_faq_privacy_q to R.string.paywall_faq_privacy_a,
            R.string.paywall_faq_manage_q to R.string.paywall_faq_manage_a
        )
        faq.forEachIndexed { index, (question, answer) ->
            if (index > 0) box.addView(MaterialDivider(this).apply {
                dividerColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOutlineVariant)
            })
            val item = inflater.inflate(R.layout.paywall_faq_item, box, false)
            item.findViewById<TextView>(R.id.faq_question).setText(question)
            val answerView = item.findViewById<TextView>(R.id.faq_answer)
            answerView.setText(answer)
            val chevron = item.findViewById<ImageView>(R.id.faq_chevron)
            item.setOnClickListener {
                TransitionManager.beginDelayedTransition(box, AutoTransition().setDuration(180))
                answerView.isVisible = !answerView.isVisible
                chevron.rotation = if (answerView.isVisible) 180f else 0f
            }
            box.addView(item)
        }
    }

    // ------------------------------------------------------------------ billing

    private fun onBillingEvent(event: BillingEvent) {
        LoggerUtils.EventLog(TAG, "billing event: $event")
        when (event) {
            BillingEvent.Connecting -> loading.isVisible = true
            is BillingEvent.OffersLoaded -> {
                loading.isVisible = false
                offers = event.offers.associateBy { it.plan }
                bindOffer(PremiumPlan.MONTHLY, R.id.paywall_monthly_price, R.id.paywall_monthly_period, monthlyCard)
                bindOffer(PremiumPlan.LIFETIME, R.id.paywall_lifetime_price, R.id.paywall_lifetime_period, lifetimeCard)
                bindExtras()
                if (offers[selected] == null) offers.keys.firstOrNull()?.let { select(it) }
                select(selected)
            }
            is BillingEvent.PurchaseGranted -> {
                AnalyticsUtil.logEventSortPurchaseCompleted(firebaseAnalytics)
                AnalyticsUtil.logEventPaywall(this, "purchased_" + event.plan.name.lowercase(), null)
                finishPremium(R.string.premium_activated)
            }
            is BillingEvent.Restored -> finishPremium(R.string.premium_restored)
            BillingEvent.NothingToRestore -> {
                loading.isVisible = false
                AppUtils.toast(this, R.string.nothing_to_restore)
            }
            BillingEvent.PurchasePending -> {
                loading.isVisible = false
                AppUtils.toast(this, R.string.purchase_pending)
            }
            BillingEvent.Cancelled -> loading.isVisible = false
            BillingEvent.BillingUnavailable -> {
                loading.isVisible = false
                summary.text = ""
                AppUtils.toast(this, R.string.update_playstore)
            }
            is BillingEvent.Error -> {
                loading.isVisible = false
                AppUtils.toast(this, R.string.something_wrong)
            }
            else -> Unit
        }
    }

    private fun bindOffer(plan: PremiumPlan, priceId: Int, periodId: Int, card: MaterialCardView) {
        val offer = offers[plan]
        card.isVisible = offer != null || plan == PremiumPlan.LIFETIME
        if (offer == null) return
        findViewById<TextView>(priceId).text = offer.formattedPrice
        findViewById<TextView>(periodId).text = when {
            plan == PremiumPlan.LIFETIME -> getString(R.string.plan_one_time_payment)
            offer.introPeriod != null -> getString(R.string.plan_per_month_with_trial)
            else -> getString(R.string.plan_per_month)
        }
    }

    /** Free trial pill on the monthly plan, "pays for itself" pill on the lifetime plan. */
    private fun bindExtras() {
        val monthly = offers[PremiumPlan.MONTHLY]
        val lifetime = offers[PremiumPlan.LIFETIME]
        findViewById<TextView>(R.id.paywall_monthly_trial).apply {
            val trial = trialText(monthly)
            isVisible = trial != null
            text = trial
        }
        findViewById<TextView>(R.id.paywall_lifetime_savings).apply {
            val months = if (monthly != null && lifetime != null)
                PlanMath.paysOffMonths(lifetime.priceMicros, lifetime.currencyCode, monthly.priceMicros, monthly.currencyCode)
            else null
            isVisible = months != null
            if (months != null) text = resources.getQuantityString(R.plurals.paywall_pays_off, months, months)
        }
    }

    private fun trialText(offer: PlanOffer?): String? {
        val trial = PlanMath.trial(offer?.introPeriod) ?: return null
        return resources.getQuantityString(
            if (trial.months) R.plurals.paywall_trial_months else R.plurals.paywall_trial_days, trial.count, trial.count
        )
    }

    private fun select(plan: PremiumPlan) {
        selected = plan
        styleCard(monthlyCard, plan == PremiumPlan.MONTHLY)
        styleCard(lifetimeCard, plan == PremiumPlan.LIFETIME)
        cta.setText(if (plan == PremiumPlan.MONTHLY) R.string.cta_start_monthly else R.string.cta_buy_lifetime)
        val offer = offers[plan]
        cta.isEnabled = offer != null
        summary.text = when {
            offer == null -> if (offers.isEmpty()) getString(R.string.paywall_loading_prices) else ""
            plan == PremiumPlan.LIFETIME -> getString(R.string.paywall_summary_lifetime, offer.formattedPrice)
            else -> trialText(offer)?.let { getString(R.string.paywall_summary_trial, it, offer.formattedPrice) }
                ?: getString(R.string.paywall_summary_monthly, offer.formattedPrice)
        }
        findViewById<TextView>(R.id.paywall_legal).setText(
            if (plan == PremiumPlan.MONTHLY) R.string.legal_subscription else R.string.legal_one_time
        )
        findViewById<View>(R.id.paywall_manage).isVisible = plan == PremiumPlan.MONTHLY
    }

    private fun styleCard(card: MaterialCardView, selected: Boolean) {
        card.isChecked = selected
        card.strokeWidth = resources.getDimensionPixelSize(if (selected) R.dimen.paywall_stroke_selected else R.dimen.paywall_stroke)
        card.strokeColor = MaterialColors.getColor(
            card,
            if (selected) androidx.appcompat.R.attr.colorPrimary else com.google.android.material.R.attr.colorOutlineVariant
        )
        card.setCardBackgroundColor(
            MaterialColors.getColor(
                card,
                if (selected) com.google.android.material.R.attr.colorPrimaryContainer else com.google.android.material.R.attr.colorSurfaceContainerLow
            )
        )
    }

    private fun finishPremium(message: Int) {
        AppUtils.toast(this, message)
        setResult(RESULT_OK)
        finish()
    }

    override fun onDestroy() {
        if (::billing.isInitialized) billing.release()
        super.onDestroy()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    companion object {
        private const val TAG = "PaywallController"
    }
}
