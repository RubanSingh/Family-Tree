package dna.familytree.detail;

import android.app.Activity;

import org.folg.gedcom.model.Note;

import dna.familytree.DetailController;
import dna.familytree.Global;
import dna.familytree.Memory;
import dna.familytree.R;
import dna.familytree.AppUtils;
import dna.familytree.util.AnalyticsUtil;
import dna.familytree.visitor.NoteReferences;

public class NoteController extends DetailController {

    Note n;

    @Override
    public void format() {
        n = (Note)cast(Note.class);
        if (n.getId() == null) {
            getToolbar().setTitle(R.string.note);
            setTitle(R.string.note);
            placeSlug("NOTE");
        } else {
            getToolbar().setTitle(R.string.shared_note);
            setTitle(R.string.shared_note);
            placeSlug("NOTE", n.getId());
        }
        place(getString(R.string.text), "Value", true, true);
        place(getString(R.string.rin), "Rin", false, false);
        placeExtensions(n);
        AppUtils.placeSourceCitations(box, n);
        AppUtils.placeChangeDate(box, n.getChange());
        if (n.getId() != null) {
            NoteReferences noteRefs = new NoteReferences(Global.gc, n.getId(), false);
            if (noteRefs.count > 0)
                AppUtils.placeCabinet(box, noteRefs.leaders.toArray(), R.string.shared_by);
        } else if (((Activity)box.getContext()).getIntent().getBooleanExtra("fromNotes", false)) {
            AppUtils.placeCabinet(box, Memory.getLeaderObject(), R.string.written_in);
        }
    }

    @Override
    public void delete() {
        AppUtils.updateChangeDate(AppUtils.deleteNote(n, null));
    }

    @Override
    protected void onResume() {
        super.onResume();
        AnalyticsUtil.logEventNoteActivity(getFirebaseAnalytics());
    }
}
