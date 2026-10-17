package dna.familytree.feature.backup

import android.content.Intent
import android.os.Bundle
import android.provider.DocumentsContract
import android.text.format.DateUtils
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.snackbar.Snackbar
import dna.familytree.BaseController
import dna.familytree.Global
import dna.familytree.NewTreeController
import dna.familytree.R
import dna.familytree.Settings
import dna.familytree.designsystem.PremiumBadge
import dna.familytree.feature.billing.PremiumGate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Local backup screen: switch, backup folder, trees to include, backup now, saved backups to restore or delete.
 * Open with [EXTRA_PICK_FOLDER] to go straight to the folder picker (from the trees screen invitation).
 */
class BackupController : BaseController() {

    companion object {
        const val EXTRA_PICK_FOLDER = "pickFolder"
    }

    private lateinit var progress: View
    private lateinit var content: View
    private lateinit var treesBox: LinearLayout
    private lateinit var filesBox: LinearLayout
    private lateinit var backupNow: MaterialButton
    private var working = false

    private val folderPicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = result.data?.data
        if (result.resultCode == RESULT_OK && uri != null) {
            try {
                BackupManager.setFolder(this, uri)
            } catch (e: Exception) {
                Snackbar.make(content, e.localizedMessage ?: getString(R.string.something_wrong), Snackbar.LENGTH_LONG).show()
            }
            refresh()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.backup)
        setToolbar()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        progress = findViewById(R.id.backup_progress)
        content = findViewById(R.id.backup_content)
        treesBox = findViewById(R.id.backup_trees)
        filesBox = findViewById(R.id.backup_files)
        backupNow = findViewById(R.id.backup_now)

        findViewById<View>(R.id.backup_choose).setOnClickListener { pickFolder() }
        backupNow.setOnClickListener { backupSelectedTrees() }

        if (savedInstanceState == null && intent.getBooleanExtra(EXTRA_PICK_FOLDER, false)) pickFolder()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun pickFolder() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        BackupManager.folderUri()?.let { intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, it) }
        try {
            folderPicker.launch(intent)
        } catch (e: Exception) { // No document provider
            Snackbar.make(content, e.localizedMessage ?: getString(R.string.something_wrong), Snackbar.LENGTH_LONG).show()
        }
    }

    private fun refresh() {
        bindAutomatic()
        bindFolder()
        bindTrees()
        loadBackups()
    }

    /** Automatic backup needs Premium; backing up now and restoring are for everyone. */
    private fun bindAutomatic() {
        val switch = findViewById<MaterialSwitch>(R.id.backup_switch)
        findViewById<TextView>(R.id.backup_auto_title).text = getString(R.string.backup_auto_title).let {
            if (PremiumGate.isPremium()) it else PremiumBadge.withDiamond(this, it)
        }
        switch.setOnCheckedChangeListener(null)
        switch.isChecked = BackupManager.isAutomatic()
        switch.setOnCheckedChangeListener { button, checked ->
            if (checked && !PremiumGate.isPremium()) {
                button.isChecked = false
                PremiumGate.open(this, R.string.backup_auto_title)
                return@setOnCheckedChangeListener
            }
            Global.settings.backup = checked
            Global.settings.save()
        }
    }

    /** Folder name and whether it can be written. */
    private fun bindFolder() {
        val name = findViewById<TextView>(R.id.backup_folder_name)
        val status = findViewById<TextView>(R.id.backup_folder_status)
        val choose = findViewById<MaterialButton>(R.id.backup_choose)
        val uri = BackupManager.folderUri()
        if (uri == null) {
            name.setText(R.string.backup_no_folder)
            status.setText(R.string.backup_prompt_desc)
            choose.setText(R.string.choose_folder)
        } else {
            name.text = BackupManager.folderLabel()
            choose.setText(R.string.backup_change)
            lifecycleScope.launch { // Asks the document provider
                val ready = withContext(Dispatchers.IO) { BackupManager.openFolder(this@BackupController) }
                status.text = ready.fold({ getString(R.string.backup_folder_ready) }, { it.localizedMessage })
            }
        }
        updateBackupButton()
    }

    /** Shared trees received back as derived copies are not backed up. */
    private fun Settings.Tree.isBackupable() = grade < 20

    /** Trees that can be backed up, each with a checkbox. */
    private fun bindTrees() {
        treesBox.removeAllViews()
        Global.settings.trees.filter { it.isBackupable() }.forEach { tree ->
            val row = LayoutInflater.from(this).inflate(R.layout.backup_tree_row, treesBox, false)
            row.findViewById<TextView>(R.id.backup_tree_avatar).text = tree.title?.trim()?.firstOrNull()?.uppercase() ?: "?"
            row.findViewById<TextView>(R.id.backup_tree_title).text = tree.title
            row.findViewById<TextView>(R.id.backup_tree_detail).text =
                resources.getQuantityString(R.plurals.backup_persons, tree.persons, tree.persons)
            val check = row.findViewById<MaterialCheckBox>(R.id.backup_tree_check)
            check.isChecked = tree.backup
            row.setOnClickListener {
                tree.backup = !tree.backup
                check.isChecked = tree.backup
                Global.settings.save()
                updateBackupButton()
            }
            treesBox.addView(row)
        }
    }

    private fun updateBackupButton() {
        backupNow.isEnabled = !working && BackupManager.folderUri() != null
                && Global.settings.trees.any { it.backup && it.isBackupable() }
    }

    private fun backupSelectedTrees() {
        val trees = Global.settings.trees.filter { it.backup && it.isBackupable() }
        setWorking(true)
        lifecycleScope.launch {
            val failures = withContext(Dispatchers.IO) {
                trees.mapNotNull { tree -> BackupManager.backup(tree.id).exceptionOrNull()?.let { tree.title to it } }
            }
            setWorking(false)
            val message = if (failures.isEmpty()) getString(R.string.backup_done)
            else getString(R.string.backup_failed, failures.joinToString { "${it.first}: ${it.second.localizedMessage}" })
            Snackbar.make(content, message, Snackbar.LENGTH_LONG).show()
            loadBackups()
        }
    }

    /** Lists the backup files of the folder. */
    private fun loadBackups() {
        if (BackupManager.folderUri() == null) {
            showFilesMessage(getString(R.string.backup_none))
            return
        }
        setWorking(true)
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { BackupManager.listBackups(this@BackupController) }
            setWorking(false)
            result.onSuccess { files ->
                if (files.isEmpty()) showFilesMessage(getString(R.string.backup_none))
                else {
                    filesBox.removeAllViews()
                    files.forEach { addFileRow(it) }
                }
            }.onFailure { showFilesMessage(it.localizedMessage ?: getString(R.string.something_wrong)) }
        }
    }

    private fun addFileRow(file: BackupManager.BackupFile) {
        val row = LayoutInflater.from(this).inflate(R.layout.backup_file_row, filesBox, false)
        val document = file.document
        row.findViewById<TextView>(R.id.backup_file_title).text = file.title ?: document.name
        val time = DateUtils.getRelativeTimeSpanString(file.modified, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS)
        val size = Formatter.formatShortFileSize(this, file.size)
        row.findViewById<TextView>(R.id.backup_file_detail).text =
            if (file.valid) "$time · $size" else "${getString(R.string.backup_invalid_file)} · $size"
        val restore = row.findViewById<MaterialButton>(R.id.backup_file_restore)
        if (file.valid) {
            restore.setOnClickListener { confirmRestore(file) }
        } else {
            restore.visibility = View.GONE
            row.findViewById<ImageView>(R.id.backup_file_icon).alpha = 0.5f
        }
        row.findViewById<View>(R.id.backup_file_delete).setOnClickListener {
            MaterialAlertDialogBuilder(this).setTitle(R.string.backup_delete_title).setMessage(document.name)
                .setPositiveButton(R.string.delete) { _, _ ->
                    document.delete()
                    loadBackups()
                }.setNeutralButton(R.string.cancel, null).show()
        }
        filesBox.addView(row)
    }

    /** Restores as a new tree with the same code used by "Recover a ZIP backup". */
    private fun confirmRestore(file: BackupManager.BackupFile) {
        MaterialAlertDialogBuilder(this).setTitle(R.string.backup_restore_title)
            .setMessage(getString(R.string.backup_restore_message, file.title ?: file.document.name))
            .setPositiveButton(R.string.recover) { _, _ -> NewTreeController.unZip(this, null, file.document.uri) }
            .setNeutralButton(R.string.cancel, null).show()
    }

    private fun showFilesMessage(message: String) {
        filesBox.removeAllViews()
        val text = LayoutInflater.from(this).inflate(R.layout.backup_tree_row, filesBox, false)
        text.findViewById<View>(R.id.backup_tree_avatar).visibility = View.GONE
        text.findViewById<View>(R.id.backup_tree_check).visibility = View.GONE
        text.findViewById<View>(R.id.backup_tree_title).visibility = View.GONE
        text.findViewById<TextView>(R.id.backup_tree_detail).text = message
        text.isClickable = false
        text.background = null
        filesBox.addView(text)
    }

    private fun setWorking(on: Boolean) {
        working = on
        progress.visibility = if (on) View.VISIBLE else View.INVISIBLE
        updateBackupButton()
    }
}
