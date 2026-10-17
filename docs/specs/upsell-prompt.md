# Premium invitation (upsell prompt)

_Last updated: 2026-10-17_

## Goal

Invite people who clearly enjoy the app to Premium, at a good moment and with their own numbers, without nagging
people who use it rarely or who already said "Not now".

## Behaviour

### When it appears (`MonetizationPolicy.shouldShowUpsell`)

Checked when the **Trees** screen resumes, or when the user **comes back** to the tree screen (`PrincipalController`)
from a profile or an editor — never right when a tree is opened — 0.7 s later, and only if the screen still has the
focus (no dialog, no menu open). All of these must hold:

| Rule | Value |
|------|-------|
| Not Premium | — |
| Enough use | ≥ 3 sessions, ≥ 8 engagement points, ≥ 1 day since the first launch |
| Not at launch | ≥ 30 s into the session and into the current stay in foreground |
| Once per session | at most one invitation per session |
| Not after an ad | ≥ 2 min since the last interstitial |
| Back-off | after the 1st invitation wait 3 days, then 7, 14, 30, then every 45 days |
| Still in use | ≥ 5 new engagement points since the last invitation |

While the invitation is due, interstitials are not shown, so the user never gets both at the same moment.

**Engagement points** (`UsageStats`): person edited 1, person added 2, tree opened 1, tree created or imported 3,
diagram exported 2, reminder added 2.

### What it shows (`layout/upsell_sheet.xml`)

A bottom sheet, fully expanded:

1. Gold hero: diamond tile, "Your family story deserves Premium", subtitle.
2. **The user's numbers**: People / Trees / Generations from `Settings.totalPersons()`, `trees.size`,
   `maxGenerations()` (digits localised). With no people yet, the tiles are hidden and the subtitle becomes
   "Protect your research and enjoy Family Tree without ads."
3. Four highlights (`PremiumFeature.highlights`): No ads, Automatic backup, Smart family reminders, Print-ready exports.
4. "and 5 more Premium features" → paywall.
5. Live prices from Google Play when available: "Plans from ₹X per month / or pay once ₹Y and keep it forever".
   Connecting to Play also restores a purchase made elsewhere (the sheet then closes with "Premium restored").
6. **See Premium plans** (primary) → paywall; **Not now**; "Cancel anytime · Secure payment by Google Play".

## Code

| File | Role |
|------|------|
| `feature/billing/UpsellPrompt.kt` | `maybeShow(activity)`, `show(activity)` |
| `feature/ads/MonetizationPolicy.kt` | Timing rules (unit tested) |
| `feature/ads/UsageStats.kt` | Sessions, engagement, invitation history |
| `layout/upsell_sheet.xml`, `upsell_stat.xml`, `upsell_feature.xml` | UI |
| `TreesController.onResume`, `PrincipalController.onResume` | Trigger points |

## Data

SharedPreferences `monetization`: `upsellTimes`, `upsellLast`, `upsellSession`, `upsellEngagement`,
`premiumPromptLast` (also set whenever the paywall opens).

## Analytics

Event `premium_invitation` with `action` = `shown`, `cta`, `more`, `later`, `dismiss`.

## Testing

Unit: `MonetizationPolicyTest` (first time, once a session, back-off, needs new use, not after an interstitial).
Emulator: set `shared_prefs/monetization.xml` to `sessions` 5, `engagement` 20, `firstSeen` 3 days ago, then open the
app, wait 30 s and open/close a tree.

## Open items

* A/B test the thresholds (3 sessions / 8 points) and the copy with Firebase Remote Config.
* Optional: an introductory offer (first month discount) in Play Console would show as "N-day free trial" on the
  paywall automatically.
