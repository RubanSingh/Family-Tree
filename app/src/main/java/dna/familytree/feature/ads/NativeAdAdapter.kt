package dna.familytree.feature.ads

import android.content.Context
import android.view.ViewGroup
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.nativead.NativeAd

/**
 * One native ad as the first row of a list, to put in a ConcatAdapter before the list's own adapter: it scrolls away
 * with the content instead of taking space for good. Only a preloaded ad is used, so the list never moves when an ad
 * arrives (no accidental clicks).
 */
class NativeAdAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var ad: NativeAd? = null
    private var hidden = false

    /** Takes a preloaded ad, kept until [owner] is destroyed. */
    fun attach(context: Context, owner: LifecycleOwner) {
        if (ad != null) return
        ad = AdsManager.takeNative(context) ?: return
        owner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                ad?.destroy()
                ad = null
            }
        })
        notifyItemInserted(0)
    }

    /** Hides the ad while the user searches, for instance. */
    fun setHidden(hide: Boolean) {
        if (hide == hidden) return
        val before = itemCount
        hidden = hide
        if (before == 1 && itemCount == 0) notifyItemRemoved(0)
        else if (before == 0 && itemCount == 1) notifyItemInserted(0)
    }

    override fun getItemCount() = if (ad != null && !hidden && AdsManager.adsEnabled()) 1 else 0

    override fun getItemViewType(position: Int) = VIEW_TYPE

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val card = NativeAdCard(parent.context)
        card.layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        return object : RecyclerView.ViewHolder(card) {}
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        ad?.let { (holder.itemView as NativeAdCard).bind(it, false) }
    }

    companion object {
        private const val VIEW_TYPE = 0x7AD
    }
}
