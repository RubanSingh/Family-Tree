package dna.familytree.detail;

import static dna.familytree.Global.gc;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import org.folg.gedcom.model.Repository;
import org.folg.gedcom.model.RepositoryRef;
import org.folg.gedcom.model.Source;

import dna.familytree.DetailController;
import dna.familytree.Memory;
import dna.familytree.R;
import dna.familytree.AppUtils;
import dna.familytree.util.AnalyticsUtil;

public class RepositoryRefController extends DetailController {

    RepositoryRef r;

    @Override
    public void format() {
        placeSlug("REPO");
        r = (RepositoryRef)cast(RepositoryRef.class);
        if (r.getRepository(gc) != null) { // An actual repository is referenced
            getToolbar().setTitle(R.string.repository_citation);
            setTitle(R.string.repository_citation);
            View repositoryCard = putRepository(box, r.getRepository(gc));
            repositoryCard.setTag(R.id.tag_object, r.getRepository(gc)); // For the context menu TODO: still needed?
            registerForContextMenu(repositoryCard);
        } else if (r.getRef() != null) { // Ref to a non-existent repository (perhaps deleted)
            getToolbar().setTitle(R.string.inexistent_repository_citation);
            setTitle(R.string.inexistent_repository_citation);
        } else { // Without ref
            getToolbar().setTitle(R.string.repository_note);
            setTitle(R.string.repository_note);
        }
        place(getString(R.string.value), "Value", false, true);
        place(getString(R.string.call_number), "CallNumber");
        place(getString(R.string.media_type), "MediaType");
        placeExtensions(r);
        AppUtils.placeNotes(box, r, true);
    }

    public static View putRepository(LinearLayout layout, final Repository repo) {
        final Context context = layout.getContext();
        View repoView = LayoutInflater.from(context).inflate(R.layout.pezzo_fonte, layout, false);
        layout.addView(repoView);
        ((TextView)repoView.findViewById(R.id.fonte_testo)).setText(repo.getName());
        ((CardView)repoView).setCardBackgroundColor(context.getResources().getColor(R.color.repository));
        repoView.setOnClickListener(v -> {
            Memory.setLeader(repo);
            context.startActivity(new Intent(context, RepositoryController.class));
        });
        return repoView;
    }

    @Override
    public void delete() {
        // Delete the citation from the archive and update the date of the source that contained it
        Source container = (Source)Memory.getSecondToLastObject();
        container.setRepositoryRef(null);
        AppUtils.updateChangeDate(container);
        Memory.setInstanceAndAllSubsequentToNull(r);
    }

    @Override
    protected void onResume() {
        super.onResume();
        AnalyticsUtil.logEventRepositoryRefActivity(getFirebaseAnalytics());
    }
}
