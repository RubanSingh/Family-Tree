package dna.familytree;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.firebase.analytics.FirebaseAnalytics;

import dna.familytree.feature.ads.NativeAdCard;

public class BaseFragment extends Fragment {

    public FirebaseAnalytics getFirebaseAnalytics(){
        return ((BaseController)requireActivity()).getFirebaseAnalytics();
    }

    /**
     * Shows the native ad card R.id.my_template of the view to free users, only if an ad is already loaded:
     * the card is at the top of the page and must not push the content down later. Call it from onCreateView().
     */
    public void showNativeAdIfReady(@NonNull View container) {
        NativeAdCard card = container.findViewById(R.id.my_template);
        if (card != null)
            card.show(getViewLifecycleOwner(), false);
    }
}
