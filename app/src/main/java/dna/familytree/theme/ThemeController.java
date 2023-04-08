package dna.familytree.theme;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.common.primitives.Ints;

import dna.familytree.BaseController;
import dna.familytree.Global;
import dna.familytree.R;
import dna.familytree.util.AnalyticsUtil;
import dna.familytree.util.MemoryUtil;

/**
 * A simple {@link Fragment} subclass.
 */
public class ThemeController extends BaseController implements ThemeSelectionInterface {

    private static final String TAG = ThemeController.class.getSimpleName();
    protected RecyclerView recyclerThemes;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_theme);

        setToolbar();

        int selectedThemeColor = MemoryUtil.getInstance(getApplicationContext()).getApplicationTheme(this);
        int[] themeArray = getResources().getIntArray(R.array.default_color_choice_values);
        int selectedIndex = Ints.indexOf(themeArray, selectedThemeColor);
        int[] secondaryArray = getResources().getIntArray(R.array.secondary_color_choice_values);
        String[] themeNames = getResources().getStringArray(R.array.default_theme_names);
        recyclerThemes = findViewById(R.id.recycler_themes);
        recyclerThemes.setLayoutManager(new LinearLayoutManager(getBaseContext()));
        ThemeAdapter themeAdapter = new ThemeAdapter(themeArray, secondaryArray, themeNames, selectedIndex, this);
        recyclerThemes.setAdapter(themeAdapter);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem i) {
        onBackPressed();
        return true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onThemeSelected(int themeColor, boolean darkMode, boolean isPremiumTheme) {
        changeTheme(darkMode);
        MemoryUtil.getInstance(getApplicationContext()).getSharedPreferences().edit()
                .putInt(getString(R.string.theme_key), themeColor).apply();
    }

    private void changeTheme(boolean isDarkMode) {

        Global.forceRestartSettings = true;
        Global.forceRestartLauncher = true;

        // Log Event to Analytics
        AnalyticsUtil.logEventChangeApplicationTheme(getFirebaseAnalytics());

        recreate();
    }
}
