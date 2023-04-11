package dna.familytree.detail;

import static dna.familytree.Global.gc;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import org.folg.gedcom.model.RepositoryRef;
import org.folg.gedcom.model.Source;

import dna.familytree.DetailController;
import dna.familytree.Memory;
import dna.familytree.R;
import dna.familytree.AppUtils;
import dna.familytree.list.SourcesFragment;
import dna.familytree.util.AnalyticsUtil;
import dna.familytree.visitor.ListOfSourceCitations;

public class SourceController extends DetailController {

    Source f;

    @Override
    public void format() {
        getToolbar().setTitle(R.string.source);
        setTitle(R.string.source);
        f = (Source)cast(Source.class);
        placeSlug("SOUR", f.getId());
        ListOfSourceCitations citations = new ListOfSourceCitations(gc, f.getId());
        f.putExtension("citaz", citations.list.size()); // For SourcesFragment
        place(getString(R.string.abbreviation), "Abbreviation");
        place(getString(R.string.title), "Title", true, true);
        place(getString(R.string.type), "Type", false, true); // '_TYPE' tag not GEDCOM standard
        place(getString(R.string.author), "Author", true, true);
        place(getString(R.string.publication_facts), "PublicationFacts", true, true);
        place(getString(R.string.date), "Date"); // Always null in my Gedcom
        place(getString(R.string.text), "Text", true, true);
        place(getString(R.string.call_number), "CallNumber", false, false); // CALN tag should be in the SOURCE_REPOSITORY_CITATION per GEDCOM specs
        place(getString(R.string.italic), "Italic", false, false); // _italic indicates source title to be in italics
        place(getString(R.string.media_type), "MediaType", false, false); // MEDI tag, should be in SOURCE_REPOSITORY_CITATION
        place(getString(R.string.parentheses), "Paren", false, false); // _PAREN indicates source facts are to be enclosed in parentheses
        place(getString(R.string.reference_number), "ReferenceNumber"); // REFN tag TODO: maybe set it common = false?
        place(getString(R.string.rin), "Rin", false, false);
        place(getString(R.string.user_id), "Uid", false, false);
        placeExtensions(f);

        // Places the citation to the repository
        if (f.getRepositoryRef() != null) {
            View refView = LayoutInflater.from(this).inflate(R.layout.pezzo_citazione_fonte, box, false);
            box.addView(refView);
            refView.setBackgroundColor(getResources().getColor(R.color.repository_citation));
            final RepositoryRef repositoryRef = f.getRepositoryRef();
            if (repositoryRef.getRepository(gc) != null) {
                ((TextView)refView.findViewById(R.id.fonte_testo)).setText(repositoryRef.getRepository(gc).getName());
                ((CardView)refView.findViewById(R.id.citazione_fonte)).setCardBackgroundColor(getResources().getColor(R.color.repository));
            } else refView.findViewById(R.id.citazione_fonte).setVisibility(View.GONE);
            String txt = "";
            if (repositoryRef.getValue() != null) txt += repositoryRef.getValue() + "\n";
            if (repositoryRef.getCallNumber() != null) txt += repositoryRef.getCallNumber() + "\n";
            if (repositoryRef.getMediaType() != null) txt += repositoryRef.getMediaType() + "\n";
            TextView textView = refView.findViewById(R.id.citazione_testo);
            if (txt.isEmpty()) textView.setVisibility(View.GONE);
            else textView.setText(txt.substring(0, txt.length() - 1));
            AppUtils.placeNotes(refView.findViewById(R.id.citazione_note), repositoryRef, false);
            refView.setOnClickListener(v -> {
                Memory.add(repositoryRef);
                startActivity(new Intent(SourceController.this, RepositoryRefController.class));
            });
            registerForContextMenu(refView);
            refView.setTag(R.id.tag_object, repositoryRef); // For the context menu
        }
        AppUtils.placeNotes(box, f, true);
        AppUtils.placeMedia(box, f, true);
        AppUtils.placeChangeDate(box, f.getChange());
        if (!citations.list.isEmpty())
            AppUtils.placeCabinet(box, citations.getProgenitors(), R.string.cited_by);
    }

    @Override
    public void delete() {
        AppUtils.updateChangeDate(SourcesFragment.deleteSource(f));
    }

    @Override
    protected void onResume() {
        super.onResume();
        AnalyticsUtil.logEventSourceActivity(getFirebaseAnalytics());
    }
}
