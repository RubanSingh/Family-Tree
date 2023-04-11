package dna.familytree.merge

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import dna.familytree.Global
import dna.familytree.PurchaseController
import dna.familytree.R
import dna.familytree.constant.Extra
import dna.familytree.databinding.MergeResultFragmentBinding

class ResultFragment : BaseFragment(R.layout.merge_result_fragment) {

    private lateinit var bind: MergeResultFragmentBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bind = MergeResultFragmentBinding.bind(view)
        setupTreeView(bind.mergeFirstTree, model.firstTree)
        setupTreeView(bind.mergeSecondTree, model.secondTree)
        // Setups radio buttons
        bind.mergeRadioAnnex.text = getString(R.string.merge_into, model.firstTree.title, model.secondTree.title)
        bind.mergeRetitle.setText("${model.firstTree.title} ${model.secondTree.title}")
        // Selecting a radio button
        bind.mergeRadiogroup.setOnCheckedChangeListener { _, checked ->
            if (checked == R.id.merge_radio_generate) bind.mergeRetitleContainer.visibility = View.VISIBLE
            else bind.mergeRetitleContainer.visibility = View.GONE
            bind.mergeButton.isEnabled = true
        }
        // Observable state
        model.state.observe(viewLifecycleOwner) {
            when (it) {
                State.ACTIVE -> {
                    // Disables all views and displays the progress wheel
                    bind.mergeRadioAnnex.isEnabled = false
                    bind.mergeRadioGenerate.isEnabled = false
                    bind.mergeRetitle.isEnabled = false
                    bind.mergeButton.isEnabled = false
                    activity?.findViewById<ProgressBar>(R.id.progress_wheel)?.visibility = View.VISIBLE
                }
                State.COMPLETE -> {
                    Global.gc = if (Global.settings.openTree == model.firstNum) model.firstGedcom else null
                    requireActivity().finish()
                    Toast.makeText(context, R.string.merge_complete, Toast.LENGTH_LONG).show()
                }
                else -> { // Normal state
                    bind.mergeRadioAnnex.isEnabled = true
                    bind.mergeRadioGenerate.isEnabled = true
                    bind.mergeRetitle.isEnabled = true
                    bind.mergeButton.isEnabled = bind.mergeRadioAnnex.isChecked || bind.mergeRadioGenerate.isChecked
                    activity?.findViewById<ProgressBar>(R.id.progress_wheel)?.visibility = View.GONE
                }
            }
            // Merge button
            bind.mergeButton.setOnClickListener {
                if (Global.settings.premium) {
                    if (bind.mergeRadioAnnex.isChecked)
                        model.performAnnexMerge(requireContext())
                    else if (bind.mergeRadioGenerate.isChecked)
                        model.performGenerateMerge(requireContext(), bind.mergeRetitle.text.toString())
                } else {
                    startActivity(Intent(context, PurchaseController::class.java).putExtra(Extra.STRING, R.string.merge_tree))
                }
            }
        }

        setToolbar(view)
    }
}
