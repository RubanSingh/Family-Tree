# Family Tree — Pro Rebrand & Monetisation Plan

_Last updated: 2026-10-17_

This document is the single source of truth for the rebrand. It covers the brand
direction, the module split, the billing model (monthly + one-time) and a
step-by-step execution checklist. Each step lists the files it touches so the
work can be reviewed and shipped incrementally.

---

## 1. Goals

| # | Goal | Measure of done |
|---|------|-----------------|
| G1 | Look and feel of a Google Play **Editors' Choice** app: Material 3, tonal surfaces, clear hierarchy, generous whitespace, consistent iconography | Every screen inherits one theme; no hard-coded colours/sizes in rebranded screens |
| G2 | **Productive** feel: fast scanning, grouped settings, obvious primary actions, no clutter | Settings grouped into sections; home list shows tree metadata at a glance; one CTA per screen |
| G3 | **Billing**: Premium available as a **Monthly subscription** and a **Lifetime one-time** purchase | Paywall shows both plans with live Play prices; entitlement survives restarts and is reconciled with Play on launch |
| G4 | **Module-wise separation** so the design system and billing can be reused and tested independently | `:core:designsystem` and `:core:billing` Gradle modules; app features live in `feature.*` packages |

---

## 2. Brand direction — "Heritage, made simple"

**Name**: Family Tree (unchanged, package `dna.familytree`).
**Tagline**: _Your family story, beautifully kept._

### 2.1 Colour (Material 3 tonal palette)

The launcher icon is blue, so the brand seed stays in the blue family and
matches Google's own product blue. Gold is reserved for Premium.

| Role | Light | Dark | Use |
|------|-------|------|-----|
| Primary | `#0B57D0` | `#A8C7FA` | Buttons, FAB, active states, links |
| Primary container | `#D3E3FD` | `#0842A0` | Tonal buttons, selected cards, avatar chips |
| Secondary | `#575E71` | `#BFC6DC` | Supporting text/icons |
| Tertiary (Premium gold) | `#725C00` | `#EAC248` | Premium badge, "Best value" pill |
| Surface | `#FFFFFF` | `#131314` | Page background |
| Surface container low / high | `#F3F6FC` / `#E9EEF6` | `#1B1B1D` / `#282A2C` | Cards, toolbar, grouped settings |
| On surface / variant | `#1F1F1F` / `#444746` | `#E3E3E3` / `#C4C7C5` | Text |
| Outline / variant | `#747775` / `#C4C7C5` | `#8E918F` / `#444746` | Dividers, outlined cards |
| Error | `#B3261E` | `#F2B8B5` | Destructive actions |

The 20 user-selectable colour themes are kept (Premium feature) and now inherit
all component styles from the brand base theme, so they only change colour.

### 2.2 Typography (M3 type scale, system sans)

| Token | Size / weight | Where |
|-------|---------------|-------|
| Display small | 36sp / regular | Paywall price |
| Headline medium | 28sp / regular | Screen hero titles |
| Title large | 22sp / regular | Toolbar titles |
| Title medium | 16sp / medium | Card titles, settings row titles |
| Body large | 16sp / regular | Primary content |
| Body medium | 14sp / regular | Secondary text, descriptions |
| Label large | 14sp / medium | Buttons, section headers |
| Label medium | 12sp / medium | Chips, badges |

### 2.3 Shape & spacing

* Corner radius: small 8dp, medium 16dp, large 28dp, full (pill) for buttons.
* Spacing scale: 4 / 8 / 12 / 16 / 24 / 32 / 48dp.
* Minimum touch target 48dp. Icon 24dp inside a 40dp tonal container.

### 2.4 Iconography

Material Symbols (rounded) as vector drawables, prefixed `ds_ic_*`, tinted
with `?attr/colorOnSurfaceVariant`; primary when active.

---

## 3. Module map

```
FamilyTree/
├── app/                                   ← application (features + legacy screens)
│   └── src/main/java/dna/familytree/
│       ├── feature/
│       │   ├── billing/                   ← Paywall UI, premium gate, entitlement sync
│       │   │   ├── PaywallController.kt
│       │   │   ├── PremiumGate.kt
│       │   │   ├── SettingsEntitlementStore.kt
│       │   │   └── EntitlementSync.kt
│       │   └── settings/                  ← Settings + About
│       │       ├── SettingsController.java
│       │       └── AboutController.java
│       ├── theme/                         ← colour theme picker (existing, gated)
│       ├── list/ detail/ merge/ share/    ← existing feature packages (unchanged)
│       └── ... legacy root classes
├── core/
│   ├── designsystem/                      ← :core:designsystem (Android library)
│   │   └── src/main/
│   │       ├── java/dna/familytree/designsystem/
│   │       │   └── SettingsRowView.kt     ← reusable list row (icon/title/subtitle/switch|chevron|value)
│   │       └── res/
│   │           ├── values/ds_colors.xml, ds_themes.xml, ds_type.xml, ds_styles.xml, ds_dimens.xml, ds_attrs.xml
│   │           ├── values-night/ds_colors.xml
│   │           ├── drawable/ds_ic_*.xml, ds_bg_*.xml
│   │           └── layout/ds_section_header.xml, ds_settings_row.xml
│   └── billing/                           ← :core:billing (Android library, Kotlin)
│       └── src/main/java/dna/familytree/billing/
│           ├── PremiumPlan.kt             ← MONTHLY (subs) / LIFETIME (in-app)
│           ├── PlanOffer.kt               ← price + period resolved from Play
│           ├── BillingEvent.kt            ← sealed event stream
│           ├── EntitlementStore.kt        ← interface the app implements
│           └── BillingManager.kt          ← connection, query, purchase, restore, acknowledge
├── cropper/                               ← existing image cropper library
└── docs/REBRAND_PLAN.md                   ← this file
```

**Dependency rule**: `app → core:*`, `core:billing → (nothing in app)`,
`core:designsystem → Material only`. Core modules never import `dna.familytree.Global`.

---

## 4. Billing model

| Plan | Play product id | Type | Base plan / offer | Notes |
|------|-----------------|------|-------------------|-------|
| Monthly | `family_tree_premium_monthly` | Subscription | base plan `monthly` (P1M) | Auto-renews, cancel anytime, 7-day grace |
| Lifetime | `family_tree_premium` | In-app (one-time, non-consumable) | — | Existing product; keeps old buyers entitled |

### 4.1 Play Console checklist (manual)

1. Monetise → Products → **Subscriptions** → create `family_tree_premium_monthly`, add base plan id `monthly`, period 1 month, price per country, activate.
2. Confirm `family_tree_premium` (one-time) is still **Active**.
3. Add a licence tester account to test purchases without charges.
4. Upload the new build to Internal testing before opening the paywall in production.

### 4.2 Entitlement rules (implemented in `:core:billing` + `feature.billing`)

* On paywall open: connect → query owned purchases (SUBS + INAPP) → if any
  purchased & acknowledged → grant; otherwise query product details and show offers.
* On purchase: acknowledge (required within 3 days or Play refunds) → grant → persist plan + token.
* On app start (`EntitlementSync.reconcile`): if stored plan is **Monthly**, re-query
  SUBS; if no active subscription is returned → revoke Premium. Lifetime is never
  revoked automatically (only if Play reports it refunded).
* Restore purchases is always available on the paywall (required by Play policy).
* Manage subscription deep link: `https://play.google.com/store/account/subscriptions?sku=<id>&package=<app>`.

---

## 5. Screen inventory & status

| Screen | Layout | Status | Rebrand notes |
|--------|--------|--------|---------------|
| Splash | `splash.xml` | ✅ Rebranded | Tonal gradient, logo in rounded container, name + tagline, loader |
| Trees (home) | `trees.xml`, `trees_bar.xml`, `pezzo_albero.xml` | ✅ Rebranded | Card rows with avatar initial + meta; Premium pill; tonal top bar |
| Settings | `settings.xml` | ✅ Rebranded | Grouped sections; Premium banner; `SettingsRowView` rows |
| About | `about_activity.xml` | ✅ Rebranded | Icon hero, version, Premium status card, links |
| Paywall | `paywall_activity.xml`, `paywall_plan_card.xml` | ✅ New | Monthly + Lifetime plan cards, feature list, restore, legal |
| Theme picker | `activity_theme.xml`, `theme_row_item.xml` | ✅ Rebranded | Premium lock badge wired up |
| Tree diagram | `diagram.xml`, `diagram_card.xml`, `diagram_minicard.xml`, `popup.xml`, `diagram_button.xml` | ✅ Redesigned | Midnight brand canvas, frosted round controls, 16dp person cards with silhouette avatar, gold focus card + glow, ribbon mourning badge, gold-ringed marriage year |
| Drawer | `principe.xml`, `menu_principe_testata.xml`, `menu_item_extra.xml`, `menu/menu_principe.xml` | ✅ Redesigned | Theme gradient hero with photo scrim, "Current tree" title, glass "Trees" chip, gold Save; M3 drawer with pill indicator, Material Symbols icons, count badges |
| Diagram settings | `diagram_settings.xml` | ✅ Redesigned | Intro card + Ancestry / Partners / Descendants groups, icon rows with descriptions, value pill per slider, M3-style slider, whole-row partner switch |
| Person profile | `individuo.xml`, `individuo_scheda.xml`, `individuo_eventi_pezzo.xml`, `piece_note.xml`, `pezzo_citazione_fonte.xml`, `pezzo_fonte.xml`, `pezzo_data_cambiamenti.xml` | ✅ Redesigned | Hero with ringed avatar (silhouette fallback), photo fades into surface, M3 tabs; facts as tonal cards with source-count pills |
| Person / family cards | `piece_person.xml`, `pezzo_individuo_piccolo.xml`, `families_item.xml`, `piece_family.xml` | ✅ Redesigned | Leading avatar, role overline, gender outline shared with diagram cards; list containers get a 16dp gutter |
| Person editor | `edita_individuo.xml`, `barra_edita.xml` | ✅ Redesigned | Name & sex / Birth / Death cards; Cancel + Done (with check) in the toolbar |
| New tree / Tree info | `new_tree.xml`, `info_albero.xml`, `albero_nomina.xml` | ✅ Redesigned | Illustrated hero, one primary path + "Other ways to start"; overview and header cards |
| Detail rows / dialogs | `pezzo_fatto.xml`, `nuovo_parente.xml` | ✅ Restyled | Label/value typography, 48dp touch targets |
| Persons list | `persons_list.xml` (new), `piece_person.xml` | ✅ Redesigned | Always-visible pill search bar (same `SearchView` + filter), one-tap sort chips mirroring "Order by" (tap again = reverse), "Showing X of Y", empty state; avatar gender ring, role pill, chevron |
| Media tiles | `pezzo_media.xml`, `gallery.xml` | ✅ Redesigned | Rounded full-bleed image, caption on a scrim, glass usage-count pill |
| Splash (system + app) | `values-v31/styles.xml`, `splash.xml`, `drawable/splash_icon.xml`, `drawable/brand_logo.xml` | ✅ Fixed | Logo vectorised from the launcher art (no baked-in blue plate); Android 12+ splash and app splash share one centred 160dp medallion |
| Premium markers | `SettingsRowView` (`dsPremiumBadge`), `PremiumBadge.withDiamond()` | ✅ New | Gold "◆ PREMIUM" pill + gold icon on Theme / Advanced tools, lock on Theme, diamond on "Merge tree" menu item and Merge button; hidden once Premium |
| Remaining screens (merge, share, compare, sources/notes lists) | — | ✅ Theme-level | Inherit components via `Theme.FamilyTree` |

---

### 5.1 Tree surfaces — design notes

* **Tokens** live in `:core:designsystem` → `values/ds_colors_tree.xml` (+ night), `values/ds_styles_tree.xml`.
  The canvas stays deep in light *and* dark mode, so the existing light-on-dark system-bar handling in
  `DiagramFragment` is unchanged; only colour values moved to tokens (`ds_canvas_*`, `ds_tree_*`).
* **One person-card language** everywhere: 16dp corners, gender = outline colour (`male_bg` / `female_bg` / `nutral`,
  1.5dp), silhouette avatar behind the photo (shows automatically when `FileUtils.showMainImageForPerson` hides the photo).
* **No behaviour changes**: every view id and every view type that Java casts is preserved. Java touched only for
  visuals: `DiagramFragment` glow colour (`R.color.diagram_glow`) and `DiagramSettingsController`
  (value pills + drag bubble positioned in its parent's coordinates now that sliders sit inside cards).
* New English strings are in `values/strings_tree_ui.xml`; other locales fall back until translated.
* **List gender cue** is a ring around the avatar (`person_ring_*`); the diagram and merge cards keep the outline
  (`male_bg` / `female_bg`). Java only swaps which drawable is set (PersonsFragment, AppUtils).
* **Fixes found while verifying**: profile name drawn twice (multi-line collapsing title removed); collapsed profile
  header now uses the content scrim (trigger 220dp, tabs drawn above it); Settings dark-theme switch reverted itself
  after recreation because every `SettingsRowView` switch shares an id — the switch no longer saves/restores state.
* **Not changed (pre-existing)**: the GEDCOM picker filters on `application/*`, so files with a `.ged` extension can
  appear greyed out in the system picker on some devices.

## 6. Execution steps

### Phase 0 — Foundation (this change)
1. **Create `:core:designsystem`** — tokens, theme, component styles, icons, `SettingsRowView`.
2. **Create `:core:billing`** — `BillingManager` with monthly + lifetime, restore, reconcile.
3. **Wire app theme** — `AppTheme`, the 20 colour themes and `DarkTheme` now inherit `Theme.FamilyTree`.
4. **Toolbar sweep** — every `MaterialToolbar` uses `Widget.FamilyTree.Toolbar` (one sed over all layouts).
5. **Feature packages** — move Settings/About to `feature.settings`; replace `PurchaseController` + `ProductLayout` with `feature.billing.PaywallController`; add `PremiumGate` and use it at all 5 gate sites (settings theme, settings expert mode, trees premium pill, merge result, about).
6. **Persist plan** — `Settings.premiumPlan`, `Settings.premiumToken`, `Settings.premiumSince` added; `EntitlementSync` runs on home screen start.
7. **Rebrand screens** — splash, trees home, settings, about, paywall, theme picker.
8. **Strings** — new English strings added to `values/strings.xml` (other locales fall back).
9. **Build** — `./gradlew :app:assembleDebug` green.

### Phase 1 — Play Console & QA
10. Create the monthly subscription in Play Console (section 4.1).
11. Internal-testing release; test: buy monthly, buy lifetime, cancel monthly → reopen app after expiry → premium revoked, restore on second device.
12. Verify ads are hidden for both plans; verify themes/expert/merge unlock.

### Phase 2 — Branding assets
13. New adaptive launcher icon (foreground vector on `#0B57D0`), Play feature graphic, 8 store screenshots in the new UI.
14. Update `README.md` and store listing copy with the new tagline.

### Phase 3 — Per-screen polish (optional, can ship after)
15. Person profile: collapsing hero with avatar, tonal fact cards.
16. Diagram: bottom app bar with zoom/center/share actions; card styling.
17. Lists (persons/families/media/sources): search chip row, empty states using `ds_empty_state`.
18. New tree / Share / Merge: stepper headers; replace `RelativeLayout` rows with `SettingsRowView` or cards.

---

## 7. Gate sites (where Premium is checked)

| Site | File | Behaviour |
|------|------|-----------|
| Premium pill | `TreesController.setToolbar()` | Opens paywall |
| Colour themes | `feature/settings/SettingsController` | Gated → paywall (reason: theme) |
| Advanced tools | `feature/settings/SettingsController` | Gated → paywall (reason: advanced tools) |
| Merge trees | `merge/ResultFragment.kt` | Gated → paywall (reason: merge) |
| Ads (native/interstitial) | `feature/ads/AdsManager`, `NativeAdCard` | Never loaded for Premium (docs/specs/ads.md) |
| Premium themes rows | `theme/ThemeAdapter` | Lock badge; tap → paywall |
| Automatic local backup | `feature/backup/BackupManager.isAutomatic()`, `BackupController` | Switch with ◆; "Back up now" and restore stay free |
| Diagram PNG export | `DiagramFragment` (export menu) | Item with ◆ → paywall |
| Diagram PDF export | `DiagramFragment` (export menu) | Free for users from before 1.6 (`Settings.legacyUser`); ◆ → paywall for new installs |
| Bulk "Copy to app storage" | `feature/media/MediaToolsUi.copyAllToAppStorage` | Gallery menu item with ◆ → paywall |
| Anniversary / remembrance alerts | `feature/reminders/ReminderScheduler.dueBetween` | Listed for everyone, notified with Premium |
| Early alerts, more than 2 reminders | `feature/reminders/ReminderEditor` | Chips with ◆; new reminder beyond the free 2 → paywall |
| Family places (all places) | `feature/places/PlacesController` | Free: first 3 places + invitation card |

Only features added in version 1.6 are gated for existing users: nothing free before becomes Premium for them
(birthday notifications, PDF export, manual ZIP backup, single-media tools and "Show on map / Directions" stay free).
For **new installs** only, PDF export is Premium too (docs/specs/premium-features.md).

---

## 8. Definition of done for the rebrand

* `assembleDebug` and `assembleRelease` green.
* No `colorSecondaryContainer` toolbar backgrounds left in layouts.
* Paywall shows two live prices on a licence-tester device.
* Dark and light mode both verified on Settings, Home, Paywall, About.
* Lint: no new errors in `:core:*`.

---

## 9. Verification log (2026-10-15)

| Check | Result |
|-------|--------|
| `./gradlew :app:assembleDebug` | ✅ green |
| `./gradlew :app:assembleRelease` (R8 minify + shrinkResources) | ✅ green |
| Installed on emulator (Pixel, API 36), launched Splash → Trees → Settings → Paywall → About → open tree | ✅ no crashes in logcat |
| Light + dark mode on Splash, Home, Settings, Paywall, About | ✅ screenshots in `docs/screenshots/` |
| Toolbars using `colorSecondaryContainer` left in layouts | 0 |
| Old `PurchaseController` / `ProductLayout` references | 0 |

Notes:
* On an emulator without Google Play the paywall shows "Please update Play Store app" and prices stay empty — expected; prices render only with Play services + the products created in Play Console (section 4.1).
* **Dynamic (wallpaper) colours were disabled** in `Global` because they overrode the brand palette (the Premium gold turned pink on the test device). Re-enable only if a wallpaper-tinted look is wanted.
* `AppTheme` no longer overrides the Material colour roles; it inherits everything from `Theme.FamilyTree`. The 20 Premium colour themes still override colours and now inherit shapes/type/components.

## 10. Verification log (2026-10-17) — tree surfaces

| Check | Result |
|-------|--------|
| `./gradlew :app:assembleDebug` / `:app:assembleRelease` | ✅ green |
| Emulator (Pixel 7, API 35), light + dark: splash → trees → import GEDCOM → diagram → drawer → diagram settings (slider bubble) → persons (search, sort chips, empty state) → families → profile (facts, relatives, media, collapse) → new person editor → settings (premium markers, dark toggle) → tree menu (merge marker) | ✅ no crashes in logcat |

## 11. Features adapted from Family Gem upstream (2026-10-17)

Source: github.com/michelesalvador/FamilyGem (v1.3.0.3, GPL-3.0 like this app). Adapted, not copied: same behaviour,
this app's architecture and premium UI. Existing flows and data are unchanged; defaults reproduce the old behaviour.

| Feature | Where | Notes |
|---------|-------|-------|
| GEDCOM picker accepts `.ged` everywhere | `NewTreeController` | MIME list = old `application/*` + GEDCOM/plain-text types (superset, nothing becomes unselectable) |
| Automatic local backup | `feature/backup/BackupManager.kt`, `BackupController.kt`, `layout/backup*.xml`, Settings row, trees-screen card | ZIP per tree (`<id>_<yyyyMMdd>_<HHmmss>.zip`, same format as manual backup) a minute after each save, flushed when the app goes to background; 3 newest kept; restore = existing `unZip` (adds a new tree). In-process scheduler, not WorkManager: `Notifier` calls `WorkManager.cancelAllWork()` on every save |
| Export diagram as PNG | `DiagramFragment` (options menu) | Print styling as the PDF, white background, size capped to available memory, compressed off the main thread |
| Tree settings (life span, reference date) | `TreeSettingsController.kt`, tree menu + diagram options | Defaults 120 years / device date = previous hard-coded behaviour. Trees left at defaults store no `settings` object. Fixed date ⇒ no birthday notifications |
| Birthday notification time | Settings row + `Notifier` (`UPDATE`) | Default 12:00 as before; changing it re-times scheduled alarms |
| Grouped Relatives tab | `ProfileRelativesFragment` | Origin / Adoptive / Foster (GEDCOM PEDI), Paternal / Maternal half-family, Own family headers; card order unchanged |
| Translations | `values-*/strings_familygem.xml` | Family Gem's Weblate translations of the reused labels (24 locales) |

**Second round (same day) — library upgrades and remaining items**

| Item | Where | Customer-impact analysis / result |
|------|-------|-----------------------------------|
| `gedcomgraph` 3.2 → 3.14 | `app/libs`, `DiagramFragment`, `MoveLayout`, `layout/diagram.xml` | Rendering only (no stored data). Breaking API ported as upstream: `Graph()`+`setGedcom`, `setLayoutDirection` (library mirrors RTL ⇒ canvas pinned LTR, app-side mirroring removed), `Bond.marriageYear` + `FamilyNode.marriageYearProvider` (same year text as before), absolute centers. Duplicated persons are full cards (asterisk removed); every card of the selected person is highlighted, glow stays on the fulcrum. All 15 GEDCOM calls of 3.14 exist in our GEDCOM jar. Verified LTR, RTL (Arabic), cousin marriage, 3 spouses in one family, PDF/PNG, release build |
| Diagram "Numbers" / "Duplicates lines" | Diagram settings (Display section), `Settings.Diagram` | Old settings get `numbers:true` by JSON migration in `Global` (count cards were always shown), `duplicates:false` ⇒ existing diagrams look as before |
| `gedcom` 1.14.0-SNAPSHOT → 1.15.0 | `app/libs` | Model diff is additive only (`EventFact.age/husb/wife`, `Spouse`). Real trees round-trip **byte-identical** through 1.14 and 1.15; GEDCOM imports without AGE identical; with AGE, 1.15 stores real fields instead of generic extensions (existing trees keep their extensions, shown as before) |
| Age at event | `AgeConverter`, `EventController`, `ProfileFactsFragment`, `DetailController.writeEvent` | Person events: editable "Age" (same generic piece as Date/Place). Family events: husband/wife age read-only. Ages in words in summaries ("Less than 25 years") |
| Date validity hint | `GedcomDateConverter.isValidGedcomDate`, `DateEditorLayout` | Error under the field for malformed structured dates (e.g. "BET 1900"); free text is a date phrase (saved in parentheses) and not flagged; never blocks saving. INT kind deliberately not added (would change how existing INT dates display) |
| Merge "Search for duplicates" / "Skip", "Yes / No to all remaining" | `merge/*` | Skip = `findMatches(false)`; "to all" loops the existing `nextMatch` (same parent-matching logic). Merge remains Premium |
| "One error found." | `TreesController` | Singular message instead of "1 errors found." |
| Translations | `values-*/strings_familygem.xml` | 31 reused upstream labels × 24 locales |

**Media file tools** — ported later (see §13) on top of this fork's own path resolution, with link verification. Old jars (`gedcomgraph-3.2.jar`, `gedcom-1.14.0-SNAPSHOT.jar`)
stay in `app/libs`, unreferenced, for a one-line rollback.

**Upgrade safety** — new `settings.json` fields are optional; `Global.start` + `BackupManager.init` migrate on first run
(backup on, no folder ⇒ nothing happens until the user picks one). `proguard-rules.pro` keeps `Settings$TreeSettings`
member names. Verified: in-place upgrade on the emulator kept both tree files byte-identical; release build stores the
new keys with their real names.

## 12. In-app updates (Google Play)

`feature/update/InAppUpdates.kt`, used by `TreesController`, `PrincipalController` and Settings ("Check for updates").

* **Forced update**: publish the release with in-app update priority **4 or 5** (Play Developer API field
  `inAppUpdatePriority`; e.g. Gradle Play Publisher `play { updatePriority.set(5) }` or fastlane
  `in_app_update_priority: 5`). The Play Console UI can't set it. Users see Play's full-screen update; backing out shows
  a blocking "Update required" dialog (Update / Close app).
* **Optional update** (priority 0–3): Play's flexible update is offered at most once a day; when downloaded, a snackbar
  offers Restart.
* Works only for installs from Google Play. To test, upload two builds to **Internal app sharing** (or an internal track),
  install the lower version from Play, then open the app. Debug/sideloaded builds show "Couldn't check for updates".

---

## 13. Media file tools (2026-10-17)

Adapted from Family Gem, built on our resolver (`FileUtils.mediaPath` / `mediaUri`): a link is rewritten only after
checking that the resolver finds the same file from the new link.

| Tool | Where | Notes |
|------|-------|-------|
| File info sheet | Media screen menu "Info" | Location badge (App storage / Device / Media folder / Web / Not found), folder, size, type, dimensions, modified, used by; contextual Rename / Copy |
| Copy to app storage | Media screen (free), gallery menu for all files (Premium) | Copies to a temp file, checks the size, then renames; web pages are refused; progress + cancel; links set to the absolute path and the tree folder added to media folders (as when adding media) |
| Rename file | Media screen, info sheet | Only files in the tree storage; name validated (invalid / existing); every media linked to the file follows it |
| Shrink file path | Media screen and gallery menu (expert mode) | Only files directly in tree storage, only if the name alone finds the same file |
| Make shared / simple media | Media screen menu, gallery and profile media long-press | `MediaConversions` (pure GEDCOM): identical simple media (same JSON) merge into one record; simple copies made with the GEDCOM JSON parser, extensions kept. JVM test: round trip leaves every record's media unchanged |
| Reuse identical file | Adding a media | Same name and same bytes in tree storage → "Reuse existing" (no crop) / "Make a copy" (as before) |
| Crop safety | `FileUtils.cropImage` | A file in tree storage linked by other media is no longer overwritten: the crop goes to a new file |

## 14. Events & reminders, Family places (2026-10-17)

* **Events & reminders** (drawer): birthdays, wedding anniversaries (couple still together, nobody dead), remembrance
  days and the user's reminders (baby shower, graduation, reunion…), grouped Today / This week / Within a month / Later,
  filters with counts, bell on notified events, notification permission banner (Android 13+ never asked before).
  Profile menu "Add reminder" presets the person.
* Storage: `Settings.Tree.reminders` (also in backup ZIPs), `Settings.Tree.occasions` (anniversaries/remembrance cached
  at save, like birthdays), `Settings.anniversaryAlertsOff`, `remembranceAlerts`, `remindersCheckedAt`. Keep rules in
  `proguard-rules.pro`; release JSON keys verified.
* `ReminderScheduler`: one alarm at the next due reminder of all trees (no 500-alarm limit); on ring, notifies
  everything due since the last check (max 2 days late), then sets the next. Exact alarm when allowed, otherwise a
  10-minute window. Rescheduled at app start, boot, save, tree delete, notification time, tree settings, Premium change.
  Birthdays stay with `Notifier`, unchanged.
* **Places & maps**: "Show on map" / "Directions" in the toolbar of event and address screens and on profile fact cards
  (plain map links, no API key). **Family places** (drawer): all places of the tree from event places and addresses,
  search, country chips, events of each place. An in-app map with all places is not feasible without a paid Maps key
  or geocoding through the shared GeoNames quota.

### Verification (emulator `ft_test`, debug and release)

* Upgrade over the previous build: tree JSON byte-identical, settings gained only the new keys.
* GEDCOM import computes occasions; reminder saved, alarm at the right time, notification posted when due.
* Free vs Premium gating as in §7; backup with `.part` rename, ZIP with settings first and reminders; restore keeps them.
* Media: add / reuse / make a copy, rename (two media follow), crop of a shared file writes a new file,
  shared → simple → shared round trip in a profile.
* Code review fixes: optional update could become forced; stacked "Update required" dialogs; crash after rotation during
  copy; backup list overflow crash; ZIP stream leaks; pickers losing results after rotation; backup scheduling races;
  heavy tree parsing on the main thread; translatable error messages; Gson keep rules with constructors.


## 15. Libraries and translations (2026-10-17)

**Libraries** (checked against Google Maven): Play Billing 9.1.0 and Mobile Ads 25.5.0 are the latest; app-update 2.1.0,
Material 1.14.0 and the AndroidX set are current. Firebase BoM bumped 35.0.1 → 35.0.2. `BillingManager` now uses
`enableAutoServiceReconnection()` and no longer creates a second client while one is connecting.
**Open:** the app has no consent SDK (UMP). For ads in the EEA/UK/Switzerland AdMob requires a Google-certified CMP:
create the GDPR message in AdMob → Privacy & messaging, then add `com.google.android.ump:user-messaging-platform`
(4.0.0) and gate ad requests on `canRequestAds()`. Not added yet: without the AdMob message configured it would stop
ads in those regions.

**Translations:** all 30 locales complete (685 keys each, 0 missing). 1,016 strings reused from Family Gem where the
English text is identical; the rest translated per locale with the existing terminology, validated by script
(placeholders, line breaks, plural categories per CLDR) and by lint (no MissingTranslation / MissingQuantity /
StringFormat issues). Counts are plurals (`plurals_events.xml`); "tomorrow" is a separate string because in some
languages the "one" category also covers 21, 31… Technical strings marked `translatable="false"`.
`locales_config.xml`: Javanese and Tamil added, completion percentages removed (all 100%). Fixed along the way:
Tamil "Premium" was translated as "insurance"; Norwegian "Geburtsdager"/"Etterfølgere"; German typos; Kurdish
"repositories" equal to "sources". Dates in the events list use the platform ICU formatter (Joda rejected Russian
stand-alone patterns: crash), digits and times follow the locale, mixed-script parts are bidi-isolated.
Low-resource locales (ti, ckb, jv, kn, mr) deserve a native speaker's review.

## 16. Large trees (2026-10-17)

Tested with a generated tree of 4,000 people (665 families, 1,289 living with full birth dates), release build on a
Galaxy S24 Ultra (Android 16) and on the emulator:

| Action | Phone (release) |
|--------|-----------------|
| Import GEDCOM | ~1.5–3 s (one time) |
| Open tree → diagram | 0.35 s |
| Persons list, sort by relatives, search ("Showing 184 of 4000") | smooth, no skipped-frame warnings |
| Events & reminders (4,005 events) | computed in 0.2 s off the main thread; list drawn in < 35 ms |
| Family places | 0.6 s |
| Edit + save | under 250 ms of UI pause (was ~2.8 s on the emulator before the fix) |

**Fixed:** birthdays used one alarm per living person. Android allows 500 alarms per app ("Maximum limit of
concurrent alarms 500 reached"): in big trees later birthdays were never notified, the reminders alarm could fail,
and every save cancelled and re-created up to ~1,800 alarms. Birthdays are now notified by `ReminderScheduler`'s single
alarm (channel "birthdays" unchanged); old alarms are cancelled once (`Settings.birthdayAlarmsMigrated`); birthday
entries that pre-1.6 release builds saved with obfuscated field names are rebuilt in the background
(`ReminderScheduler.repairBirthdays`); `Settings$Birthday` is now kept by R8.
The whole tree is still kept in memory and written as one JSON at each save: fine at 4,000 people; very large trees
(tens of thousands) would benefit from saving off the main thread.

## 17. Ads, Premium invitation and rich paywall (2026-10-17)

Specs: [docs/specs/](specs/README.md) — [ads](specs/ads.md), [Premium invitation](specs/upsell-prompt.md),
[paywall](specs/paywall.md), [Premium features](specs/premium-features.md).

* **Ads** (`feature/ads`): branded native cards that never move content (preloaded pool; late loading only at the end
  of a page), new placements (persons list row, profile end, events list), interstitials only at natural breaks
  (person saved, tree closed, export done) with grace period, cooldown and caps; Google sample ad units in debug
  builds; SDK initialised off the main thread and not at all for Premium. Old `nativeads/TemplateView` removed.
* **Premium invitation** after enough use (sessions, engagement points, back-off 3/7/14/30/45 days), with the
  user's own numbers and live prices.
* **Paywall** rebuilt: plans first, Free vs Premium table, grouped feature cards with "How it works", trust, FAQ,
  sticky purchase bar, trial and "pays for itself" pills, highlighted feature when opened from a gate.
* **Rewarded videos** (unit `<your-rewarded-unit-id>`): free users can export the diagram as HD PNG,
  or PDF for new installs, once after a video they choose to watch (`RewardedUnlock`).
* **PDF export** Premium for new installs only (`Settings.legacyUser`).
* Dutch `repositories` fixed ("Begraafplaatsen" → "Opslagplaatsen").
* Unit tests added (`app/src/test`): `MonetizationPolicyTest`, `PlanMathTest`.


## 18. Photo and video permissions removed (2026-10-17)

Google Play allows `READ_MEDIA_IMAGES` / `READ_MEDIA_VIDEO` only to apps whose core purpose needs broad photo access,
so both are gone from the manifest (`READ_EXTERNAL_STORAGE` up to API 32 and `READ_MEDIA_AUDIO`, never requested
anymore, stay). Android 12 and older behave exactly as before.

* **Android 13+, adding media:** no permission prompt; a chosen file the app can't read by path is copied into the
  tree's app storage (`FileUtils.proposeCropping`), as already happened for most pickers.
* **Media folders:** the folder picker grants the access, the folder is kept as a tree URI (no path conversion).
* **Existing users on Android 13+:** photos linked by path outside app storage stop showing after the update. When a
  tree with such media folders opens, `MediaAccess.maybeAskReconnect` invites once (until "Not now") to reconnect;
  Media folders then opens the system picker on each folder in turn (rows show "Tap to reconnect"). Files are found
  through the granted folder by their original path (`MediaAccess.resolve`, used by `FileUtils.mediaUri`), so tree
  files are never rewritten. Folders Android won't grant (storage root, Download, Android/data|obb) show why.

**Verified on emulator `ft_test` (API 35):** old build with permission showed a DCIM photo → new build installed over
it: permissions dropped, photo missing, invitation shown → Reconnect opened the picker on DCIM/Camera → Allow → photo
back in diagram and profile; Download marked as not grantable; no invitation afterwards; "Not now" remembered per
tree; new media copied into app storage and cropped; adding a folder opens the picker with no permission dialog.
Release bundle permissions checked with aapt2. Android 12 and older not run (no image installed): code paths there are
unchanged behind `MediaAccess.needsGrantedFolders()`.
