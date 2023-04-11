package dna.familytree.util;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import dna.familytree.BaseController;
import dna.familytree.BuildConfig;
import dna.familytree.Global;
import dna.familytree.R;

public class AppClientUtils {

    private static final String TAG = AppClientUtils.class.getSimpleName();
    public static void rateApp(BaseController activity, String appPackageName) {
        try {
            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
        } catch (android.content.ActivityNotFoundException anfe) {
            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
        }

        //analytics event
        AnalyticsUtil.logEventSettingsRateApp(activity.getFirebaseAnalytics());
    }

    public static void setFeedback(BaseController activity) {
        String message = "Device Modal : " + android.os.Build.MODEL + StringUtils.TAG_NEWLINE +
                "Device Manufacturer : " + android.os.Build.MANUFACTURER + StringUtils.TAG_NEWLINE +
                "Android Version : " + Build.VERSION.SDK_INT + StringUtils.TAG_NEWLINE +
                "App Version : " + BuildConfig.VERSION_CODE +
                "Premium Enabled : " + Global.settings.premium;

        String mailID = activity.getString(R.string.app_mail);

        String subject = "Feedback";

        Intent selectorIntent = new Intent(Intent.ACTION_SENDTO);
        selectorIntent.setData(Uri.parse(ApplicationConstants.TAG_MAIL));

        final Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{mailID});
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        emailIntent.putExtra(Intent.EXTRA_TEXT, message);
        emailIntent.setSelector(selectorIntent);
        activity.startActivity(Intent.createChooser(emailIntent, "Send email..."));

        //analytics event
        AnalyticsUtil.logEventSettingsFeedBack(activity.getFirebaseAnalytics());
    }

    public static void shareAppLink(BaseController context) {
        AnalyticsUtil.logEventSettingsShare(context.getFirebaseAnalytics());
        try {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.app_name));
            String shareMessage = "\nLet me recommend you this application\n\n";
            shareMessage = shareMessage + "https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID + "\n\n";
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
            context.startActivity(Intent.createChooser(shareIntent, "choose one"));
        } catch (Exception e) {
            LoggerUtils.ErrorLog(TAG, "Exception in shareAppLink :" + e.getMessage(), e);
        }
    }
}
