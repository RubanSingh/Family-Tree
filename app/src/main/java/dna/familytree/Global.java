package dna.familytree;

import android.content.Context;
import android.content.res.Configuration;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.multidex.MultiDexApplication;

import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.material.color.DynamicColors;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.gson.Gson;

import org.apache.commons.io.FileUtils;
import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.model.Media;

import java.io.File;
import java.util.Locale;

import dna.familytree.util.LoggerUtils;
import dna.familytree.util.MemoryUtil;

public class Global extends MultiDexApplication {

    private static final String TAG = Global.class.getSimpleName();
    public static boolean forceRestartSettings;
    public static boolean forceRestartLauncher;
    private FirebaseAnalytics mFirebaseAnalytics;
    public static Gedcom gc;
    public static Context context;
    public static Settings settings;
    public static String indi; // ID of the selected person displayed across the app
    /**
     * Which parents' family to show in the diagram, usually 0.
     */
    public static int familyNum;
    static View mainView;
    public static boolean edited; // There has been an editing in ProfileActivity or in DetailActivity and therefore the content of the previous pages must be updated
    static boolean shouldSave; // The Gedcom content has been changed and needs to be saved
    /**
     * Path where a camera app puts the taken photo.
     */
    public static String pathOfCameraDestination;
    public static Media croppedMedia; // Temporary parking of the Media in the cropping process
    public static Gedcom gc2; // A shared tree, for comparison of updates
    public static int treeId2; // ID of the shared tree

    private static MemoryUtil memoryUtil;

    public static MemoryUtil getMemoryUtil() {
        return memoryUtil;
    }

    /**
     * This is called when the application starts, and also when it is restarted.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();

        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        memoryUtil = MemoryUtil.getInstance(this);

        //apply dynamic theme colors
        DynamicColors.applyToActivitiesIfAvailable(this);

        //Call the function to initialize AdMob SDK
        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(@NonNull InitializationStatus initializationStatus) {
            }
        });

        applyDarkMode();
        start(context);
    }

    public FirebaseAnalytics getFirebaseInstance() {
        return mFirebaseAnalytics;
    }

    public static void start(Context context) {
        File settingsFile = new File(context.getFilesDir(), "settings.json");
        // Renames "preferenze.json" to "settings.json" (introduced in version 0.8)
        File preferencesFile = new File(context.getFilesDir(), "preferenze.json");
        if (preferencesFile.exists() && !settingsFile.exists()) {
            if (!preferencesFile.renameTo(settingsFile)) {
                Toast.makeText(context, R.string.something_wrong, Toast.LENGTH_LONG).show();
                settingsFile = preferencesFile;
            }
        }
        try {
            String jsonString = FileUtils.readFileToString(settingsFile, "UTF-8");
            jsonString = updateSettings(jsonString);
            Gson gson = new Gson();
            settings = gson.fromJson(jsonString, Settings.class);
        } catch (Exception e) {
            LoggerUtils.ErrorLog(TAG, "Exception in start", e);
            // At first boot avoids to show the toast saying that settings.json doesn't exist
            if (!(e instanceof java.io.FileNotFoundException)) {
                Toast.makeText(context, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
        }
        if (settings == null) {
            settings = new Settings();
            settings.init();
            // Restores possibly lost trees
            for (File file : context.getFilesDir().listFiles()) {
                String name = file.getName();
                if (file.isFile() && name.endsWith(".json")) {
                    try {
                        int treeId = Integer.parseInt(name.substring(0, name.lastIndexOf(".json")));
                        File mediaDir = new File(context.getExternalFilesDir(null), String.valueOf(treeId));
                        settings.trees.add(new Settings.Tree(treeId, String.valueOf(treeId),
                                mediaDir.exists() ? mediaDir.getPath() : null,
                                0, 0, null, null, 0));
                    } catch (Exception e) {
                        LoggerUtils.ErrorLog(TAG, "Exception in start", e);
                    }
                }
            }
            // Some tree has been restored
            if (!settings.trees.isEmpty())
                settings.referrer = null;
            settings.save();
        }
        // Diagram settings were (probably) introduced in version 0.7.4
        if (settings.diagram == null) {
            settings.diagram = new Settings.Diagram().init();
            settings.save();
        }
    }

    /**
     * Modifications to the text coming from files/settings.json
     */
    private static String updateSettings(String json) {
        // Version 0.8 added new settings for the diagram
        return json
                .replace("\"siblings\":true", "siblings:2,cousins:2,spouses:true")
                .replace("\"siblings\":false", "siblings:0,cousins:0,spouses:true")

                // Italian translated to English (version 0.8)
                .replace("\"alberi\":", "\"trees\":")
                .replace("\"alberi\":", "\"trees\":")
                .replace("\"idAprendo\":", "\"openTree\":")
                .replace("\"autoSalva\":", "\"autoSave\":")
                .replace("\"caricaAlbero\":", "\"loadTree\":")
                .replace("\"esperto\":", "\"expert\":")
                .replace("\"nome\":", "\"title\":")
                .replace("\"cartelle\":", "\"dirs\":")
                .replace("\"individui\":", "\"persons\":")
                .replace("\"generazioni\":", "\"generations\":")
                .replace("\"radice\":", "\"root\":")
                .replace("\"condivisioni\":", "\"shares\":")
                .replace("\"radiceCondivisione\":", "\"shareRoot\":")
                .replace("\"grado\":", "\"grade\":")
                .replace("\"data\":", "\"dateId\":");
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // Keep the app locale if system language is changed while the app is running
        Locale appLocale = AppCompatDelegate.getApplicationLocales().get(0);
        if (appLocale != null) {
            Locale.setDefault(appLocale); // Keep the gedcom.jar library locale
            newConfig.setLocale(appLocale);
            getApplicationContext().getResources().updateConfiguration(newConfig, null); // Keep global context
        }
        super.onConfigurationChanged(newConfig);
    }

    public void applyDarkMode() {
        boolean isDarkTheme = getMemoryUtil().isDarkThemeEnabled(getApplicationContext());
        if (isDarkTheme) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }
}
