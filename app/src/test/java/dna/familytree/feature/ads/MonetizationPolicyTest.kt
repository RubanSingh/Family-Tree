package dna.familytree.feature.ads

import dna.familytree.feature.ads.MonetizationPolicy.DAY
import dna.familytree.feature.ads.MonetizationPolicy.MINUTE
import dna.familytree.feature.ads.MonetizationPolicy.Usage
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MonetizationPolicyTest {

    private val now = 100 * DAY

    /** A regular user: installed long ago, in the middle of a session, ready for an interstitial. */
    private val regular = Usage(
        firstSeen = now - 30 * DAY,
        sessions = 20,
        sessionStart = now - 10 * MINUTE,
        engagement = 50,
        interstitialLastShown = now - 10 * MINUTE,
        breaksSinceInterstitial = 2,
        upsellTimesShown = 1,
        upsellLastShown = now - DAY, // Invitation not due yet
        engagementAtLastUpsell = 45
    )

    @Test
    fun interstitial_atNaturalBreak() = assertTrue(MonetizationPolicy.canShowInterstitial(regular, now))

    @Test
    fun interstitial_notOnFirstDayNorFirstSession() {
        assertFalse(MonetizationPolicy.canShowInterstitial(regular.copy(firstSeen = now - 2 * MINUTE * 60), now))
        assertFalse(MonetizationPolicy.canShowInterstitial(regular.copy(sessions = 1), now))
    }

    @Test
    fun interstitial_notRightAfterLaunch() =
        assertFalse(MonetizationPolicy.canShowInterstitial(regular.copy(sessionStart = now - 30_000), now))

    @Test
    fun interstitial_cooldownAndCaps() {
        assertFalse(MonetizationPolicy.canShowInterstitial(regular.copy(interstitialLastShown = now - 3 * MINUTE), now))
        assertFalse(MonetizationPolicy.canShowInterstitial(regular.copy(interstitialsThisSession = 3), now))
        assertFalse(MonetizationPolicy.canShowInterstitial(regular.copy(interstitialsToday = 6), now))
    }

    @Test
    fun interstitial_everyOtherBreak() =
        assertFalse(MonetizationPolicy.canShowInterstitial(regular.copy(breaksSinceInterstitial = 1), now))

    @Test
    fun interstitial_notAfterPremiumPrompt() =
        assertFalse(MonetizationPolicy.canShowInterstitial(regular.copy(premiumPromptLastShown = now - MINUTE), now))

    @Test
    fun interstitial_yieldsToDueInvitation() =
        assertFalse(MonetizationPolicy.canShowInterstitial(regular.copy(upsellTimesShown = 0), now))

    private val newcomer = Usage(
        firstSeen = now - 2 * DAY,
        sessions = 3,
        sessionStart = now - 5 * MINUTE,
        engagement = 8
    )

    @Test
    fun upsell_firstTimeAfterEnoughUse() {
        assertTrue(MonetizationPolicy.shouldShowUpsell(newcomer, now))
        assertFalse(MonetizationPolicy.shouldShowUpsell(newcomer.copy(sessions = 2), now))
        assertFalse(MonetizationPolicy.shouldShowUpsell(newcomer.copy(engagement = 7), now))
        assertFalse(MonetizationPolicy.shouldShowUpsell(newcomer.copy(firstSeen = now - DAY / 2), now))
    }

    @Test
    fun upsell_onceASessionAndNotAtLaunch() {
        assertFalse(MonetizationPolicy.shouldShowUpsell(newcomer.copy(upsellShownThisSession = true), now))
        assertFalse(MonetizationPolicy.shouldShowUpsell(newcomer.copy(sessionStart = now - 10_000), now))
    }

    @Test
    fun upsell_notRightAfterInterstitial() =
        assertFalse(MonetizationPolicy.shouldShowUpsell(newcomer.copy(interstitialLastShown = now - MINUTE), now))

    @Test
    fun upsell_backsOffAndNeedsNewUse() {
        val shownOnce = newcomer.copy(upsellTimesShown = 1, engagementAtLastUpsell = 8, engagement = 20)
        assertFalse(MonetizationPolicy.shouldShowUpsell(shownOnce.copy(upsellLastShown = now - 2 * DAY), now))
        assertTrue(MonetizationPolicy.shouldShowUpsell(shownOnce.copy(upsellLastShown = now - 3 * DAY), now))
        // Not used since: no new invitation
        assertFalse(MonetizationPolicy.shouldShowUpsell(shownOnce.copy(upsellLastShown = now - 3 * DAY, engagement = 10), now))
        val shownFiveTimes = shownOnce.copy(upsellTimesShown = 5)
        assertFalse(MonetizationPolicy.shouldShowUpsell(shownFiveTimes.copy(upsellLastShown = now - 44 * DAY), now))
        assertTrue(MonetizationPolicy.shouldShowUpsell(shownFiveTimes.copy(upsellLastShown = now - 45 * DAY), now))
        // The last wait repeats
        assertFalse(MonetizationPolicy.shouldShowUpsell(shownFiveTimes.copy(upsellTimesShown = 9, upsellLastShown = now - 30 * DAY), now))
    }
}
