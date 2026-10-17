package dna.familytree.feature.media

import android.content.Context
import android.net.Uri
import android.webkit.URLUtil
import androidx.documentfile.provider.DocumentFile
import dna.familytree.Global
import dna.familytree.R
import dna.familytree.util.FileUtils
import dna.familytree.visitor.MediaLeaders
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import org.folg.gedcom.model.Gedcom
import org.folg.gedcom.model.Media
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * File tools for media (adapted from Family Gem): where a media file is, copying it into the app storage of the tree,
 * shortening links, renaming files and reusing identical files.
 *
 * Files are always found with the same rules used to display them ([FileUtils.mediaPath], [FileUtils.mediaUri]),
 * and a media link is rewritten only after checking that it still leads to the expected file.
 */
object MediaFileTools {

    enum class Location {
        /** In the storage folder of the tree, managed by the app. */
        APP_STORAGE,
        /** Somewhere else on the device. */
        DEVICE,
        /** In a document folder chosen in Media folders. */
        DOCUMENT,
        /** A web address. */
        WEB,
        /** Linked file not found. */
        MISSING,
        /** No file linked. */
        NONE
    }

    /** The file of a media as found on the device. */
    class Resolved(@JvmField val location: Location, @JvmField val file: File? = null, @JvmField val uri: Uri? = null,
                   @JvmField val link: String? = null) {
        /** Identifies the file, to treat media linked to the same file together. */
        val key: String? get() = file?.let { canonical(it) } ?: uri?.toString() ?: link
        fun canCopy() = location == Location.DEVICE || location == Location.DOCUMENT || location == Location.WEB
        fun canRename() = location == Location.APP_STORAGE
    }

    private val NOT_FILES = arrayOf("text/html", "text/javascript", "application/json", "text/css", "text/xml", "application/xhtml")
    private const val BUFFER = 64 * 1024
    private const val CONNECT_TIMEOUT = 15_000 // ms
    private const val READ_TIMEOUT = 30_000

    /** Storage folder of the tree, e.g. '/storage/emulated/0/Android/data/<package>/files/5'. */
    @JvmStatic
    @JvmOverloads
    fun treeDir(context: Context, treeId: Int = Global.settings.openTree): File? = context.getExternalFilesDir(treeId.toString())

    @JvmStatic
    fun isWebLink(link: String?) = link != null && (link.startsWith("http://", true) || link.startsWith("https://", true))

    /**
     * Finds the file of a media. May access the disk, and with [withDocuments] also the document folders (slow):
     * call it on a background thread for many media.
     */
    @JvmStatic
    @JvmOverloads
    fun resolve(context: Context, media: Media, treeId: Int = Global.settings.openTree, withDocuments: Boolean = true,
                knownUri: Uri? = null): Resolved {
        val link = media.file?.trim()
        if (link.isNullOrEmpty()) return Resolved(Location.NONE)
        if (isWebLink(link)) return Resolved(Location.WEB, link = link)
        FileUtils.mediaPath(treeId, media)?.let { path ->
            val file = File(path)
            if (!isInside(file, context.cacheDir)) { // The cache of web images is not a linked file
                val dir = treeDir(context, treeId)
                val location = if (dir != null && isInside(file, dir)) Location.APP_STORAGE else Location.DEVICE
                return Resolved(location, file = file, link = link)
            }
        }
        val uri = knownUri ?: if (withDocuments) runCatching { FileUtils.mediaUri(treeId, media) }.getOrNull() else null
        return if (uri != null) Resolved(Location.DOCUMENT, uri = uri, link = link) else Resolved(Location.MISSING, link = link)
    }

    // Copy to app storage

    /** One file to copy, with all the media linked to it. */
    class CopyGroup(val resolved: Resolved, val entries: List<MediaLeaders.Entry>)

    /** Media of the tree whose file can be copied into app storage, grouped by file. Background thread. */
    suspend fun findCopyable(context: Context, gedcom: Gedcom, treeId: Int, onProgress: (Int, Int) -> Unit): List<CopyGroup> {
        val entries = MediaLeaders(gedcom).list.filterNot { it.media.file.isNullOrBlank() }
        val groups = LinkedHashMap<String, MutableList<MediaLeaders.Entry>>()
        val resolvedByKey = HashMap<String, Resolved>()
        entries.forEachIndexed { index, entry ->
            currentCoroutineContext().ensureActive()
            val resolved = resolve(context, entry.media, treeId)
            if (resolved.canCopy()) {
                val key = resolved.key!!
                resolvedByKey.putIfAbsent(key, resolved)
                groups.getOrPut(key) { mutableListOf() }.add(entry)
            }
            onProgress(index + 1, entries.size)
        }
        return groups.map { (key, list) -> CopyGroup(resolvedByKey[key]!!, list) }
    }

    /** Total bytes of the files, where known. */
    fun knownSize(context: Context, groups: List<CopyGroup>): Long = groups.sumOf { group ->
        val r = group.resolved
        when {
            r.file != null -> r.file.length()
            r.uri != null -> runCatching { DocumentFile.fromSingleUri(context, r.uri)?.length() ?: 0 }.getOrDefault(0)
            else -> 0L
        }
    }

    /**
     * Copies a file into app storage, keeping its name (a number is added if the name is taken).
     * The copy is written to a temporary file and checked before getting its name. Background thread.
     * @return The new file
     */
    suspend fun copyToAppStorage(context: Context, resolved: Resolved, treeId: Int): File {
        val dir = treeDir(context, treeId) ?: throw IOException(context.getString(R.string.media_error_storage))
        if (!dir.isDirectory && !dir.mkdirs()) throw IOException(context.getString(R.string.media_error_storage))
        var connection: HttpURLConnection? = null
        try {
            var expected = -1L
            var contentType: String? = null
            val input: InputStream = when (resolved.location) {
                Location.DEVICE -> FileInputStream(resolved.file!!).also { expected = resolved.file.length() }
                Location.DOCUMENT -> {
                    expected = runCatching { DocumentFile.fromSingleUri(context, resolved.uri!!)?.length() ?: -1 }.getOrDefault(-1)
                    context.contentResolver.openInputStream(resolved.uri!!)
                        ?: throw IOException(context.getString(R.string.media_error_read, resolved.link))
                }
                Location.WEB -> {
                    connection = (URL(resolved.link).openConnection() as HttpURLConnection).apply {
                        connectTimeout = CONNECT_TIMEOUT
                        readTimeout = READ_TIMEOUT
                        instanceFollowRedirects = true
                    }
                    val code = connection.responseCode
                    if (code != HttpURLConnection.HTTP_OK) throw IOException(context.getString(R.string.media_error_server, code))
                    val type = connection.contentType
                    contentType = type
                    if (type != null && NOT_FILES.any { type.startsWith(it, true) })
                        throw IOException(context.getString(R.string.media_error_web_page))
                    expected = connection.contentLengthLong
                    connection.inputStream
                }
                else -> throw IllegalArgumentException("Nothing to copy from ${resolved.location}") // Never: see canCopy()
            }
            val name = safeName(when {
                resolved.file != null -> resolved.file.name
                resolved.uri != null -> runCatching { DocumentFile.fromSingleUri(context, resolved.uri)?.name }.getOrNull()
                else -> URLUtil.guessFileName(resolved.link, null, contentType)
            }) ?: "media"
            val temp = File.createTempFile("copy", ".part", dir)
            try {
                input.use { source ->
                    temp.outputStream().use { target ->
                        val buffer = ByteArray(BUFFER)
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val read = source.read(buffer)
                            if (read < 0) break
                            target.write(buffer, 0, read)
                        }
                    }
                }
                if (expected > 0 && temp.length() != expected) throw IOException(context.getString(R.string.media_error_incomplete, name))
                val target = availableFile(dir, name)
                if (!temp.renameTo(target)) throw IOException(context.getString(R.string.media_error_create, target.name))
                return target
            } finally {
                if (temp.exists()) temp.delete()
            }
        } finally {
            connection?.disconnect()
        }
    }

    /** Links the media to their copy in app storage, which becomes a media folder of the tree. Main thread. */
    fun linkToCopy(treeId: Int, entries: List<MediaLeaders.Entry>, copy: File): List<Any> {
        val tree = Global.settings.getTree(treeId)
        if (tree.dirs.add(copy.parent)) Global.settings.save()
        entries.forEach { it.media.file = copy.absolutePath }
        return entries.map { it.leader }
    }

    // Shorten paths

    /**
     * The file name only, if the media is linked with a longer path to a file directly in app storage.
     * Before using it, [resolvesTo] must confirm that the name alone finds the same file.
     */
    @JvmStatic
    fun shortName(context: Context, media: Media, resolved: Resolved, treeId: Int = Global.settings.openTree): String? {
        if (resolved.location != Location.APP_STORAGE) return null
        val link = media.file?.replace('\\', '/') ?: return null
        if (!link.contains('/')) return null // Already a name only
        val file = resolved.file ?: return null
        val dir = treeDir(context, treeId) ?: return null
        if (!sameFile(file.parentFile, dir) || File(link).name != file.name) return null
        return file.name
    }

    /** A link made of [name] only finds [file], with the media folders of the tree as they are now. */
    @JvmStatic
    fun resolvesTo(treeId: Int, name: String, file: File): Boolean {
        val probe = Media().apply { this.file = name }
        return FileUtils.mediaPath(treeId, probe)?.let { sameFile(File(it), file) } == true
    }

    /** Makes sure the storage folder of the tree is one of its media folders (as when adding a media). Main thread. */
    fun listTreeDir(context: Context, treeId: Int) {
        val dir = treeDir(context, treeId) ?: return
        if (Global.settings.getTree(treeId).dirs.add(dir.path)) Global.settings.save()
    }

    // Rename

    /** Error message for a new file name, or 0 if valid. */
    @JvmStatic
    fun checkNewName(file: File, newName: String): Int {
        val name = newName.trim()
        if (name.isEmpty() || name == "." || name == ".." || name.length > 255 || safeName(name) != name) return R.string.invalid_name
        if (name == file.name) return 0
        val other = File(file.parentFile, name)
        // On case-insensitive storage a different case is the same file
        if (other.exists() && !sameFile(other, file)) return R.string.existing_name
        return 0
    }

    /** Media of the tree linked to [file]. */
    fun mediaLinkedTo(gedcom: Gedcom, treeId: Int, file: File): List<MediaLeaders.Entry> =
        MediaLeaders(gedcom).list.filter { entry ->
            val link = entry.media.file?.replace('\\', '/')
            link != null && File(link).name.equals(file.name, true)
                    && FileUtils.mediaPath(treeId, entry.media)?.let { sameFile(File(it), file) } == true
        }

    /**
     * Renames a file in app storage and updates every media linked to it.
     * @return The leader objects of the updated media, or null if the file could not be renamed
     */
    fun renameFile(gedcom: Gedcom, treeId: Int, file: File, newName: String): List<Any>? {
        val name = newName.trim()
        if (name == file.name) return emptyList()
        val linked = mediaLinkedTo(gedcom, treeId, file)
        val target = File(file.parentFile, name)
        if (!file.renameTo(target)) return null
        linked.forEach { entry ->
            val link = entry.media.file.replace('\\', '/')
            // Name-only links stay short if the new name finds the file, otherwise the full path is used
            entry.media.file = if (!link.contains('/') && resolvesTo(treeId, name, target)) name else target.absolutePath
        }
        return linked.map { it.leader }.distinct()
    }

    // Reuse of identical files

    /**
     * A file in app storage with the given name and the same content as [uri], to reuse instead of making another copy.
     */
    @JvmStatic
    fun findIdenticalFile(context: Context, uri: Uri, name: String?): File? = try {
        val dir = treeDir(context)
        val candidate = if (dir != null && !name.isNullOrBlank()) File(dir, name) else null
        if (candidate == null || !candidate.isFile) null
        else {
            val size = DocumentFile.fromSingleUri(context, uri)?.length() ?: 0
            if (size <= 0 || size != candidate.length()) null
            else context.contentResolver.openInputStream(uri)?.use { a ->
                FileInputStream(candidate).use { b -> if (sameBytes(a, b)) candidate else null }
            }
        }
    } catch (e: Exception) {
        null
    }

    /**
     * Another media of the open tree is linked to [file], other than [media]:
     * cropping must then write a new file instead of changing that one.
     */
    @JvmStatic
    fun isLinkedByOtherMedia(file: File, media: Media?): Boolean = try {
        val gedcom = Global.gc
        gedcom != null && mediaLinkedTo(gedcom, Global.settings.openTree, file).any { it.media !== media }
    } catch (e: Exception) {
        false
    }

    // Utilities

    /** [name] in [dir], or 'name1.ext', 'name2.ext'... if taken. */
    @JvmStatic
    fun availableFile(dir: File, name: String): File {
        val dot = name.lastIndexOf('.').takeIf { it >= 0 } ?: name.length // As the app always did, also for '.name'
        val base = name.substring(0, dot)
        val extension = name.substring(dot)
        var file = File(dir, name)
        var increment = 0
        while (file.exists()) file = File(dir, "$base${++increment}$extension")
        return file
    }

    /** A valid file name, or null. */
    @JvmStatic
    fun safeName(name: String?): String? = name?.trim()
        ?.replace(Regex("[\\\\/:*?\"<>|\\u0000-\\u001F]"), "_")
        ?.takeIf { it.isNotEmpty() && it != "." && it != ".." }

    @JvmStatic
    fun isInside(file: File, dir: File): Boolean = canonical(file).startsWith(canonical(dir) + File.separator)

    @JvmStatic
    fun sameFile(a: File?, b: File?): Boolean = a != null && b != null && canonical(a) == canonical(b)

    private fun canonical(file: File): String = try {
        file.canonicalPath
    } catch (e: IOException) {
        file.absolutePath
    }

    private fun sameBytes(a: InputStream, b: InputStream): Boolean {
        val bufferA = ByteArray(BUFFER)
        val bufferB = ByteArray(BUFFER)
        while (true) {
            val readA = a.readNBytesCompat(bufferA)
            val readB = b.readNBytesCompat(bufferB)
            if (readA != readB) return false
            if (readA <= 0) return true
            for (i in 0 until readA) if (bufferA[i] != bufferB[i]) return false
        }
    }

    /** Fills the buffer as much as possible: returns the bytes read, 0 at the end. */
    private fun InputStream.readNBytesCompat(buffer: ByteArray): Int {
        var total = 0
        while (total < buffer.size) {
            val read = read(buffer, total, buffer.size - total)
            if (read < 0) break
            total += read
        }
        return total
    }
}
