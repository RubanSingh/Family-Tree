package dna.familytree.detail;

import org.folg.gedcom.model.Repository;
import org.folg.gedcom.model.Source;

import java.util.ArrayList;
import java.util.List;

import dna.familytree.DetailController;
import dna.familytree.Global;
import dna.familytree.R;
import dna.familytree.AppUtils;
import dna.familytree.list.RepositoriesFragment;
import dna.familytree.util.AnalyticsUtil;

public class RepositoryController extends DetailController {

    Repository a;

    @Override
    public void format() {
        getToolbar().setTitle(R.string.repository);
        setTitle(R.string.repository);
        a = (Repository)cast(Repository.class);
        placeSlug("REPO", a.getId());
        place(getString(R.string.value), "Value", false, true); // Not really GEDCOM standard
        place(getString(R.string.name), "Name");
        place(getString(R.string.address), a.getAddress());
        place(getString(R.string.www), "Www");
        place(getString(R.string.email), "Email");
        place(getString(R.string.telephone), "Phone");
        place(getString(R.string.fax), "Fax");
        place(getString(R.string.rin), "Rin", false, false);
        placeExtensions(a);
        AppUtils.placeNotes(box, a, true);
        AppUtils.placeChangeDate(box, a.getChange());

        // Collects and displays the sources citing this Repository
        List<Source> citingSources = new ArrayList<>();
        for (Source source : Global.gc.getSources())
            if (source.getRepositoryRef() != null && source.getRepositoryRef().getRef() != null
                    && source.getRepositoryRef().getRef().equals(a.getId()))
                citingSources.add(source);
        if (!citingSources.isEmpty())
            AppUtils.placeCabinet(box, citingSources.toArray(), R.string.sources);
    }

    @Override
    public void delete() {
        AppUtils.updateChangeDate((Object[])RepositoriesFragment.delete(a));
    }

    @Override
    protected void onResume() {
        super.onResume();
        AnalyticsUtil.logEventRepositoryActivity(getFirebaseAnalytics());
    }
}
