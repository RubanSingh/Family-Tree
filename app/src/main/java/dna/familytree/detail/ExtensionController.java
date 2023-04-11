package dna.familytree.detail;

import org.folg.gedcom.model.GedcomTag;

import dna.familytree.DetailController;
import dna.familytree.Memory;
import dna.familytree.R;
import dna.familytree.AppUtils;
import dna.familytree.util.AnalyticsUtil;

public class ExtensionController extends DetailController {

    GedcomTag e;

    @Override
    public void format() {
        getToolbar().setTitle(getString(R.string.extension));
        setTitle(getString(R.string.extension));
        e = (GedcomTag)cast(GedcomTag.class);
        placeSlug(e.getTag());
        place(getString(R.string.id), "Id", false, false);
        place(getString(R.string.value), "Value", true, true);
        place("Ref", "Ref", false, false);
        place("ParentTagName", "ParentTagName", false, false); // Not sure if it is used in real life
        for (GedcomTag child : e.getChildren()) {
            String text = AppUtils.traverseExtension(child, 0);
            if (text.endsWith("\n"))
                text = text.substring(0, text.length() - 1);
            placePiece(child.getTag(), text, child, true);
        }
    }

    @Override
    public void delete() {
        AppUtils.deleteExtension(e, Memory.getSecondToLastObject(), null);
        AppUtils.updateChangeDate(Memory.getLeaderObject());
    }

    @Override
    protected void onResume() {
        super.onResume();
        AnalyticsUtil.logEventExtensionActivity(getFirebaseAnalytics());
    }
}
