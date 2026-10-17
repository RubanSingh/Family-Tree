package dna.familytree.feature.reminders

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import dna.familytree.R
import dna.familytree.designsystem.R as DsR

/** The kinds of reminder the user can create, with their label and icon. */
enum class ReminderKinds(@StringRes val label: Int, @DrawableRes val icon: Int) {
    BIRTHDAY(R.string.reminder_kind_birthday, DsR.drawable.ds_ic_cake),
    ANNIVERSARY(R.string.reminder_kind_anniversary, DsR.drawable.ds_ic_favorite),
    BABY_SHOWER(R.string.reminder_kind_baby_shower, DsR.drawable.ds_ic_stroller),
    WEDDING(R.string.reminder_kind_wedding, DsR.drawable.ds_ic_family),
    GRADUATION(R.string.reminder_kind_graduation, DsR.drawable.ds_ic_school),
    REUNION(R.string.reminder_kind_reunion, DsR.drawable.ds_ic_groups),
    MEMORIAL(R.string.reminder_kind_memorial, DsR.drawable.ds_ic_ribbon),
    OTHER(R.string.reminder_kind_other, DsR.drawable.ds_ic_event);

    companion object {
        /** The kind saved in a reminder; unknown values are [OTHER]. */
        @JvmStatic
        fun of(name: String?): ReminderKinds = entries.firstOrNull { it.name == name } ?: OTHER

        @JvmStatic
        fun label(context: Context, name: String?): String = context.getString(of(name).label)
    }
}
