package dna.familytree.feature.ads

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import dna.familytree.R
import dna.familytree.feature.billing.PremiumGate

/**
 * A native ad drawn as a card of the design system: "Ad" badge, icon, headline, body, tonal call-to-action and, in the
 * large variant, the media. A "Remove ads" link under the ad opens the paywall. Hidden for Premium.
 *
 * ```java
 * NativeAdCard card = findViewById(R.id.my_template);
 * card.show(this, true); // activity or fragment view lifecycle
 * ```
 */
class NativeAdCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    /** Large variant, with the media: set in XML with app:nativeAdSize="large" or before binding. */
    var large: Boolean
    private var ad: NativeAd? = null
    /** The card destroys the ad when done; false when the ad belongs to a list adapter. */
    private var owned = true
    private var destroyed = false

    init {
        val array = context.obtainStyledAttributes(attrs, R.styleable.NativeAdCard)
        large = array.getInt(R.styleable.NativeAdCard_nativeAdSize, 0) == 1
        array.recycle()
        visibility = GONE
    }

    /**
     * Shows a preloaded ad. When none is ready and [lateLoad] is true one is requested and the card appears when it
     * arrives: use it only where nothing below can move (end of a page). The ad lives as long as [owner].
     */
    fun show(owner: LifecycleOwner, lateLoad: Boolean) {
        if (!AdsManager.adsEnabled() || owner.lifecycle.currentState == Lifecycle.State.DESTROYED) return
        owner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) = destroy()
        })
        val ready = AdsManager.takeNative(context)
        if (ready != null) bind(ready)
        else if (lateLoad) AdsManager.loadNative(context) { loaded -> if (destroyed) loaded.destroy() else bind(loaded) }
    }

    @JvmOverloads
    fun bind(nativeAd: NativeAd, owned: Boolean = true) {
        if (destroyed || !AdsManager.adsEnabled()) {
            if (owned) nativeAd.destroy()
            return
        }
        if (ad !== nativeAd) releaseAd()
        ad = nativeAd
        this.owned = owned
        removeAllViews()
        val card = LayoutInflater.from(context).inflate(if (large) R.layout.ad_native_large else R.layout.ad_native_small, this, false)
        addView(card)
        val adView = card.findViewById<NativeAdView>(R.id.ad_native_view)

        val headline = adView.findViewById<TextView>(R.id.ad_headline)
        headline.text = nativeAd.headline
        adView.headlineView = headline

        val body = adView.findViewById<TextView>(R.id.ad_body)
        body.text = nativeAd.body
        body.isVisible = !nativeAd.body.isNullOrBlank()
        adView.bodyView = body

        val icon = adView.findViewById<ImageView>(R.id.ad_icon)
        val iconImage = nativeAd.icon?.drawable
        icon.setImageDrawable(iconImage)
        icon.isVisible = iconImage != null
        adView.iconView = icon

        val advertiser = adView.findViewById<TextView>(R.id.ad_advertiser)
        val by = nativeAd.advertiser ?: nativeAd.store
        advertiser.text = by
        advertiser.isVisible = !by.isNullOrBlank()
        adView.advertiserView = advertiser

        val cta = adView.findViewById<TextView>(R.id.ad_call_to_action)
        cta.text = nativeAd.callToAction
        cta.isVisible = !nativeAd.callToAction.isNullOrBlank()
        adView.callToActionView = cta

        adView.findViewById<MediaView>(R.id.ad_media)?.let { media ->
            adView.mediaView = media
            media.setImageScaleType(ImageView.ScaleType.CENTER_CROP)
            media.isVisible = nativeAd.mediaContent != null
        }

        adView.setNativeAd(nativeAd)

        card.findViewById<View>(R.id.ad_remove).setOnClickListener {
            PremiumGate.open(context, R.string.benefit_no_ads)
        }
        visibility = VISIBLE
    }

    fun destroy() {
        destroyed = true
        releaseAd()
        removeAllViews()
        visibility = GONE
    }

    private fun releaseAd() {
        if (owned) ad?.destroy()
        ad = null
    }

    /** Premium bought while this screen was in background: the ad goes away on return. */
    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (visibility == VISIBLE && ad != null && PremiumGate.isPremium()) {
            releaseAd()
            removeAllViews()
            this.visibility = GONE
        }
    }
}
