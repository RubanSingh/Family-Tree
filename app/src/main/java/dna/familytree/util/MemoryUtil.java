package dna.familytree.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import dna.familytree.R;

public class MemoryUtil {

    private static final String TAG = MemoryUtil.class.getSimpleName();

    private final SharedPreferences mSharedPreferences;
    private static volatile MemoryUtil INSTANCE;

    private MemoryUtil(Context application) {
        mSharedPreferences = android.preference.PreferenceManager
                .getDefaultSharedPreferences(application.getApplicationContext());
    }

    public static MemoryUtil getInstance(final Context applicationContext) {
        if (INSTANCE == null) {
            synchronized (MemoryUtil.class) {
                if (INSTANCE == null) {
                    Log.d(TAG, "Creating new MemoryUtil instance..");
                    INSTANCE = new MemoryUtil(applicationContext);
                }
            }
        }
        return INSTANCE;
    }

    // getting application theme preference
    public int getApplicationTheme(Context context) {
        return mSharedPreferences.getInt(context.getString(R.string.theme_key),
                context.getResources().getColor(R.color.md_indigo_700));
    }

    public SharedPreferences getSharedPreferences() {
        return mSharedPreferences;
    }

    // getting application dark theme preference
    public boolean isDarkThemeEnabled(Context context) {
        return mSharedPreferences.getBoolean(context.getString(R.string.dark_key), true);
    }

    public void setDarkThemeEnabled(Context context,boolean isDarkMode) {
        MemoryUtil.getInstance(context).getSharedPreferences().edit()
                .putBoolean(context.getString(R.string.dark_key), isDarkMode).apply();
    }
}