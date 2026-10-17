package dna.familytree.util

import android.app.Activity
import android.view.View
import com.google.android.material.snackbar.Snackbar

/** Short messages at the bottom of a screen. */
object Snackbars {

    /** Shows [message] unless the activity is closing. */
    @JvmStatic
    fun show(activity: Activity, message: CharSequence) {
        if (activity.isFinishing || activity.isDestroyed) return
        val root = activity.findViewById<View>(android.R.id.content) ?: return
        Snackbar.make(root, message, Snackbar.LENGTH_LONG).show()
    }
}
