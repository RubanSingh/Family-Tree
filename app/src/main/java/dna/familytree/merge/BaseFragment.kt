package dna.familytree.merge

import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import dna.familytree.R
import dna.familytree.Settings
import dna.familytree.TreesController
import dna.familytree.AppUtils
import dna.familytree.util.LoggerUtils

open class BaseFragment(layout: Int) : Fragment(layout) {

    private val TAG: String = "BaseFragment"
    val model: MergeViewModel by activityViewModels()

    fun setupTreeView(treeView: View, tree: Settings.Tree) {
        treeView.background = ResourcesCompat.getDrawable(resources, R.drawable.generic_background, null)
        treeView.setPadding(AppUtils.dpToPx(15f), AppUtils.dpToPx(5f), AppUtils.dpToPx(15f), AppUtils.dpToPx(7f))
        treeView.findViewById<TextView>(R.id.albero_titolo).text = tree.title
        treeView.findViewById<TextView>(R.id.albero_dati).text = TreesController.writeData(context, tree)
        treeView.findViewById<View>(R.id.albero_menu).visibility = View.GONE
    }

    open fun setToolbar(view: View) {
        // Renews activity title when in-app language is changed
        try {
            val activity = requireActivity() as AppCompatActivity
            val label: Int = activity.packageManager.getActivityInfo(activity.componentName, 0).labelRes
            if (label != 0) {
                val toolbar: Toolbar? = getToolbar(view)
                if (toolbar != null) {
                    toolbar.title = resources.getString(label)
                    activity.setSupportActionBar(toolbar)
                    if (activity.supportActionBar != null)
                        activity.supportActionBar!!.setDisplayHomeAsUpEnabled(true)
                }
            }
        } catch (e: Exception) {
            LoggerUtils.ErrorLog(TAG, "Exception in setToolbar", e)
        }
    }

    private fun getToolbar(view: View): Toolbar? {
        return view.findViewById(R.id.toolbar)
    }
}
