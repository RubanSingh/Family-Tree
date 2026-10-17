package dna.familytree.feature.reminders

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings as AndroidSettings
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.text.BidiFormatter
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.color.MaterialColors
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import dna.familytree.BaseController
import dna.familytree.Global
import dna.familytree.Memory
import dna.familytree.ProfileController
import dna.familytree.R
import dna.familytree.designsystem.PremiumBadge
import dna.familytree.detail.FamilyController
import dna.familytree.feature.ads.AdsManager
import dna.familytree.feature.ads.NativeAdCard
import dna.familytree.feature.billing.PremiumGate
import dna.familytree.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.joda.time.Days
import org.joda.time.LocalDate
import org.joda.time.LocalTime
import java.text.NumberFormat
import dna.familytree.designsystem.R as DsR

/**
 * Events & reminders of the open tree: birthdays, wedding anniversaries, remembrance days and the user's reminders,
 * grouped by how soon they come.
 */
class EventsController : BaseController() {

    companion object {
        /** Opens the editor of a new reminder for this person. */
        const val EXTRA_NEW_FOR_PERSON = "newReminderFor"
        private const val MENU_ANNIVERSARIES = 1
        private const val MENU_REMEMBRANCE = 2
    }

    private enum class Filter(val label: Int, val kind: FamilyEvents.Kind?) {
        ALL(R.string.events_filter_all, null),
        BIRTHDAYS(R.string.events_filter_birthdays, FamilyEvents.Kind.BIRTHDAY),
        ANNIVERSARIES(R.string.events_filter_anniversaries, FamilyEvents.Kind.ANNIVERSARY),
        REMEMBRANCE(R.string.events_filter_remembrance, FamilyEvents.Kind.REMEMBRANCE),
        REMINDERS(R.string.events_filter_reminders, FamilyEvents.Kind.REMINDER)
    }

    /** Rows of the list. */
    private sealed class Row {
        class Hero(val event: FamilyEvents.Upcoming) : Row()
        object Filters : Row()
        object NotifyBanner : Row()
        object PremiumBanner : Row()
        class Section(val title: Int) : Row()
        class Event(val event: FamilyEvents.Upcoming) : Row()
        object Empty : Row()
        object Ad : Row()
    }

    private lateinit var list: RecyclerView
    private val adapter = Adapter()
    private var filter = Filter.ALL
    private var events: List<FamilyEvents.Upcoming> = emptyList()
    private var today: LocalDate = LocalDate.now()
    /** Native ad shown after the first group of events, taken from the preloaded ones at the first display only. */
    private var nativeAd: NativeAd? = null
    private var adChecked = false

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { refresh() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Global.gc == null || Global.settings.currentTree == null) {
            finish()
            return
        }
        setContentView(R.layout.events)
        setToolbar()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        list = findViewById(R.id.events_list)
        list.layoutManager = LinearLayoutManager(this)
        list.adapter = adapter
        val add = findViewById<ExtendedFloatingActionButton>(R.id.events_add)
        add.setOnClickListener { ReminderEditor.show(this, null, null) { refresh() } }
        list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy > 8) add.shrink() else if (dy < -8) add.extend()
            }
        })
        if (savedInstanceState == null) intent.getStringExtra(EXTRA_NEW_FOR_PERSON)?.let { personId ->
            list.post { ReminderEditor.show(this, null, personId) { refresh() } }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::list.isInitialized) refresh()
    }

    override fun onDestroy() {
        nativeAd?.destroy()
        nativeAd = null
        super.onDestroy()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun refresh() {
        val gedcom = Global.gc ?: return
        val tree = Global.settings.currentTree ?: return
        today = LocalDate.now()
        lifecycleScope.launch {
            events = withContext(Dispatchers.Default) { FamilyEvents.upcoming(gedcom, tree, today) }
            adapter.submit(buildRows())
        }
    }

    private fun buildRows(): List<Row> {
        val rows = mutableListOf<Row>()
        events.firstOrNull { !it.isPast(today) }?.let { rows += Row.Hero(it) }
        if (!notificationsAllowed()) rows += Row.NotifyBanner
        if (!PremiumGate.isPremium() && events.any { it.kind == FamilyEvents.Kind.ANNIVERSARY || it.kind == FamilyEvents.Kind.REMEMBRANCE })
            rows += Row.PremiumBanner
        if (events.isNotEmpty()) rows += Row.Filters
        val shown = events.filter { filter.kind == null || it.kind == filter.kind }
        var section = 0
        for (event in shown) {
            val title = sectionOf(event)
            if (title != section) {
                rows += Row.Section(title)
                section = title
            }
            rows += Row.Event(event)
        }
        if (shown.isEmpty()) rows += Row.Empty
        else insertAd(rows)
        return rows
    }

    /** After the first group of events (or at the end), so the next events are never pushed down while visible. */
    private fun insertAd(rows: MutableList<Row>) {
        if (!adChecked) {
            adChecked = true
            nativeAd = AdsManager.takeNative(this)
        }
        if (nativeAd == null || !AdsManager.adsEnabled()) return
        val firstSection = rows.indexOfFirst { it is Row.Section }
        val secondSection = rows.withIndex().indexOfFirst { (i, row) -> i > firstSection && row is Row.Section }
        if (secondSection > 0) rows.add(secondSection, Row.Ad) else rows += Row.Ad
    }

    private fun sectionOf(event: FamilyEvents.Upcoming): Int {
        if (event.isPast(today)) return R.string.events_section_past
        val days = Days.daysBetween(today, event.date).days
        return when {
            days == 0 -> R.string.events_section_today
            days <= 7 -> R.string.events_section_week
            days <= 31 -> R.string.events_section_month
            else -> R.string.events_section_later
        }
    }

    // Notifications

    private fun notificationsAllowed(): Boolean = NotificationManagerCompat.from(this).areNotificationsEnabled()

    private fun askNotifications() {
        val prefs = getSharedPreferences("events", MODE_PRIVATE)
        val askedBefore = prefs.getBoolean("notificationsAsked", false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            && (!askedBefore || shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS))) {
            prefs.edit().putBoolean("notificationsAsked", true).apply()
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else { // Denied for good, or switched off: only the system settings can turn them on
            startActivity(Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(AndroidSettings.EXTRA_APP_PACKAGE, packageName))
        }
    }

    // Options menu: which yearly events are notified

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val premium = PremiumGate.isPremium()
        fun title(text: Int): CharSequence = if (premium) getString(text) else PremiumBadge.withDiamond(this, getString(text))
        menu.add(0, MENU_ANNIVERSARIES, 0, title(R.string.events_notify_anniversaries)).apply {
            isCheckable = true
            isChecked = premium && !Global.settings.anniversaryAlertsOff
        }
        menu.add(0, MENU_REMEMBRANCE, 0, title(R.string.events_notify_remembrance)).apply {
            isCheckable = true
            isChecked = premium && Global.settings.remembranceAlerts
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId != MENU_ANNIVERSARIES && item.itemId != MENU_REMEMBRANCE) return super.onOptionsItemSelected(item)
        if (!PremiumGate.isPremium()) {
            PremiumGate.open(this, if (item.itemId == MENU_ANNIVERSARIES) R.string.event_anniversary else R.string.event_remembrance)
            return true
        }
        if (item.itemId == MENU_ANNIVERSARIES) Global.settings.anniversaryAlertsOff = item.isChecked
        else Global.settings.remembranceAlerts = !item.isChecked
        Global.settings.save()
        ReminderScheduler.reschedule(this)
        invalidateOptionsMenu()
        refresh()
        return true
    }

    // Texts

    private fun kindLabel(event: FamilyEvents.Upcoming): String = when (event.kind) {
        FamilyEvents.Kind.BIRTHDAY -> getString(R.string.event_birthday)
        FamilyEvents.Kind.ANNIVERSARY -> getString(R.string.event_anniversary)
        FamilyEvents.Kind.REMEMBRANCE -> getString(R.string.event_remembrance)
        FamilyEvents.Kind.REMINDER -> ReminderKinds.label(this, event.reminder?.kind)
    }

    private fun kindIcon(event: FamilyEvents.Upcoming): Int = when (event.kind) {
        FamilyEvents.Kind.BIRTHDAY -> DsR.drawable.ds_ic_cake
        FamilyEvents.Kind.ANNIVERSARY -> DsR.drawable.ds_ic_favorite
        FamilyEvents.Kind.REMEMBRANCE -> DsR.drawable.ds_ic_ribbon
        FamilyEvents.Kind.REMINDER -> ReminderKinds.of(event.reminder?.kind).icon
    }

    /** "Turns 34 · Saturday, 12 March" */
    private fun detail(event: FamilyEvents.Upcoming): String {
        val parts = mutableListOf<String>()
        val years = event.years
        when (event.kind) {
            FamilyEvents.Kind.BIRTHDAY -> if (years > 0) parts += resources.getQuantityString(R.plurals.events_turns, years, years)
            FamilyEvents.Kind.ANNIVERSARY -> if (years > 0) parts += resources.getQuantityString(R.plurals.events_married_years, years, years)
            FamilyEvents.Kind.REMEMBRANCE -> if (years > 0) parts += resources.getQuantityString(R.plurals.events_years_since, years, years)
            FamilyEvents.Kind.REMINDER -> {
                event.reminder?.personId?.let { id -> Global.gc.getPerson(id) }?.let { parts += dna.familytree.AppUtils.properName(it) }
                if (event.reminder?.yearly == true && years > 0) parts += resources.getQuantityString(R.plurals.events_reminder_years, years, years)
            }
        }
        parts += format(event.date, if (event.isPast(today)) "EEEdMMMy" else "EEEEdMMMM")
        event.reminder?.time?.let { time ->
            runCatching { LocalTime.parse(time) }.getOrNull()?.let { parts += DateFormat.getTimeFormat(this).format(it.toDateTimeToday().toDate()) }
        }
        // Each part isolated, so separators stay in place when names and dates are in different scripts (RTL)
        val bidi = BidiFormatter.getInstance()
        return parts.joinToString(" · ") { bidi.unicodeWrap(it) }
    }

    /**
     * A date in the user's language. ICU patterns (e.g. 'cccc', 'LLL' in Russian) need the platform formatter:
     * Joda-Time doesn't know them.
     */
    private fun format(date: LocalDate, skeleton: String): String {
        val locale = java.util.Locale.getDefault()
        val pattern = if (skeleton == "LLL") skeleton else DateFormat.getBestDateTimePattern(locale, skeleton)
        return java.text.SimpleDateFormat(pattern, locale).format(date.toDate())
    }

    private fun countdown(event: FamilyEvents.Upcoming): String {
        if (event.isPast(today)) return event.date.year.toString()
        val days = Days.daysBetween(today, event.date).days
        return when (days) {
            0 -> getString(R.string.events_today)
            1 -> getString(R.string.events_tomorrow)
            else -> resources.getQuantityString(R.plurals.events_in_days, days, days)
        }
    }

    /** The event is going to be notified. */
    private fun isNotified(event: FamilyEvents.Upcoming): Boolean {
        val tree = Global.settings.currentTree
        if (tree.treeSettings.customDate) return false
        return when (event.kind) {
            FamilyEvents.Kind.BIRTHDAY -> true
            FamilyEvents.Kind.ANNIVERSARY -> PremiumGate.isPremium() && !Global.settings.anniversaryAlertsOff
            FamilyEvents.Kind.REMEMBRANCE -> PremiumGate.isPremium() && Global.settings.remembranceAlerts
            FamilyEvents.Kind.REMINDER -> !event.isPast(today)
        }
    }

    private fun open(event: FamilyEvents.Upcoming) {
        when {
            event.reminder != null -> ReminderEditor.show(this, event.reminder, null) { refresh() }
            event.family != null -> {
                Memory.setLeader(event.family)
                startActivity(Intent(this, FamilyController::class.java))
            }
            event.person != null -> {
                Memory.setLeader(event.person)
                startActivity(Intent(this, ProfileController::class.java))
            }
        }
    }

    // List

    private inner class Adapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        private var rows: List<Row> = emptyList()

        fun submit(newRows: List<Row>) {
            rows = newRows
            notifyDataSetChanged() // Few rows, rebuilt only on refresh
        }

        override fun getItemCount() = rows.size

        override fun getItemViewType(position: Int) = when (rows[position]) {
            is Row.Hero -> 0
            Row.Filters -> 1
            Row.NotifyBanner, Row.PremiumBanner -> 2
            is Row.Section -> 3
            is Row.Event -> 4
            Row.Empty -> 5
            Row.Ad -> 6
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            if (viewType == 6) return object : RecyclerView.ViewHolder(NativeAdCard(parent.context).apply {
                layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                val gutter = resources.getDimensionPixelSize(DsR.dimen.ds_page_gutter) // As the event cards
                setPadding(gutter, 0, gutter, 0)
            }) {}
            val layout = when (viewType) {
                0 -> R.layout.events_hero
                1 -> R.layout.events_filters
                2 -> R.layout.events_banner
                3 -> R.layout.events_section
                4 -> R.layout.event_card
                else -> R.layout.events_empty
            }
            return object : RecyclerView.ViewHolder(LayoutInflater.from(parent.context).inflate(layout, parent, false)) {}
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val view = holder.itemView
            when (val row = rows[position]) {
                is Row.Hero -> bindHero(view, row.event)
                Row.Filters -> bindFilters(view)
                Row.NotifyBanner -> bindBanner(view, DsR.drawable.ds_ic_notifications, R.string.events_notify_title,
                    R.string.events_notify_desc, getString(R.string.events_allow)) { askNotifications() }
                Row.PremiumBanner -> bindBanner(view, DsR.drawable.ds_ic_diamond, R.string.events_premium_title,
                    R.string.events_premium_desc, getString(R.string.events_unlock)) { PremiumGate.open(this@EventsController, R.string.events_title) }
                is Row.Section -> (view as TextView).setText(row.title)
                is Row.Event -> bindEvent(view, row.event)
                Row.Ad -> nativeAd?.let { (view as NativeAdCard).bind(it, false) }
                Row.Empty -> {
                    view.findViewById<ImageView>(R.id.events_empty_icon).setImageResource(DsR.drawable.ds_ic_event)
                    view.findViewById<TextView>(R.id.events_empty_title).setText(R.string.events_empty_title)
                    view.findViewById<TextView>(R.id.events_empty_desc).setText(R.string.events_empty_desc)
                }
            }
        }
    }

    private fun bindHero(view: View, event: FamilyEvents.Upcoming) {
        view.findViewById<ImageView>(R.id.events_hero_icon).setImageResource(kindIcon(event))
        view.findViewById<TextView>(R.id.events_hero_title).text = event.title
        view.findViewById<TextView>(R.id.events_hero_detail).text = "${BidiFormatter.getInstance().unicodeWrap(kindLabel(event))} · ${detail(event)}"
        view.findViewById<TextView>(R.id.events_hero_when).text = countdown(event)
        view.setOnClickListener { open(event) }
    }

    private fun bindFilters(view: View) {
        val group = view.findViewById<ChipGroup>(R.id.events_filters)
        if (group.childCount == 0) Filter.entries.forEach { item ->
            val chip = LayoutInflater.from(this).inflate(R.layout.reminder_kind_chip, group, false) as Chip
            chip.id = View.generateViewId()
            chip.tag = item
            chip.isChipIconVisible = false
            group.addView(chip)
        }
        group.setOnCheckedStateChangeListener(null)
        for (i in 0 until group.childCount) {
            val chip = group.getChildAt(i) as Chip
            val item = chip.tag as Filter
            val count = events.count { item.kind == null || it.kind == item.kind }
            chip.text = "${getString(item.label)} · ${NumberFormat.getInstance().format(count)}"
            chip.isChecked = item == filter
        }
        group.setOnCheckedStateChangeListener { g, checked ->
            val chosen = checked.firstOrNull()?.let { g.findViewById<Chip>(it).tag as Filter } ?: return@setOnCheckedStateChangeListener
            if (chosen != filter) {
                filter = chosen
                list.post { adapter.submit(buildRows()) }
            }
        }
    }

    private fun bindBanner(view: View, icon: Int, title: Int, desc: Int, action: String, onClick: () -> Unit) {
        view.findViewById<ImageView>(R.id.events_banner_icon).setImageResource(icon)
        view.findViewById<TextView>(R.id.events_banner_title).setText(title)
        view.findViewById<TextView>(R.id.events_banner_desc).setText(desc)
        view.findViewById<TextView>(R.id.events_banner_action).apply {
            text = action
            setOnClickListener { onClick() }
        }
    }

    private fun bindEvent(view: View, event: FamilyEvents.Upcoming) {
        val icon = view.findViewById<ImageView>(R.id.event_icon)
        val photo = view.findViewById<ImageView>(R.id.event_photo)
        val badge = view.findViewById<ImageView>(R.id.event_badge)
        icon.setImageResource(kindIcon(event))
        val hasPhoto = event.person != null && FileUtils.showMainImageForPerson(Global.gc, event.person, photo) != null
        photo.visibility = if (hasPhoto) View.VISIBLE else View.GONE
        badge.visibility = if (hasPhoto) View.VISIBLE else View.GONE
        if (hasPhoto) badge.setImageResource(kindIcon(event))
        view.findViewById<TextView>(R.id.event_kind).text = kindLabel(event)
        view.findViewById<TextView>(R.id.event_title).text = event.title
        view.findViewById<TextView>(R.id.event_detail).apply {
            text = detail(event)
            // A small bell when the event is notified
            val bell = if (isNotified(event)) ContextCompat.getDrawable(context, DsR.drawable.ds_ic_notifications)?.mutate() else null
            val size = resources.getDimensionPixelSize(R.dimen.inline_icon_small)
            bell?.setBounds(0, 0, size, size)
            bell?.setTint(currentTextColor)
            setCompoundDrawablesRelative(bell, null, null, null)
            contentDescription = "$text. ${getString(if (bell != null) R.string.events_notified else R.string.events_not_notified)}"
        }
        val isToday = event.date == today
        val onTile = MaterialColors.getColor(view, if (isToday) com.google.android.material.R.attr.colorOnTertiaryContainer
        else com.google.android.material.R.attr.colorOnSurface)
        view.findViewById<TextView>(R.id.event_day).apply {
            text = NumberFormat.getInstance().format(event.date.dayOfMonth) // Digits of the user's language
            setTextColor(onTile)
        }
        view.findViewById<TextView>(R.id.event_month).apply {
            text = format(event.date, "LLL") // Stand-alone month name
            setTextColor(onTile)
        }
        view.findViewById<TextView>(R.id.event_countdown).apply {
            text = countdown(event)
            setTextColor(if (isToday) onTile else MaterialColors.getColor(view, androidx.appcompat.R.attr.colorPrimary))
        }
        view.findViewById<View>(R.id.event_tile).apply {
            setBackgroundResource(if (isToday) R.drawable.event_date_tile_today else R.drawable.event_date_tile)
            alpha = if (event.isPast(today)) 0.5f else 1f
        }
        view.setOnClickListener { open(event) }
    }
}
