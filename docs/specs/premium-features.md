# Premium features

_Last updated: 2026-10-17_

## Goal

Premium is worth paying for, while nobody loses something they already had for free.

## Plans

| Plan | Play product | Type |
|------|--------------|------|
| Monthly | `family_tree_premium_monthly` (base plan `monthly`) | Subscription |
| Lifetime | `family_tree_premium` | One-time, non-consumable |

Both unlock everything below. Details: [paywall.md](paywall.md), docs/REBRAND_PLAN.md §4.

## What Premium includes

The catalogue in code is `feature/billing/PremiumFeature.kt` (paywall table, cards and invitation are built from it).

| Group | Feature | Free | Premium | Gate in code |
|-------|---------|------|---------|--------------|
| Keep it safe | Automatic backup | Manual backup (and restore) | Automatic, 3 copies per tree | `BackupManager.isAutomatic()`, `BackupController` |
| Keep it safe | Media organiser | One file at a time | All files of a tree at once | `MediaToolsUi.copyAllToAppStorage` |
| Never miss a moment | Smart family reminders | Birthdays + 2 reminders | Unlimited reminders, wedding anniversaries, remembrance days, early alerts | `ReminderScheduler.dueBetween`, `ReminderEditor`, `EventsController` |
| Never miss a moment | Family places | First 3 places | All places | `PlacesController` |
| Create & share | Print-ready exports | Once per rewarded video (PDF always free for legacy users) | Diagram as PDF and HD PNG, unlimited | `DiagramFragment` (options menu) → `RewardedUnlock` |
| Create & share | Merge trees | — | Included | `merge/ResultFragment` |
| Create & share | Advanced tools | — | Sources, repositories, submitters, GEDCOM export, ZIP backup, compare, shrink paths | `SettingsController` (expert mode) |
| Make it yours | No ads | With ads | No ads | `AdsManager.adsEnabled()` ([ads.md](ads.md)) |
| Make it yours | 20 colour themes | Classic theme | 20 themes | `SettingsController`, `ThemeController`, `BaseController.setApplicationTheme` |

Every gate shows the gold ◆ (`PremiumBadge.withDiamond`, `SettingsRowView` premium badge) and opens the paywall
with the feature's title, so the paywall highlights that feature.

## Existing features moved to Premium (new installs only)

Asked on 2026-10-17: "if required add already available features to Premium". Rule kept from before: **people who
already had the app keep what was free for them** (they were promised it; taking it away hurts ratings and trust).

| Feature | Before | Now, new installs | Now, users from before 1.6 |
|---------|--------|-------------------|----------------------------|
| Export diagram as PDF | Free | Premium (◆ in the diagram menu) | Free, as before |

How "user from before" is decided (`Settings.legacyUser`, `Settings.installedAt`, set in `Global.start`):

* `settings.json` loaded and without `installedAt` → the app was used before this version → `legacyUser = true`.
* `settings.json` missing but tree files found (lost settings) → `legacyUser = true`.
* Fresh install → `installedAt` = now, `legacyUser = false`.

Gate: `PremiumGate.isPremiumOrLegacy()`. The paywall shows "Free: PDF" to legacy users. New installs can still export
a PDF once after a rewarded video ([ads.md](ads.md#rewarded-videos--one-off-premium-actions-by-choice)).

### Considered and not moved

| Feature | Why it stays free |
|---------|-------------------|
| GEDCOM export / backups / restore | Data portability: people must always be able to take their family data out. (GEDCOM export is already in Advanced tools.) |
| Birthday notifications | Core daily value and a reason to keep the app; anniversaries etc. are already Premium |
| Number of trees / people | Limits would block people from adding their own family; bad first impression |
| Find errors, tree settings, sharing | Maintenance or collaboration features; gating them feels punitive |

Candidates if more revenue is needed later (new installs only): a limit of 3 trees for free users (restore and
received shares exempt), Premium diagram styles, statistics/insights.

## Code

| File | Role |
|------|------|
| `feature/billing/PremiumGate.kt` | `isPremium()`, `isPremiumOrLegacy()`, `open(context, reason)` |
| `feature/billing/PremiumFeature.kt` | Catalogue |
| `feature/billing/SettingsEntitlementStore.kt` | Grant / revoke Premium in `settings.json` |
| `feature/billing/EntitlementSync.kt` | Revokes a lapsed monthly plan at start |

## Data

`settings.json`: `premium`, `premiumPlan`, `premiumToken`, `premiumSince`, `installedAt`, `legacyUser`
(top-level `Settings` fields, kept by R8 through `-keepclassmembernames class dna.familytree.Settings`).

## Testing

* Upgrade over the previous build: `legacyUser` becomes `true`, PDF export has no ◆ (verified on `ft_test`).
* Fresh install (or `legacyUser:false` in `settings.json`): PDF shows ◆ and opens the paywall on "Export PDF"
  with the *Print-ready exports* card highlighted (verified).
