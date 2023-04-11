package dna.familytree;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import dna.familytree.util.AnalyticsUtil;

public class AboutController extends BaseController {

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.about_activity);
        showNativeAd();

        setToolbar();

        TextView version = findViewById(R.id.app_version);
        version.setText(getString(R.string.version_name, BuildConfig.VERSION_NAME));

        TextView webSite = findViewById(R.id.app_link);
        webSite.setOnClickListener(view -> startActivity(
                new Intent(Intent.ACTION_VIEW, Uri.parse("https://digitalailabs.blogspot.com"))));

        // Premium product layout
        View subscribedLayout = findViewById(R.id.about_subscribed);
        if (Global.settings.premium) {
            subscribedLayout.setVisibility(View.VISIBLE);
        } else {
            webSite.setVisibility(View.GONE);
            ProductLayout productLayout = findViewById(R.id.about_product);
            productLayout.initialize(() -> { // Makes it also visible
                runOnUiThread(() -> {
                    productLayout.setVisibility(View.GONE);
                    subscribedLayout.setVisibility(View.VISIBLE);
                });
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        AnalyticsUtil.logEventAboutActivity(getFirebaseAnalytics());
    }
}
