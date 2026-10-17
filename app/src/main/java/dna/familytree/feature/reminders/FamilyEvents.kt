package dna.familytree.feature.reminders

import dna.familytree.AppUtils
import dna.familytree.GedcomDateConverter
import dna.familytree.Settings
import dna.familytree.constant.Format
import org.folg.gedcom.model.EventFact
import org.folg.gedcom.model.Family
import org.folg.gedcom.model.Gedcom
import org.folg.gedcom.model.Person
import org.joda.time.LocalDate
import org.joda.time.format.ISODateTimeFormat

/**
 * Yearly family events found in a tree (birthdays, wedding anniversaries, remembrance days) and the user's reminders.
 * Only exact dates are used: day and month, with or without year.
 */
object FamilyEvents {

    enum class Kind { BIRTHDAY, ANNIVERSARY, REMEMBRANCE, REMINDER }

    /** A day and month, with the year if known (0 otherwise). */
    class YearlyDate(val month: Int, val day: Int, val year: Int) {
        /** The next time it occurs from [from] (included). February 29 falls on the 28th in common years. */
        fun nextFrom(from: LocalDate): LocalDate {
            val thisYear = on(from.year)
            return if (thisYear.isBefore(from)) on(from.year + 1) else thisYear
        }

        fun on(year: Int): LocalDate =
            if (month == 2 && day == 29 && !LocalDate(year, 1, 1).year().isLeap) LocalDate(year, 2, 28)
            else LocalDate(year, month, day)
    }

    /** One occurrence for the events screen. */
    class Upcoming(
        val kind: Kind,
        val title: String,
        val date: LocalDate,
        /** Years turned / married / since; 0 when unknown. */
        val years: Int,
        val person: Person? = null,
        val family: Family? = null,
        val reminder: Settings.Reminder? = null
    ) {
        /** A one-time reminder already gone. */
        fun isPast(today: LocalDate) = date.isBefore(today)
    }

    /** Exact day and month of a GEDCOM date, or null. */
    @JvmStatic
    fun yearlyDate(gedcomDate: String?): YearlyDate? {
        if (gedcomDate.isNullOrBlank()) return null
        return try {
            val converter = GedcomDateConverter(gedcomDate)
            val data = converter.data1
            val date = data.date
            if (!converter.isSingleKind || date == null || date.year + 1900 <= 0) null
            else if (data.isFormat(Format.D_M_Y)) YearlyDate(date.month + 1, date.date, date.year + 1900)
            else if (data.isFormat(Format.D_M)) YearlyDate(date.month + 1, date.date, 0)
            else null
        } catch (e: Exception) {
            null
        }
    }

    private fun firstDate(events: List<EventFact>, tag: String): YearlyDate? =
        events.firstOrNull { it.tag == tag && it.date != null }?.let { yearlyDate(it.date) }

    private fun spouses(gedcom: Gedcom, family: Family): List<Person> = family.getHusbands(gedcom) + family.getWives(gedcom)

    /** A married couple still together: no divorce or annulment, nobody dead. */
    private fun isLivingCouple(gedcom: Gedcom, family: Family): Boolean =
        family.eventsFacts.none { it.tag == "DIV" || it.tag == "ANUL" } && spouses(gedcom, family).none { AppUtils.isDead(it) }

    /** "John & Mary Doe" style name of a couple. */
    @JvmStatic
    fun coupleName(gedcom: Gedcom, family: Family): String {
        val names = spouses(gedcom, family).map { AppUtils.properName(it) }.filter { it.isNotBlank() }
        return names.joinToString(" & ").ifEmpty { "?" }
    }

    /** Wedding anniversaries and remembrance days of a tree, to be notified even when the tree is closed. */
    @JvmStatic
    fun occasions(gedcom: Gedcom): List<Settings.Occasion> {
        val list = mutableListOf<Settings.Occasion>()
        for (family in gedcom.families) {
            val date = firstDate(family.eventsFacts, "MARR") ?: continue
            if (!isLivingCouple(gedcom, family)) continue
            list += Settings.Occasion().apply {
                kind = "MARR"
                id = family.id
                personId = spouses(gedcom, family).firstOrNull()?.id
                name = coupleName(gedcom, family)
                month = date.month; day = date.day; year = date.year
            }
        }
        for (person in gedcom.people) {
            if (!AppUtils.isDead(person)) continue
            val date = firstDate(person.eventsFacts, "DEAT") ?: continue
            list += Settings.Occasion().apply {
                kind = "DEAT"
                id = person.id
                personId = person.id
                name = AppUtils.properName(person)
                month = date.month; day = date.day; year = date.year
            }
        }
        return list
    }

    /** Everything to show in the events screen, sorted by date; past one-time reminders at the end. */
    @JvmStatic
    fun upcoming(gedcom: Gedcom, tree: Settings.Tree, today: LocalDate): List<Upcoming> {
        val list = mutableListOf<Upcoming>()
        val lifeSpan = tree.treeSettings.lifeSpan
        for (person in gedcom.people) {
            if (AppUtils.isDead(person)) {
                val death = firstDate(person.eventsFacts, "DEAT") ?: continue
                val next = death.nextFrom(today)
                list += Upcoming(Kind.REMEMBRANCE, AppUtils.properName(person), next,
                    if (death.year > 0) next.year - death.year else 0, person = person)
            } else {
                val birth = firstDate(person.eventsFacts, "BIRT") ?: continue
                val next = birth.nextFrom(today)
                val age = if (birth.year > 0) next.year - birth.year else 0
                if (birth.year > 0 && age > lifeSpan) continue // Beyond the life span: not considered living
                list += Upcoming(Kind.BIRTHDAY, AppUtils.properName(person), next, age, person = person)
            }
        }
        for (family in gedcom.families) {
            val marriage = firstDate(family.eventsFacts, "MARR") ?: continue
            if (!isLivingCouple(gedcom, family)) continue
            val next = marriage.nextFrom(today)
            list += Upcoming(Kind.ANNIVERSARY, coupleName(gedcom, family), next,
                if (marriage.year > 0) next.year - marriage.year else 0, family = family,
                person = spouses(gedcom, family).firstOrNull())
        }
        tree.reminders?.forEach { reminder ->
            val date = parseDate(reminder.date) ?: return@forEach
            val next = if (reminder.yearly) YearlyDate(date.monthOfYear, date.dayOfMonth, date.year).nextFrom(today) else date
            list += Upcoming(Kind.REMINDER, reminder.title ?: "", next,
                if (reminder.yearly) next.year - date.year else 0,
                person = reminder.personId?.let { gedcom.getPerson(it) }, reminder = reminder)
        }
        return list.sortedWith(compareBy<Upcoming> { it.isPast(today) }.thenBy { if (it.isPast(today)) -it.date.toDate().time else it.date.toDate().time })
    }

    @JvmStatic
    fun parseDate(iso: String?): LocalDate? = try {
        if (iso.isNullOrBlank()) null else ISODateTimeFormat.date().parseLocalDate(iso)
    } catch (e: Exception) {
        null
    }
}
