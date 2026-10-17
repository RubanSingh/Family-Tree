# Paywall (Premium page)

_Last updated: 2026-10-17_

## Goal

One page where people understand exactly what Premium gives them, compared with the free app, and can buy it in one
tap with confidence.

## Behaviour

Opened by `PremiumGate.open(context, reason)` from every Premium gate, the Premium pill, Settings, About, the
"Remove ads" link and the invitation. `reason` is the title of the feature that was tapped (0 = generic).

### Sections (top to bottom)

1. **Hero** — gold gradient, diamond tile, "Family Tree Premium", "Everything you need to protect, celebrate and share
   your family story", the pill "Unlocks: <feature>" when opened from a gate, and three glass chips (No ads,
   Auto backup, Unlimited reminders).
2. **Choose your plan** — Monthly and Lifetime cards with live Play prices.
   * Monthly: price per month, "N-day free trial" pill if the base plan has a free phase (`PlanMath.trial`).
   * Lifetime (selected by default, "Best value"): one-time price, "Pays for itself in N months" pill
     (`PlanMath.paysOffMonths`, shown for 2–36 months and same currency).
3. **Free vs Premium** — a table built from `PremiumFeature`: feature, what free users get, what Premium adds (✓).
4. **Everything in Premium** — feature cards grouped as *Keep it safe*, *Never miss a moment*, *Create & share*,
   *Make it yours*. Each card: gold icon, title, summary, "Free: …" and "Premium: …" pills ("Only in Premium" when
   free users get nothing), and a "How it works" section that opens on tap. The feature the user tried to use
   (`PremiumFeature.forReason`) is highlighted with a gold outline and already open.
5. **Buy with confidence** — Secure payment by Google Play; cancel the monthly plan anytime; restore on all devices;
   your family data stays on your device.
6. **Questions & answers** — Monthly vs Lifetime; what happens if I stop Premium; new phone; is my data sent
   anywhere; how to cancel.
7. Restore purchases, Manage subscription (Monthly selected), legal text for the selected plan.
8. **Sticky purchase bar** (always visible): price summary of the selected plan ("₹X once · yours forever",
   "₹X per month · cancel anytime", or "7-day free trial, then ₹X per month") and the main button.

Edge-to-edge is handled by the screen itself (`InsetsUtil.SelfInsetting`): toolbar under the status bar, purchase
bar above the navigation bar, page padded to end above the bar.

### Billing (unchanged)

`BillingManager` (`:core:billing`): connect → restore owned purchases (closes the page with "Premium restored") →
otherwise load offers. Purchase → acknowledge → `SettingsEntitlementStore.grant` (also drops loaded ads).
Without Google Play billing (emulators without Play, old Play Store) prices stay empty and a toast says to update
the Play Store.

## Code

| File | Role |
|------|------|
| `feature/billing/PaywallController.kt` | Screen |
| `feature/billing/PremiumFeature.kt` | Catalogue: groups, icons, texts, free/Premium values, details, reasons |
| `feature/billing/PlanMath.kt` | Trial period and "pays for itself" maths (unit tested) |
| `layout/paywall_activity.xml` | Page skeleton (ids kept from the previous paywall) |
| `layout/paywall_compare_row.xml`, `paywall_feature_group.xml`, `paywall_feature_card.xml`, `paywall_detail_line.xml`, `paywall_faq_item.xml` | Built rows |
| `values/strings_monetization.xml` (+ 30 locales) | Copy |

## Analytics

Event `paywall`: `action` = `open` (with `reason` = resource name of the feature, or `generic`),
`purchase_monthly` / `purchase_lifetime` (button), `purchased_monthly` / `purchased_lifetime` (done).
The existing `ActionPremium` / `PurchaseCompleted` screen events are still logged.

## Adding a Premium feature

1. Gate it with `PremiumGate` and open the paywall with the feature's title as `reason`.
2. Add an entry to `PremiumFeature` (group, icon, title, summary, free, premium, details, reasons).
3. Add the strings to `strings_monetization.xml` and translate them (docs/REBRAND_PLAN.md §15).
4. Update [premium-features.md](premium-features.md).

## Testing

`PlanMathTest`; on a licence-tester device from Internal testing: both prices, trial pill (if configured), savings
pill, buy each plan, restore on a second device.
