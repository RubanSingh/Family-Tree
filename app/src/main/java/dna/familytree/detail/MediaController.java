package dna.familytree.detail;

import static dna.familytree.Global.gc;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.StrictMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import androidx.core.content.FileProvider;
import dna.familytree.BuildConfig;

import org.folg.gedcom.model.Media;

import java.io.File;
import java.util.List;

import dna.familytree.ImageController;
import dna.familytree.DetailController;
import dna.familytree.util.FileUtils;
import dna.familytree.Global;
import dna.familytree.Memory;
import dna.familytree.R;
import dna.familytree.AppUtils;
import dna.familytree.feature.media.MediaConversions;
import dna.familytree.feature.media.MediaFileTools;
import dna.familytree.feature.media.MediaToolsUi;
import dna.familytree.list.MediaFragment;
import dna.familytree.util.AnalyticsUtil;
import dna.familytree.util.Snackbars;
import dna.familytree.util.LoggerUtils;
import dna.familytree.visitor.FindStack;
import dna.familytree.visitor.MediaReferences;

public class MediaController extends DetailController {

    Media m;
    View imageView;
    private final String TAG = MediaController.class.getSimpleName();
    // File tools in the options menu
    private static final int MENU_INFO = 420;
    private static final int MENU_COPY = 421;
    private static final int MENU_RENAME = 422;
    private static final int MENU_SHRINK = 423;
    private static final int MENU_SHARED = 424;
    private static final int MENU_SIMPLE = 425;

    @Override
    public void format() {
        m = (Media)cast(Media.class);
        if (m.getId() != null) { // Only shared Media have ID, inline Media don't
            getToolbar().setTitle(R.string.shared_media);
            setTitle(R.string.shared_media);
            placeSlug("OBJE", m.getId());
        } else {
            getToolbar().setTitle(R.string.media);
            setTitle(R.string.media);
            placeSlug("OBJE", null);
        }
        displayMedia(m, box.getChildCount());
        place(getString(R.string.title), "Title");
        place(getString(R.string.type), "Type", false, false); // Tag '_TYPE' not GEDCOM standard
        if (Global.settings.expert)
            place(getString(R.string.file), "File"); // File name, visible only with advanced tools
        // TODO: File string should be max 259 characters according to GEDCOM 5.5.5 specs
        place(getString(R.string.format), "Format", Global.settings.expert, false); // File format, e.g. 'jpeg'
        place(getString(R.string.primary), "Primary"); // Tag '_PRIM' not GEDCOM standard, but used to select main media
        place(getString(R.string.scrapbook), "Scrapbook", false, false); // Scrapbook that contains the Media record, not GEDCOM standard
        place(getString(R.string.slideshow), "SlideShow", false, false); // Not GEDCOM standard
        place(getString(R.string.blob), "Blob", false, true);
        //m.getFileTag(); // The tag, could be 'FILE' or '_FILE'
        placeExtensions(m);
        AppUtils.placeNotes(box, m, true);
        AppUtils.placeChangeDate(box, m.getChange());
        // List of records in which the media is used
        MediaReferences mediaReferences = new MediaReferences(gc, m, false);
        if (mediaReferences.leaders.size() > 0)
            AppUtils.placeCabinet(box, mediaReferences.leaders.toArray(), R.string.used_by);
        else if (((Activity)box.getContext()).getIntent().getBooleanExtra("daSolo", false))
            AppUtils.placeCabinet(box, Memory.getLeaderObject(), R.string.into);
    }

    void displayMedia(Media media, int position) {
        imageView = LayoutInflater.from(this).inflate(R.layout.immagine_immagine, box, false);
        box.addView(imageView, position);
        ImageView imageView = this.imageView.findViewById(R.id.immagine_foto);
        FileUtils.showImage(media, imageView, this.imageView.findViewById(R.id.immagine_circolo));
        this.imageView.setOnClickListener(vista -> {
            String path = (String)imageView.getTag(R.id.tag_percorso);
            Uri uri = (Uri)imageView.getTag(R.id.tag_uri);
            int fileType = (int)imageView.getTag(R.id.tag_file_type);
            if (fileType == 0) { // Placeholder instead of image, the media has to be found
                FileUtils.displayImageCaptureDialog(this, null, 5173, null);
            } else if (fileType == 2 || fileType == 3) { // Open the media with some other app
                // TODO: if the type is 3 but it is a url (web page without images) tries to open it as a file://
                if (path != null) {
                    File file = new File(path);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                            && path.startsWith(getExternalFilesDir(null).getPath())) // An app can be a file provider only of its folders
                        uri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", file);
                    else // Under KitKat all folders can be accessed
                        uri = Uri.fromFile(file);
                }
                String mimeType = getContentResolver().getType(uri);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(uri, mimeType);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // Necessary for folders owned by the app (provider)
                List<ResolveInfo> resolvers = getPackageManager().queryIntentActivities(intent, 0);
                /* It's possible that the mime type of some extension (e.g. '.pdf') is properly found,
                   but maybe there is no any predefined app to open the file */
                if (mimeType == null || resolvers.isEmpty()) {
                    intent.setDataAndType(uri, "*/*"); // Ugly list of generic apps
                }
                // From android 7 (Nougat API 24) uri file:// are banned in favor of uri content:// so the file can't be opened
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    try {
                        StrictMode.class.getMethod("disableDeathOnFileUriExposure").invoke(null); // TODO: don't use reflection to use functions you shouldn't!
                    } catch (Exception e) {
                        LoggerUtils.ErrorLog(TAG, "Exception in displayMedia", e);
                    }
                }
                startActivity(intent);
            } else { // Proper image that can be zoomed
                Intent intent = new Intent(MediaController.this, ImageController.class);
                intent.putExtra("path", path);
                if (uri != null)
                    intent.putExtra("uri", uri.toString());
                startActivity(intent);
            }
        });
        this.imageView.setTag(R.id.tag_object, 43614 /* TODO: magic number */); // For the image context menu
        registerForContextMenu(this.imageView);
    }

    public void updateImage() {
        int position = box.indexOfChild(imageView);
        box.removeView(imageView);
        displayMedia(m, position);
    }

    @Override
    public void delete() {
        AppUtils.updateChangeDate(MediaFragment.deleteMedia(m, null));
    }

    /**
     * Adds the file tools to the standard menu, before Delete.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        if (menu.findItem(MENU_CHOOSE_FILE) == null) // Editor mode, without options menu
            return true;
        boolean deletable = menu.findItem(MENU_DELETE) != null;
        menu.removeItem(MENU_DELETE);
        View photo = box.findViewById(R.id.immagine_foto);
        Uri knownUri = photo != null && photo.getTag(R.id.tag_uri) instanceof Uri ? (Uri)photo.getTag(R.id.tag_uri) : null;
        int treeId = Global.settings.openTree;
        MediaFileTools.Resolved resolved = MediaFileTools.resolve(this, m, treeId, false, knownUri);
        menu.add(0, MENU_INFO, 0, R.string.info);
        if (resolved.canCopy())
            menu.add(0, MENU_COPY, 0, R.string.copy_app_storage);
        if (resolved.canRename())
            menu.add(0, MENU_RENAME, 0, R.string.media_rename_file);
        if (Global.settings.expert && MediaFileTools.shortName(this, m, resolved, treeId) != null)
            menu.add(0, MENU_SHRINK, 0, R.string.shrink_path);
        if (m.getId() == null)
            menu.add(0, MENU_SHARED, 0, R.string.make_shared_media);
        else if (MediaConversions.canMakeSimple(gc, m))
            menu.add(0, MENU_SIMPLE, 0, R.string.make_media);
        if (deletable)
            menu.add(0, MENU_DELETE, 0, R.string.delete);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_INFO:
                MediaToolsUi.showFileInfo(this, m, this::mediaChanged);
                return true;
            case MENU_COPY:
                MediaToolsUi.copyToAppStorage(this, m, this::mediaChanged);
                return true;
            case MENU_RENAME:
                MediaToolsUi.rename(this, m, this::mediaChanged);
                return true;
            case MENU_SHRINK:
                MediaToolsUi.shrinkPath(this, m, this::mediaChanged);
                return true;
            case MENU_SHARED:
                makeShared();
                return true;
            case MENU_SIMPLE:
                makeSimple();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void mediaChanged() {
        refresh();
        invalidateOptionsMenu();
    }

    /**
     * The simple media becomes a shared media record, which now leads its own breadcrumb.
     */
    private void makeShared() {
        Object[] leaders = MediaConversions.makeShared(gc, m, AppUtils.newID(gc, Media.class));
        AppUtils.save(true, leaders);
        Memory.stepBack();
        Memory.setInstanceAndAllSubsequentToNull(m);
        Memory.setLeader(m);
        getIntent().removeExtra("daSolo");
        mediaChanged();
        Snackbars.show(this, getString(R.string.media_made_shared));
    }

    /**
     * The shared media becomes a simple media in each record that linked it; this one is shown inside its first record.
     */
    private void makeSimple() {
        Object[] leaders = MediaConversions.makeSimple(gc, m);
        if (leaders.length == 0) return;
        AppUtils.save(true, leaders);
        Memory.stepBack();
        Memory.setInstanceAndAllSubsequentToNull(m);
        new FindStack(gc, m); // Temporary stack, removed going back
        getIntent().putExtra("daSolo", true); // To show the record containing it
        mediaChanged();
        Snackbars.show(this, getString(R.string.media_made_simple));
    }

    @Override
    protected void onResume() {
        super.onResume();
        AnalyticsUtil.logEventMediaActivity(getFirebaseAnalytics());
    }
}
