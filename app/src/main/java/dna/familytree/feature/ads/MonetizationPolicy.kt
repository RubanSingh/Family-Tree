package dna.familytree.feature.ads

/**
 * When a full-screen ad or the Premium invitation may appear. Pure logic, no Android: see docs/specs/ads.md and
 * docs/specs/upsell-prompt.md, and MonetizationPolicyTest. All times are epoch milliseconds.
 */
object MonetizationPolicy {

    const val MINUTE = 60_000L
    const val HOUR = 60 * MINUTE
    const val DAY = 24 * HOUR

    // Interstitials
    /** New people use the app ad-free (full screen) for their first day... */
    const val INTERSTITIAL_GRACE = DAY
    /** ...and their first session. */
    const val INTERSTITIAL_MIN_SESSIONS = 2
    /** Never right after the app opens. */
    const val INTERSTITIAL_SESSION_QUIET = MINUTE
    const val INTERSTITIAL_COOLDOWN = 4 * MINUTE
    const val INTERSTITIAL_MAX_PER_SESSION = 3
    const val INTERSTITIAL_MAX_PER_DAY = 6
    /** Natural breaks (task completed) needed between two ads: one ad at most every other break. */
    const val INTERSTITIAL_BREAKS_BETWEEN = 2
    /** No ad right after the Premium invitation or the paywall. */
    const val INTERSTITIAL_QUIET_AFTER_PREMIUM = 3 * MINUTE

    // Premium invitation
    const val UPSELL_MIN_SESSIONS = 3
    const val UPSELL_MIN_ENGAGEMENT = 8
    const val UPSELL_MIN_AGE = DAY
    /** Not in the first moments of a session: people open the app to get to their tree. */
    const val UPSELL_SESSION_QUIET = 30_000L
    const val UPSELL_QUIET_AFTER_INTERSTITIAL = 2 * MINUTE
    /** New engagement needed before inviting again: people who stopped using the app aren't nagged. */
    const val UPSELL_NEW_ENGAGEMENT = 5

    /** Wait after the n-th invitation (index 0 = after the first one). The last value repeats. */
    val UPSELL_COOLDOWNS = longArrayOf(3 * DAY, 7 * DAY, 14 * DAY, 30 * DAY, 45 * DAY)

    data class Usage(
        val firstSeen: Long,
        val sessions: Int,
        /** Start of the session, or of the current stay in foreground if more recent. */
        val sessionStart: Long,
        /** Points for meaningful actions (person saved, tree created, export...). */
        val engagement: Int,
        val interstitialLastShown: Long = 0,
        val interstitialsToday: Int = 0,
        val interstitialsThisSession: Int = 0,
        val breaksSinceInterstitial: Int = 0,
        /** Last time the paywall or the invitation was shown. */
        val premiumPromptLastShown: Long = 0,
        val upsellTimesShown: Int = 0,
        val upsellLastShown: Long = 0,
        val upsellShownThisSession: Boolean = false,
        val engagementAtLastUpsell: Int = 0
    )

    fun canShowInterstitial(usage: Usage, now: Long): Boolean = with(usage) {
        now - firstSeen >= INTERSTITIAL_GRACE
                && sessions >= INTERSTITIAL_MIN_SESSIONS
                && now - sessionStart >= INTERSTITIAL_SESSION_QUIET
                && now - interstitialLastShown >= INTERSTITIAL_COOLDOWN
                && interstitialsThisSession < INTERSTITIAL_MAX_PER_SESSION
                && interstitialsToday < INTERSTITIAL_MAX_PER_DAY
                && breaksSinceInterstitial >= INTERSTITIAL_BREAKS_BETWEEN
                && now - premiumPromptLastShown >= INTERSTITIAL_QUIET_AFTER_PREMIUM
                && !isUpsellDue(usage, now) // One prompt at a time: the invitation goes first
    }

    fun isUpsellDue(usage: Usage, now: Long): Boolean = with(usage) {
        if (upsellShownThisSession) return false
        if (sessions < UPSELL_MIN_SESSIONS || engagement < UPSELL_MIN_ENGAGEMENT || now - firstSeen < UPSELL_MIN_AGE) return false
        if (now - interstitialLastShown < UPSELL_QUIET_AFTER_INTERSTITIAL) return false
        if (upsellTimesShown == 0) return true
        val cooldown = UPSELL_COOLDOWNS[minOf(upsellTimesShown - 1, UPSELL_COOLDOWNS.size - 1)]
        now - upsellLastShown >= cooldown && engagement - engagementAtLastUpsell >= UPSELL_NEW_ENGAGEMENT
    }

    /** [isUpsellDue] and the session is old enough for an invitation to be welcome. */
    fun shouldShowUpsell(usage: Usage, now: Long): Boolean =
        now - usage.sessionStart >= UPSELL_SESSION_QUIET && isUpsellDue(usage, now)
}
