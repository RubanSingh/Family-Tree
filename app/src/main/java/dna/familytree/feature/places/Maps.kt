package dna.familytree.feature.places

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import dna.familytree.DetailController
import dna.familytree.R
import org.folg.gedcom.model.Address
import org.folg.gedcom.model.EventFact
import dna.familytree.designsystem.R as DsR

/**
 * Opens places in the maps app of the device (no API key: plain map links).
 */
object Maps {

    private const val MENU_SHOW = 430
    private const val MENU_DIRECTIONS = 431

    /** Adds "Show on map" and "Directions" to the toolbar, if there is something to look for. */
    @JvmStatic
    fun addMenu(menu: Menu, query: String?) {
        if (query.isNullOrBlank()) return
        menu.add(0, MENU_SHOW, 0, R.string.places_show_map).setIcon(DsR.drawable.ds_ic_map)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        menu.add(0, MENU_DIRECTIONS, 0, R.string.places_directions).setIcon(DsR.drawable.ds_ic_directions)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
    }

    /** Handles the items of [addMenu]: returns true if it was one of them. */
    @JvmStatic
    fun onMenuItem(context: Context, item: MenuItem, query: String?): Boolean {
        if (query.isNullOrBlank()) return false
        when (item.itemId) {
            MENU_SHOW -> show(context, query)
            MENU_DIRECTIONS -> directions(context, query)
            else -> return false
        }
        return true
    }

    /** Shows the place on the map. */
    @JvmStatic
    fun show(context: Context, place: String) {
        val query = Uri.encode(place)
        open(context, Uri.parse("geo:0,0?q=$query"), Uri.parse("https://www.google.com/maps/search/?api=1&query=$query"))
    }

    /** Directions from the current position to the place. */
    @JvmStatic
    fun directions(context: Context, place: String) {
        val url = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${Uri.encode(place)}")
        open(context, url, url)
    }

    /** What to search on the map for an event: its address and place, without empty parts. Null if none. */
    @JvmStatic
    fun queryOf(event: EventFact?): String? = if (event == null) null else join(event.address?.let { DetailController.writeAddress(it, true) }, event.place)

    /** What to search on the map for an address. Null if empty. */
    @JvmStatic
    fun queryOf(address: Address?): String? = join(address?.let { DetailController.writeAddress(it, true) })

    private fun join(vararg parts: String?): String? =
        parts.filterNotNull().map { clean(it) }.filter { it.isNotEmpty() }.distinct().joinToString(", ").ifEmpty { null }

    /** GEDCOM place without empty jurisdictions: "Rome, , Lazio, Italy" → "Rome, Lazio, Italy". */
    @JvmStatic
    fun clean(place: String): String = place.split(',').map { it.trim() }.filter { it.isNotEmpty() }.joinToString(", ")

    private fun open(context: Context, uri: Uri, fallback: Uri) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (e: ActivityNotFoundException) {
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, fallback))
            } catch (noBrowser: ActivityNotFoundException) {
                Toast.makeText(context, R.string.places_no_maps, Toast.LENGTH_LONG).show()
            }
        }
    }
}
