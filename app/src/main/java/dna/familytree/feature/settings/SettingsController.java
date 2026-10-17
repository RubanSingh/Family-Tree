package dna.familytree.feature.settings;

import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.LocaleList;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textview.MaterialTextView;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import dna.familytree.BaseController;
import dna.familytree.BuildConfig;
import dna.familytree.Global;
import dna.familytree.Notifier;
import dna.familytree.R;
import dna.familytree.billing.PremiumPlan;
import dna.familytree.designsystem.SettingsRowView;
import dna.familytree.feature.ads.ConsentManager;
import dna.familytree.feature.backup.BackupController;
import dna.familytree.feature.backup.BackupManager;
import dna.familytree.feature.billing.PremiumGate;
import dna.familytree.feature.reminders.ReminderScheduler;
import dna.familytree.feature.update.InAppUpdates;
import dna.familytree.theme.ThemeController;
import dna.familytree.util.AnalyticsUtil;
import dna.familytree.util.AppClientUtils;
import dna.familytree.util.LoggerUtils;
import dna.familytree.util.MemoryUtil;

/**
 * App settings, grouped into General / Appearance / Advanced / Support,
 * with a Premium banner on top.
 */
public class SettingsController extends BaseController {

    private static final String TAG = SettingsController.class.getSimpleName();
    private static final String NOTIFY_TIME_PICKER = "notifyTime";
    List<Language> languages;
    private InAppUpdates updates;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.settings);
        setToolbar();

        // ---- General
        SettingsRowView save = findViewById(R.id.opzioni_salva);
        save.setCheckedSilently(Global.settings.autoSave);
        save.setOnCheckedChangeListener(active -> {
            AnalyticsUtil.logEventSettingsAutoSave(getFirebaseAnalytics());
            Global.settings.autoSave = active;
            Global.settings.save();
            return null;
        });

        SettingsRowView load = findViewById(R.id.opzioni_carica);
        load.setCheckedSilently(Global.settings.loadTree);
        load.setOnCheckedChangeListener(active -> {
            AnalyticsUtil.logEventSettingsLastLoad(getFirebaseAnalytics());
            Global.settings.loadTree = active;
            Global.settings.save();
            return null;
        });

        // ---- Appearance
        SettingsRowView darkTheme = findViewById(R.id.dark_theme);
        darkTheme.setCheckedSilently(MemoryUtil.getInstance(this).isDarkThemeEnabled(this));
        darkTheme.setOnCheckedChangeListener(active -> {
            MemoryUtil.getInstance(this).setDarkThemeEnabled(this, active);
            ((Global)getApplicationContext()).applyDarkMode();
            AnalyticsUtil.logEventDarkTheme(getFirebaseAnalytics());
            return null;
        });

        SettingsRowView theme = findViewById(R.id.theme);
        theme.setOnClickListener(view -> {
            if (PremiumGate.isPremium()) {
                startActivity(new Intent(this, ThemeController.class));
            } else {
                AnalyticsUtil.logEventPremiumAction(getFirebaseAnalytics());
                PremiumGate.open(this, R.string.theme);
            }
        });

        bindLanguagePicker();

        // ---- Advanced
        SettingsRowView expert = findViewById(R.id.opzioni_esperto);
        expert.setCheckedSilently(Global.settings.expert);
        expert.setOnCheckedChangeListener(active -> {
            AnalyticsUtil.logEventSettingsExpertMode(getFirebaseAnalytics());
            if (PremiumGate.isPremium()) {
                Global.settings.expert = active;
                Global.settings.save();
            } else {
                expert.setCheckedSilently(false);
                AnalyticsUtil.logEventPremiumAction(getFirebaseAnalytics());
                PremiumGate.open(this, R.string.show_advanced_functions);
            }
            return null;
        });

        // ---- Birthday notification time
        findViewById(R.id.notify_time).setOnClickListener(view -> pickNotifyTime());
        bindNotifyTime();
        // A picker restored after rotation gets back its listener
        MaterialTimePicker restoredPicker = (MaterialTimePicker)getSupportFragmentManager().findFragmentByTag(NOTIFY_TIME_PICKER);
        if (restoredPicker != null) listenNotifyTime(restoredPicker);

        // ---- Local backup
        findViewById(R.id.local_backup).setOnClickListener(view -> startActivity(new Intent(this, BackupController.class)));
        bindPremiumState();

        // ---- Updates from Google Play
        updates = new InAppUpdates(this);
        SettingsRowView updateRow = findViewById(R.id.check_updates);
        updateRow.setSubtitle(getString(R.string.update_version, BuildConfig.VERSION_NAME));
        updateRow.setOnClickListener(view -> updates.checkNow(result -> {
            if (result == InAppUpdates.CheckResult.UP_TO_DATE)
                Toast.makeText(this, R.string.update_latest, Toast.LENGTH_SHORT).show();
            else if (result == InAppUpdates.CheckResult.UNAVAILABLE)
                Toast.makeText(this, R.string.update_unavailable, Toast.LENGTH_LONG).show();
            return null;
        }));

        // ---- Support
        findViewById(R.id.rate_app).setOnClickListener(view -> AppClientUtils.rateApp(this, getPackageName()));
        findViewById(R.id.feedback).setOnClickListener(view -> AppClientUtils.setFeedback(this));
        findViewById(R.id.share).setOnClickListener(view -> AppClientUtils.shareAppLink(this));
        findViewById(R.id.privacy_options).setOnClickListener(view -> ConsentManager.showPrivacyOptions(this));
        findViewById(R.id.opzioni_lapide).setOnClickListener(view -> {
            AnalyticsUtil.logEventSettingsAbout(getFirebaseAnalytics());
            startActivity(new Intent(this, AboutController.class));
        });
    }

    private void bindPremiumBanner() {
        MaterialCardView banner = findViewById(R.id.settings_premium_banner);
        MaterialTextView title = findViewById(R.id.settings_premium_title);
        MaterialTextView subtitle = findViewById(R.id.settings_premium_subtitle);
        View cta = findViewById(R.id.settings_premium_cta);
        if (PremiumGate.isPremium()) {
            PremiumPlan plan = PremiumPlan.Companion.fromKey(Global.settings.premiumPlan);
            title.setText(plan == PremiumPlan.MONTHLY ? R.string.premium_status_monthly : R.string.premium_status_lifetime);
            subtitle.setText(R.string.premium_status_active);
            cta.setVisibility(View.GONE);
            banner.setOnClickListener(null);
            banner.setClickable(false);
        } else {
            title.setText(R.string.go_premium);
            subtitle.setText(R.string.go_premium_desc);
            cta.setVisibility(View.VISIBLE);
            View.OnClickListener open = v -> {
                AnalyticsUtil.logEventPremiumAction(getFirebaseAnalytics());
                PremiumGate.open(this, R.string.family_tree_premium);
            };
            banner.setOnClickListener(open);
            cta.setOnClickListener(open);
        }
    }

    private void bindLanguagePicker() {
        languages = new ArrayList<>();
        languages.add(new Language(null, 0)); // System language
        try { // Gets languages from locales_config.xml
            XmlPullParser xpp = getResources().getXml(R.xml.locales_config);
            while (xpp.getEventType() != XmlPullParser.END_DOCUMENT) {
                if (xpp.getEventType() == XmlPullParser.START_TAG && xpp.getName().equals("locale")) {
                    String percent = xpp.getAttributeValue(null, "percent");
                    languages.add(new Language(xpp.getAttributeValue(0), percent == null ? 100 : Integer.parseInt(percent)));
                }
                xpp.next();
            }
        } catch (Exception e) {
            LoggerUtils.ErrorLog(TAG, "Exception reading locales_config", e);
        }
        Collections.sort(languages);

        SettingsRowView row = findViewById(R.id.opzioni_language);
        Language actual = getActualLanguage();
        row.setValue(actual.toString());
        String[] languageArray = new String[languages.size()];
        for (int i = 0; i < languages.size(); i++) {
            languageArray[i] = languages.get(i).toString();
        }
        row.setOnClickListener(view -> new MaterialAlertDialogBuilder(view.getContext())
                .setTitle(R.string.language)
                .setSingleChoiceItems(languageArray, languages.indexOf(actual), (dialog, item) -> {
                    AnalyticsUtil.logEventSettingsLanguage(getFirebaseAnalytics());
                    String code = languages.get(item).code;
                    // Sets app locale and store it for the future
                    LocaleListCompat appLocale = LocaleListCompat.forLanguageTags(code);
                    AppCompatDelegate.setApplicationLocales(appLocale);
                    // Updates app context configuration for this session only
                    Configuration configuration = getResources().getConfiguration();
                    if (code != null) {
                        configuration.setLocale(new Locale(code));
                    } else { // Takes the system locale
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            // Finds the first system locale supported by this app
                            Locale firstSupportedLocale = new Locale("en"); // English default language
                            LocaleList systemLocales = Resources.getSystem().getConfiguration().getLocales();
                            for (int i = 0; i < systemLocales.size(); i++) {
                                Locale sysLoc = systemLocales.get(i);
                                String tag = sysLoc.toLanguageTag();
                                if (languages.stream().anyMatch(lang -> lang.code != null && tag.startsWith(lang.code))) {
                                    firstSupportedLocale = new Locale(tag.substring(0, 2)); // Just the 2 chars language code
                                    break;
                                }
                            }
                            configuration.setLocale(firstSupportedLocale);
                        } else {
                            configuration.setLocale(Resources.getSystem().getConfiguration().locale);
                        }
                    }
                    getApplicationContext().getResources().updateConfiguration(configuration, null);
                    dialog.dismiss();
                    if (code == null) recreate();
                }).show());
    }

    /**
     * Returns the actual Language of the app, otherwise the "system language"
     */
    private Language getActualLanguage() {
        Locale firstLocale = AppCompatDelegate.getApplicationLocales().get(0);
        if (firstLocale != null) {
            for (int i = 1; i < languages.size(); i++) {
                Language language = languages.get(i);
                if (firstLocale.toString().startsWith(language.code)) return language;
            }
        }
        return languages.get(0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        AnalyticsUtil.logEventSettingsActivity(getFirebaseAnalytics());
        if (Global.forceRestartSettings) {
            recreate();
            Global.forceRestartSettings = false;
        } else {
            bindPremiumState(); // Premium may have been bought from the paywall opened by this screen
        }
    }

    /** Everything that changes with Premium. */
    private void bindPremiumState() {
        boolean premium = PremiumGate.isPremium();
        // Ad privacy choices: free users in regions where the consent can be changed (EEA, UK, Switzerland, US states)
        findViewById(R.id.privacy_options).setVisibility(!premium && ConsentManager.isPrivacyOptionsRequired(this) ? View.VISIBLE : View.GONE);
        bindPremiumBanner();
        SettingsRowView theme = findViewById(R.id.theme);
        theme.setTrailing(premium ? SettingsRowView.Trailing.CHEVRON : SettingsRowView.Trailing.LOCK);
        SettingsRowView expert = findViewById(R.id.opzioni_esperto);
        expert.setSubtitle(premium ? R.string.show_advanced_functions_desc : R.string.unlock_function);
        bindPremiumMarkers();
        bindBackupRow();
    }

    /** Birthday notification time, in the device's 12/24-hour format. */
    private void bindNotifyTime() {
        int[] time = Notifier.notifyTime();
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, time[0]);
        calendar.set(Calendar.MINUTE, time[1]);
        SettingsRowView row = findViewById(R.id.notify_time);
        row.setValue(DateFormat.getTimeFormat(this).format(calendar.getTime()));
    }

    private void pickNotifyTime() {
        int[] time = Notifier.notifyTime();
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(DateFormat.is24HourFormat(this) ? TimeFormat.CLOCK_24H : TimeFormat.CLOCK_12H)
                .setHour(time[0]).setMinute(time[1])
                .setTitleText(R.string.notification_time)
                .build();
        listenNotifyTime(picker);
        picker.show(getSupportFragmentManager(), NOTIFY_TIME_PICKER);
    }

    private void listenNotifyTime(MaterialTimePicker picker) {
        picker.addOnPositiveButtonClickListener(v -> {
            Global.settings.notifyTime = String.format(Locale.US, "%02d:%02d", picker.getHour(), picker.getMinute());
            Notifier.updateNotifyTime(this); // Moves the scheduled birthdays and saves the settings
            ReminderScheduler.reschedule(this);
            bindNotifyTime();
        });
    }

    /** Local backup row: Off, no folder yet, or the folder in use. */
    private void bindBackupRow() {
        SettingsRowView row = findViewById(R.id.local_backup);
        if (!PremiumGate.isPremium())
            row.setSubtitle(R.string.backup_premium);
        else if (!Global.settings.backup)
            row.setSubtitle(R.string.backup_off);
        else if (BackupManager.folderUri() == null)
            row.setSubtitle(R.string.backup_no_folder);
        else
            row.setSubtitle(BackupManager.folderLabel());
    }

    /** Gold "Premium" badge and gold icon on the rows that need Premium, until the user has it. */
    private void bindPremiumMarkers() {
        boolean locked = !PremiumGate.isPremium();
        SettingsRowView.IconStyle style = locked ? SettingsRowView.IconStyle.PREMIUM : SettingsRowView.IconStyle.NEUTRAL;
        for (int id : new int[]{R.id.theme, R.id.opzioni_esperto}) {
            SettingsRowView row = findViewById(id);
            row.setPremiumBadgeVisible(locked);
            row.setIconStyle(style);
        }
    }

    private class Language implements Comparable<Language> {
        String code;
        int percent;

        public Language(String code, int percent) {
            this.code = code;
            this.percent = percent;
        }

        @NonNull
        @Override
        public String toString() {
            if (code == null) {
                // Returns the string "System language" on the system locale, not on the app locale
                Configuration config = new Configuration(getResources().getConfiguration());
                config.setLocale(Resources.getSystem().getConfiguration().locale); // TODO: on API 33 is the app locale instead of the system locale
                return createConfigurationContext(config).getText(R.string.system_language).toString();
            } else {
                Locale locale = new Locale(code);
                String txt = locale.getDisplayLanguage(locale);
                txt = txt.substring(0, 1).toUpperCase() + txt.substring(1);
                if (percent < 100) {
                    txt += " (" + percent + "%)";
                }
                return txt;
            }
        }

        @Override
        public int compareTo(Language lang) {
            if (lang.code == null) {
                return 1;
            }
            return toString().compareTo(lang.toString());
        }
    }
}
