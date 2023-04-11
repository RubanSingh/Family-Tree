package dna.familytree;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.widget.Toolbar;

import dna.familytree.constant.Extra;

/**
 * Here you can buy Family Gem Premium from Google Play.
 */
public class PurchaseController extends BaseController {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.purchase_activity);

        Toolbar toolbar = getToolbar();
        setSupportActionBar(toolbar);
        int titleId = getIntent().getIntExtra(Extra.STRING, 0);
        getToolbar().setTitle(titleId);

        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ProductLayout productLayout = findViewById(R.id.purchase_product);
        productLayout.initialize(() -> runOnUiThread(this::onBackPressed));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        onBackPressed();
        return true;
    }
}
