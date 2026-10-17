package dna.familytree.feature.billing

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import dna.familytree.R
import dna.familytree.designsystem.R as DsR

/**
 * Everything Premium includes, in display order: the paywall's comparison table and feature cards, and the Premium
 * invitation, are built from this list. Keep it in step with docs/specs/premium-features.md.
 *
 * @param free what free users get ([R.string.compare_none] = nothing)
 * @param premium what Premium adds, short, for the comparison table
 * @param details "how it works" lines shown when the feature card is expanded
 * @param reasons paywall reasons (titles passed to [PremiumGate.open]) that point to this feature
 */
enum class PremiumFeature(
    val group: Group,
    @DrawableRes val icon: Int,
    @StringRes val title: Int,
    @StringRes val summary: Int,
    @StringRes val free: Int,
    @StringRes val premium: Int,
    val details: IntArray,
    val reasons: IntArray = intArrayOf()
) {
    AUTO_BACKUP(
        Group.SAFE, DsR.drawable.ds_ic_backup, R.string.pf_backup_title, R.string.pf_backup_summary,
        R.string.pf_backup_free, R.string.pf_backup_premium,
        intArrayOf(R.string.pf_backup_detail1, R.string.pf_backup_detail2, R.string.pf_backup_detail3),
        intArrayOf(R.string.backup_auto_title)
    ),
    MEDIA_TOOLS(
        Group.SAFE, DsR.drawable.ds_ic_photo, R.string.pf_media_title, R.string.pf_media_summary,
        R.string.pf_media_free, R.string.pf_media_premium,
        intArrayOf(R.string.pf_media_detail1, R.string.pf_media_detail2),
        intArrayOf(R.string.copy_app_storage)
    ),
    REMINDERS(
        Group.MOMENTS, DsR.drawable.ds_ic_notifications, R.string.pf_reminders_title, R.string.pf_reminders_summary,
        R.string.pf_reminders_free, R.string.pf_reminders_premium,
        intArrayOf(R.string.pf_reminders_detail1, R.string.pf_reminders_detail2, R.string.pf_reminders_detail3),
        intArrayOf(R.string.events_title, R.string.event_anniversary, R.string.event_remembrance,
            R.string.reminder_unlimited, R.string.reminder_early_alerts)
    ),
    PLACES(
        Group.MOMENTS, DsR.drawable.ds_ic_place, R.string.pf_places_title, R.string.pf_places_summary,
        R.string.pf_places_free, R.string.pf_places_premium,
        intArrayOf(R.string.pf_places_detail1, R.string.pf_places_detail2),
        intArrayOf(R.string.places_title)
    ),
    EXPORTS(
        Group.SHARE, DsR.drawable.ds_ic_pdf, R.string.pf_exports_title, R.string.pf_exports_summary,
        R.string.pf_exports_free_video, R.string.pf_exports_premium,
        intArrayOf(R.string.pf_exports_detail1, R.string.pf_exports_detail2, R.string.pf_exports_detail3,
            R.string.pf_exports_detail4),
        intArrayOf(R.string.export_png, R.string.export_pdf)
    ),
    MERGE(
        Group.SHARE, DsR.drawable.ds_ic_merge, R.string.pf_merge_title, R.string.pf_merge_summary,
        R.string.compare_none, R.string.compare_included,
        intArrayOf(R.string.pf_merge_detail1, R.string.pf_merge_detail2),
        intArrayOf(R.string.merge_tree)
    ),
    ADVANCED(
        Group.SHARE, DsR.drawable.ds_ic_bolt, R.string.pf_advanced_title, R.string.pf_advanced_summary,
        R.string.compare_none, R.string.compare_included,
        intArrayOf(R.string.pf_advanced_detail1, R.string.pf_advanced_detail2, R.string.pf_advanced_detail3),
        intArrayOf(R.string.show_advanced_functions)
    ),
    NO_ADS(
        Group.YOURS, DsR.drawable.ds_ic_block, R.string.pf_no_ads_title, R.string.pf_no_ads_summary,
        R.string.pf_no_ads_free, R.string.pf_no_ads_premium,
        intArrayOf(R.string.pf_no_ads_detail1, R.string.pf_no_ads_detail2),
        intArrayOf(R.string.benefit_no_ads)
    ),
    THEMES(
        Group.YOURS, DsR.drawable.ds_ic_palette, R.string.pf_themes_title, R.string.pf_themes_summary,
        R.string.pf_themes_free, R.string.pf_themes_premium,
        intArrayOf(R.string.pf_themes_detail1, R.string.pf_themes_detail2),
        intArrayOf(R.string.theme)
    );

    enum class Group(@StringRes val title: Int, @StringRes val subtitle: Int) {
        SAFE(R.string.pf_group_safe, R.string.pf_group_safe_desc),
        MOMENTS(R.string.pf_group_moments, R.string.pf_group_moments_desc),
        SHARE(R.string.pf_group_share, R.string.pf_group_share_desc),
        YOURS(R.string.pf_group_yours, R.string.pf_group_yours_desc)
    }

    companion object {
        /** The feature a paywall [reason] is about, or null for a generic opening. */
        @JvmStatic
        fun forReason(@StringRes reason: Int): PremiumFeature? =
            if (reason == 0) null else entries.firstOrNull { reason in it.reasons }

        /** The highlights of the Premium invitation, most wanted first. */
        val highlights = listOf(NO_ADS, AUTO_BACKUP, REMINDERS, EXPORTS)
    }
}
