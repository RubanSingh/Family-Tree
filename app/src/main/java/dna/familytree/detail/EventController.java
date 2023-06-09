package dna.familytree.detail;

import org.folg.gedcom.model.EventFact;
import org.folg.gedcom.model.Family;
import org.folg.gedcom.model.PersonFamilyCommonContainer;

import java.util.Arrays;

import dna.familytree.DetailController;
import dna.familytree.Memory;
import dna.familytree.ProfileFactsFragment;
import dna.familytree.R;
import dna.familytree.AppUtils;
import dna.familytree.util.AnalyticsUtil;

public class EventController extends DetailController {

    EventFact e;
    /**
     * List of event tags useful to avoid putting the Value of the EventFact.
     */
    String[] eventTags = {"BIRT", "CHR", "DEAT", "BURI", "CREM", "ADOP", "BAPM", "BARM", "BASM", "BLES", // Individual events
            "CHRA", "CONF", "FCOM", "ORDN", "NATU", "EMIG", "IMMI", "CENS", "PROB", "WILL", "GRAD", "RETI",
            "ANUL", "DIV", "DIVF", "ENGA", "MARB", "MARC", "MARR", "MARL", "MARS"}; // Family events

    @Override
    public void format() {
        e = (EventFact)cast(EventFact.class);
        if (Memory.getLeaderObject() instanceof Family) {
            getToolbar().setTitle(writeEventTitle((Family)Memory.getLeaderObject(), e));
            setTitle(writeEventTitle((Family)Memory.getLeaderObject(), e));
        } else {
            getToolbar().setTitle(ProfileFactsFragment.writeEventTitle(e)); // The title includes e.getDisplayType()
            setTitle(ProfileFactsFragment.writeEventTitle(e));
        }
        placeSlug(e.getTag());
        if (Arrays.asList(eventTags).contains(e.getTag())) // It's an event (without Value)
            place(getString(R.string.value), "Value", false, true);
        else // All other cases, usually attributes (with Value)
            place(getString(R.string.value), "Value", true, true);
        if (e.getTag().equals("EVEN") || e.getTag().equals("MARR"))
            place(getString(R.string.type), "Type"); // Type of event or relationship
        else
            place(getString(R.string.type), "Type", false, false);
        place(getString(R.string.date), "Date");
        place(getString(R.string.place), "Place");
        place(getString(R.string.address), e.getAddress());
        if (e.getTag() != null && e.getTag().equals("DEAT"))
            place(getString(R.string.cause), "Cause");
        else
            place(getString(R.string.cause), "Cause", false, false);
        place(getString(R.string.www), "Www", false, false);
        place(getString(R.string.email), "Email", false, false);
        place(getString(R.string.telephone), "Phone", false, false);
        place(getString(R.string.fax), "Fax", false, false);
        place(getString(R.string.rin), "Rin", false, false);
        place(getString(R.string.user_id), "Uid", false, false);
        // Other methods are "WwwTag", "EmailTag", "UidTag"
        placeExtensions(e);
        AppUtils.placeNotes(box, e, true);
        AppUtils.placeMedia(box, e, true);
        AppUtils.placeSourceCitations(box, e);
    }

    @Override
    public void delete() {
        ((PersonFamilyCommonContainer)Memory.getSecondToLastObject()).getEventsFacts().remove(e);
        AppUtils.updateChangeDate(Memory.getLeaderObject());
        Memory.setInstanceAndAllSubsequentToNull(e);
    }

    /**
     * Delete the main empty tags and possibly set 'Y' as value.
     */
    public static void cleanUpTag(EventFact ef) {
        if (ef.getType() != null && ef.getType().isEmpty()) ef.setType(null);
        if (ef.getDate() != null && ef.getDate().isEmpty()) ef.setDate(null);
        if (ef.getPlace() != null && ef.getPlace().isEmpty()) ef.setPlace(null);
        String tag = ef.getTag();
        if (tag != null && (tag.equals("BIRT") || tag.equals("CHR") || tag.equals("DEAT")
                || tag.equals("MARR") || tag.equals("DIV"))) {
            if (ef.getType() == null && ef.getDate() == null && ef.getPlace() == null
                    && ef.getAddress() == null && ef.getCause() == null)
                ef.setValue("Y");
            else
                ef.setValue(null);
        }
        if (ef.getValue() != null && ef.getValue().isEmpty()) ef.setValue(null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        AnalyticsUtil.logEventEventActivity(getFirebaseAnalytics());
    }
}
