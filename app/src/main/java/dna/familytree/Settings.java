
package dna.familytree;

import android.widget.Toast;

import com.google.gson.Gson;

import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import dna.familytree.util.LoggerUtils;

/**
 * Represents the app preferences saved in '/data/data/app.familygem/files/settings.json'.
 */
public class Settings {

    private static final String TAG = Settings.class.getSimpleName();
    /**
     * It is "start" as soon as the app is installed (when 'settings.json' doesn't exist).
     * If the installation comes from a sharing it receives a 'dateId' as "20203112005959" to download the shared tree.
     * Then it becomes null and remains null forever.
     * If 'settings.json' is deleted it re-becomes "start" and then immediately null.
     */
    String referrer;
    public List<Tree> trees;
    /**
     * Number of the tree currently opened. '0' means not any particular tree.
     * Must be consistent with the {@link Global#gc} opened tree.
     * It is not reset by closing the tree, to load last opened tree at startup.
     */
    public int openTree;
    public boolean autoSave;
    /**
     * At startup load last opened tree.
     */
    public boolean loadTree;

    /**
     * Used to display or hide all advanced tools.
     */
    public boolean expert;
    public boolean shareAgreement;
    public boolean premium;
    /**
     * Automatic local backup of the trees into a folder chosen by the user (introduced in version 1.6, works with Premium).
     * Settings saved before don't have {@link #backupUri}: BackupManager.init() then switches this on, without a folder,
     * so nothing is backed up until a folder is chosen.
     */
    public boolean backup;
    /** The backup folder chosen with the system picker, or BackupManager.NO_URI. */
    public String backupUri;
    /** The user closed the "choose a backup folder" card on the trees screen. */
    public boolean backupPromptDismissed;
    /** Time of birthday notifications as "HH:mm" (introduced in version 1.6). Null means midday, as before. */
    public String notifyTime;
    /** Wedding anniversaries are not notified. Negative, so that settings saved before keep the default (notified, with Premium). */
    public boolean anniversaryAlertsOff;
    /** Remembrance days (anniversaries of death) are notified, with Premium. Off by default. */
    public boolean remembranceAlerts;
    /** Last time the reminders were checked, so that none is missed or notified twice. */
    public long remindersCheckedAt;
    /** The one alarm per birthday of versions before 1.6 has been cancelled: birthdays are now notified by ReminderScheduler. */
    public boolean birthdayAlarmsMigrated;
    /**
     * Which plan granted Premium: "MONTHLY" or "LIFETIME" (see dna.familytree.billing.PremiumPlan).
     * Null for legacy buyers until the next reconcile with Google Play.
     */
    public String premiumPlan;
    public String premiumToken;
    public long premiumSince;
    /**
     * First launch of version 1.6 or later. 0 in settings saved by older versions: see {@link #legacyUser}.
     */
    public long installedAt;
    /**
     * The app was installed before version 1.6, when Premium started to include features that were free until then
     * (export of the diagram as PDF): they stay free for these users (see PremiumGate.isPremiumOrLegacy()).
     */
    public boolean legacyUser;
    Diagram diagram;

    /**
     * Initializes first boot values.
     * False booleans don't need to be initialized.
     */
    void init() {
        referrer = "start";
        trees = new ArrayList<>();
        autoSave = true;
        diagram = new Diagram().init();
        installedAt = System.currentTimeMillis();
    }

    /**
     * Number of persons in all the trees, as counted at the last save of each tree.
     */
    public int totalPersons() {
        int total = 0;
        if (trees != null)
            for (Tree tree : trees)
                total += tree.persons;
        return total;
    }

    /**
     * Highest number of generations among the trees.
     */
    public int maxGenerations() {
        int max = 0;
        if (trees != null)
            for (Tree tree : trees)
                max = Math.max(max, tree.generations);
        return max;
    }

    public int max() {
        int num = 0;
        for (Tree tree : trees) {
            if (tree.id > num)
                num = tree.id;
        }
        return num;
    }

    public void addTree(Tree tree) {
        trees.add(tree);
    }

    void aggiungi(Tree tree) {
        trees.add(tree);
    }

    void rinomina(int id, String nuovoNome) {
        for (Tree tree : trees) {
            if (tree.id == id) {
                tree.title = nuovoNome;
                break;
            }
        }
        save();
    }

    void deleteTree(int id) {
        for (Tree tree : trees) {
            if (tree.id == id) {
                trees.remove(tree);
                break;
            }
        }
        if (id == openTree) {
            openTree = 0;
        }
        save();
    }

    public void save() {
        try {
            Gson gson = new Gson();
            String json = gson.toJson(this);
            FileUtils.writeStringToFile(new File(Global.context.getFilesDir(), "settings.json"), json, "UTF-8");
        } catch (Exception e) {
            LoggerUtils.ErrorLog(TAG, "Exception in save", e);
            Toast.makeText(Global.context, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // The tree currently open
    public Tree getCurrentTree() {
        for (Tree alb : trees) {
            if (alb.id == openTree)
                return alb;
        }
        return null;
    }

    public Tree getTree(int treeId) {
        /* Da quando ho installato Android Studio 4.0, quando compilo con minifyEnabled true
           misteriosamente 'alberi' qui è null.
           Però non è null se DOPO c'è 'trees = Global.settings.trees'
           Davvero incomprensibile!
        */
        if (trees == null) {
            trees = Global.settings.trees;
        }
        if (trees != null)
            for (Tree tree : trees) {
                if (tree.id == treeId) {
                    if (tree.uris == null) // traghettatore inserito in Family Gem 0.7.15
                        tree.uris = new LinkedHashSet<>();
                    return tree;
                }
            }
        return null;
    }

    static class Diagram {
        int ancestors;
        int uncles;
        int descendants;
        int siblings;
        int cousins;
        boolean spouses;
        /** Little cards with the number of hidden ancestors/descendants (always shown before version 1.6). */
        boolean numbers;
        /** Lines connecting a person displayed more than once (introduced in version 1.6). */
        boolean duplicates;

        // Default values
        Diagram init() {
            ancestors = 3;
            uncles = 2;
            descendants = 3;
            siblings = 2;
            cousins = 1;
            spouses = true;
            numbers = true;
            duplicates = false;
            return this;
        }
    }

    public static class Tree {
        public int id;
        public String title;
        public Set<String> dirs;
        public Set<String> uris;
        int persons;
        public int generations;
        int media;
        public String root;
        public List<Share> shares; // Dati identificativi delle condivisioni attraverso il tempo e lo spazio
        public String shareRoot; // Id della Person radice dell'albero in Condivisione
        /**
         * Sharing grade:
         * <ol>
         *     <li value="0">Albero creato da zero in Italia.
         *     Rimane 0 anche aggiungendo il submitter principale, condividendolo e ricevendo novità.
         *     <li value="9">Albero spedito per la condivisione in attesa di marchiare con 'passato' tutti i submitter.
         *     <li value="10">Albero ricevuto tramite condivisione in Australia. Non potrà mai più ritornare 0.
         *     <li value="20">Albero ritornato in Italia dimostratosi un derivato da uno zero (o da un 10).
         *     Solo se è 10 può diventare 20. Se per caso perde lo status di derivato ritorna 10 (mai 0).
         *     <li value="30">Albero derivato da cui sono state estratte tutte le novità OPPURE privo di novità già all'arrivo (grigio).
         *     Può essere eliminato.
         * </ol>
         */
        public int grade;
        Set<Birthday> birthdays;
        /** Included in the local backup (introduced in version 1.6). */
        public boolean backup;
        /** Tree-level settings. Null on trees saved before version 1.6, which means the defaults: see {@link #getTreeSettings()}. */
        public TreeSettings settings;
        /** Reminders added by the user (introduced in version 1.6). Null if none. */
        public List<Reminder> reminders;
        /** Wedding anniversaries and remembrance days found in the tree, refreshed when it's saved. Null if none. */
        public List<Occasion> occasions;

        public Tree(int id, String title, String dir, int persons, int generations, String root, List<Share> shares, int grade) {
            this.id = id;
            this.title = title;
            dirs = new LinkedHashSet<>();
            if (dir != null)
                dirs.add(dir);
            uris = new LinkedHashSet<>();
            this.persons = persons;
            this.generations = generations;
            this.root = root;
            this.shares = shares;
            this.grade = grade;
            birthdays = new HashSet<>();
            backup = true;
        }

        public int getPersons() {
            return persons;
        }

        /** Next birthdays of the living people, saved when the tree is saved. May be null. */
        public Set<Birthday> getBirthdays() {
            return birthdays;
        }

        /** Settings of this tree, or the defaults (same behaviour as before tree settings existed). Never null. */
        public TreeSettings getTreeSettings() {
            return settings != null ? settings : new TreeSettings();
        }

        public void aggiungiCondivisione(Share share) {
            if (shares == null)
                shares = new ArrayList<>();
            shares.add(share);
        }
    }

    /**
     * Settings at tree level, saved for each tree and inside its backup ZIP.
     * Defaults reproduce the behaviour of the app before they existed.
     */
    public static class TreeSettings {
        public static final int DEFAULT_LIFE_SPAN = 120;
        /** A person without a recorded death is considered living only up to this age. */
        public int lifeSpan = DEFAULT_LIFE_SPAN;
        /** True to compute ages at {@link #fixedDate} instead of today. */
        public boolean customDate;
        /** ISO date 'yyyy-MM-dd', used when {@link #customDate} is true. */
        public String fixedDate;
    }

    /**
     * An event to remember, added by the user: a baby shower, a reunion, a birthday not in the tree...
     */
    public static class Reminder {
        /** Unique, also among trees. */
        public long id;
        /** One of {@code BIRTHDAY, ANNIVERSARY, BABY_SHOWER, WEDDING, GRADUATION, REUNION, MEMORIAL, OTHER}. */
        public String kind;
        public String title;
        /** The person it's about, or null. */
        public String personId;
        /** ISO date 'yyyy-MM-dd'. */
        public String date;
        /** 'HH:mm', or null for the time of birthday notifications. */
        public String time;
        /** Repeats every year on the same day. */
        public boolean yearly;
        /** Notified also this number of days before (0 = only on the day). */
        public int daysBefore;
        public String note;
    }

    /**
     * A yearly event found in the tree: wedding anniversary ('MARR') or remembrance day ('DEAT').
     */
    public static class Occasion {
        public String kind;
        /** Family or person ID. */
        public String id;
        /** The person to open from the notification. */
        public String personId;
        public String name;
        public int month; // 1-12
        public int day;
        /** 0 if unknown. */
        public int year;
    }

    // The essential data of a share
    public static class Share {
        String dateId; // on compressed date and time format: YYYYMMDDhhmmss
        public String submitter; // Submitter id

        public Share(String dateId, String submitter) {
            this.dateId = dateId;
            this.submitter = submitter;
        }
    }

    // Birthday of one person
    public static class Birthday {
        String id; // E.g. 'I123'
        String given; // 'John'
        String name; // 'John Doe III'
        long date; // Date of next birthday in Unix time
        int age; // Turned years

        public Birthday(String id, String given, String name, long date, int age) {
            this.id = id;
            this.given = given;
            this.name = name;
            this.date = date;
            this.age = age;
        }

        public String getId() {
            return id;
        }

        public String getGiven() {
            return given;
        }

        public String getName() {
            return name;
        }

        public long getDate() {
            return date;
        }

        public int getAge() {
            return age;
        }

        /**
         * Read correctly from the settings: in release builds before 1.6 the field names were obfuscated,
         * so entries saved by an older version may come back empty.
         */
        public boolean isValid() {
            return id != null && name != null && date > 0 && age >= 0;
        }

        @Override
        public String toString() {
            DateFormat sdf = new SimpleDateFormat("d MMM y", Locale.US);
            return "[" + name + ": " + age + " (" + sdf.format(date) + ")]";
        }
    }

    /**
     * Model of the file 'settings.json' inside a backup, shared or example ZIP file.
     * It contains basic info of the zipped tree.
     */
    static class ZippedTree {
        String title;
        int persons;
        int generations;
        String root;
        List<Share> shares;
        /**
         * Coming from {@link Tree#grade}.
         */
        int grade;
        /** Coming from {@link Tree#settings}; null in ZIPs made before version 1.6. */
        TreeSettings settings;
        /** Coming from {@link Tree#reminders}. */
        List<Reminder> reminders;

        ZippedTree(String title, int persons, int generations, String root, List<Share> shares, int grade) {
            this.title = title;
            this.persons = persons;
            this.generations = generations;
            this.root = root;
            this.shares = shares;
            this.grade = grade;
        }

        File save() {
            return save(Global.context.getCacheDir());
        }

        /** Writes 'settings.json' in the given folder (background backups use their own folder). */
        File save(File folder) {
            File settingsFile = new File(folder, "settings.json");
            Gson gson = new Gson();
            String json = gson.toJson(this);
            try {
                FileUtils.writeStringToFile(settingsFile, json, "UTF-8");
            } catch (Exception e) {
                LoggerUtils.ErrorLog(TAG, "Exception in save", e);
            }
            return settingsFile;
        }
    }
}
