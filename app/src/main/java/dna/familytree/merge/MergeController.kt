package dna.familytree.merge

import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import dna.familytree.BaseController
import dna.familytree.R
import dna.familytree.TreesController

class MergeController : BaseController() {

    private lateinit var navController: NavController
    private val model: MergeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.merge_activity)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Physical back button
        onBackPressedDispatcher.addCallback(this) {
            onSupportNavigateUp()
        }
    }

    // Intercepts the back arrow in actionbar
    override fun onSupportNavigateUp(): Boolean {
        val destination = navController.currentDestination?.id
        if (destination == R.id.choiceFragment && model.state.value == State.ACTIVE) {
            model.coroutine.cancel()
        } else if (destination == R.id.matchFragment) {
            model.previousMatch()
        } // Cancel active merging
        else if (destination == R.id.resultFragment && model.state.value == State.ACTIVE) {
            AlertDialog.Builder(this).setMessage(R.string.sure_delete)
                    .setNegativeButton(R.string.no) { dialog, _ -> dialog.dismiss() }
                    .setPositiveButton(R.string.yes) { _, _ ->
                        model.coroutine.cancel()
                        model.state.value = State.QUIET
                        if (model.newNum > 0) {
                            TreesController.deleteTree(this, model.newNum)
                            model.newNum = 0
                        }
                        navController.navigateUp()
                    }.show()
            return false
        }
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
