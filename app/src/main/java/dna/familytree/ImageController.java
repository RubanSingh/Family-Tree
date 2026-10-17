package dna.familytree;

import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;

import java.io.File;

import dna.familytree.util.AnalyticsUtil;
import dna.familytree.util.InsetsUtil;

public class ImageController extends BaseController implements InsetsUtil.SelfInsetting {

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.lavagna);
        // Full screen image: system bars are hidden, a swipe from the edge shows them temporarily
        WindowInsetsControllerCompat insetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        insetsController.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        insetsController.hide(WindowInsetsCompat.Type.systemBars());
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
