package dna.familytree;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.firebase.analytics.FirebaseAnalytics;

import dna.familytree.nativeads.NativeTemplateStyle;
import dna.familytree.nativeads.TemplateView;
import dna.familytree.util.ApplicationConstants;

public class BaseFragment extends Fragment {

    public FirebaseAnalytics getFirebaseAnalytics(){
        return ((BaseController)requireActivity()).getFirebaseAnalytics();
    }

    /**
     * only non premium users only can view ads
     */
    public void showSmallNativeAd(@NonNull View container) {
        ViewGroup view = container.findViewById(R.id.my_template);
        if (!Global.settings.premium && view != null) {
            loadSmallNativeAds(view);
        }
    }

    private void loadSmallNativeAds(ViewGroup view) {
        AdLoader adLoader = new AdLoader.Builder(requireContext(), ApplicationConstants.NATIVE_AD_UNIT_ID)
                .forNativeAd(nativeAd -> {
                    NativeTemplateStyle styles = new
                            NativeTemplateStyle.Builder().build();
                    TemplateView template = (TemplateView)view;
                    template.setVisibility(View.VISIBLE);
                    template.setStyles(styles);
                    template.setNativeAd(nativeAd);
                })
                .build();

        adLoader.loadAd(new AdRequest.Builder().build());
    }
}
