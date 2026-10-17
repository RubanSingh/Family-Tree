package dna.familytree.feature.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.gson.JsonParser
import dna.familytree.Exporter
import dna.familytree.Global
import dna.familytree.R
import dna.familytree.Settings
import dna.familytree.feature.billing.PremiumGate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.joda.time.LocalDateTime
import org.joda.time.format.DateTimeFormat
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.ZipInputStream

/**
 * Local backup of the trees into a folder chosen by the user (adapted from Family Gem).
 *
 * After a tree is saved, its backup is scheduled one minute later (saving again restarts the wait),
 * and any pending backup runs at once when the app goes to the background.
 * Each backup is a ZIP named `<treeId>_<yyyyMMdd>_<HHmmss>.zip`, in the same format as the manual ZIP backup,
 * so it can be restored from here or with "Recover a ZIP backup". Only the 3 newest per tree are kept.
 *
 * Nothing happens until a folder is chosen.
 */
object BackupManager {

    /** [Settings.backupUri] value meaning that no folder was chosen yet. */
    const val NO_URI = "noUri"
    private const val TAG = "BackupManager"
    private const val DELAY_MILLIS = 60_000L
    private const val KEEP_PER_TREE = 3
    private const val PART = ".part"
    private val TREE_ID = "^(\\d+)_".toRegex()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val pending = ConcurrentHashMap<Int, Job>()
    private val running = Mutex() // One backup at a time
    private lateinit var appContext: Context

    /** A backup file found in the folder. */
    data class BackupFile(val document: DocumentFile, val treeId: Int, val title: String?, val valid: Boolean,
                          val modified: Long, val size: Long)

    /**
     * Called once at app start, after the settings are loaded.
     * Settings saved before version 1.6 get the backup switched on for every tree, but no folder: nothing is backed up yet.
     */
    @JvmStatic
    fun init(context: Context) {
        appContext = context.applicationContext
        val settings = Global.settings ?: return
        if (settings.backupUri == null) {
            settings.backup = true
            settings.backupUri = NO_URI
            settings.trees?.forEach { it.backup = true }
            settings.save()
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) = flushPending()
        })
    }

    /** The chosen backup folder, or null. */
    @JvmStatic
    fun folderUri(): Uri? = Global.settings?.backupUri?.takeIf { it != NO_URI }?.let(Uri::parse)

    /** Readable folder name, e.g. "primary:Documents/Backups" → "Documents/Backups" (full path in expert mode). */
    @JvmStatic
    fun folderLabel(): String? {
        val uri = folderUri() ?: return null
        val path = Uri.decode(uri.lastPathSegment ?: return uri.toString())
        return if (Global.settings.expert) path else path.substringAfter(':').ifEmpty { path }
    }

    /** Automatic backup switched on, with Premium. */
    @JvmStatic
    fun isAutomatic(): Boolean = Global.settings?.backup == true && PremiumGate.isPremium()

    /** Automatic backup switched on and folder chosen. */
    @JvmStatic
    fun isActive(): Boolean = isAutomatic() && folderUri() != null

    /** The trees screen invites to choose a folder: automatic backup on, no folder yet, at least one tree, not dismissed. */
    @JvmStatic
    fun shouldPromptForFolder(): Boolean {
        val settings = Global.settings ?: return false
        return isAutomatic() && folderUri() == null && !settings.backupPromptDismissed && !settings.trees.isNullOrEmpty()
    }

    /** Remembers the folder picked with ACTION_OPEN_DOCUMENT_TREE, keeping write access across restarts. */
    fun setFolder(context: Context, uri: Uri) {
        context.contentResolver.takePersistableUriPermission(uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        Global.settings.backupUri = uri.toString()
        Global.settings.backup = true
        Global.settings.save()
    }

    /** The folder, if it can still be written; otherwise an error with a message for the user. */
    fun openFolder(context: Context): Result<DocumentFile> = try {
        val uri = folderUri() ?: throw IllegalStateException(context.getString(R.string.backup_no_folder))
        val folder = DocumentFile.fromTreeUri(context, uri)
        if (folder == null || !folder.isDirectory) throw IllegalStateException(context.getString(R.string.backup_folder_unavailable))
        if (context.contentResolver.persistedUriPermissions.none { it.uri == uri && it.isWritePermission })
            throw IllegalStateException(context.getString(R.string.backup_folder_not_writable))
        Result.success(folder)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /** Called after a tree is saved: backs it up one minute later. Never throws. */
    @JvmStatic
    fun schedule(treeId: Int) {
        try {
            if (!wanted(treeId)) return
            pending.remove(treeId)?.cancel()
            lateinit var job: Job
            job = scope.launch {
                delay(DELAY_MILLIS)
                if (pending.remove(treeId, job) && wanted(treeId)) backup(treeId) // Still wanted, and not replaced by a newer save
            }
            pending[treeId] = job
        } catch (e: Exception) {
            Log.w(TAG, "Can't schedule backup of tree $treeId", e)
        }
    }

    /** Automatic backup is on and includes the tree. */
    private fun wanted(treeId: Int): Boolean = isActive() && Global.settings.getTree(treeId)?.backup == true

    /** Runs the scheduled backups now (the app is going to the background). */
    private fun flushPending() {
        pending.keys.toList().forEach { treeId ->
            pending.remove(treeId)?.let { job -> // Null if the backup already started
                job.cancel()
                scope.launch { if (wanted(treeId)) backup(treeId) }
            }
        }
    }

    /**
     * Backs up one tree now; old backups of the same tree beyond [KEEP_PER_TREE] are deleted.
     * The ZIP is written with a temporary name, so an interrupted backup never looks like a valid one.
     */
    suspend fun backup(treeId: Int): Result<DocumentFile> = withContext(Dispatchers.IO) {
        running.withLock {
            val context = appContext
            val workDir = File(context.cacheDir, "backup").apply { mkdirs() }
            try {
                val folder = openFolder(context).getOrThrow()
                // Leftovers of interrupted backups
                folder.listFiles().filter { it.name?.startsWith("${treeId}_") == true && it.name?.endsWith(PART) == true }
                    .forEach { it.delete() }
                val exporter = Exporter(context)
                if (!exporter.openTreeForBackup(treeId, workDir)) throw IllegalStateException(exporter.errorMessage)
                val name = "${treeId}_${LocalDateTime.now().toString(DateTimeFormat.forPattern("yyyyMMdd_HHmmss"))}.zip"
                val file = write(folder, name, exporter, workDir)
                // Keeps only the newest backups of this tree: the name contains date and time
                val pattern = "^${treeId}_\\d{8}_\\d{6}\\.zip$".toRegex()
                folder.listFiles().filter { it.isFile && it.name?.matches(pattern) == true }
                    .sortedByDescending { it.name }.drop(KEEP_PER_TREE).forEach { it.delete() }
                Log.i(TAG, "Backup of tree $treeId completed")
                Result.success(file)
            } catch (e: Throwable) {
                Log.w(TAG, "Backup of tree $treeId failed: ${e.message}")
                Result.failure(e)
            } finally {
                workDir.listFiles()?.forEach { it.delete() }
            }
        }
    }

    /** Writes the ZIP as '<name>.part' and renames it when complete; directly with its name if the folder can't rename. */
    private fun write(folder: DocumentFile, name: String, exporter: Exporter, workDir: File): DocumentFile {
        val context = appContext
        val temp = folder.createFile("application/octet-stream", name + PART)
        if (temp != null) {
            if (!exporter.exportBackup(temp.uri, workDir)) {
                temp.delete()
                throw IllegalStateException(exporter.errorMessage)
            }
            if (runCatching { temp.renameTo(name) }.getOrDefault(false)) return temp
            temp.delete()
        }
        val file = folder.createFile("application/zip", name)
            ?: throw IllegalStateException(context.getString(R.string.backup_cant_create, name))
        if (!exporter.exportBackup(file.uri, workDir)) {
            file.delete()
            throw IllegalStateException(exporter.errorMessage)
        }
        return file
    }

    /** Backup files in the folder, newest first, each checked for a valid 'settings.json'. Call on a background thread. */
    fun listBackups(context: Context): Result<List<BackupFile>> = openFolder(context).mapCatching { folder ->
        folder.listFiles().filter { it.isFile && it.name?.endsWith(".zip", true) == true }
            .mapNotNull { document ->
                val name = document.name ?: return@mapNotNull null
                val treeId = TREE_ID.find(name)?.groupValues?.get(1)?.toIntOrNull() ?: -1
                val title = try {
                    ZipInputStream(context.contentResolver.openInputStream(document.uri)).use { zip ->
                        generateSequence { zip.nextEntry }.firstOrNull { it.name == "settings.json" }?.let {
                            // No reflection: stays valid when R8 renames fields
                            JsonParser.parseString(zip.bufferedReader().readText()).asJsonObject.get("title")
                                ?.takeIf { it.isJsonPrimitive }?.asString
                        }
                    }
                } catch (e: Exception) {
                    null
                }
                BackupFile(document, treeId, title, title != null, document.lastModified(), document.length())
            }.sortedByDescending { it.modified }
    }
}
