package dna.familytree.feature.reminders

import android.text.format.DateFormat
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import dna.familytree.AppUtils
import dna.familytree.Global
import dna.familytree.Notifier
import dna.familytree.R
import dna.familytree.Settings
import dna.familytree.designsystem.PremiumBadge
import dna.familytree.feature.ads.UsageStats
import dna.familytree.feature.billing.PremiumGate
import dna.familytree.util.Snackbars
import org.joda.time.DateTimeZone
import org.joda.time.LocalDate
import org.joda.time.LocalTime
import java.util.Locale

/**
 * Bottom sheet to create or edit a reminder of the open tree.
 * Without Premium there can be [FREE_REMINDERS] reminders, notified on the day only.
 */
object ReminderEditor {

    const val FREE_REMINDERS = 2

    /** A person of the tree in the people dropdown. */
    private class PersonItem(val id: String, val label: String) {
        override fun toString() = label
    }

    /**
     * @param reminder The reminder to edit, or null for a new one
     * @param personId The person of a new reminder, or null
     */
    @JvmStatic
    fun show(activity: AppCompatActivity, reminder: Settings.Reminder?, personId: String?, onChanged: Runnable?) {
        val gedcom = Global.gc ?: return
        val tree = Global.settings.currentTree ?: return
        val isNew = reminder == null
        if (isNew && !PremiumGate.isPremium() && countReminders() >= FREE_REMINDERS) {
            PremiumGate.open(activity, R.string.reminder_unlimited)
            return
        }
        val view = LayoutInflater.from(activity).inflate(R.layout.reminder_editor, null)
        val sheet = BottomSheetDialog(activity)
        sheet.setContentView(view)
        sheet.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        sheet.behavior.skipCollapsed = true

        // State
        var kind = ReminderKinds.of(reminder?.kind ?: ReminderKinds.BABY_SHOWER.name)
        var date = FamilyEvents.parseDate(reminder?.date) ?: LocalDate.now()
        var time: LocalTime? = reminder?.time?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
        var selectedPerson: String? = reminder?.personId ?: personId

        view.findViewById<TextView>(R.id.reminder_heading).setText(if (isNew) R.string.reminder_new else R.string.reminder_edit)
        val titleLayout = view.findViewById<TextInputLayout>(R.id.reminder_title_layout)
        val titleEdit = view.findViewById<TextInputEditText>(R.id.reminder_title)
        titleEdit.setText(reminder?.title ?: activity.getString(kind.label))
        titleEdit.doAfterTextChanged { titleLayout.error = null }

        // Kinds
        val kinds = view.findViewById<ChipGroup>(R.id.reminder_kinds)
        ReminderKinds.entries.forEach { item ->
            val chip = LayoutInflater.from(activity).inflate(R.layout.reminder_kind_chip, kinds, false) as Chip
            chip.id = android.view.View.generateViewId()
            chip.text = activity.getString(item.label)
            chip.setChipIconResource(item.icon)
            chip.tag = item
            kinds.addView(chip)
            if (item == kind) chip.isChecked = true
        }
        kinds.setOnCheckedStateChangeListener { group, checked ->
            val chosen = checked.firstOrNull()?.let { group.findViewById<Chip>(it).tag as ReminderKinds } ?: return@setOnCheckedStateChangeListener
            // The title follows the kind until the user writes their own
            val current = titleEdit.text?.toString()?.trim().orEmpty()
            if (current.isEmpty() || current == activity.getString(kind.label)) titleEdit.setText(activity.getString(chosen.label))
            kind = chosen
        }

        // Person
        val people = gedcom.people.map { PersonItem(it.id, AppUtils.properName(it)) }.filter { it.label.isNotBlank() }.sortedBy { it.label.lowercase() }
        val personView = view.findViewById<MaterialAutoCompleteTextView>(R.id.reminder_person)
        personView.setAdapter(ArrayAdapter(activity, android.R.layout.simple_list_item_1, people))
        people.firstOrNull { it.id == selectedPerson }?.let { personView.setText(it.label, false) }
        personView.setOnItemClickListener { parent, _, position, _ -> selectedPerson = (parent.getItemAtPosition(position) as PersonItem).id }
        personView.doAfterTextChanged { text ->
            if (people.none { it.id == selectedPerson && it.label == text?.toString() }) selectedPerson = null
        }

        // Date and time
        val dateButton = view.findViewById<MaterialButton>(R.id.reminder_date)
        val timeButton = view.findViewById<MaterialButton>(R.id.reminder_time)
        fun showDate() {
            dateButton.text = java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM).format(date.toDate())
        }
        fun showTime() {
            val shown = time ?: Notifier.notifyTime().let { LocalTime(it[0], it[1]) }
            timeButton.text = DateFormat.getTimeFormat(activity).format(shown.toDateTimeToday().toDate())
        }
        showDate()
        showTime()
        dateButton.setOnClickListener {
            MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.reminder_date)
                .setSelection(date.toDateTimeAtStartOfDay(DateTimeZone.UTC).millis)
                .build().apply {
                    addOnPositiveButtonClickListener { millis ->
                        date = LocalDate(millis, DateTimeZone.UTC)
                        showDate()
                    }
                }.show(activity.supportFragmentManager, "reminderDate")
        }
        timeButton.setOnClickListener {
            val shown = time ?: Notifier.notifyTime().let { LocalTime(it[0], it[1]) }
            val picker = MaterialTimePicker.Builder()
                .setTimeFormat(if (DateFormat.is24HourFormat(activity)) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H)
                .setHour(shown.hourOfDay).setMinute(shown.minuteOfHour)
                .setTitleText(R.string.reminder_time)
                .build()
            picker.addOnPositiveButtonClickListener {
                time = LocalTime(picker.hour, picker.minute)
                showTime()
            }
            picker.show(activity.supportFragmentManager, "reminderTime")
        }

        // Repeat
        val repeat = view.findViewById<MaterialButtonToggleGroup>(R.id.reminder_repeat)
        repeat.check(if (reminder?.yearly ?: kind.yearlyByDefault()) R.id.reminder_yearly else R.id.reminder_once)

        // Early alerts (Premium)
        val before = view.findViewById<ChipGroup>(R.id.reminder_before)
        val beforeChips = mapOf(0 to R.id.reminder_before_0, 1 to R.id.reminder_before_1, 7 to R.id.reminder_before_7)
        if (!PremiumGate.isPremium()) listOf(R.id.reminder_before_1, R.id.reminder_before_7).forEach { id ->
            view.findViewById<Chip>(id).apply { text = PremiumBadge.withDiamond(activity, text) }
        }
        before.check(beforeChips[reminder?.daysBefore ?: 0] ?: R.id.reminder_before_0)
        before.setOnCheckedStateChangeListener { group, checked ->
            if (checked.firstOrNull() != R.id.reminder_before_0 && !PremiumGate.isPremium()) {
                group.check(R.id.reminder_before_0)
                PremiumGate.open(activity, R.string.reminder_early_alerts)
            }
        }

        val noteEdit = view.findViewById<TextInputEditText>(R.id.reminder_note)
        noteEdit.setText(reminder?.note)

        // Delete
        view.findViewById<MaterialButton>(R.id.reminder_delete).apply {
            visibility = if (isNew) android.view.View.GONE else android.view.View.VISIBLE
            setOnClickListener {
                MaterialAlertDialogBuilder(activity)
                    .setTitle(R.string.reminder_delete_confirm)
                    .setMessage(reminder?.title)
                    .setPositiveButton(R.string.delete) { _, _ ->
                        tree.reminders?.removeAll { it.id == reminder?.id }
                        if (tree.reminders?.isEmpty() == true) tree.reminders = null
                        saved(activity, sheet, R.string.reminder_deleted, onChanged)
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            }
        }

        // Save
        view.findViewById<MaterialButton>(R.id.reminder_save).setOnClickListener {
            val title = titleEdit.text?.toString()?.trim().orEmpty()
            if (title.isEmpty()) {
                titleLayout.error = activity.getString(R.string.reminder_title_required)
                return@setOnClickListener
            }
            val target = reminder ?: Settings.Reminder().apply { id = newId() }
            target.kind = kind.name
            target.title = title
            target.personId = selectedPerson
            target.date = date.toString()
            target.time = time?.let { String.format(Locale.US, "%02d:%02d", it.hourOfDay, it.minuteOfHour) }
            target.yearly = repeat.checkedButtonId == R.id.reminder_yearly
            target.daysBefore = beforeChips.entries.firstOrNull { it.value == before.checkedChipId }?.key ?: 0
            target.note = noteEdit.text?.toString()?.trim()?.ifEmpty { null }
            if (isNew) {
                if (tree.reminders == null) tree.reminders = ArrayList()
                tree.reminders.add(target)
                UsageStats.engage(UsageStats.POINTS_REMINDER_ADDED)
            }
            saved(activity, sheet, R.string.reminder_saved, onChanged)
        }
        sheet.show()
    }

    private fun ReminderKinds.yearlyByDefault() = this == ReminderKinds.BIRTHDAY || this == ReminderKinds.ANNIVERSARY || this == ReminderKinds.MEMORIAL

    private fun saved(activity: AppCompatActivity, sheet: BottomSheetDialog, message: Int, onChanged: Runnable?) {
        Global.settings.save()
        ReminderScheduler.reschedule(activity)
        sheet.dismiss()
        onChanged?.run()
        Snackbars.show(activity, activity.getString(message))
    }

    /** Reminders of all trees. */
    @JvmStatic
    fun countReminders(): Int = Global.settings.trees.orEmpty().sumOf { it.reminders?.size ?: 0 }

    /** Unique among the reminders of all trees. */
    private fun newId(): Long {
        val used = Global.settings.trees.orEmpty().flatMap { it.reminders.orEmpty() }.map { it.id }.toSet()
        var id = System.currentTimeMillis()
        while (id in used) id++
        return id
    }
}
