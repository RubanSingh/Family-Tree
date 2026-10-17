package dna.familytree.feature.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import dna.familytree.BuildConfig
import dna.familytree.feature.billing.PremiumGate

/**
 * Privacy consent for ads with Google's User Messaging Platform (docs/specs/ads.md, "Consent"): the GDPR message
 * (EEA, UK, Switzerland) and the US states message configured in AdMob › Privacy & messaging are shown when required,
 * and no ad is requested before [canRequestAds]. Premium users are never asked: they get no ads.
 */
object ConsentManager {

    private const val TAG = "ConsentManager"
    /** Debug builds only, in SharedPreferences "monetization": force the EEA geography / a test device for testing. */
    private const val DEBUG_EEA = "debugEea"
    private const val DEBUG_DEVICE = "debugDeviceHash"
    private const val DEBUG_RESET = "debugResetConsent"

    private var gathering = false
    private var gathered = false

    private fun info(context: Context): ConsentInformation = UserMessagingPlatform.getConsentInformation(context)

    /** Consent obtained or not required (also from a previous session). */
    @JvmStatic
    fun canRequestAds(context: Context): Boolean = info(context).canRequestAds()

    /**
     * Updates the consent status and shows the consent form if the user's region requires it, then starts the ads.
     * Called by the first screens; works once per process.
     */
    @JvmStatic
    fun gatherAndStartAds(activity: Activity) {
        if (PremiumGate.isPremium()) return
        if (BuildConfig.DEBUG) { // Testing: forget the choice once, as a first launch
            val prefs = activity.getSharedPreferences("monetization", Context.MODE_PRIVATE)
            if (prefs.getBoolean(DEBUG_RESET, false)) {
                info(activity).reset()
                prefs.edit().remove(DEBUG_RESET).apply()
            }
        }
        // Consent given in a previous session: ads start right away, while the status is updated
        if (canRequestAds(activity)) AdsManager.init(activity)
        if (gathered || gathering) return
        gathering = true
        info(activity).requestConsentInfoUpdate(activity, parameters(activity), {
            if (activity.isFinishing || activity.isDestroyed) {
                gathering = false // The next screen asks again
                return@requestConsentInfoUpdate
            }
            UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { error ->
                if (error != null) Log.w(TAG, "consent form: ${error.errorCode} ${error.message}")
                finishGathering(activity)
            }
        }, { error ->
            Log.w(TAG, "consent update: ${error.errorCode} ${error.message}")
            finishGathering(activity)
        })
    }

    private fun finishGathering(activity: Activity) {
        gathering = false
        gathered = true
        if (canRequestAds(activity)) AdsManager.init(activity)
    }

    /** The user's region requires a way to change the choice later (Settings › Ad privacy choices). */
    @JvmStatic
    fun isPrivacyOptionsRequired(context: Context): Boolean =
        info(context).privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    /** Shows the form where the user reviews or changes the choice. */
    @JvmStatic
    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { error ->
            if (error != null) Log.w(TAG, "privacy options: ${error.errorCode} ${error.message}")
            if (canRequestAds(activity)) AdsManager.init(activity)
        }
    }

    private fun parameters(context: Context): ConsentRequestParameters {
        val builder = ConsentRequestParameters.Builder()
        if (BuildConfig.DEBUG) {
            val prefs = context.getSharedPreferences("monetization", Context.MODE_PRIVATE)
            if (prefs.getBoolean(DEBUG_EEA, false)) {
                val debug = ConsentDebugSettings.Builder(context)
                    .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                prefs.getString(DEBUG_DEVICE, null)?.let { debug.addTestDeviceHashedId(it) }
                builder.setConsentDebugSettings(debug.build())
            }
        }
        return builder.build()
    }
}
