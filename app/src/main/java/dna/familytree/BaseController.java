package dna.familytree;

import android.app.ActivityManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.common.primitives.Ints;
import com.google.firebase.analytics.FirebaseAnalytics;

import dna.familytree.util.LoggerUtils;
import dna.familytree.util.MemoryUtil;

public class BaseController extends AppCompatActivity {

    private static final String TAG = BaseController.class.getSimpleName();
    public static final int DEFAULT_THEME = 7;
    protected int[] light_themeArray = new int[]{R.style.RedTheme, R.style.PinkTheme,
            R.style.PurpleTheme, R.style.DeepPurpleTheme, R.style.IndigoTheme,
            R.style.BlueTheme, R.style.LightBlueTheme, R.style.CyanTheme,
            R.style.TealTheme, R.style.GreenTheme, R.style.LightGreenTheme,
            R.style.LimeTheme, R.style.YellowTheme, R.style.AmberTheme,
            R.style.OrangeTheme, R.style.DeepOrangeTheme, R.style.BrownTheme,
            R.style.GreyTheme, R.style.BlueGreyTheme, R.style.DarkTheme};

    protected int[] dark_themeArray = new int[]{R.style.RedTheme_Dark, R.style.PinkTheme_Dark,
            R.style.PurpleTheme_Dark, R.style.DeepPurpleTheme_Dark, R.style.IndigoTheme_Dark,
            R.style.BlueTheme_Dark, R.style.LightBlueTheme_Dark, R.style.CyanTheme_Dark,
            R.style.TealTheme_Dark, R.style.GreenTheme_Dark, R.style.LightGreenTheme_Dark,
            R.style.LimeTheme_Dark, R.style.YellowTheme_Dark, R.style.AmberTheme_Dark,
            R.style.OrangeTheme_Dark, R.style.DeepOrangeTheme_Dark, R.style.BrownTheme_Dark,
            R.style.GreyTheme_Dark, R.style.BlueGreyTheme_Dark, R.style.DarkTheme};

    public int[] surfaceColor = new int[]{R.color.m3_red_surface, R.color.m3_pink_surface,
            R.color.m3_purple_surface, R.color.m3_deep_purple_surface, R.color.m3_indigo_surface,
            R.color.m3_blue_surface, R.color.m3_light_blue_surface, R.color.m3_cyan_surface,
            R.color.m3_teal_surface, R.color.m3_green_surface, R.color.m3_light_green_surface,
            R.color.m3_lime_surface, R.color.m3_yellow_surface, R.color.m3_amber_surface,
            R.color.m3_orange_surface, R.color.m3_deep_orange_surface, R.color.m3_brown_surface,
            R.color.m3_grey_surface, R.color.m3_blue_grey_surface, R.color.m3_blue_surface};

    protected int[] colorOnSurface = new int[]{R.color.m3_red_on_surface, R.color.m3_pink_on_surface,
            R.color.m3_purple_on_surface, R.color.m3_deep_purple_on_surface, R.color.m3_indigo_on_surface,
            R.color.m3_blue_on_surface, R.color.m3_light_blue_on_surface, R.color.m3_cyan_on_surface,
            R.color.m3_teal_on_surface, R.color.m3_green_on_surface, R.color.m3_light_green_on_surface,
            R.color.m3_lime_on_surface, R.color.m3_yellow_on_surface, R.color.m3_amber_on_surface,
            R.color.m3_orange_on_surface, R.color.m3_deep_orange_on_surface, R.color.m3_brown_on_surface,
            R.color.m3_grey_on_surface, R.color.m3_blue_grey_on_surface, R.color.m3_blue_on_surface};

    protected int[] colorOnPrimaryContainer = new int[]{R.color.m3_red_on_primary_container, R.color.m3_pink_on_primary_container,
            R.color.m3_purple_on_primary_container, R.color.m3_deep_purple_on_primary_container, R.color.m3_indigo_on_primary_container,
            R.color.m3_blue_on_primary_container, R.color.m3_light_blue_on_primary_container, R.color.m3_cyan_on_primary_container,
            R.color.m3_teal_on_primary_container, R.color.m3_green_on_primary_container, R.color.m3_light_green_on_primary_container,
            R.color.m3_lime_on_primary_container, R.color.m3_yellow_on_primary_container, R.color.m3_amber_on_primary_container,
            R.color.m3_orange_on_primary_container, R.color.m3_deep_orange_on_primary_container, R.color.m3_brown_on_primary_container,
            R.color.m3_grey_on_primary_container, R.color.m3_blue_grey_on_primary_container, R.color.m3_blue_on_primary_container};

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setApplicationTheme();
    }

    public void setToolbar() {
        // Renews activity title when in-app language is changed
        try {
            int label = getPackageManager().getActivityInfo(getComponentName(), 0).labelRes;
            if (label != 0) {
                Toolbar toolbar = getToolbar();
                if (toolbar != null)
                    toolbar.setTitle(getResources().getString(label));

                setSupportActionBar(toolbar);

                if (getSupportActionBar() != null)
                    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
        } catch (Exception e) {
            LoggerUtils.ErrorLog(TAG, "Exception in setToolbar", e);
        }
    }

    public Toolbar getToolbar() {
        return findViewById(R.id.toolbar);
    }

    public FirebaseAnalytics getFirebaseAnalytics() {
        return ((Global)getApplicationContext()).getFirebaseInstance();
    }

    /**
     * Set Application Theme based on theme settings
     */
    protected void setApplicationTheme() {
        int selectedThemeColor = getMemoryUtil().getApplicationTheme(getApplicationContext());
        boolean isDarkTheme = getMemoryUtil().isDarkThemeEnabled(this);
        int[] stringArray = getResources().getIntArray(R.array.default_color_choice_values);
        int index = Ints.indexOf(stringArray, selectedThemeColor);
        if (index < 0)
            index = DEFAULT_THEME;

        setTheme(isDarkTheme ? dark_themeArray[index] : light_themeArray[index]);

//        if (isDarkTheme)
//            AppUtils.setNavigationBarColor(this);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            ActivityManager.TaskDescription description
                    = new ActivityManager.TaskDescription(getString(R.string.app_name), R.mipmap.ic_launcher, getColor(R.color.md_white_1000));
            this.setTaskDescription(description);
        } else {
            Bitmap mIcon = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
            ActivityManager.TaskDescription description;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                description = new ActivityManager.TaskDescription(getString(R.string.app_name), mIcon, getResources().getColor(R.color.md_white_1000));
                this.setTaskDescription(description);
            }
        }
    }

    public MemoryUtil getMemoryUtil() {
        return Global.getMemoryUtil();
    }
}
