package dna.familytree;

import androidx.fragment.app.Fragment;

import com.google.firebase.analytics.FirebaseAnalytics;

public class BaseFragment extends Fragment {

    public FirebaseAnalytics getFirebaseAnalytics(){
        return ((BaseController)requireActivity()).getFirebaseAnalytics();
    }
}
