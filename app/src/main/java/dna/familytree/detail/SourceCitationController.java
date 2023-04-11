package dna.familytree.detail;

import static dna.familytree.Global.gc;

import org.folg.gedcom.model.Note;
import org.folg.gedcom.model.SourceCitation;
import org.folg.gedcom.model.SourceCitationContainer;

import dna.familytree.DetailController;
import dna.familytree.Memory;
import dna.familytree.R;
import dna.familytree.AppUtils;
import dna.familytree.util.AnalyticsUtil;

public class SourceCitationController extends DetailController {

    SourceCitation c;

    @Override
    public void format() {
        placeSlug("SOUR");
        c = (SourceCitation)cast(SourceCitation.class);
        if (c.getSource(gc) != null) { // Citation of an existing source
            getToolbar().setTitle(R.string.source_citation);
            setTitle(R.string.source_citation);
            AppUtils.placeSource(box, c.getSource(gc), true);
        } else if (c.getRef() != null) { // Citation of a non-existent source (maybe deleted)
            getToolbar().setTitle(R.string.inexistent_source_citation);
            setTitle(R.string.inexistent_source_citation);
        } else { // Note-source
            getToolbar().setTitle(R.string.source_note);
            setTitle(R.string.source_note);
            place(getString(R.string.value), "Value", true, true);
        }
        place(getString(R.string.page), "Page", true, true);
        place(getString(R.string.date), "Date");
        place(getString(R.string.text), "Text", true, true); // Applies to both note-source and source citation
        place(getString(R.string.certainty), "Quality"); // A number from 0 to 3
        //c.getTextOrValue(); // Practically useless
        //if (c.getDataTagContents() != null)
        //    U.place(box, "Data Tag Contents", c.getDataTagContents().toString()); // COMBINED DATA TEXT
        //place("Ref", "Ref", false, false); // The ID of the source, useless here
        placeExtensions(c);
        AppUtils.placeNotes(box, c, true);
        AppUtils.placeMedia(box, c, true);
    }

    @Override
    public void delete() {
        Object container = Memory.getSecondToLastObject();
        if (container instanceof Note) // Note doesn't extend SourceCitationContainer
            ((Note)container).getSourceCitations().remove(c);
        else
            ((SourceCitationContainer)container).getSourceCitations().remove(c);
        AppUtils.updateChangeDate(Memory.getLeaderObject());
        Memory.setInstanceAndAllSubsequentToNull(c);
    }

    @Override
    protected void onResume() {
        super.onResume();
        AnalyticsUtil.logEventSourceCitationActivity(getFirebaseAnalytics());
    }
}
