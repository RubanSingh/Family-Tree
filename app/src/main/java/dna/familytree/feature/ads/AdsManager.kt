package dna.familytree.feature.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.material.snackbar.Snackbar
import dna.familytree.BuildConfig
import dna.familytree.R
import dna.familytree.feature.billing.PremiumGate
import dna.familytree.util.AnalyticsUtil

/**
 * Every ad of the app: AdMob initialisation, interstitials at natural breaks, rewarded videos chosen by the user to
 * unlock a one-off Premium action, and a small pool of preloaded native ads so that ad cards appear together with the
 * content instead of pushing it around (see docs/specs/ads.md).
 *
 * Premium users never load nor see an ad, and nothing is requested before the privacy consent ([ConsentManager]).
 * All calls are made on the main thread.
 */
object AdsManager {

    private const val TAG = "AdsManager"
    /** AdMob ads expire after one hour. */
    private const val AD_LIFETIME = 55 * MonetizationPolicy.MINUTE
    private const val NATIVE_POOL_SIZE = 2
    /** The "remove ads" hint follows one interstitial out of this many. */
    private const val REMOVE_ADS_HINT_EVERY = 2

    /** Natural breaks: a task has just been completed. */
    enum class Placement(val analyticsName: String) {
        PERSON_SAVED("person_saved"),
        TREE_CLOSED("tree_closed"),
        EXPORT_DONE("export_done")
    }

    private val main = Handler(Looper.getMainLooper())
    private var initialized = false

    private var interstitial: InterstitialAd? = null
    private var interstitialLoadedAt = 0L
    private var interstitialLoading = false

    private val nativePool = ArrayDeque<Pair<NativeAd, Long>>()
    private var nativeLoading = false

    private var rewarded: RewardedAd? = null
    private var rewardedLoadedAt = 0L
    private var rewardedLoading = false
    private val rewardedWaiters = mutableListOf<(RewardedAd?) -> Unit>()

    private var removeAdsHintPending = false

    @JvmStatic
    fun adsEnabled(): Boolean = !PremiumGate.isPremium()

    /** Not Premium, and the consent allows ad requests. */
    private fun canLoad(context: Context): Boolean = adsEnabled() && ConsentManager.canRequestAds(context)

    /** Starts the Mobile Ads SDK off the main thread (as Google recommends) and preloads native ads. */
    @JvmStatic
    fun init(context: Context) {
        if (initialized || !canLoad(context)) return
        initialized = true
        val app = context.applicationContext
        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder()
                // A family app: no mature ads, whatever the AdMob account defaults
                .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_PG)
                .build()
        )
        Thread {
            MobileAds.initialize(app) { main.post { prefetchNative(app) } }
        }.start()
    }

    // ------------------------------------------------------------------ interstitials

    @JvmStatic
    fun preloadInterstitial(context: Context) {
        if (!canLoad(context) || interstitialLoading) return
        if (interstitial != null && UsageStats.now() - interstitialLoadedAt < AD_LIFETIME) return
        init(context)
        interstitialLoading = true
        InterstitialAd.load(context.applicationContext, BuildConfig.AD_UNIT_INTERSTITIAL, AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialLoading = false
                    interstitial = ad
                    interstitialLoadedAt = UsageStats.now()
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialLoading = false
                    interstitial = null
                    Log.d(TAG, "interstitial not loaded: ${error.code} ${error.message}")
                }
            })
    }

    /**
     * A task has just been completed in [activity]: shows an interstitial if one is ready and [MonetizationPolicy]
     * allows it. Returns true when the ad is shown.
     */
    @JvmStatic
    fun onNaturalBreak(activity: Activity, placement: Placement): Boolean {
        if (!adsEnabled()) return false
        UsageStats.recordBreak()
        val ad = interstitial
        if (ad == null || UsageStats.now() - interstitialLoadedAt >= AD_LIFETIME) {
            interstitial = null
            preloadInterstitial(activity)
            return false
        }
        val usage = UsageStats.usage() ?: return false
        if (!MonetizationPolicy.canShowInterstitial(usage, UsageStats.now()) || activity.isFinishing || activity.isDestroyed)
            return false
        interstitial = null
        val app = activity.applicationContext
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                val total = UsageStats.recordInterstitial()
                if (total % REMOVE_ADS_HINT_EVERY == 0) removeAdsHintPending = true
                AnalyticsUtil.logEventInterstitial(app, placement.analyticsName)
            }

            override fun onAdDismissedFullScreenContent() {
                preloadInterstitial(app)
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.d(TAG, "interstitial not shown: ${error.code} ${error.message}")
                preloadInterstitial(app)
            }
        }
        ad.show(activity)
        return true
    }

    /** After some interstitials, the next screen offers to remove ads with Premium. Called by every screen on resume. */
    @JvmStatic
    fun showRemoveAdsHintIfPending(activity: Activity) {
        if (!removeAdsHintPending) return
        removeAdsHintPending = false
        if (!adsEnabled()) return
        val root = activity.findViewById<View>(android.R.id.content) ?: return
        Snackbar.make(root, R.string.ads_remove_hint, Snackbar.LENGTH_LONG)
            .setAction(R.string.ads_remove_action) { PremiumGate.open(activity, R.string.benefit_no_ads) }
            .show()
    }

    // ------------------------------------------------------------------ rewarded

    /** Loads a rewarded ad in advance, when the user is about to see an offer to unlock something with a video. */
    @JvmStatic
    fun preloadRewarded(context: Context) = withRewarded(context, null)

    /** Gives a ready rewarded ad to [callback], loading one if needed; null when none could be loaded. */
    fun withRewarded(context: Context, callback: ((RewardedAd?) -> Unit)?) {
        if (!canLoad(context)) {
            callback?.invoke(null)
            return
        }
        val ready = rewarded
        if (ready != null && UsageStats.now() - rewardedLoadedAt < AD_LIFETIME) {
            callback?.invoke(ready)
            return
        }
        rewarded = null
        callback?.let { rewardedWaiters += it }
        if (rewardedLoading) return
        init(context)
        rewardedLoading = true
        RewardedAd.load(context.applicationContext, BuildConfig.AD_UNIT_REWARDED, AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedLoading = false
                    rewarded = ad
                    rewardedLoadedAt = UsageStats.now()
                    flushRewardedWaiters(ad)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedLoading = false
                    Log.d(TAG, "rewarded not loaded: ${error.code} ${error.message}")
                    flushRewardedWaiters(null)
                }
            })
    }

    private fun flushRewardedWaiters(ad: RewardedAd?) {
        val waiters = rewardedWaiters.toList()
        rewardedWaiters.clear()
        waiters.forEach { it(ad) }
    }

    /** Shows the rewarded [ad]; [onClosed] receives true if the video was watched to the reward. */
    fun showRewarded(activity: Activity, ad: RewardedAd, onClosed: (Boolean) -> Unit) {
        if (rewarded === ad) rewarded = null
        var earned = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                UsageStats.recordFullScreenAd() // No interstitial right after
            }

            override fun onAdDismissedFullScreenContent() = onClosed(earned)

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.d(TAG, "rewarded not shown: ${error.code} ${error.message}")
                onClosed(false)
            }
        }
        ad.show(activity) { earned = true }
    }

    // ------------------------------------------------------------------ native ads

    /** Loads native ads in background until the pool is full. */
    @JvmStatic
    fun prefetchNative(context: Context) {
        if (!canLoad(context) || nativeLoading || nativePool.size >= NATIVE_POOL_SIZE) return
        nativeLoading = true
        val app = context.applicationContext
        AdLoader.Builder(app, BuildConfig.AD_UNIT_NATIVE)
            .forNativeAd { ad ->
                nativeLoading = false
                if (!adsEnabled()) {
                    ad.destroy()
                    return@forNativeAd
                }
                nativePool.addLast(ad to UsageStats.now())
                prefetchNative(app)
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    nativeLoading = false
                    Log.d(TAG, "native ad not loaded: ${error.code} ${error.message}")
                }
            })
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                    .setMediaAspectRatio(NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_LANDSCAPE)
                    .build()
            )
            .build()
            .loadAd(AdRequest.Builder().build())
    }

    /** A preloaded native ad, now owned by the caller (who destroys it), or null. Refills the pool. */
    @JvmStatic
    fun takeNative(context: Context): NativeAd? {
        if (!adsEnabled()) {
            clearNativePool()
            return null
        }
        var ad: NativeAd? = null
        while (ad == null && nativePool.isNotEmpty()) {
            val (candidate, loadedAt) = nativePool.removeFirst()
            if (UsageStats.now() - loadedAt < AD_LIFETIME) ad = candidate else candidate.destroy()
        }
        prefetchNative(context)
        return ad
    }

    /** Loads one native ad for a card that may appear late (at the end of a page). */
    fun loadNative(context: Context, onLoaded: (NativeAd) -> Unit) {
        takeNative(context)?.let {
            onLoaded(it)
            return
        }
        if (!canLoad(context)) return
        AdLoader.Builder(context.applicationContext, BuildConfig.AD_UNIT_NATIVE)
            .forNativeAd(onLoaded)
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                    .setMediaAspectRatio(NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_LANDSCAPE)
                    .build()
            )
            .build()
            .loadAd(AdRequest.Builder().build())
    }

    /** Premium was just granted: drops everything loaded. */
    @JvmStatic
    fun onPremiumGranted() {
        interstitial = null
        rewarded = null
        clearNativePool()
    }

    private fun clearNativePool() {
        while (nativePool.isNotEmpty()) nativePool.removeFirst().first.destroy()
    }
}
