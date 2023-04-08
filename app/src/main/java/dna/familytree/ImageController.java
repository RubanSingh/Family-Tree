package dna.familytree;

import android.net.Uri;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;

import java.io.File;

import dna.familytree.util.AnalyticsUtil;

public class ImageController extends BaseController {

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.lavagna);
        // Shows the file in full resolution
        String path = getIntent().getStringExtra("path");
        Picasso picasso = Picasso.get();
        RequestCreator creator;
        if (path != null) {
            creator = picasso.load(new File(path));
        } else {
            Uri uri = Uri.parse(getIntent().getStringExtra("uri"));
            creator = picasso.load(uri);
        }
        creator.into((ImageView)findViewById(R.id.lavagna_immagine));
    }

    @Override
    protected void onResume() {
        super.onResume();
        AnalyticsUtil.logEventImageActivity(getFirebaseAnalytics());
    }
}
