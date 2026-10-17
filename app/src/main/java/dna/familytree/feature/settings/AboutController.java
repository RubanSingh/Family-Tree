package dna.familytree.feature.settings;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import dna.familytree.BaseController;
import dna.familytree.BuildConfig;
import dna.familytree.Global;
import dna.familytree.R;
import dna.familytree.billing.PremiumPlan;
import dna.familytree.feature.billing.PremiumGate;
import dna.familytree.util.AnalyticsUtil;
import dna.familytree.util.AppClientUtils;

public class AboutController extends BaseController {

    private static final String WEBSITE = "https://digitalailabs.blogspot.com";

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.about_activity);
        showNativeAd();
        setToolbar();

        TextView version = findViewById(R.id.app_version);
        version.setText(getString(R.string.version_name, BuildConfig.VERSION_NAME));

        findViewById(R.id.app_link).setOnClickListener(view ->
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(WEBSITE))));
        findViewById(R.id.about_rate).setOnClickListener(view -> AppClientUtils.rateApp(this, getPackageName()));
        findViewById(R.id.about_share).setOnClickListener(view -> AppClientUtils.shareAppLink(this));

        bindPremium();
    }

    private void bindPremium() {
        View subscribed = findViewById(R.id.about_subscribed);
        View upsell = findViewById(R.id.about_product);
        if (PremiumGate.isPremium()) {
            PremiumPlan plan = PremiumPlan.Companion.fromKey(Global.settings.premiumPlan);
            ((TextView)findViewById(R.id.about_premium_title)).setText(
                    plan == PremiumPlan.MONTHLY ? R.string.premium_status_monthly : R.string.premium_status_lifetime);
            subscribed.setVisibility(View.VISIBLE);
            upsell.setVisibility(View.GONE);
        } else {
            subscribed.setVisibility(View.GONE);
            upsell.setVisibility(View.VISIBLE);
            upsell.setOnClickListener(v -> {
                AnalyticsUtil.logEventPremiumAction(getFirebaseAnalytics());
                PremiumGate.open(this, R.string.family_tree_premium);
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        AnalyticsUtil.logEventAboutActivity(getFirebaseAnalytics());
        bindPremium();
    }
}
