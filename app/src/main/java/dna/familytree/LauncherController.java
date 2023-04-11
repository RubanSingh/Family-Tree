package dna.familytree;

import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import java.util.Locale;

import dna.familytree.constant.Extra;
import dna.familytree.util.AnalyticsUtil;

public class LauncherController extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.splash);

        new Handler(this.getMainLooper()).postDelayed(() -> {
            Intent intent = getIntent();
            Uri uri = intent.getData();
            // By opening the app from the Recents screen, avoids re-importing a newly imported shared tree
            boolean fromHistory = (intent.getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) == Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY;
            if (uri != null && !fromHistory) {
                String dataId;
                if (uri.getPath().equals("/share.php")) // click on the first message received
                    dataId = uri.getQueryParameter("tree");
                else if (uri.getLastPathSegment().endsWith(".zip")) // click on the invitation page
                    dataId = uri.getLastPathSegment().replace(".zip", "");
                else {
                    AppUtils.toast(this, R.string.cant_understand_uri);
                    return;
                }
            } else {
                Intent treesIntent = new Intent(this, TreesController.class);
                // Open last tree at startup
                if (Global.settings.loadTree) {
                    treesIntent.putExtra(Extra.AUTO_LOAD_TREE, true); // To load the last opened tree
                    treesIntent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION); // perhaps ineffective but so be it
                }
                startActivity(treesIntent);
            }
        }, 2000);

        // Set app locale for application context and resources (localized gedcom.jar library)
        Locale locale = AppCompatDelegate.getApplicationLocales().get(0); // Find app locale, or null if not existing
        if (locale != null) {
            Configuration config = getResources().getConfiguration();
            config.setLocale(locale);
            getApplicationContext().getResources().updateConfiguration(config, null); // Change locale both for static methods and jar library
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        AnalyticsUtil.logEventLauncherActivity(((Global)getApplicationContext()).getFirebaseInstance());
    }
}
