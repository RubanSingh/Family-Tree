package dna.familytree.feature.media

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dna.familytree.Global
import dna.familytree.MediaFoldersController
import dna.familytree.R
import dna.familytree.Settings
import dna.familytree.constant.Extra
import dna.familytree.util.FileUtils
import java.io.File

/**
 * Photos and videos kept outside the app storage on Android 13+, where the app doesn't hold READ_MEDIA_IMAGES /
 * READ_MEDIA_VIDEO (Google Play photo and video permissions policy).
 *
 * The user grants a folder once with the system folder picker (a tree URI in Media folders), and media files are
 * found through it by their original path, so trees keep their links unchanged. Android 12 and older keep reading
 * paths with READ_EXTERNAL_STORAGE as before.
 */
object MediaAccess {

    /** Extra of [MediaFoldersController]: grants again, one after the other, the folders that lost access. */
    const val EXTRA_RECONNECT = "reconnect"

    private const val EXTERNAL_STORAGE = "com.android.externalstorage.documents"
    private const val PREFS = "media_access"
    private const val DELAY = 1200L // ms, after the tree has opened

    /** Android 13+: files outside the app storage are readable only through a folder granted by the user. */
    @JvmStatic
    fun needsGrantedFolders(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    /** The media folders of the tree that must be granted again before their files can be read. */
    @JvmStatic
    fun foldersToReconnect(context: Context, tree: Settings.Tree?): List<String> {
        if (tree == null || !needsGrantedFolders()) return emptyList()
        val granted = tree.uris.mapNotNull { treePath(Uri.parse(it)) }
        return tree.dirs.filter { dir ->
            dir.startsWith("/") && !isOwnStorage(context, dir) && granted.none { covers(it, dir) }
        }
    }

    /** Android 11+ doesn't let the folder picker grant the storage roots, Download and Android/data or obb. */
    @JvmStatic
    fun isRestricted(context: Context, dir: String): Boolean {
        val root = volumes(context).firstOrNull { covers(it.first, dir) }?.first ?: return false
        val relative = dir.removePrefix(root).trim('/')
        return relative.isEmpty() || relative.equals(Environment.DIRECTORY_DOWNLOADS, true)
                || covers("Android/data", relative) || covers("Android/obb", relative)
    }

    /** Where the folder picker opens to grant [dir] again. */
    @JvmStatic
    fun initialUri(context: Context, dir: String): Uri? {
        val (root, volume) = volumes(context).firstOrNull { covers(it.first, dir) } ?: return null
        return DocumentsContract.buildDocumentUri(EXTERNAL_STORAGE, volume + ":" + dir.removePrefix(root).trim('/'))
    }

    /**
     * Finds the file of a media through the folders granted to the tree: by the path written in the media, then
     * relative to each media folder and granted folder (the same combinations as [FileUtils.mediaPath]).
     */
    @JvmStatic
    fun resolve(context: Context, tree: Settings.Tree?, file: String): Uri? {
        if (tree == null || tree.uris.isEmpty()) return null
        val grants = tree.uris.mapNotNull { string ->
            val uri = Uri.parse(string)
            if (uri.authority == EXTERNAL_STORAGE) treePath(uri)?.let { uri to it } else null
        }
        if (grants.isEmpty()) return null
        val name = file.replace('\\', '/')
        val filename = File(name).name
        val candidates = LinkedHashSet<String>()
        if (name.startsWith("/")) candidates += name
        for (dir in tree.dirs + grants.map { it.second }) {
            if (!name.startsWith("/")) candidates += "$dir/$name"
            candidates += "$dir/$filename"
        }
        for (candidate in candidates)
            for ((uri, path) in grants) {
                if (!covers(path, candidate)) continue
                val document = documentUri(uri, path, candidate) ?: continue
                if (isFile(context, document)) return document
            }
        return null
    }

    /**
     * When the tree opens, until done or declined: invites to grant again the photo folders that lost access with
     * Android 13+. Folders the picker can't grant (Download...) don't count, as reconnecting wouldn't help.
     */
    @JvmStatic
    fun maybeAskReconnect(activity: Activity, treeId: Int) {
        if (!needsGrantedFolders() || isDismissed(activity, treeId)
            || foldersToReconnect(activity, Global.settings.getTree(treeId)).all { isRestricted(activity, it) }) return
        activity.window.decorView.postDelayed({
            // Not over another dialog, like the privacy consent
            if (activity.isFinishing || activity.isDestroyed || !activity.hasWindowFocus()) return@postDelayed
            MaterialAlertDialogBuilder(activity)
                .setIcon(dna.familytree.designsystem.R.drawable.ds_ic_image)
                .setTitle(R.string.media_reconnect_title)
                .setMessage(R.string.media_reconnect_message)
                .setPositiveButton(R.string.media_reconnect) { _, _ ->
                    activity.startActivity(Intent(activity, MediaFoldersController::class.java)
                        .putExtra(Extra.TREE_ID, treeId).putExtra(EXTRA_RECONNECT, true))
                }
                .setNegativeButton(R.string.media_reconnect_later) { _, _ -> dismiss(activity, treeId) }
                .show()
        }, DELAY)
    }

    private fun isDismissed(context: Context, treeId: Int) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("dismissed_$treeId", false)

    private fun dismiss(context: Context, treeId: Int) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("dismissed_$treeId", true).apply()

    /** Absolute path of the folder of a tree URI, or null if it isn't a local folder. */
    private fun treePath(uri: Uri): String? = runCatching { FileUtils.uriFolderPath(uri) }.getOrNull()

    /** Is [path] the folder [folder] or inside it? */
    private fun covers(folder: String, path: String) = path == folder || path.startsWith("$folder/")

    /** Folders of the app, readable without any permission. */
    private fun isOwnStorage(context: Context, path: String): Boolean {
        val own = (context.getExternalFilesDirs(null).mapNotNull { it?.parentFile } + // .../Android/data/<package>
                File(context.applicationInfo.dataDir)).map { canonical(it) }
        val canonicalPath = canonical(File(path)) // Also '/data/data/<package>' as '/data/user/0/<package>'
        return own.any { covers(it, path) || covers(it, canonicalPath) }
    }

    private fun canonical(file: File): String = runCatching { file.canonicalPath }.getOrDefault(file.absolutePath)

    /** Root path and document volume id of each storage, e.g. '/storage/emulated/0' and 'primary'. */
    private fun volumes(context: Context): List<Pair<String, String>> {
        val volumes = mutableListOf(Environment.getExternalStorageDirectory().absolutePath to "primary")
        for (dir in context.getExternalFilesDirs(null)) {
            val path = dir?.absolutePath ?: continue
            val index = path.indexOf("/Android/")
            if (index <= 0) continue
            val root = path.substring(0, index)
            if (volumes.none { it.first == root }) volumes += root to root.substringAfterLast('/')
        }
        return volumes
    }

    /** Document of [path] inside the granted folder [treeUri] at [treePath]: 'primary:DCIM' → 'primary:DCIM/Camera/a.jpg'. */
    private fun documentUri(treeUri: Uri, treePath: String, path: String): Uri? = runCatching {
        val treeId = DocumentsContract.getTreeDocumentId(treeUri)
        val relative = path.removePrefix(treePath).trim('/')
        val documentId = when {
            relative.isEmpty() -> treeId
            treeId.endsWith(":") -> treeId + relative // Root of a storage
            else -> "$treeId/$relative"
        }
        DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
    }.getOrNull()

    private fun isFile(context: Context, document: Uri): Boolean = runCatching {
        context.contentResolver.query(document, arrayOf(DocumentsContract.Document.COLUMN_MIME_TYPE), null, null, null)
            ?.use { it.moveToFirst() && it.getString(0).let { mime -> mime != null && mime != DocumentsContract.Document.MIME_TYPE_DIR } }
            ?: false
    }.getOrDefault(false)
}
