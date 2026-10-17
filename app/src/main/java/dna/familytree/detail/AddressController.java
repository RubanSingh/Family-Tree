package dna.familytree.detail;

import org.folg.gedcom.model.Address;

import dna.familytree.DetailController;
import dna.familytree.Memory;
import dna.familytree.R;
import dna.familytree.AppUtils;
import dna.familytree.feature.places.Maps;
import dna.familytree.util.AnalyticsUtil;

public class AddressController extends DetailController {

    Address a;

    @Override
    public void format() {
        getToolbar().setTitle(R.string.address);
        setTitle(R.string.address);
        placeSlug("ADDR");
        a = (Address)cast(Address.class);
        place(getString(R.string.value), "Value", false, true); // Deprecated by GEDCOM standard in favor of the fragmented address
        place(getString(R.string.name), "Name", false, false); // '_NAME' tag not GEDCOM standard
        place(getString(R.string.line_1), "AddressLine1");
        place(getString(R.string.line_2), "AddressLine2");
        place(getString(R.string.line_3), "AddressLine3");
        place(getString(R.string.postal_code), "PostalCode");
        place(getString(R.string.city), "City");
        place(getString(R.string.state), "State");
        place(getString(R.string.country), "Country");
        placeExtensions(a);
    }

    @Override
    public void delete() {
        deleteAddress(Memory.getSecondToLastObject());
        AppUtils.updateChangeDate(Memory.getLeaderObject());
        Memory.setInstanceAndAllSubsequentToNull(a);
    }

    @Override
    protected void onResume() {
        super.onResume();
        AnalyticsUtil.logEventAddressActivity(getFirebaseAnalytics());
    }

    /**
     * Map actions in the toolbar, when the standard menu is shown (not while editing).
     */
    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        super.onCreateOptionsMenu(menu);
        if (menu.size() > 0)
            Maps.addMenu(menu, Maps.queryOf(a));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        return Maps.onMenuItem(this, item, Maps.queryOf(a)) || super.onOptionsItemSelected(item);
    }
}
