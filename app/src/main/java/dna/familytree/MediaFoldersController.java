package dna.familytree;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.documentfile.provider.DocumentFile;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

import dna.familytree.constant.Extra;
import dna.familytree.feature.media.MediaAccess;
import dna.familytree.util.AnalyticsUtil;
import dna.familytree.util.FileUtils;

// Activity where you can see the list of media folders, add them, delete them
public class MediaFoldersController extends BaseController {

    int treeId;
    List<String> dirs;
    List<String> uris;
    String reconnectingDir; // Folder being granted again with the folder picker (Android 13+)
    boolean reconnectAll; // Opened by the reconnect invitation: grants again the folders one after the other

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.cartelle_media);
        treeId = getIntent().getIntExtra(Extra.TREE_ID, 0);
        dirs = new ArrayList<>(Global.settings.getTree(treeId).dirs);
        uris = new ArrayList<>(Global.settings.getTree(treeId).uris);
        updateList();
        setToolbar();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        findViewById(R.id.fab).setOnClickListener(v -> {
            // Android 13+: the folder picker itself grants the access, no permission needed
            if (MediaAccess.needsGrantedFolders()) {
                chooseFolder();
                return;
            }
            final String[] requiredPermissions = {Manifest.permission.READ_EXTERNAL_STORAGE};
            final int perm = FileUtils.checkMultiplePermissions(this, requiredPermissions);
            if (perm == PackageManager.PERMISSION_DENIED)
                ActivityCompat.requestPermissions(this, requiredPermissions, 3517);
            else if (perm == PackageManager.PERMISSION_GRANTED)
                chooseFolder();
        });
        if (Global.settings.getTree(treeId).dirs.isEmpty() && Global.settings.getTree(treeId).uris.isEmpty())
            new SpeechBubble(this, R.string.add_device_folder).show();
        if (bundle != null) {
            reconnectingDir = bundle.getString("reconnectingDir");
            reconnectAll = bundle.getBoolean("reconnectAll");
        } else if (getIntent().getBooleanExtra(MediaAccess.EXTRA_RECONNECT, false)) {
            reconnectAll = true;
            reconnectNext();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("reconnectingDir", reconnectingDir);
        outState.putBoolean("reconnectAll", reconnectAll);
    }

    /**
     * Opens the folder picker on the next folder that lost access, if any.
     */
    void reconnectNext() {
        for (String dir : MediaAccess.foldersToReconnect(this, Global.settings.getTree(treeId))) {
            if (!MediaAccess.isRestricted(this, dir)) {
                reconnect(dir);
                return;
            }
        }
        reconnectAll = false;
    }

    /**
     * Grants again a media folder with the folder picker, opened right on it.
     */
    void reconnect(String dir) {
        reconnectingDir = dir;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        Uri initialUri = MediaAccess.initialUri(this, dir);
        if (initialUri != null)
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, initialUri);
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(intent, 124);
    }

    @Override
    protected void onResume() {
        super.onResume();
        AnalyticsUtil.logEventMediaFolderActivity(getFirebaseAnalytics());
    }

    void chooseFolder() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(intent, 123);
        } else {
            // KitKat uses the selection of a file to locate the container folder
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            startActivityForResult(intent, 456);
        }
    }

    void updateList() {
        LinearLayout layout = findViewById(R.id.cartelle_scatola);
        layout.removeAllViews();
        List<String> toReconnect = MediaAccess.foldersToReconnect(this, Global.settings.getTree(treeId));
        for (String dir : dirs) {
            View folderView = getLayoutInflater().inflate(R.layout.pezzo_cartella, layout, false);
            layout.addView(folderView);
            TextView nameView = folderView.findViewById(R.id.cartella_nome);
            TextView urlView = folderView.findViewById(R.id.cartella_url);
            urlView.setText(dir);
            if (Global.settings.expert)
                urlView.setSingleLine(false);
            View deleteButton = folderView.findViewById(R.id.cartella_elimina);
            // The '/storage/.../Android/data/dna.familytree/files/X' folder should be preserved as it is the default copied media
            // Moreover, in Android 11 it is no longer reachable by the user with SAF
            if (dir.equals(getExternalFilesDir(null) + "/" + treeId)) {
                nameView.setText(R.string.app_storage);
                deleteButton.setVisibility(View.GONE);
            } else {
                nameView.setText(folderName(dir));
                deleteButton.setOnClickListener(v -> {
                    new MaterialAlertDialogBuilder(this).setMessage(R.string.sure_delete)
                            .setPositiveButton(R.string.yes, (di, id) -> {
                                dirs.remove(dir);
                                save();
                            }).setNeutralButton(R.string.cancel, null).show();
                });
            }
            // Android 13+: the files of this folder are readable only after granting it with the folder picker
            if (toReconnect.contains(dir)) {
                TextView statusView = folderView.findViewById(R.id.cartella_stato);
                statusView.setVisibility(View.VISIBLE);
                if (MediaAccess.isRestricted(this, dir)) {
                    statusView.setText(R.string.media_folder_restricted);
                    statusView.setTextColor(getResources().getColor(R.color.gray_text));
                } else {
                    statusView.setText(R.string.media_folder_reconnect);
                    folderView.setOnClickListener(v -> reconnect(dir));
                }
            }
            registerForContextMenu(folderView);
        }
        for (String uriString : uris) {
            View uriView = getLayoutInflater().inflate(R.layout.pezzo_cartella, layout, false);
            layout.addView(uriView);
            DocumentFile documentDir = DocumentFile.fromTreeUri(this, Uri.parse(uriString));
            String name = null;
            if (documentDir != null)
                name = documentDir.getName();
            ((TextView)uriView.findViewById(R.id.cartella_nome)).setText(name);
            TextView urlView = uriView.findViewById(R.id.cartella_url);
            if (Global.settings.expert) {
                urlView.setSingleLine(false);
                urlView.setText(uriString);
            } else
                urlView.setText(Uri.decode(uriString)); // lo mostra decodificato cioè un po' più leggibile
            uriView.findViewById(R.id.cartella_elimina).setOnClickListener(v -> {
                new MaterialAlertDialogBuilder(this).setMessage(R.string.sure_delete)
                        .setPositiveButton(R.string.yes, (di, id) -> {
                            // Revoca il permesso per questo uri, se l'uri non è usato in nessun altro albero
                            boolean uriEsisteAltrove = false;
                            for (Settings.Tree albero : Global.settings.trees) {
                                for (String uri : albero.uris)
                                    if (uri.equals(uriString) && albero.id != treeId) {
                                        uriEsisteAltrove = true;
                                        break;
                                    }
                            }
                            if (!uriEsisteAltrove)
                                revokeUriPermission(Uri.parse(uriString), Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            uris.remove(uriString);
                            save();
                        }).setNeutralButton(R.string.cancel, null).show();
            });
            registerForContextMenu(uriView);
        }
    }

    String folderName(String url) {
        if (url.lastIndexOf('/') > 0)
            return url.substring(url.lastIndexOf('/') + 1);
        return url;
    }

    void save() {
        Global.settings.getTree(treeId).dirs.clear();
        for (String path : dirs)
            Global.settings.getTree(treeId).dirs.add(path);
        Global.settings.getTree(treeId).uris.clear();
        for (String uri : uris)
            Global.settings.getTree(treeId).uris.add(uri);
        Global.settings.save();
        updateList();
        Global.edited = true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 124) {
            String dir = reconnectingDir;
            reconnectingDir = null;
            boolean granted = resultCode == Activity.RESULT_OK && data != null && data.getData() != null
                    && grantFolder(data.getData()) && dir != null
                    && !MediaAccess.foldersToReconnect(this, Global.settings.getTree(treeId)).contains(dir);
            if (granted && reconnectAll)
                reconnectNext();
            else
                reconnectAll = false; // Canceled, or another folder chosen
            return;
        }
        if (resultCode == Activity.RESULT_OK) {
            Uri uri = data.getData();
            if (uri != null) {
                // In KitKat a file has been selected and we get the path of the folder
                if (requestCode == 456) {
                    String path = FileUtils.uriPathFolderKitKat(this, uri);
                    if (path != null) {
                        dirs.add(path);
                        save();
                    }
                } else if (requestCode == 123) {
                    // Android 13+ keeps the granted folder: its path alone would not be readable
                    String path = MediaAccess.needsGrantedFolders() ? null : FileUtils.uriFolderPath(uri);
                    if (path != null) {
                        dirs.add(path);
                        save();
                    } else {
                        getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        DocumentFile docDir = DocumentFile.fromTreeUri(this, uri);
                        if (docDir != null && docDir.canRead()) {
                            uris.add(uri.toString());
                            save();
                        } else
                            Toast.makeText(this, "Could not read this position.", Toast.LENGTH_SHORT).show();
                    }
                }
            } else
                Toast.makeText(this, R.string.something_wrong, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Keeps the read access to a folder chosen with the folder picker and adds it to the media folders.
     */
    boolean grantFolder(Uri uri) {
        getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        DocumentFile docDir = DocumentFile.fromTreeUri(this, uri);
        if (docDir == null || !docDir.canRead()) {
            Toast.makeText(this, "Could not read this position.", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!uris.contains(uri.toString()))
            uris.add(uri.toString());
        save();
        return true;
    }

    View selectedView;

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo info) {
        selectedView = view;
        menu.add(0, 0, 0, R.string.copy);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (item.getItemId() == 0) { // Copia
            AppUtils.copyToClipboard(getText(android.R.string.copyUrl), ((TextView)selectedView.findViewById(R.id.cartella_url)).getText());
            return true;
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(code, permissions, results);
        if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED && code == 3517)
            chooseFolder();
    }
}
