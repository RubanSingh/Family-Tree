package dna.familytree.feature.places

import org.folg.gedcom.model.EventFact
import org.folg.gedcom.model.Family
import org.folg.gedcom.model.Gedcom
import org.folg.gedcom.model.Person
import java.util.Locale

/**
 * All the places of a tree, from the place and address of every event of persons and families.
 */
object FamilyPlaces {

    /** An event that happened in a place, with the person or family it belongs to. */
    class PlaceEvent(val event: EventFact, val person: Person?, val family: Family?)

    class Place(val name: String) {
        val events = mutableListOf<PlaceEvent>()
        val people = LinkedHashSet<Person>()
        private val parts = name.split(',').map { it.trim() }.filter { it.isNotEmpty() }
        /** First part, e.g. the town. */
        val title: String = parts.firstOrNull() ?: name
        /** The rest, e.g. region and country. */
        val subtitle: String? = parts.drop(1).joinToString(", ").ifEmpty { null }
        /** Last part, if the place has more than one. */
        val country: String? = if (parts.size > 1) parts.last() else null
        /** What to search on the map: the address of an event here, if any, is more precise. */
        val query: String get() = events.firstNotNullOfOrNull { it.event.address?.let { _ -> Maps.queryOf(it.event) } } ?: name
    }

    /** Places sorted by number of events, then by name. */
    @JvmStatic
    fun collect(gedcom: Gedcom): List<Place> {
        val places = LinkedHashMap<String, Place>()
        fun add(event: EventFact, person: Person?, family: Family?) {
            val name = event.place?.let { Maps.clean(it) }?.ifEmpty { null }
                ?: Maps.queryOf(event.address) ?: return
            val place = places.getOrPut(name.lowercase(Locale.getDefault()).replace(Regex("\\s+"), " ")) { Place(name) }
            place.events += PlaceEvent(event, person, family)
            if (person != null) place.people += person
            if (family != null) place.people += family.getHusbands(gedcom) + family.getWives(gedcom)
        }
        gedcom.people.forEach { person -> person.eventsFacts.forEach { add(it, person, null) } }
        gedcom.families.forEach { family -> family.eventsFacts.forEach { add(it, null, family) } }
        return places.values.sortedWith(compareByDescending<Place> { it.events.size }.thenBy { it.name.lowercase() })
    }

    /** Countries with the number of their places, most first. */
    @JvmStatic
    fun countries(places: List<Place>): List<Pair<String, Int>> =
        places.mapNotNull { it.country }.groupingBy { it }.eachCount().toList().sortedByDescending { it.second }
}
