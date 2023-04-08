package dna.familytree.detail;

import android.app.Activity;

import org.folg.gedcom.model.Note;

import dna.familytree.DetailController;
import dna.familytree.Global;
import dna.familytree.Memory;
import dna.familytree.R;
import dna.familytree.U;
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
        U.placeSourceCitations(box, n);
        U.placeChangeDate(box, n.getChange());
        if (n.getId() != null) {
            NoteReferences noteRefs = new NoteReferences(Global.gc, n.getId(), false);
            if (noteRefs.count > 0)
                U.placeCabinet(box, noteRefs.leaders.toArray(), R.string.shared_by);
        } else if (((Activity)box.getContext()).getIntent().getBooleanExtra("fromNotes", false)) {
            U.placeCabinet(box, Memory.firstObject(), R.string.written_in);
        }
    }

    @Override
    public void delete() {
        U.updateChangeDate(U.deleteNote(n, null));
    }

    @Override
    protected void onResume() {
        super.onResume();
        AnalyticsUtil.logEventNoteActivity(getFirebaseAnalytics());
    }
}
