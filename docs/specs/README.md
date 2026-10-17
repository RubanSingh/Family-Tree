# Feature specs

_Last updated: 2026-10-17_

One file per feature area: what it does, the rules it follows, where the code lives and how to test it.
Read the relevant spec before changing a feature, and update it in the same change.

| Spec | What it covers |
|------|----------------|
| [premium-features.md](premium-features.md) | What Premium includes, free vs Premium limits, every gate in the code, the "legacy user" rule |
| [paywall.md](paywall.md) | The Premium page: sections, plans and prices, highlighting the feature that was tapped, analytics |
| [upsell-prompt.md](upsell-prompt.md) | The Premium invitation shown after enough use: when it appears, what it shows |
| [ads.md](ads.md) | Native, interstitial and rewarded ads: placements, frequency rules, ad units, policy and testing |

Older design and feature notes (brand, design system, backup, reminders, places, media tools, in-app updates,
translations, large trees) are in [../REBRAND_PLAN.md](../REBRAND_PLAN.md).

## Monetisation at a glance

```
            ┌───────────── free user ─────────────┐
 app use ──►│ native ads (no layout shift)        │
            │ interstitial at natural breaks      │──► "Remove ads" link / hint ──┐
            │ ◆ one-off action ──► watch a video ─┼──► do it once                 │
            │ ◆ on Premium features ──────────────┼──► paywall (feature focus) ◄──┤
            │ invitation after enough use ────────┼──► paywall (generic)       ◄──┘
            └─────────────────────────────────────┘          │
                                                    Monthly / Lifetime (Google Play)
                                                             │
                                                  Premium: no ads, all features
```

* Rules for *when* things appear live in one pure class, `feature/ads/MonetizationPolicy.kt`, unit tested in
  `app/src/test/.../MonetizationPolicyTest.kt`.
* Usage data (sessions, engagement points, ad history) is in the SharedPreferences file `monetization`
  (`feature/ads/UsageStats.kt`), never in `settings.json` or backups.
* Analytics events: `ad_interstitial` (placement), `ad_rewarded` (action, placement), `premium_invitation` (action),
  `paywall` (action, reason).

## Spec template

```
# <Feature>
_Last updated: <date>_
## Goal            — the user / business outcome
## Behaviour       — rules, numbers, states; what free and Premium users see
## Code            — files and entry points
## Data            — what is stored, where, migration
## Testing         — how to verify (unit tests, emulator steps)
## Open items      — decisions still to take
```
