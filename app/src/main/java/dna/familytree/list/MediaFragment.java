package dna.familytree.list;

import static dna.familytree.Global.gc;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.theartofdev.edmodo.cropper.CropImageConstant;

import org.folg.gedcom.model.Media;
import org.folg.gedcom.model.MediaContainer;
import org.folg.gedcom.model.MediaRef;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import dna.familytree.BaseFragment;
import dna.familytree.constant.Extra;
import dna.familytree.cropper.CropImage;
import dna.familytree.util.FileUtils;
import dna.familytree.Global;
import dna.familytree.MediaFoldersController;
import dna.familytree.Memory;
import dna.familytree.R;
import dna.familytree.AppUtils;
import dna.familytree.constant.Choice;
import dna.familytree.util.AnalyticsUtil;
import dna.familytree.visitor.FindStack;
import dna.familytree.visitor.MediaContainerList;
import dna.familytree.visitor.MediaReferences;

/**
 * Fragment with a list of all Media of the tree.
 */
public class MediaFragment extends BaseFragment {

    MediaContainerList mediaVisitor;
    MediaAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bundle) {
        setHasOptionsMenu(true);
        View view = inflater.inflate(R.layout.gallery, container, false);
        RecyclerView recyclerView = view.findViewById(R.id.gallery_recycler);
        recyclerView.setHasFixedSize(true);
        if (gc != null) {
            mediaVisitor = new MediaContainerList(gc, !getActivity().getIntent().getBooleanExtra(Choice.MEDIA, false));
            gc.accept(mediaVisitor);
            setToolbarTitle();
            RecyclerView.LayoutManager layoutManager = new GridLayoutManager(getContext(), 2);
            recyclerView.setLayoutManager(layoutManager);
            adapter = new MediaAdapter(mediaVisitor.mediaList, true);
            recyclerView.setAdapter(adapter);
            view.findViewById(R.id.fab).setOnClickListener(v -> {
                AnalyticsUtil.logEventAddMedia(getFirebaseAnalytics());
                FileUtils.displayImageCaptureDialog(getContext(), MediaFragment.this, 4546, null);
            });
        }
        return view;
    }

    /**
     * Leaving the activity resets the extra if no shared media has been chosen.
     */
    @Override
    public void onPause() {
        super.onPause();
        getActivity().getIntent().removeExtra(Choice.MEDIA);
    }

    void setToolbarTitle() {
        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(mediaVisitor.mediaList.size()
                + " " + getString(R.string.media).toLowerCase());
    }

    /**
     * Updates the contents of the gallery.
     */
    public void recreate() {
        mediaVisitor.mediaList.clear();
        gc.accept(mediaVisitor);
        adapter.notifyDataSetChanged();
        setToolbarTitle();
    }

    public static Media newMedia(Object container) {
        Media media = new Media();
        media.setId(AppUtils.newID(gc, Media.class));
        media.setFileTag("FILE"); // Necessary to then export the GEDCOM
        gc.addMedia(media);
        if (container != null) {
            MediaRef mediaRef = new MediaRef();
            mediaRef.setRef(media.getId());
            ((MediaContainer)container).addMediaRef(mediaRef);
        }
        return media;
    }

    /**
     * Detaches a shared media from a container.
     */
    public static void disconnectMedia(String mediaId, MediaContainer container) { // TODO: unlinkMedia()
        Iterator<MediaRef> refs = container.getMediaRefs().iterator();
        while (refs.hasNext()) {
            MediaRef ref = refs.next();
            if (ref.getMedia(Global.gc) == null // Possible ref to a non-existent media
                    || ref.getRef().equals(mediaId))
                refs.remove();
        }
        if (container.getMediaRefs().isEmpty())
            container.setMediaRefs(null);
    }

    /**
     * Deletes a shared or local media and removes the references in container records.
     * Returns an array with the modified leader objects.
     */
    public static Object[] deleteMedia(Media media, View view) {
        Set<Object> leaders;
        if (media.getId() != null) { // Shared Media
            gc.getMedia().remove(media);
            // Delete references in all containers
            MediaReferences deleteMedia = new MediaReferences(gc, media, true);
            leaders = deleteMedia.leaders;
        } else { // Local Media
            new FindStack(gc, media); // Temporally generates a stack of the media to locate its container
            MediaContainer container = (MediaContainer)Memory.getSecondToLastObject();
            container.getMedia().remove(media);
            if (container.getMedia().isEmpty())
                container.setMedia(null);
            leaders = new HashSet<>(); // Set with only one leader object
            leaders.add(Memory.getLeaderObject());
            Memory.stepBack(); // Deletes the stack just created
        }
        Memory.setInstanceAndAllSubsequentToNull(media);
        if (view != null)
            view.setVisibility(View.GONE);
        return leaders.toArray(new Object[0]);
    }

    /**
     * The file retrieved from SAF becomes a shared media.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == 4546) { // File taken from the supplier app is saved in the Media and possibly cropped
                Media media = newMedia(null);
                if (FileUtils.proposeCropping(getContext(), this, data, media)) { // Checks if it is an image (therefore it can be cropped)
                    AppUtils.save(false, media);
                    // onRestart() + recreate() must not be triggered otherwise the arrival fragment will be no longer the same
                    return;
                }
            } else if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
                FileUtils.endImageCropping(data);
            }
            AppUtils.save(true, Global.croppedMedia);
        } else if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) // If user clicks the back arrow in CropImage
            Global.edited = true;
    }

    // Contextual menu
    private Media media;

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo info) {
        media = (Media)view.getTag(R.id.tag_object);
        menu.add(0, 0, 0, R.string.delete);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (item.getItemId() == 0) {
            AnalyticsUtil.logEventDeleteMedia(getFirebaseAnalytics());
            Object[] modified = deleteMedia(media, null);
            recreate();
            AppUtils.save(false, modified);
            return true;
        }
        return false;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, 0, 0, R.string.media_folders);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 0) {
            AnalyticsUtil.logEventEditMedia(getFirebaseAnalytics());
            startActivity(new Intent(getContext(), MediaFoldersController.class)
                    .putExtra(Extra.TREE_ID, Global.settings.openTree)
            );
            return true;
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permission, @NonNull int[] grantResults) {
        FileUtils.permissionsResult(getContext(), this, requestCode, permission, grantResults, null);
    }

    @Override
    public void onResume() {
        super.onResume();
        AnalyticsUtil.logMediaFragment(getFirebaseAnalytics());
    }
}
