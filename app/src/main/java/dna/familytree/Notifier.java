package dna.familytree;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.work.WorkManager;

import org.folg.gedcom.model.EventFact;
import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.model.Person;

import java.util.Date;
import java.util.HashSet;

import dna.familytree.constant.Format;
import dna.familytree.feature.reminders.ReminderScheduler;
import dna.familytree.util.LoggerUtils;

/**
 * Manager of birthday notifications.
 */
public class Notifier {
    // Extras of the intent opened by a notification (also by the reminders)
    public static final String TREE_ID_KEY = "targetTreeId";
    public static final String INDI_ID_KEY = "targetIndiId";
    public static final String NOTIFY_ID_KEY = "notifyId";
    public static final String CHANNEL_ID = "birthdays";
    private static final int LEGACY_FACTOR = 100000; // Request codes of the alarms before version 1.6
    private final Date now = new Date();
    /** Default time of birthday notifications, as before it could be chosen. */
    public static final String DEFAULT_NOTIFY_TIME = "12:00";
    private final int notifyHour;
    private final int notifyMinute;
    private String TAG = Notifier.class.getSimpleName();

    enum What {REBOOT, CREATE, DELETE, DEFAULT, UPDATE}

    Notifier(Context context, Gedcom gedcom, int treeId, What toDo) {
        int[] time = notifyTime();
        notifyHour = time[0];
        notifyMinute = time[1];
        Settings.Tree tree = Global.settings.getTree(treeId);
        // With a fixed date the ages are historical: birthday notifications make no sense
        if (tree != null && tree.getTreeSettings().customDate && toDo != What.DELETE) return;

        createChannel(context);

        // Deletes previous Workish notifications
        WorkManager.getInstance(context).cancelAllWork(); // Todo remove after version 0.9.1

        switch (toDo) {
            case REBOOT: // After reboot restores all alarms from settings
                for (Settings.Tree tree1 : Global.settings.trees) {
                    if (tree1.getTreeSettings().customDate) continue;
                    createAlarms(context, tree1);
                }
                break;
            case UPDATE: // The notification time changed: moves every birthday to the new time and recreates the alarms
                for (Settings.Tree tree1 : Global.settings.trees) {
                    if (tree1.getTreeSettings().customDate || tree1.birthdays == null) continue;
                    deleteAlarms(context, tree1);
                    for (Settings.Birthday birthday : tree1.birthdays) {
                        Date date = new Date(birthday.date);
                        date.setHours(notifyHour);
                        date.setMinutes(notifyMinute);
                        date.setSeconds(0);
                        // A birthday moved to next year because today's old time had passed, but the new time is still ahead
                        if (date.getYear() > now.getYear() && date.getMonth() == now.getMonth() && date.getDate() == now.getDate()) {
                            Date today = new Date(date.getTime());
                            today.setYear(now.getYear());
                            if (today.after(now)) {
                                date = today;
                                birthday.age -= 1;
                            }
                        }
                        birthday.date = date.getTime();
                    }
                    createAlarms(context, tree1);
                }
                Global.settings.save();
                break;
            case DELETE: // Only deletes alarms
                deleteAlarms(context, tree);
                break;
            case CREATE: // Creates birthday notifications saving them to settings.json
                findBirthdays(gedcom, tree);
                createAlarms(context, tree);
                break;
            default:// Deletes old alarms too
                deleteAlarms(context, tree);
                findBirthdays(gedcom, tree);
                createAlarms(context, tree);
        }
    }

    // Selects people who have to celebrate their birthday and add them to the settings
    // Eventually save settings
    void findBirthdays(Gedcom gedcom, Settings.Tree tree) {
        if (tree.birthdays == null)
            tree.birthdays = new HashSet<>();
        else
            tree.birthdays.clear();
        for (Person person : gedcom.getPeople()) {
            Date birth = findBirth(person);
            if (birth != null) {
                int years = findAge(birth, tree.getTreeSettings().lifeSpan);
                if (years >= 0) {
                    tree.birthdays.add(new Settings.Birthday(person.getId(), AppUtils.givenName(person),
                            AppUtils.properName(person), nextBirthday(birth), years));
                }
            }
        }
        Global.settings.save();
    }

    // Possibly finds the birth Date of a person
    private Date findBirth(Person person) {
        if (!AppUtils.isDead(person)) {
            for (EventFact event : person.getEventsFacts()) {
                if (event.getTag().equals("BIRT") && event.getDate() != null) {
                    GedcomDateConverter datator = new GedcomDateConverter(event.getDate());
                    if (datator.isSingleKind() && datator.data1.isFormat(Format.D_M_Y)) {
                        return datator.data1.date;
                    }
                }
            }
        }
        return null;
    }

    // Counts the number of years that will be turned on the next birthday (-1 if beyond the tree's life span)
    private int findAge(Date birth, int lifeSpan) {
        int years = now.getYear() - birth.getYear();
        if (birth.getMonth() < now.getMonth()
                || (birth.getMonth() == now.getMonth() && birth.getDate() < now.getDate())
                || (birth.getMonth() == now.getMonth() && birth.getDate() == now.getDate()
                && now.getHours() * 60 + now.getMinutes() >= notifyHour * 60 + notifyMinute))
            years++;
        return years <= lifeSpan ? years : -1;
    }

    // From birth Date finds next birthday as long timestamp
    private long nextBirthday(Date birth) {
        birth.setYear(now.getYear());
        birth.setHours(notifyHour);
        birth.setMinutes(notifyMinute);
        birth.setSeconds(0);
        if (now.after(birth))
            birth.setYear(now.getYear() + 1);
        return birth.getTime();
    }

    /** Notification time from the settings as {hour, minute}; midday when not set or invalid. */
    public static int[] notifyTime() {
        String time = Global.settings != null && Global.settings.notifyTime != null ? Global.settings.notifyTime : DEFAULT_NOTIFY_TIME;
        try {
            String[] parts = time.split(":");
            int hour = Integer.parseInt(parts[0]), minute = Integer.parseInt(parts[1]);
            if (hour >= 0 && hour < 24 && minute >= 0 && minute < 60) return new int[]{hour, minute};
        } catch (Exception ignored) {
        }
        return new int[]{12, 0};
    }

    /** Applies a new notification time to all the scheduled birthdays. */
    public static void updateNotifyTime(Context context) {
        new Notifier(context, null, 0, What.UPDATE);
    }

    /** Creates the notification channel of birthdays, necessary only on API 26+. */
    public static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, context.getText(R.string.birthdays),
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(context.getString(R.string.birthday_notified_midday));
            context.getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    /**
     * Birthdays of the tree are notified by the single alarm of {@link ReminderScheduler}:
     * Android allows at most 500 alarms per app, too few for big trees, and setting them at every save was slow.
     */
    void createAlarms(Context context, Settings.Tree tree) {
        ReminderScheduler.reschedule(context);
    }

    /**
     * Cancels once the alarms set by versions before 1.6, one per birthday, of all trees.
     */
    public static void migrateLegacyAlarms(Context context) {
        if (Global.settings == null || Global.settings.birthdayAlarmsMigrated) return;
        for (Settings.Tree tree : Global.settings.trees)
            cancelLegacyAlarms(context, tree);
        Global.settings.birthdayAlarmsMigrated = true;
        Global.settings.save();
    }

    /** Recomputes and saves the birthdays of a tree. */
    public static void refreshBirthdays(Context context, Gedcom gedcom, int treeId) {
        new Notifier(context, gedcom, treeId, What.CREATE);
    }

    /** The birthdays saved for the tree can be used (see {@link Settings.Birthday#isValid()}). */
    public static boolean birthdaysValid(Settings.Tree tree) {
        if (tree.birthdays == null) return false;
        for (Settings.Birthday birthday : tree.birthdays)
            if (!birthday.isValid()) return false;
        return true;
    }

    // Deletes all alarms already set for a tree: only those of versions before 1.6 can exist
    void deleteAlarms(Context context, Settings.Tree tree) {
        if (!Global.settings.birthdayAlarmsMigrated) cancelLegacyAlarms(context, tree);
    }

    private static void cancelLegacyAlarms(Context context, Settings.Tree tree) {
        if (tree.birthdays == null) return;
        int eventId = tree.id * LEGACY_FACTOR;
        AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        for (Settings.Birthday b : tree.birthdays) {
            Intent intent = new Intent(context, NotifyReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, eventId++, intent,
                    // Flags also need to be identical to alarm creator
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_CANCEL_CURRENT);
            alarmManager.cancel(pendingIntent);
        }
    }
}
