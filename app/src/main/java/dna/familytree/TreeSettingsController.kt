package dna.familytree

import android.os.Bundle
import android.widget.RadioGroup
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import dna.familytree.constant.Extra
import dna.familytree.feature.reminders.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.folg.gedcom.parser.JsonParser
import org.joda.time.DateTimeZone
import org.joda.time.LocalDate
import java.io.File
import java.text.DateFormat

/**
 * Settings of one tree (adapted from Family Gem): maximum life span of living people and the date ages are counted to.
 * Open with [Extra.TREE_ID]; defaults to the open tree. Saved when leaving the screen.
 */
class TreeSettingsController : BaseController() {

    private companion object {
        const val DATE_PICKER = "fixedDate"
    }

    private lateinit var tree: Settings.Tree
    private lateinit var yearsLayout: TextInputLayout
    private lateinit var yearsEdit: TextInputEditText
    private lateinit var dateGroup: RadioGroup
    private lateinit var dateButton: MaterialButton
    private var fixedDate: LocalDate? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val found = Global.settings.getTree(intent.getIntExtra(Extra.TREE_ID, Global.settings.openTree))
        if (found == null) {
            finish()
            return
        }
        tree = found
        setContentView(R.layout.tree_settings)
        setToolbar()
        val treeSettings = tree.treeSettings
        findViewById<TextView>(R.id.tree_settings_intro).text = getString(R.string.tree_settings_intro, tree.title)

        yearsLayout = findViewById(R.id.tree_settings_years_layout)
        yearsEdit = findViewById(R.id.tree_settings_years)
        yearsEdit.setText(treeSettings.lifeSpan.toString())
        yearsEdit.doAfterTextChanged { yearsLayout.error = if (validYears() == null) getString(R.string.tree_settings_years_error) else null }

        dateGroup = findViewById(R.id.tree_settings_date_group)
        dateButton = findViewById(R.id.tree_settings_date)
        fixedDate = treeSettings.fixedDate?.let { runCatching { LocalDate(it) }.getOrNull() }
        dateGroup.check(if (treeSettings.customDate) R.id.tree_settings_fixed_date else R.id.tree_settings_device_date)
        dateGroup.setOnCheckedChangeListener { _, checked ->
            if (checked == R.id.tree_settings_fixed_date && fixedDate == null) fixedDate = LocalDate.now()
            showDate()
        }
        dateButton.setOnClickListener { pickDate() }
        showDate()
        // A picker restored after rotation gets back its listener
        @Suppress("UNCHECKED_CAST")
        (supportFragmentManager.findFragmentByTag(DATE_PICKER) as? MaterialDatePicker<Long>)?.let { listen(it) }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    /** Life span typed, or null if not a number between 1 and 999. */
    private fun validYears(): Int? = yearsEdit.text?.toString()?.trim()?.toIntOrNull()?.takeIf { it in 1..999 }

    private fun showDate() {
        val fixed = dateGroup.checkedRadioButtonId == R.id.tree_settings_fixed_date
        val date = if (fixed) fixedDate ?: LocalDate.now() else LocalDate.now()
        dateButton.text = DateFormat.getDateInstance(DateFormat.LONG).format(date.toDate())
        dateButton.alpha = if (fixed) 1f else 0.6f
    }

    private fun pickDate() {
        val start = (fixedDate ?: LocalDate.now()).toDateTimeAtStartOfDay(DateTimeZone.UTC).millis
        MaterialDatePicker.Builder.datePicker()
            .setTitleText(R.string.fixed_date)
            .setSelection(start)
            .build().also { listen(it) }
            .show(supportFragmentManager, DATE_PICKER)
    }

    private fun listen(picker: MaterialDatePicker<Long>) {
        picker.addOnPositiveButtonClickListener { millis ->
            fixedDate = LocalDate(millis, DateTimeZone.UTC)
            dateGroup.check(R.id.tree_settings_fixed_date)
            showDate()
        }
    }

    override fun onPause() {
        super.onPause()
        if (!::tree.isInitialized) return
        val old = tree.treeSettings
        val lifeSpan = validYears() ?: old.lifeSpan // Invalid input keeps the previous value
        val customDate = dateGroup.checkedRadioButtonId == R.id.tree_settings_fixed_date
        val date = if (customDate) (fixedDate ?: LocalDate.now()).toString() else old.fixedDate
        if (lifeSpan == old.lifeSpan && customDate == old.customDate && date == old.fixedDate) return // Nothing changed

        tree.settings = Settings.TreeSettings().apply {
            this.lifeSpan = lifeSpan
            this.customDate = customDate
            this.fixedDate = date
        }
        Global.settings.save()
        Global.edited = true // Ages in the diagram and lists are refreshed
        ReminderScheduler.reschedule(this) // Trees with a fixed date have no reminders
        // Birthday notifications: none with a fixed date, otherwise recomputed with the new life span
        if (customDate) {
            Notifier(this, null, tree.id, Notifier.What.DELETE)
            tree.birthdays?.clear()
            Global.settings.save()
        } else if (Global.gc != null && Global.settings.openTree == tree.id) {
            Notifier(this, Global.gc, tree.id, Notifier.What.DEFAULT)
        } else { // A closed tree is read in the background, then notified here (also if the screen is gone)
            val context = applicationContext
            val treeId = tree.id
            ProcessLifecycleOwner.get().lifecycleScope.launch {
                val gedcom = withContext(Dispatchers.IO) { // Quietly: no toasts outside the main thread
                    runCatching { JsonParser().fromJson(TreesController.updateLanguage(File(context.filesDir, "$treeId.json").readText())) }
                        .getOrNull()
                }
                if (gedcom != null) Notifier(context, gedcom, treeId, Notifier.What.DEFAULT)
            }
        }
    }
}
