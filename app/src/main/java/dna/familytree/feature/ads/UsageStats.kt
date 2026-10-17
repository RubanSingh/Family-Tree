package dna.familytree.feature.ads

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import java.util.concurrent.TimeUnit

/**
 * How much the app is used, for [MonetizationPolicy]: sessions, engagement points and the history of ads and Premium
 * invitations. Kept in its own SharedPreferences file ("monetization"), not in settings.json nor in backups.
 *
 * A session starts when the app comes to the foreground after at least [SESSION_GAP] in background.
 */
object UsageStats {

    private const val PREFS = "monetization"
    private const val SESSION_GAP = 30 * MonetizationPolicy.MINUTE

    // Engagement points
    const val POINTS_PERSON_SAVED = 1
    const val POINTS_PERSON_ADDED = 2
    const val POINTS_TREE_OPENED = 1
    const val POINTS_TREE_CREATED = 3
    const val POINTS_EXPORT = 2
    const val POINTS_REMINDER_ADDED = 2

    private const val FIRST_SEEN = "firstSeen"
    private const val SESSIONS = "sessions"
    private const val SESSION_START = "sessionStart"
    private const val LAST_BACKGROUND = "lastBackground"
    private const val ENGAGEMENT = "engagement"
    private const val INTERSTITIAL_LAST = "interstitialLast"
    private const val INTERSTITIAL_DAY = "interstitialDay"
    private const val INTERSTITIAL_TODAY = "interstitialToday"
    private const val INTERSTITIAL_SESSION = "interstitialSession"
    private const val INTERSTITIAL_TOTAL = "interstitialTotal"
    private const val BREAKS = "breaks"
    private const val PREMIUM_PROMPT_LAST = "premiumPromptLast"
    private const val UPSELL_TIMES = "upsellTimes"
    private const val UPSELL_LAST = "upsellLast"
    private const val UPSELL_SESSION = "upsellSession"
    private const val UPSELL_ENGAGEMENT = "upsellEngagement"

    private lateinit var prefs: SharedPreferences
    /** When the app last came to the foreground, in this process: quiet times also apply after coming back. */
    private var foregroundSince = 0L
    private val clock: () -> Long = System::currentTimeMillis

    @JvmStatic
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.contains(FIRST_SEEN)) prefs.edit().putLong(FIRST_SEEN, now()).apply()
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                val time = now()
                foregroundSince = time
                if (prefs.getInt(SESSIONS, 0) == 0 || time - prefs.getLong(LAST_BACKGROUND, 0) >= SESSION_GAP) {
                    prefs.edit().putInt(SESSIONS, prefs.getInt(SESSIONS, 0) + 1)
                        .putLong(SESSION_START, time)
                        .putInt(INTERSTITIAL_SESSION, 0)
                        .putBoolean(UPSELL_SESSION, false)
                        .apply()
                }
            }

            override fun onStop(owner: LifecycleOwner) {
                prefs.edit().putLong(LAST_BACKGROUND, now()).apply()
            }
        })
    }

    private val ready get() = ::prefs.isInitialized

    /** Records a meaningful action. */
    @JvmStatic
    fun engage(points: Int) {
        if (ready) prefs.edit().putInt(ENGAGEMENT, prefs.getInt(ENGAGEMENT, 0) + points).apply()
    }

    fun recordBreak() {
        if (ready) prefs.edit().putInt(BREAKS, prefs.getInt(BREAKS, 0) + 1).apply()
    }

    /** Returns how many interstitials were shown so far, this one included. */
    fun recordInterstitial(): Int {
        val time = now()
        val day = TimeUnit.MILLISECONDS.toDays(time)
        val today = if (prefs.getLong(INTERSTITIAL_DAY, -1) == day) prefs.getInt(INTERSTITIAL_TODAY, 0) else 0
        val total = prefs.getInt(INTERSTITIAL_TOTAL, 0) + 1
        prefs.edit().putLong(INTERSTITIAL_LAST, time)
            .putLong(INTERSTITIAL_DAY, day)
            .putInt(INTERSTITIAL_TODAY, today + 1)
            .putInt(INTERSTITIAL_SESSION, prefs.getInt(INTERSTITIAL_SESSION, 0) + 1)
            .putInt(INTERSTITIAL_TOTAL, total)
            .putInt(BREAKS, 0)
            .apply()
        return total
    }

    /** A rewarded video was shown: it counts as a full-screen ad for the cooldown of interstitials. */
    fun recordFullScreenAd() {
        prefs.edit().putLong(INTERSTITIAL_LAST, now()).putInt(BREAKS, 0).apply()
    }

    /** The paywall was opened, from anywhere. */
    @JvmStatic
    fun recordPremiumPrompt() {
        if (ready) prefs.edit().putLong(PREMIUM_PROMPT_LAST, now()).apply()
    }

    fun recordUpsell() {
        val time = now()
        prefs.edit().putInt(UPSELL_TIMES, prefs.getInt(UPSELL_TIMES, 0) + 1)
            .putLong(UPSELL_LAST, time)
            .putBoolean(UPSELL_SESSION, true)
            .putInt(UPSELL_ENGAGEMENT, prefs.getInt(ENGAGEMENT, 0))
            .putLong(PREMIUM_PROMPT_LAST, time)
            .apply()
    }

    fun usage(): MonetizationPolicy.Usage? {
        if (!ready) return null
        val time = now()
        val day = TimeUnit.MILLISECONDS.toDays(time)
        return MonetizationPolicy.Usage(
            firstSeen = prefs.getLong(FIRST_SEEN, time),
            sessions = prefs.getInt(SESSIONS, 0),
            // The session, or the current stay in foreground if more recent
            sessionStart = maxOf(prefs.getLong(SESSION_START, time), foregroundSince),
            engagement = prefs.getInt(ENGAGEMENT, 0),
            interstitialLastShown = prefs.getLong(INTERSTITIAL_LAST, 0),
            interstitialsToday = if (prefs.getLong(INTERSTITIAL_DAY, -1) == day) prefs.getInt(INTERSTITIAL_TODAY, 0) else 0,
            interstitialsThisSession = prefs.getInt(INTERSTITIAL_SESSION, 0),
            breaksSinceInterstitial = prefs.getInt(BREAKS, 0),
            premiumPromptLastShown = prefs.getLong(PREMIUM_PROMPT_LAST, 0),
            upsellTimesShown = prefs.getInt(UPSELL_TIMES, 0),
            upsellLastShown = prefs.getLong(UPSELL_LAST, 0),
            upsellShownThisSession = prefs.getBoolean(UPSELL_SESSION, false),
            engagementAtLastUpsell = prefs.getInt(UPSELL_ENGAGEMENT, 0)
        )
    }

    fun now(): Long = clock()

    /** Days since the first launch (of a version with these statistics). */
    fun daysSinceFirstSeen(): Int =
        if (!ready) 0 else TimeUnit.MILLISECONDS.toDays(now() - prefs.getLong(FIRST_SEEN, now())).toInt()
}
