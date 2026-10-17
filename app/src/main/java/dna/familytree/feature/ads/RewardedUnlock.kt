package dna.familytree.feature.ads

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import dna.familytree.AppUtils
import dna.familytree.R
import dna.familytree.feature.billing.PremiumGate
import dna.familytree.util.AnalyticsUtil

/**
 * Rewarded videos (docs/specs/ads.md): a free user who taps a one-off Premium action chooses between using it once
 * after a short video or going Premium. Nothing plays without that choice.
 *
 * ```java
 * if (PremiumGate.isPremium()) export(); else RewardedUnlock.offer(activity, R.string.export_png, icon, this::export);
 * ```
 */
object RewardedUnlock {

    /** Waiting longer for a video than this is worse than saying there is none. */
    private const val LOAD_TIMEOUT = 10_000L

    /**
     * Offers [reason] (the action's title, also the paywall reason) once after a video. [action] runs after the
     * video was watched to the end; Premium users get it at once.
     */
    @SuppressLint("InflateParams")
    @JvmStatic
    fun offer(activity: Activity, @StringRes reason: Int, @DrawableRes icon: Int, action: Runnable) {
        if (PremiumGate.isPremium()) {
            action.run()
            return
        }
        val placement = activity.resources.getResourceEntryName(reason)
        AnalyticsUtil.logEventRewarded(activity, "offered", placement)
        AdsManager.preloadRewarded(activity)
        val dialog = BottomSheetDialog(activity)
        val view = LayoutInflater.from(activity).inflate(R.layout.reward_unlock_sheet, null)
        dialog.setContentView(view)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.behavior.skipCollapsed = true
        view.findViewById<ImageView>(R.id.reward_icon).setImageResource(icon)
        view.findViewById<TextView>(R.id.reward_title).setText(reason)

        val watch = view.findViewById<View>(R.id.reward_watch)
        val watchDesc = view.findViewById<TextView>(R.id.reward_watch_desc)
        val loading = view.findViewById<View>(R.id.reward_loading)
        val main = Handler(Looper.getMainLooper())
        var waiting = false
        fun unavailable() {
            waiting = false
            loading.isVisible = false
            watch.isEnabled = true
            watchDesc.setText(R.string.reward_unavailable)
            AnalyticsUtil.logEventRewarded(activity, "unavailable", placement)
        }
        watch.setOnClickListener {
            if (waiting) return@setOnClickListener
            AnalyticsUtil.logEventRewarded(activity, "watch", placement)
            waiting = true
            watch.isEnabled = false
            loading.isVisible = true
            watchDesc.setText(R.string.reward_loading)
            main.postDelayed({ if (waiting && dialog.isShowing) unavailable() }, LOAD_TIMEOUT)
            AdsManager.withRewarded(activity) { ad ->
                if (!waiting || !dialog.isShowing) return@withRewarded
                if (ad == null || activity.isFinishing || activity.isDestroyed) {
                    unavailable()
                    return@withRewarded
                }
                waiting = false
                dialog.dismiss()
                AdsManager.showRewarded(activity, ad) { earned ->
                    AnalyticsUtil.logEventRewarded(activity, if (earned) "earned" else "not_earned", placement)
                    if (activity.isFinishing || activity.isDestroyed) return@showRewarded
                    if (earned) action.run()
                    else AppUtils.toast(activity, R.string.reward_not_earned)
                }
            }
        }
        view.findViewById<View>(R.id.reward_premium).setOnClickListener {
            AnalyticsUtil.logEventRewarded(activity, "premium", placement)
            dialog.dismiss()
            PremiumGate.open(activity, reason)
        }
        dialog.setOnDismissListener { waiting = false }
        dialog.show()
    }
}
