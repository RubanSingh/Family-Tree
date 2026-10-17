package dna.familytree.feature.reminders

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import dna.familytree.Global
import dna.familytree.Notifier
import dna.familytree.R
import dna.familytree.Settings
import dna.familytree.TreesController
import dna.familytree.feature.billing.PremiumGate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.folg.gedcom.model.Gedcom
import org.folg.gedcom.parser.JsonParser
import org.joda.time.DateTime
import org.joda.time.LocalDate
import org.joda.time.LocalTime
import org.joda.time.format.DateTimeFormat
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Notifications of birthdays (computed by [Notifier]), wedding anniversaries, remembrance days (both Premium)
 * and the user's reminders.
 *
 * A single alarm is set at the next reminder of all trees; when it rings every reminder due since the last check is notified,
 * then the following alarm is set. So there is no limit to the number of reminders and none is notified twice.
 */
object ReminderScheduler {

    const val CHANNEL_ID = "reminders"
    private const val TAG = "ReminderScheduler"
    internal const val ACTION_ALARM = "dna.familytree.action.REMINDER_ALARM"
    private const val REQUEST_CODE = -1_420_001 // Negative: never used by the birthday alarms
    private val DAY = TimeUnit.DAYS.toMillis(1)
    private val HORIZON = 400 * DAY
    /** After a long time switched off, older reminders are not notified all together. */
    private val MAX_LATE = 2 * DAY
    /** Delivery window of inexact alarms: the shortest Android allows. */
    private val WINDOW = TimeUnit.MINUTES.toMillis(10)

    /** A notification to show at [at]. */
    class Due(val treeId: Int, val key: String, val at: Long, val title: String, val text: String, val personId: String?,
              val channel: String = CHANNEL_ID)

    /** Called after a tree is saved: refreshes its anniversaries and remembrance days, and the alarm. Never throws. */
    @JvmStatic
    fun onTreeSaved(context: Context, gedcom: Gedcom?, treeId: Int) {
        try {
            val tree = Global.settings?.getTree(treeId) ?: return
            if (gedcom != null) {
                val found = FamilyEvents.occasions(gedcom)
                if (fingerprint(found) != fingerprint(tree.occasions)) {
                    tree.occasions = found.ifEmpty { null }
                    Global.settings.save()
                }
            }
            reschedule(context)
        } catch (e: Exception) {
            Log.w(TAG, "Can't update reminders of tree $treeId", e)
        }
    }

    private fun fingerprint(list: List<Settings.Occasion>?): String =
        list.orEmpty().joinToString("|") { "${it.kind},${it.id},${it.personId},${it.name},${it.month},${it.day},${it.year}" }

    /** Sets the alarm at the next reminder, or removes it if there are none. Never throws. */
    @JvmStatic
    fun reschedule(context: Context) {
        try {
            val settings = Global.settings ?: return
            val now = System.currentTimeMillis()
            if (settings.remindersCheckedAt <= 0 || settings.remindersCheckedAt > now + DAY) { // First time, or clock moved back
                settings.remindersCheckedAt = now
                settings.save()
            }
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val pendingIntent = alarmIntent(context)
            alarmManager.cancel(pendingIntent)
            val next = dueBetween(context, maxOf(now, settings.remindersCheckedAt), now + HORIZON).minOfOrNull { it.at } ?: return
            // From Android 14 exact alarms are denied by default: then the reminder comes within a few minutes
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms())
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pendingIntent)
            else alarmManager.setWindow(AlarmManager.RTC_WAKEUP, next, WINDOW, pendingIntent)
        } catch (e: Exception) {
            Log.w(TAG, "Can't schedule reminders", e)
        }
    }

    private fun alarmIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(context, REQUEST_CODE,
        Intent(context, ReminderReceiver::class.java).setAction(ACTION_ALARM),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    /** The alarm rang: notifies what is due and sets the next alarm. */
    internal fun onAlarm(context: Context) {
        val settings = Global.settings ?: return
        val now = System.currentTimeMillis()
        val until = now + 60_000 // Includes what is due in a moment
        val from = maxOf(settings.remindersCheckedAt, now - MAX_LATE)
        dueBetween(context, from, until).forEach { notify(context, it) }
        settings.remindersCheckedAt = until
        settings.save()
        reschedule(context)
    }

    /** Notifications of all trees due in (from, to]. */
    fun dueBetween(context: Context, from: Long, to: Long): List<Due> {
        val settings = Global.settings ?: return emptyList()
        val (hour, minute) = Notifier.notifyTime().let { it[0] to it[1] }
        val defaultTime = LocalTime(hour, minute)
        val premium = PremiumGate.isPremium()
        val list = mutableListOf<Due>()
        val firstYear = DateTime(from).year - 1
        val lastYear = DateTime(to).year + 1
        fun add(tree: Settings.Tree, key: String, date: LocalDate, time: LocalTime, title: String, text: String, personId: String?,
                channel: String = CHANNEL_ID) {
            val at = date.toLocalDateTime(time).toDateTime().millis
            if (at > from && at <= to) list += Due(tree.id, "${tree.id}/$key@$date", at, "$title (${tree.title})", text, personId, channel)
        }
        for (tree in settings.trees.orEmpty()) {
            if (tree.treeSettings.customDate) continue // Historical tree: ages at a fixed date, no reminders
            // Birthdays (free), saved by Notifier for the living people when the tree is saved
            val lifeSpan = tree.treeSettings.lifeSpan
            tree.birthdays?.forEach { birthday ->
                if (!birthday.isValid) return@forEach
                val next = LocalDate(birthday.date)
                val birthYear = next.year - birthday.age
                val yearly = FamilyEvents.YearlyDate(next.monthOfYear, next.dayOfMonth, birthYear)
                for (year in firstYear..lastYear) {
                    val age = year - birthYear
                    if (age <= 0 || age > lifeSpan) continue
                    add(tree, "B:${birthday.id}", yearly.on(year), defaultTime, birthday.name,
                        context.getString(R.string.turns_years_old, birthday.given ?: birthday.name, age), birthday.id, Notifier.CHANNEL_ID)
                }
            }
            if (premium) tree.occasions?.forEach { occasion ->
                val isAnniversary = occasion.kind == "MARR"
                if (if (isAnniversary) settings.anniversaryAlertsOff else !settings.remembranceAlerts) return@forEach
                val yearly = FamilyEvents.YearlyDate(occasion.month, occasion.day, occasion.year)
                for (year in firstYear..lastYear) {
                    val years = if (occasion.year > 0) year - occasion.year else 0
                    if (occasion.year > 0 && years <= 0) continue
                    val text = if (isAnniversary) {
                        if (years > 0) context.resources.getQuantityString(R.plurals.event_anniversary_years, years, years)
                        else context.getString(R.string.event_anniversary)
                    } else {
                        if (years > 0) context.resources.getQuantityString(R.plurals.event_remembrance_years, years, years)
                        else context.getString(R.string.event_remembrance)
                    }
                    add(tree, "${occasion.kind}:${occasion.id}", yearly.on(year), defaultTime, occasion.name ?: "", text, occasion.personId)
                }
            }
            tree.reminders?.forEach { reminder ->
                val date = FamilyEvents.parseDate(reminder.date) ?: return@forEach
                val time = parseTime(reminder.time) ?: defaultTime
                val dates = if (reminder.yearly) (firstYear..lastYear).filter { it >= date.year }
                    .map { FamilyEvents.YearlyDate(date.monthOfYear, date.dayOfMonth, date.year).on(it) }
                else listOf(date)
                val offsets = if (reminder.daysBefore > 0) listOf(0, reminder.daysBefore) else listOf(0)
                for (day in dates) for (offset in offsets) {
                    val dayText = DateTimeFormat.mediumDate().print(day)
                    val text = when (offset) {
                        0 -> reminder.note?.takeIf { it.isNotBlank() } ?: ReminderKinds.label(context, reminder.kind)
                        1 -> context.getString(R.string.reminder_tomorrow, dayText)
                        else -> context.resources.getQuantityString(R.plurals.reminder_in_days, offset, offset, dayText)
                    }
                    add(tree, "R:${reminder.id}:$offset", day.minusDays(offset), time, reminder.title ?: "", text, reminder.personId)
                }
            }
        }
        return list.sortedBy { it.at }
    }

    private fun parseTime(time: String?): LocalTime? = try {
        if (time.isNullOrBlank()) null else LocalTime.parse(time)
    } catch (e: Exception) {
        null
    }

    private fun notify(context: Context, due: Due) {
        if (due.channel == Notifier.CHANNEL_ID) Notifier.createChannel(context) else createChannel(context)
        val id = -(1 + (due.key.hashCode() and 0x3fffffff)) // Negative: never the same as birthday notifications
        val open = Intent(context, TreesController::class.java)
            .putExtra(Notifier.TREE_ID_KEY, due.treeId)
            .putExtra(Notifier.NOTIFY_ID_KEY, id)
        if (due.personId != null) open.putExtra(Notifier.INDI_ID_KEY, due.personId)
        val contentIntent = PendingIntent.getActivity(context, id, open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notification = NotificationCompat.Builder(context, due.channel)
            .setSmallIcon(R.drawable.tree)
            .setContentTitle(due.title)
            .setContentText(due.text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(due.text))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setCategory(if (due.channel == Notifier.CHANNEL_ID) NotificationCompat.CATEGORY_EVENT else NotificationCompat.CATEGORY_REMINDER)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (e: SecurityException) { // Notifications not allowed
            Log.w(TAG, "Can't notify", e)
        }
    }

    /**
     * Rebuilds in the background, once after an update, the birthdays that older release builds saved in a form
     * this version can't read (field names obfuscated by R8). Trees with a fixed date have none.
     */
    @JvmStatic
    fun repairBirthdays(context: Context) {
        val trees = Global.settings?.trees.orEmpty()
            .filter { it.birthdays != null && !Notifier.birthdaysValid(it) && !it.treeSettings.customDate }
        if (trees.isEmpty()) return
        val app = context.applicationContext
        ProcessLifecycleOwner.get().lifecycleScope.launch {
            for (tree in trees) {
                val gedcom = withContext(Dispatchers.IO) { // Quietly: no toasts outside the main thread
                    runCatching { JsonParser().fromJson(TreesController.updateLanguage(File(app.filesDir, "${tree.id}.json").readText())) }
                        .getOrNull()
                } ?: continue
                Notifier.refreshBirthdays(app, gedcom, tree.id)
            }
        }
    }

    @JvmStatic
    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, context.getString(R.string.reminders_channel), NotificationManager.IMPORTANCE_DEFAULT)
            channel.description = context.getString(R.string.reminders_channel_desc)
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}
