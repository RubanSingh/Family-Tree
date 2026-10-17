package dna.familytree.feature.update

import android.app.Activity
import android.content.Context
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import dna.familytree.R
import java.util.concurrent.TimeUnit

/**
 * Google Play in-app updates for one screen. Create it in `onCreate()`; it checks on every resume.
 *
 * - **Forced update**: when the published release has in-app update priority 4 or 5 (set with the Play Developer API,
 *   e.g. `inAppUpdatePriority` in Gradle Play Publisher or fastlane), Play's full-screen update starts;
 *   if the user backs out, a blocking dialog offers only Update or Close app.
 * - **Optional update**: otherwise Play's flexible update is offered at most once a day; it downloads in the
 *   background and a snackbar offers to restart when ready.
 * - Builds not installed from Google Play (debug, sideloaded) can't check: automatic checks stay silent.
 */
class InAppUpdates(private val activity: AppCompatActivity) {

    enum class CheckResult { UPDATE_STARTED, UP_TO_DATE, UNAVAILABLE }

    companion object {
        private const val FORCED_PRIORITY = 4
        private const val PREFS = "in_app_updates"
        private const val KEY_FLEXIBLE_OFFERED = "flexibleOfferedAt"
        private val FLEXIBLE_INTERVAL_MILLIS = TimeUnit.DAYS.toMillis(1)
    }

    private val manager = AppUpdateManagerFactory.create(activity)
    private var forcedInProgress = false
    private var restartShown = false
    private var requiredDialog: AlertDialog? = null

    private val updateLauncher = activity.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (forcedInProgress && result.resultCode != Activity.RESULT_OK) showUpdateRequired()
        forcedInProgress = false
    }

    private val installListener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) showRestart()
    }

    init {
        manager.registerListener(installListener)
        activity.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) = checkAutomatically()
            override fun onDestroy(owner: LifecycleOwner) = manager.unregisterListener(installListener)
        })
    }

    private fun checkAutomatically() {
        if (requiredDialog?.isShowing == true) return // The user must choose first
        manager.appUpdateInfo.addOnSuccessListener { info ->
            if (isGone()) return@addOnSuccessListener
            when {
                info.installStatus() == InstallStatus.DOWNLOADED -> showRestart()
                // A forced update was interrupted: resume it (an optional one downloading reports the same state)
                info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS && isForced(info) -> startForced(info)
                info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE && isForced(info) -> startForced(info)
                info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                        && info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) && flexibleDue() -> {
                    markFlexibleOffered()
                    start(info, AppUpdateType.FLEXIBLE)
                }
            }
        } // Failure: not installed from Google Play or no connection; nothing to do
    }

    /** "Check for updates" from Settings. */
    fun checkNow(onResult: (CheckResult) -> Unit) {
        manager.appUpdateInfo.addOnSuccessListener { info ->
            if (isGone()) return@addOnSuccessListener
            when {
                info.installStatus() == InstallStatus.DOWNLOADED -> {
                    showRestart(); onResult(CheckResult.UPDATE_STARTED)
                }
                info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                        || info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> {
                    val started = when {
                        isForced(info) -> startForced(info)
                        info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) -> start(info, AppUpdateType.FLEXIBLE)
                        else -> start(info, AppUpdateType.IMMEDIATE)
                    }
                    onResult(if (started) CheckResult.UPDATE_STARTED else CheckResult.UNAVAILABLE)
                }
                else -> onResult(CheckResult.UP_TO_DATE)
            }
        }.addOnFailureListener { if (!isGone()) onResult(CheckResult.UNAVAILABLE) }
    }

    /**
     * The screen was closed while Play answered. (Listeners bound to the activity would add a hidden fragment to it,
     * which screens counting their fragments don't expect.)
     */
    private fun isGone() = activity.isFinishing || activity.isDestroyed

    private fun isForced(info: AppUpdateInfo) =
        info.updatePriority() >= FORCED_PRIORITY && info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)

    private fun startForced(info: AppUpdateInfo): Boolean {
        forcedInProgress = true
        return start(info, AppUpdateType.IMMEDIATE).also { if (!it) forcedInProgress = false }
    }

    private fun start(info: AppUpdateInfo, type: Int): Boolean = try {
        manager.startUpdateFlowForResult(info, updateLauncher, AppUpdateOptions.newBuilder(type).build())
    } catch (e: Exception) {
        false
    }

    /** Blocking dialog after the user backs out of a forced update. */
    private fun showUpdateRequired() {
        if (isGone() || requiredDialog?.isShowing == true) return
        requiredDialog = MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.update_required_title)
            .setMessage(R.string.update_required_message)
            .setCancelable(false)
            .setPositiveButton(R.string.update_now) { _, _ ->
                manager.appUpdateInfo.addOnSuccessListener { if (!isGone()) startForced(it) }
                    .addOnFailureListener { showUpdateRequired() }
            }
            .setNegativeButton(R.string.update_close_app) { _, _ -> activity.finishAffinity() }
            .show()
    }

    /** Flexible update downloaded: restart to install. */
    private fun showRestart() {
        if (restartShown || activity.isFinishing) return
        restartShown = true
        val root = activity.findViewById<View>(android.R.id.content) ?: return
        Snackbar.make(root, R.string.update_downloaded, Snackbar.LENGTH_INDEFINITE)
            .setAction(R.string.update_restart) { manager.completeUpdate() }
            .show()
    }

    private fun flexibleDue(): Boolean {
        val last = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(KEY_FLEXIBLE_OFFERED, 0)
        return System.currentTimeMillis() - last > FLEXIBLE_INTERVAL_MILLIS
    }

    private fun markFlexibleOffered() {
        activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putLong(KEY_FLEXIBLE_OFFERED, System.currentTimeMillis()).apply()
    }
}
