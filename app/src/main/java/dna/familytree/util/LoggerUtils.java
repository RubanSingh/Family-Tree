package dna.familytree.util;

import android.util.Log;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

/**
 * Created by rubansingh.john on 4/24/2021.
 */
public class LoggerUtils {

    public static void DebugLog(String tag, String message) {
        Log.d(tag, message);
        FirebaseCrashlytics.getInstance().log(tag + StringUtils.TAG_COLON + message);
    }

    public static void EventLog(String tag, String message) {
        Log.v(tag, message);
        FirebaseCrashlytics.getInstance().log(tag + StringUtils.TAG_COLON + message);
    }

    public static void ErrorLog(String tag, String message, Exception e) {
        FirebaseCrashlytics.getInstance().recordException(e);
    }
}
