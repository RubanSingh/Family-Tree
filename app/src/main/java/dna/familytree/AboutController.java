package dna.familytree;

import android.content.Intent;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;

import dna.familytree.util.AnalyticsUtil;

public class AboutController extends BaseController {

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.lapide);

        setToolbar();

        TextView version = findViewById(R.id.app_version);
        version.setText(getString(R.string.version_name, BuildConfig.VERSION_NAME));

        TextView link = findViewById(R.id.app_link);
        link.setPaintFlags(link.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        link.setOnClickListener(v -> startActivity(
                new Intent(Intent.ACTION_VIEW, Uri.parse("https://digitalailabs.blogspot.com/")))
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        AnalyticsUtil.logEventAboutActivity(getFirebaseAnalytics());
    }
}
