# Ads (native, interstitial and rewarded)

_Last updated: 2026-10-17_

## Goal

Earn more from free users without hurting the experience: ads look like part of the app, never move content under a
finger, and full-screen ads only appear when a task is finished. Premium removes every ad.

## Behaviour

### Who sees ads

* Free users only. `AdsManager.adsEnabled()` = not Premium. Premium users never initialise the Mobile Ads SDK.
* Buying Premium drops loaded ads at once (`AdsManager.onPremiumGranted()`); cards still on screen disappear when
  their screen is shown again (`NativeAdCard.onWindowVisibilityChanged`). If a monthly plan lapses, ads come back
  (`SettingsEntitlementStore.revoke()` → `AdsManager.init`).

### Native ads — placements

| Screen | Where | Size | Ad source |
|--------|-------|------|-----------|
| Trees (home) | After the tree cards (list footer) | small | preloaded, or loaded late |
| Persons list | First row of the list, scrolls away; hidden while searching | small | preloaded only |
| Families list | Top of the page | small | preloaded only |
| Events & reminders | After the first group of events (or at the end) | small | preloaded only, taken once per screen |
| Person profile → Facts | After the last fact | large (media) | preloaded, or loaded late |
| Detail screens (event, family, note, source…) | After the content | small | preloaded, or loaded late |
| About | Bottom of the page | large (media) | preloaded, or loaded late |

**No layout shift rule:** a card above content may only show an ad that is *already loaded* when the screen is built
(`NativeAdCard.show(owner, lateLoad = false)` / `NativeAdAdapter`). Only cards at the end of a page may load late,
because appearing there moves nothing. This avoids accidental clicks (an AdMob policy violation) and jumping lists.

**Preloading:** `AdsManager` keeps a pool of up to 2 native ads, filled after SDK initialisation and refilled each
time one is taken. Ads older than 55 minutes are discarded (AdMob ads expire after 1 hour).

**Card design** (`layout/ad_native_small.xml`, `ad_native_large.xml`, `ad_native_header.xml`): outlined design-system
card, "Ad" badge (required attribution) next to the advertiser, 44dp rounded icon, headline, body, tonal
call-to-action; the large card adds a 180dp `MediaView`. Space is left in the top-end corner for the AdChoices icon.
A "◆ Remove ads" link sits under the ad, *outside* the `NativeAdView`, so it is never counted as an ad click; it opens
the paywall on "No ads".

Each card owns its `NativeAd` and destroys it with the screen's lifecycle (activity, or fragment *view* lifecycle).
List adapters bind a shared ad without owning it (`bind(ad, owned = false)`); the screen destroys it.

### Interstitials — natural breaks

Shown only right after a task is completed:

| Placement | Trigger |
|-----------|---------|
| `PERSON_SAVED` | Done in the person editor (`PersonEditorController`) |
| `TREE_CLOSED` | Back from a tree to the trees list (`PrincipalController.onBack`) |
| `EXPORT_DONE` | Diagram exported as PDF (`DiagramFragment`) |

Every call counts a *natural break*; an ad is shown only if one is loaded and **all** of these hold
(`MonetizationPolicy.canShowInterstitial`):

| Rule | Value |
|------|-------|
| Grace for new users | no interstitial in the first 24 h after the first launch, nor in the first session |
| Quiet start | not in the first 60 s of a session or of coming back to the app |
| Cooldown | 4 min since the last interstitial |
| Frequency | at most one every 2 natural breaks |
| Caps | 3 per session, 6 per day |
| After Premium prompts | not within 3 min after the paywall or the invitation |
| One prompt at a time | not when the Premium invitation is due (the invitation goes first) |

The interstitial is preloaded when a tree is opened and after each display. After every 2nd interstitial, the next
screen shows a snackbar "Enjoy Family Tree without ads with Premium" with a "Remove ads" action.

A session starts when the app comes to the foreground after 30 min or more in background (`UsageStats`).

### Rewarded videos — one-off Premium actions, by choice

A free user who taps a **one-off** Premium action gets a bottom sheet (`RewardedUnlock.offer`, `layout/reward_unlock_sheet.xml`):
the feature (icon, title, "This is a Premium feature."), **Watch a short video** ("Use it once for free, right after
the video.") and **Go Premium** (opens the paywall on that feature).

| Action | Where | Who gets the sheet |
|--------|-------|--------------------|
| Export diagram as HD PNG | `DiagramFragment` options menu | every free user |
| Export diagram as PDF | `DiagramFragment` options menu | free users who installed from 1.6 (legacy users export PDF for free) |

Rules:
* The video plays **only after the user taps "Watch a short video"** (AdMob rewarded policy: opt-in, clear reward).
* The reward is the action itself, run once after the video is dismissed if `onUserEarnedReward` fired; closing the
  video early shows "Watch the video to the end to unlock it." and nothing happens.
* The ad is preloaded when the diagram options menu opens for a free user (intent signal), and loaded on demand
  otherwise; after 10 s without a video the sheet says none is available and offers Premium.
* A rewarded video counts as a full-screen ad for the interstitial cooldown (no interstitial right after it, e.g. at
  `EXPORT_DONE`), and for the 2-minute quiet time of the Premium invitation.
* Only actions done once at a time are rewardable. Lasting benefits (no ads, themes, automatic backup, reminders,
  places, merge) stay Premium-only so the video never replaces the subscription.

### Content and privacy

* Max ad content rating **PG** (`RequestConfiguration`, set in `AdsManager.init`): a family app. Raising it to T
  would add some demand; it's one constant.

### Consent (UMP) — EEA, UK, Switzerland, US states

`feature/ads/ConsentManager.kt` with Google's User Messaging Platform 4.0.0:

* The first screen of a session (`TreesController` or `PrincipalController`) calls `gatherAndStartAds`: if consent
  from a previous session allows ads they start at once; the status is updated and the message configured in
  AdMob › Privacy & messaging is shown when the user's region requires it; then ads start if `canRequestAds()`.
* **No ad is requested before `canRequestAds()`** (`AdsManager.canLoad`): init, interstitial, rewarded, native.
* "Do not consent" still allows ads (Google serves limited / non-personalised ads); the form isn't shown again.
* Settings › Support › **Ad privacy choices** (`privacy_options`) reopens the form; visible only for free users
  whose region requires it (`isPrivacyOptionsRequired`).
* Premium users are never asked.
* The GDPR message ("Digital AI Labs asks for your consent…", Consent / Do not consent / Manage options) was already
  published in AdMob on 2026-10-17 and shows correctly.

Testing (debug builds only), in `shared_prefs/monetization.xml`: `debugEea` = true forces the EEA geography,
`debugDeviceHash` = the hashed id that UMP logs ("addTestDeviceHashedId(...)"; emulator `ft_test`:
`B3EEABB8EE11C2BE770B684D95219ECB`), `debugResetConsent` = true forgets the choice once.

## Code

| File | Role |
|------|------|
| `feature/ads/AdsManager.kt` | SDK init (background thread), interstitial load/show, native pool, remove-ads hint |
| `feature/ads/NativeAdCard.kt` | The card view: binds a `NativeAd`, lifecycle, hides for Premium |
| `feature/ads/NativeAdAdapter.kt` | One preloaded ad as the first row of a RecyclerView (in a `ConcatAdapter`) |
| `feature/ads/RewardedUnlock.kt` | "Watch a video or go Premium" sheet for one-off actions |
| `feature/ads/ConsentManager.kt` | Privacy consent (UMP) before any ad request; privacy options form |
| `feature/ads/MonetizationPolicy.kt` | Pure rules (unit tested) |
| `feature/ads/UsageStats.kt` | Sessions, engagement, ad history (SharedPreferences `monetization`) |
| `BaseController.showNativeAd()` / `BaseFragment.showNativeAdIfReady()` | Show the layout's `R.id.my_template` card |
| `BaseController.onResume()` | Shows the pending "remove ads" hint |
| `app/build.gradle` | `BuildConfig.AD_UNIT_NATIVE`, `AD_UNIT_INTERSTITIAL`, `AD_UNIT_REWARDED` per build type |

## Ad units

| Build | Native | Interstitial | Rewarded |
|-------|--------|--------------|----------|
| debug | `ca-app-pub-3940256099942544/2247696110` (Google sample) | `ca-app-pub-3940256099942544/1033173712` (Google sample) | `ca-app-pub-3940256099942544/5224354917` (Google sample) |
| release | set in `keystore.properties` | set in `keystore.properties` | set in `keystore.properties` |

Debug builds use Google's sample units: real ads requested by development builds count as invalid traffic and can
get the AdMob account limited. The app id (`com.google.android.gms.ads.APPLICATION_ID` in the manifest) is the same.

## Analytics

`ad_interstitial` (placement), `ad_rewarded` (action = offered, watch, unavailable, earned, not_earned, premium;
placement = `export_png` / `export_pdf`).

## Testing

* Unit: `./gradlew :app:testDebugUnitTest` (`MonetizationPolicyTest`).
* Emulator: make the user free and "used" by editing, with `adb shell run-as dna.familytree`, `files/settings.json`
  (`premium:false`) and `shared_prefs/monetization.xml` (`firstSeen` 3 days ago, `sessions` 5, `breaks` 1). Then open a
  tree, wait 60 s and go back: an interstitial appears (if the invitation isn't due).
* Known emulator issue (2026-10-17): Google's sample *native* units fail with "Incorrect native ad response. Click
  actions were not properly specified" (error 0) on the `ft_test` emulator, and the production unit returns "No fill"
  there. Cards then simply stay hidden (verified). Verify native rendering on a real device registered as a test
  device in AdMob (Settings › Test devices), never by clicking production ads.

## Open items

* AdMob console: add the "US state regulations" message (same code handles it), add all app languages to the
  messages, publish `app-ads.txt` on the developer website, register real test devices.
* More rewardable one-off actions could be added later with the same `RewardedUnlock.offer` (e.g. "copy all media
  to app storage" once); avoid lasting benefits.
* Tune the numbers above from Firebase (`ad_interstitial`, retention) after a release.
