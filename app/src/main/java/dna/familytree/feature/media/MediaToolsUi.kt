package dna.familytree.feature.media

import android.content.Context
import android.graphics.BitmapFactory
import android.text.format.DateUtils
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.squareup.picasso.Picasso
import dna.familytree.AppUtils
import dna.familytree.Global
import dna.familytree.R
import dna.familytree.feature.billing.PremiumGate
import dna.familytree.util.FileUtils
import dna.familytree.util.Snackbars
import dna.familytree.visitor.MediaLeaders
import dna.familytree.visitor.MediaReferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.folg.gedcom.model.Media
import java.net.URLConnection
import dna.familytree.designsystem.R as DsR

/**
 * Screens of the media file tools: file info sheet, copy to app storage, rename, shorten paths.
 * Every task shows its progress, can be cancelled, and saves the tree once at the end with the change dates of the edited records.
 */
object MediaToolsUi {

    /** Context menu items of the media lists. */
    const val MENU_MAKE_SHARED = 410
    const val MENU_MAKE_SIMPLE = 411

    // Shared and simple media

    /** Adds "Make shared media" or "Make simple media" to the context menu of a media, when possible. */
    @JvmStatic
    fun addConversionItem(menu: android.view.Menu, media: Media) {
        if (media.id == null) menu.add(0, MENU_MAKE_SHARED, 0, R.string.make_shared_media)
        else if (MediaConversions.canMakeSimple(Global.gc, media)) menu.add(0, MENU_MAKE_SIMPLE, 0, R.string.make_media)
    }

    /**
     * Converts a simple media to shared or a shared media to simple, for the items of [addConversionItem].
     * @return The leader objects to save, or null if [itemId] is not one of those items
     */
    @JvmStatic
    fun convert(activity: AppCompatActivity, itemId: Int, media: Media): Array<Any>? {
        val gedcom = Global.gc ?: return null
        val leaders = when (itemId) {
            MENU_MAKE_SHARED -> MediaConversions.makeShared(gedcom, media, AppUtils.newID(gedcom, Media::class.java))
            MENU_MAKE_SIMPLE -> MediaConversions.makeSimple(gedcom, media)
            else -> return null
        }
        dna.familytree.Memory.setInstanceAndAllSubsequentToNull(media)
        Snackbars.show(activity, activity.getString(if (itemId == MENU_MAKE_SHARED) R.string.media_made_shared else R.string.media_made_simple))
        return leaders
    }

    // File info

    /** Bottom sheet with the details of the file of a media, and the actions that apply to it. */
    @JvmStatic
    fun showFileInfo(activity: AppCompatActivity, media: Media, onChanged: Runnable) {
        val treeId = Global.settings.openTree
        activity.lifecycleScope.launch {
            val info = withContext(Dispatchers.IO) { FileDetails.of(activity, media, treeId) }
            if (activity.isFinishing) return@launch
            val sheet = BottomSheetDialog(activity)
            val view = LayoutInflater.from(activity).inflate(R.layout.media_file_info, null)
            val resolved = info.resolved
            view.findViewById<TextView>(R.id.media_info_name).text = info.name ?: activity.getString(R.string.media_location_none)
            bindLocation(view.findViewById(R.id.media_info_location), resolved.location)
            FileUtils.showImage(media, view.findViewById(R.id.media_info_thumb), null)

            val rows = view.findViewById<LinearLayout>(R.id.media_info_rows)
            fun row(icon: Int, label: Int, value: String?) {
                if (value.isNullOrBlank()) return
                val item = LayoutInflater.from(activity).inflate(R.layout.media_file_info_row, rows, false)
                item.findViewById<ImageView>(R.id.media_info_row_icon).setImageResource(icon)
                item.findViewById<TextView>(R.id.media_info_row_label).setText(label)
                item.findViewById<TextView>(R.id.media_info_row_value).text = value
                rows.addView(item)
            }
            row(DsR.drawable.ds_ic_folder, R.string.media_info_folder, info.folder)
            row(DsR.drawable.ds_ic_save, R.string.media_info_size, info.size?.let { Formatter.formatFileSize(activity, it) })
            row(DsR.drawable.ds_ic_layers, R.string.media_info_type, info.type)
            row(DsR.drawable.ds_ic_photo, R.string.media_info_dimensions,
                info.dimensions?.let { activity.getString(R.string.media_dimensions_value, it.first, it.second) })
            row(DsR.drawable.ds_ic_history, R.string.media_info_modified, info.modified?.let {
                DateUtils.formatDateTime(activity, it, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_SHOW_TIME)
            })
            if (Global.settings.expert) row(DsR.drawable.ds_ic_open_in_new, R.string.media_info_link, media.file)
            if (info.usedBy > 0) row(DsR.drawable.ds_ic_group, R.string.media_info_used_by,
                activity.resources.getQuantityString(R.plurals.media_info_records_count, info.usedBy, info.usedBy))

            view.findViewById<TextView>(R.id.media_info_hint).apply {
                val hint = when {
                    resolved.location == MediaFileTools.Location.MISSING -> R.string.media_hint_missing
                    resolved.canCopy() -> R.string.media_hint_outside
                    else -> 0
                }
                if (hint != 0) {
                    setText(hint)
                    visibility = View.VISIBLE
                }
            }
            view.findViewById<View>(R.id.media_info_rename).apply {
                visibility = if (resolved.canRename()) View.VISIBLE else View.GONE
                setOnClickListener {
                    sheet.dismiss()
                    rename(activity, media, onChanged)
                }
            }
            view.findViewById<View>(R.id.media_info_copy).apply {
                visibility = if (resolved.canCopy()) View.VISIBLE else View.GONE
                setOnClickListener {
                    sheet.dismiss()
                    copyToAppStorage(activity, media, onChanged)
                }
            }
            sheet.setContentView(view)
            sheet.show()
        }
    }

    private fun bindLocation(pill: TextView, location: MediaFileTools.Location) {
        val (text, icon) = when (location) {
            MediaFileTools.Location.APP_STORAGE -> R.string.media_location_app to DsR.drawable.ds_ic_check_circle
            MediaFileTools.Location.DEVICE -> R.string.media_location_device to DsR.drawable.ds_ic_folder
            MediaFileTools.Location.DOCUMENT -> R.string.media_location_document to DsR.drawable.ds_ic_folder
            MediaFileTools.Location.WEB -> R.string.media_location_web to DsR.drawable.ds_ic_language
            MediaFileTools.Location.MISSING -> R.string.media_location_missing to DsR.drawable.ds_ic_block
            MediaFileTools.Location.NONE -> R.string.media_location_none to DsR.drawable.ds_ic_image
        }
        pill.setText(text)
        val drawable = androidx.appcompat.content.res.AppCompatResources.getDrawable(pill.context, icon)?.mutate()
        val size = pill.resources.getDimensionPixelSize(R.dimen.inline_icon)
        drawable?.setBounds(0, 0, size, size)
        drawable?.setTint(pill.currentTextColor)
        pill.setCompoundDrawablesRelative(drawable, null, null, null)
    }

    /** What the file info sheet shows; collected on a background thread. */
    private class FileDetails(val resolved: MediaFileTools.Resolved, val name: String?, val folder: String?, val size: Long?,
                              val type: String?, val dimensions: Pair<Int, Int>?, val modified: Long?, val usedBy: Int) {
        companion object {
            fun of(context: Context, media: Media, treeId: Int): FileDetails {
                val r = MediaFileTools.resolve(context, media, treeId)
                val usedBy = if (media.id != null && Global.gc != null) MediaReferences(Global.gc, media, false).leaders.size else 0
                val link = media.file?.replace('\\', '/')
                return when {
                    r.file != null -> {
                        val dims = BitmapFactory.Options().run {
                            inJustDecodeBounds = true
                            BitmapFactory.decodeFile(r.file.path, this)
                            if (outWidth > 0 && outHeight > 0) outWidth to outHeight else null
                        }
                        val folder = if (r.location == MediaFileTools.Location.APP_STORAGE) context.getString(R.string.media_location_app)
                        else r.file.parent
                        FileDetails(r, r.file.name, folder, r.file.length(), mime(r.file.name), dims, r.file.lastModified(), usedBy)
                    }
                    r.uri != null -> {
                        val document = runCatching { androidx.documentfile.provider.DocumentFile.fromSingleUri(context, r.uri) }.getOrNull()
                        val dims = runCatching {
                            context.contentResolver.openInputStream(r.uri)?.use { input ->
                                BitmapFactory.Options().run {
                                    inJustDecodeBounds = true
                                    BitmapFactory.decodeStream(input, null, this)
                                    if (outWidth > 0 && outHeight > 0) outWidth to outHeight else null
                                }
                            }
                        }.getOrNull()
                        FileDetails(r, document?.name ?: link?.substringAfterLast('/'), context.getString(R.string.media_location_document),
                            document?.length(), document?.type, dims, document?.lastModified()?.takeIf { it > 0 }, usedBy)
                    }
                    else -> FileDetails(r, link?.substringAfterLast('/')?.ifEmpty { link }, null, null,
                        link?.let { mime(it) }, null, null, usedBy)
                }
            }

            private fun mime(name: String): String? = runCatching { URLConnection.guessContentTypeFromName(name) }.getOrNull()
        }
    }

    // Copy to app storage

    /** Copies the file of one media into app storage and links the media to the copy. */
    @JvmStatic
    fun copyToAppStorage(activity: AppCompatActivity, media: Media, onDone: Runnable) {
        val treeId = Global.settings.openTree
        val progress = Progress(activity, R.string.copy_app_storage)
        progress.job = activity.lifecycleScope.launch {
            var leaders: List<Any> = emptyList()
            var error: String? = null
            try {
                val resolved = withContext(Dispatchers.IO) { MediaFileTools.resolve(activity, media, treeId) }
                if (!resolved.canCopy()) return@launch
                progress.text(activity.getString(R.string.media_copying, 1, 1))
                val copy = withContext(Dispatchers.IO) { MediaFileTools.copyToAppStorage(activity, resolved, treeId) }
                leaders = MediaFileTools.linkToCopy(treeId, listOf(entryOf(media)), copy)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e.localizedMessage ?: e.javaClass.simpleName
            } finally {
                withContext(NonCancellable) {
                    progress.dismiss()
                    if (leaders.isNotEmpty()) {
                        AppUtils.save(true, *leaders.toTypedArray())
                        if (!activity.isDestroyed) onDone.run()
                        Snackbars.show(activity, activity.getString(R.string.media_copied_one))
                    } else if (error != null) Snackbars.show(activity, activity.getString(R.string.media_copy_failed, error))
                }
            }
        }
    }

    /** Gallery: copies every file outside app storage (Premium). */
    @JvmStatic
    fun copyAllToAppStorage(activity: AppCompatActivity, onDone: Runnable) {
        if (!PremiumGate.isPremium()) {
            PremiumGate.open(activity, R.string.copy_app_storage)
            return
        }
        val gedcom = Global.gc ?: return
        val treeId = Global.settings.openTree
        val checking = Progress(activity, R.string.copy_app_storage)
        checking.text(activity.getString(R.string.media_checking))
        checking.job = activity.lifecycleScope.launch {
            val groups = try {
                withContext(Dispatchers.IO) {
                    MediaFileTools.findCopyable(activity, gedcom, treeId) { done, total -> checking.post(done, total) }
                }
            } finally {
                checking.dismiss()
            }
            if (groups.isEmpty()) {
                Snackbars.show(activity, activity.getString(R.string.media_copy_none))
                return@launch
            }
            val size = withContext(Dispatchers.IO) { MediaFileTools.knownSize(activity, groups) }
            var message = activity.resources.getQuantityString(R.plurals.media_copy_confirm_count, groups.size, groups.size)
            // The size is known for files on the device only
            if (size > 0 && groups.none { it.resolved.location == MediaFileTools.Location.WEB })
                message += "\n\n" + activity.getString(R.string.media_copy_size, Formatter.formatShortFileSize(activity, size))
            MaterialAlertDialogBuilder(activity)
                .setIcon(DsR.drawable.ds_ic_download)
                .setTitle(R.string.copy_app_storage)
                .setMessage(message)
                .setPositiveButton(R.string.copy_app_storage) { _, _ -> copyGroups(activity, groups, treeId, onDone) }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    private fun copyGroups(activity: AppCompatActivity, groups: List<MediaFileTools.CopyGroup>, treeId: Int, onDone: Runnable) {
        val progress = Progress(activity, R.string.copy_app_storage)
        progress.job = activity.lifecycleScope.launch {
            val leaders = LinkedHashSet<Any>()
            var copied = 0
            var failed = 0
            try {
                groups.forEachIndexed { index, group ->
                    progress.text(activity.getString(R.string.media_copying, index + 1, groups.size))
                    progress.post(index, groups.size)
                    try {
                        val copy = withContext(Dispatchers.IO) { MediaFileTools.copyToAppStorage(activity, group.resolved, treeId) }
                        leaders.addAll(MediaFileTools.linkToCopy(treeId, group.entries, copy)) // Each copy is linked at once
                        copied++
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        failed++
                    }
                }
            } finally {
                withContext(NonCancellable) {
                    progress.dismiss()
                    if (leaders.isNotEmpty()) AppUtils.save(true, *leaders.toTypedArray())
                    if (copied > 0 && !activity.isDestroyed) onDone.run() // The screen may be gone
                    Snackbars.show(activity, if (failed == 0) activity.resources.getQuantityString(R.plurals.media_copied_count, copied, copied)
                    else activity.getString(R.string.media_copied_partly, copied, failed))
                }
            }
        }
    }

    // Shorten paths

    /** Shortens the link of one media to the file name. */
    @JvmStatic
    fun shrinkPath(activity: AppCompatActivity, media: Media, onDone: Runnable) {
        val treeId = Global.settings.openTree
        val resolved = MediaFileTools.resolve(activity, media, treeId, false)
        val name = MediaFileTools.shortName(activity, media, resolved, treeId) ?: return
        MediaFileTools.listTreeDir(activity, treeId)
        if (MediaFileTools.resolvesTo(treeId, name, resolved.file!!)) {
            media.file = name
            AppUtils.save(true, entryOf(media).leader)
            onDone.run()
            Snackbars.show(activity, activity.getString(R.string.media_shrunk_one))
        } else Snackbars.show(activity, activity.getString(R.string.media_shrink_none))
    }

    /** Gallery: shortens the links of all media whose file is in app storage. */
    @JvmStatic
    fun shrinkAllPaths(activity: AppCompatActivity, onDone: Runnable) {
        val gedcom = Global.gc ?: return
        val treeId = Global.settings.openTree
        val checking = Progress(activity, R.string.shrink_path)
        checking.text(activity.getString(R.string.media_checking))
        checking.job = activity.lifecycleScope.launch {
            val candidates = try {
                withContext(Dispatchers.IO) {
                    val entries = MediaLeaders(gedcom).list
                    entries.mapIndexedNotNull { index, entry ->
                        checking.post(index + 1, entries.size)
                        val resolved = MediaFileTools.resolve(activity, entry.media, treeId, false)
                        MediaFileTools.shortName(activity, entry.media, resolved, treeId)?.let { Triple(entry, it, resolved.file!!) }
                    }
                }
            } finally {
                checking.dismiss()
            }
            if (candidates.isEmpty()) {
                Snackbars.show(activity, activity.getString(R.string.media_shrink_none))
                return@launch
            }
            MaterialAlertDialogBuilder(activity)
                .setIcon(DsR.drawable.ds_ic_tune)
                .setTitle(R.string.shrink_path)
                .setMessage(activity.resources.getQuantityString(R.plurals.media_shrink_confirm_count, candidates.size, candidates.size))
                .setPositiveButton(R.string.shrink_path) { _, _ ->
                    MediaFileTools.listTreeDir(activity, treeId)
                    activity.lifecycleScope.launch {
                        // Only links that will find the same file with the name alone
                        val verified = withContext(Dispatchers.IO) {
                            candidates.filter { MediaFileTools.resolvesTo(treeId, it.second, it.third) }
                        }
                        verified.forEach { it.first.media.file = it.second }
                        if (verified.isNotEmpty()) {
                            AppUtils.save(true, *verified.map { it.first.leader }.distinct().toTypedArray())
                            onDone.run()
                        }
                        Snackbars.show(activity, activity.resources.getQuantityString(R.plurals.media_shrunk_count, verified.size, verified.size))
                    }
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    // Rename

    /** Renames the file of a media in app storage; every media linked to the file follows it. */
    @JvmStatic
    fun rename(activity: AppCompatActivity, media: Media, onDone: Runnable) {
        val gedcom = Global.gc ?: return
        val treeId = Global.settings.openTree
        val resolved = MediaFileTools.resolve(activity, media, treeId, false)
        val file = resolved.file
        if (!resolved.canRename() || file == null) return
        val view = LayoutInflater.from(activity).inflate(R.layout.media_rename, null)
        val layout = view.findViewById<TextInputLayout>(R.id.media_rename_layout)
        val edit = view.findViewById<TextInputEditText>(R.id.media_rename_edit)
        edit.setText(file.name)
        val dialog = MaterialAlertDialogBuilder(activity)
            .setIcon(DsR.drawable.ds_ic_edit_note)
            .setTitle(R.string.media_rename_file)
            .setView(view)
            .setPositiveButton(R.string.rename, null)
            .setNegativeButton(R.string.cancel, null)
            .create()
        fun validate(): Boolean {
            val error = MediaFileTools.checkNewName(file, edit.text.toString())
            layout.error = if (error != 0) activity.getString(error) else null
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = error == 0
            return error == 0
        }
        dialog.setOnShowListener {
            // Selects the name without extension, ready to type
            val dot = file.name.lastIndexOf('.').takeIf { it > 0 } ?: file.name.length
            edit.requestFocus()
            edit.setSelection(0, dot)
            dialog.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
            edit.doAfterTextChanged { validate() }
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (!validate()) return@setOnClickListener
                val newName = edit.text.toString().trim()
                dialog.dismiss()
                val leaders = MediaFileTools.renameFile(gedcom, treeId, file, newName)
                when {
                    leaders == null -> Snackbars.show(activity, activity.getString(R.string.media_rename_failed))
                    leaders.isEmpty() -> Unit // Same name
                    else -> {
                        Picasso.get().invalidate(file)
                        AppUtils.save(true, *leaders.toTypedArray())
                        onDone.run()
                        Snackbars.show(activity, activity.getString(R.string.media_renamed))
                    }
                }
            }
        }
        dialog.show()
    }

    // Utilities

    private fun entryOf(media: Media): MediaLeaders.Entry =
        MediaLeaders(Global.gc).list.firstOrNull { it.media === media } ?: MediaLeaders.Entry(media, null, media)

    /** Modal progress dialog with a Cancel button that cancels [job]. */
    private class Progress(private val activity: AppCompatActivity, title: Int) {
        var job: Job? = null
        private val view = LayoutInflater.from(activity).inflate(R.layout.media_tools_progress, null as ViewGroup?)
        private val textView = view.findViewById<TextView>(R.id.media_progress_text)
        private val bar = view.findViewById<LinearProgressIndicator>(R.id.media_progress_bar)
        private val dialog = MaterialAlertDialogBuilder(activity)
            .setIcon(DsR.drawable.ds_ic_image)
            .setTitle(title)
            .setView(view)
            .setCancelable(false)
            .setNegativeButton(R.string.cancel) { _, _ -> job?.cancel() }
            .show()

        fun text(text: String) {
            textView.text = text
        }

        /** Thread-safe progress update. */
        fun post(done: Int, total: Int) {
            bar.post {
                if (total <= 0) return@post
                if (bar.isIndeterminate) {
                    bar.visibility = View.INVISIBLE // Switching mode requires the indicator to be hidden
                    bar.isIndeterminate = false
                    bar.visibility = View.VISIBLE
                }
                bar.max = total
                bar.setProgressCompat(done, true)
            }
        }

        fun dismiss() {
            if (dialog.isShowing && !activity.isDestroyed) dialog.dismiss()
        }
    }
}
