package dna.familytree.feature.places

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import dna.familytree.AppUtils
import dna.familytree.BaseController
import dna.familytree.GedcomDateConverter
import dna.familytree.Global
import dna.familytree.Memory
import dna.familytree.ProfileController
import dna.familytree.R
import dna.familytree.detail.FamilyController
import dna.familytree.feature.billing.PremiumGate
import dna.familytree.feature.reminders.FamilyEvents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import dna.familytree.designsystem.R as DsR

/**
 * Family places: every place of the open tree with its events, searchable and filtered by country,
 * each one opened on the map or with directions. Without Premium only the first [FREE_PLACES] are shown.
 */
class PlacesController : BaseController() {

    companion object {
        const val FREE_PLACES = 3
    }

    private sealed class Row {
        class Hero(val places: Int, val countries: Int) : Row()
        class Place(val place: FamilyPlaces.Place) : Row()
        object PremiumBanner : Row()
        class Empty(val title: Int, val desc: Int?) : Row()
    }

    private lateinit var list: RecyclerView
    private lateinit var countries: ChipGroup
    private val adapter = Adapter()
    private var places: List<FamilyPlaces.Place> = emptyList()
    private var query = ""
    private var country: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Global.gc == null) {
            finish()
            return
        }
        setContentView(R.layout.places)
        setToolbar()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        list = findViewById(R.id.places_list)
        list.layoutManager = LinearLayoutManager(this)
        list.adapter = adapter
        countries = findViewById(R.id.places_countries)
        findViewById<TextInputEditText>(R.id.places_search).doAfterTextChanged {
            query = it?.toString()?.trim().orEmpty()
            show()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!::list.isInitialized) return
        val gedcom = Global.gc ?: return
        lifecycleScope.launch {
            places = withContext(Dispatchers.Default) { FamilyPlaces.collect(gedcom) }
            bindCountries()
            show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun bindCountries() {
        val all = FamilyPlaces.countries(places)
        findViewById<View>(R.id.places_countries_scroll).visibility = if (all.size > 1) View.VISIBLE else View.GONE
        countries.setOnCheckedStateChangeListener(null)
        countries.removeAllViews()
        if (country != null && all.none { it.first == country }) country = null
        (listOf<Pair<String?, Int>>(null to places.size) + all).forEach { (name, count) ->
            val chip = LayoutInflater.from(this).inflate(R.layout.reminder_kind_chip, countries, false) as Chip
            chip.id = View.generateViewId()
            chip.isChipIconVisible = false
            chip.text = "${name ?: getString(R.string.places_all_countries)} · ${java.text.NumberFormat.getInstance().format(count)}"
            chip.tag = name
            countries.addView(chip)
            chip.isChecked = name == country
        }
        countries.setOnCheckedStateChangeListener { group, checked ->
            country = checked.firstOrNull()?.let { group.findViewById<Chip>(it).tag as String? }
            show()
        }
    }

    private fun show() {
        val rows = mutableListOf<Row>()
        if (places.isEmpty()) {
            rows += Row.Empty(R.string.places_empty_title, R.string.places_empty_desc)
        } else {
            rows += Row.Hero(places.size, FamilyPlaces.countries(places).size)
            val locale = Locale.getDefault()
            val shown = places.filter { place ->
                (country == null || place.country == country)
                        && (query.isEmpty() || place.name.lowercase(locale).contains(query.lowercase(locale)))
            }
            if (PremiumGate.isPremium() || shown.size <= FREE_PLACES) shown.forEach { rows += Row.Place(it) }
            else {
                shown.take(FREE_PLACES).forEach { rows += Row.Place(it) }
                rows += Row.PremiumBanner
            }
            if (shown.isEmpty()) rows += Row.Empty(R.string.places_no_results, null)
        }
        adapter.submit(rows)
    }

    /** Bottom sheet with the events of a place. */
    private fun showPlace(place: FamilyPlaces.Place) {
        val sheet = BottomSheetDialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.place_sheet, null)
        view.findViewById<TextView>(R.id.place_sheet_title).text = place.title
        view.findViewById<TextView>(R.id.place_sheet_subtitle).apply {
            text = place.subtitle
            visibility = if (place.subtitle == null) View.GONE else View.VISIBLE
        }
        view.findViewById<View>(R.id.place_sheet_map).setOnClickListener { Maps.show(this, place.query) }
        view.findViewById<View>(R.id.place_sheet_directions).setOnClickListener { Maps.directions(this, place.query) }
        val box = view.findViewById<LinearLayout>(R.id.place_sheet_events)
        place.events.sortedBy { FamilyEvents.yearlyDate(it.event.date)?.year?.takeIf { year -> year > 0 } ?: Int.MAX_VALUE }
            .forEach { item ->
                val row = LayoutInflater.from(this).inflate(R.layout.place_event_row, box, false)
                row.findViewById<ImageView>(R.id.place_event_icon)
                    .setImageResource(if (item.family != null) DsR.drawable.ds_ic_family else DsR.drawable.ds_ic_person)
                row.findViewById<TextView>(R.id.place_event_name).text = item.person?.let { AppUtils.properName(it) }
                    ?: item.family?.let { FamilyEvents.coupleName(Global.gc, it) }
                val date = item.event.date?.let { GedcomDateConverter(it).writeDateLong() }?.ifEmpty { null }
                row.findViewById<TextView>(R.id.place_event_what).text = listOfNotNull(item.event.displayType, date).joinToString(" · ")
                row.setOnClickListener {
                    sheet.dismiss()
                    if (item.person != null) {
                        Memory.setLeader(item.person)
                        startActivity(Intent(this, ProfileController::class.java))
                    } else if (item.family != null) {
                        Memory.setLeader(item.family)
                        startActivity(Intent(this, FamilyController::class.java))
                    }
                }
                box.addView(row)
            }
        sheet.setContentView(view)
        sheet.show()
    }

    private inner class Adapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        private var rows: List<Row> = emptyList()

        fun submit(newRows: List<Row>) {
            rows = newRows
            notifyDataSetChanged() // Rebuilt at each search: the list is short-lived and simple
        }

        override fun getItemCount() = rows.size

        override fun getItemViewType(position: Int) = when (rows[position]) {
            is Row.Hero -> 0
            is Row.Place -> 1
            Row.PremiumBanner -> 2
            is Row.Empty -> 3
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val layout = when (viewType) {
                0 -> R.layout.events_hero
                1 -> R.layout.place_card
                2 -> R.layout.events_banner
                else -> R.layout.events_empty
            }
            return object : RecyclerView.ViewHolder(LayoutInflater.from(parent.context).inflate(layout, parent, false)) {}
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val view = holder.itemView
            when (val row = rows[position]) {
                is Row.Hero -> {
                    view.findViewById<ImageView>(R.id.events_hero_icon).setImageResource(DsR.drawable.ds_ic_map)
                    view.findViewById<TextView>(R.id.events_hero_overline).setText(R.string.places_title)
                    view.findViewById<TextView>(R.id.events_hero_title).text =
                        resources.getQuantityString(R.plurals.places_count, row.places, row.places)
                    view.findViewById<TextView>(R.id.events_hero_detail).text = if (row.countries > 0)
                        "${resources.getQuantityString(R.plurals.places_countries, row.countries, row.countries)} · ${getString(R.string.places_hero_desc)}"
                    else getString(R.string.places_hero_desc)
                    view.findViewById<View>(R.id.events_hero_when).visibility = View.GONE
                }
                is Row.Place -> {
                    val place = row.place
                    view.findViewById<TextView>(R.id.place_title).text = place.title
                    view.findViewById<TextView>(R.id.place_subtitle).apply {
                        text = place.subtitle
                        visibility = if (place.subtitle == null) View.GONE else View.VISIBLE
                    }
                    val events = resources.getQuantityString(R.plurals.places_events, place.events.size, place.events.size)
                    view.findViewById<TextView>(R.id.place_stats).text = if (place.people.isEmpty()) events
                    else "$events · ${resources.getQuantityString(R.plurals.places_people, place.people.size, place.people.size)}"
                    view.findViewById<View>(R.id.place_map).setOnClickListener { Maps.show(this@PlacesController, place.query) }
                    view.findViewById<View>(R.id.place_directions).setOnClickListener { Maps.directions(this@PlacesController, place.query) }
                    view.setOnClickListener { showPlace(place) }
                }
                Row.PremiumBanner -> {
                    view.findViewById<ImageView>(R.id.events_banner_icon).setImageResource(DsR.drawable.ds_ic_diamond)
                    view.findViewById<TextView>(R.id.events_banner_title).setText(R.string.places_premium_title)
                    view.findViewById<TextView>(R.id.events_banner_desc).setText(R.string.places_premium_desc)
                    view.findViewById<TextView>(R.id.events_banner_action).apply {
                        setText(R.string.events_unlock)
                        setOnClickListener { PremiumGate.open(this@PlacesController, R.string.places_title) }
                    }
                }
                is Row.Empty -> {
                    view.findViewById<ImageView>(R.id.events_empty_icon).setImageResource(DsR.drawable.ds_ic_place)
                    view.findViewById<TextView>(R.id.events_empty_title).setText(row.title)
                    view.findViewById<TextView>(R.id.events_empty_desc).apply {
                        if (row.desc != null) setText(row.desc) else text = null
                        visibility = if (row.desc != null) View.VISIBLE else View.GONE
                    }
                }
            }
        }
    }
}
