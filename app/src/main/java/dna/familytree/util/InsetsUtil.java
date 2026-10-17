package dna.familytree.util;

import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ScrollView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentContainerView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import dna.familytree.R;

/**
 * Edge-to-edge support: every screen is drawn behind the status bar and the navigation bar,
 * so its layout must keep the content clear of system bars, display cutout and keyboard.
 */
public final class InsetsUtil {

    /**
     * Implemented by activities and fragments that handle the window insets by themselves.
     */
    public interface SelfInsetting {
    }

    private InsetsUtil() {
    }

    /**
     * Applies the window insets to the layout of an activity or a fragment:
     * <ul>
     * <li>the top bar (toolbar, or AppBarLayout) extends its background behind the status bar,
     * or receives the insets if it handles them with fitsSystemWindows;</li>
     * <li>the main scrolling view scrolls behind the navigation bar, with its last content above it;</li>
     * <li>floating action buttons stay above the navigation bar;</li>
     * <li>without a scrolling view, the whole layout keeps clear of the navigation bar;</li>
     * <li>a layout hosting fragments leaves to them the remaining insets;</li>
     * <li>the keyboard resizes the layout, as adjustResize did before edge-to-edge.</li>
     * </ul>
     */
    public static void applyTo(@NonNull View layout) {
        if (layout instanceof DrawerLayout) { // The drawer handles its own insets
            View content = ((DrawerLayout)layout).getChildAt(0);
            if (content != null) applyTo(content);
            return;
        }
        if (layout.getFitsSystemWindows()) return; // The layout handles its own insets
        // Views inside hosted fragments are left to the fragments themselves
        View topBar = find(layout, view -> view.getId() == R.id.toolbar);
        if (topBar == null) topBar = find(layout, view -> view instanceof AppBarLayout);
        final View bar = topBar;
        List<View> fragmentHosts = findAll(layout, InsetsUtil::isFragmentHost);
        boolean hostsFragments = !fragmentHosts.isEmpty();
        // A scrolling view inside the layout is padded on its own; a scrolling layout just gets the layout padding
        View found = hostsFragments ? null : find(layout, InsetsUtil::isScrollingView);
        final View scrollingView = found != layout ? found : null;
        if (scrollingView != null)
            ((ViewGroup)scrollingView).setClipToPadding(false); // Content is visible behind the navigation bar
        List<View> fabs = findAll(layout, view -> view instanceof FloatingActionButton || view instanceof ExtendedFloatingActionButton);
        Rect layoutPadding = initialPadding(layout);
        Rect scrollingPadding = scrollingView != null ? initialPadding(scrollingView) : null;
        Rect barPadding = topBar != null ? initialPadding(topBar) : null;
        int barHeight = topBar != null ? initialHeight(topBar) : 0;

        ViewCompat.setOnApplyWindowInsetsListener(layout, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            int ime = insets.isVisible(WindowInsetsCompat.Type.ime()) ? insets.getInsets(WindowInsetsCompat.Type.ime()).bottom : 0;
            // The top inset goes to the visible top bar, otherwise to the layout, or to the hosted fragments
            boolean barVisible = bar != null && bar.getVisibility() != View.GONE;
            boolean layoutTakesTop = !barVisible && !hostsFragments;
            WindowInsetsCompat sidesApplied = insets.inset(bars.left, 0, bars.right, 0); // The layout takes the side insets
            if (barVisible && bar.getFitsSystemWindows()) { // As a collapsing toolbar with an image behind the status bar
                ViewCompat.dispatchApplyWindowInsets(bar, sidesApplied);
            } else if (barVisible) {
                bar.setPadding(barPadding.left, barPadding.top + bars.top, barPadding.right, barPadding.bottom);
                if (barHeight > 0) { // Fixed height, as ?attr/actionBarSize
                    bar.getLayoutParams().height = barHeight + bars.top;
                    bar.requestLayout();
                }
            }
            int layoutBottom;
            if (hostsFragments) layoutBottom = 0; // Fragments take the bottom insets
            else if (ime > 0) layoutBottom = ime; // The keyboard covers the navigation bar
            else layoutBottom = scrollingView != null ? 0 : bars.bottom;
            layout.setPadding(layoutPadding.left + bars.left, layoutPadding.top + (layoutTakesTop ? bars.top : 0),
                    layoutPadding.right + bars.right, layoutPadding.bottom + layoutBottom);
            if (scrollingView != null)
                scrollingView.setPadding(scrollingPadding.left, scrollingPadding.top, scrollingPadding.right,
                        scrollingPadding.bottom + (ime > 0 ? 0 : bars.bottom));
            // FABs inside a padded layout are already above the navigation bar
            int fabBottom = layoutBottom == 0 ? bars.bottom : 0;
            for (View fab : fabs) {
                ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams)fab.getLayoutParams();
                params.bottomMargin = initialMargin(fab).bottom + fabBottom;
                fab.setLayoutParams(params);
            }
            // Hosted fragments take the insets not yet applied
            WindowInsetsCompat remaining = sidesApplied.inset(0, barVisible ? bars.top : 0, 0, 0);
            if (fragmentHosts.contains(layout))
                return remaining; // The layout itself dispatches them to its fragments
            for (View host : fragmentHosts)
                ViewCompat.dispatchApplyWindowInsets(host, remaining);
            return WindowInsetsCompat.CONSUMED;
        });
        requestApplyInsetsWhenAttached(layout);
    }

    /**
     * Views attached after the insets dispatch, like fragment views, have to ask for them.
     */
    public static void requestApplyInsetsWhenAttached(@NonNull View view) {
        if (view.isAttachedToWindow()) {
            ViewCompat.requestApplyInsets(view);
        } else {
            view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(@NonNull View attached) {
                    attached.removeOnAttachStateChangeListener(this);
                    ViewCompat.requestApplyInsets(attached);
                }

                @Override
                public void onViewDetachedFromWindow(@NonNull View detached) {
                }
            });
        }
    }

    /**
     * Original margins of a view, before the insets are added to them.
     */
    public static Rect initialMargin(@NonNull View view) {
        Rect margin = (Rect)view.getTag(R.id.insets_initial_margin);
        if (margin == null) {
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams)view.getLayoutParams();
            margin = new Rect(params.leftMargin, params.topMargin, params.rightMargin, params.bottomMargin);
            view.setTag(R.id.insets_initial_margin, margin);
        }
        return margin;
    }

    private static Rect initialPadding(View view) {
        Rect padding = (Rect)view.getTag(R.id.insets_initial_padding);
        if (padding == null) {
            padding = new Rect(view.getPaddingLeft(), view.getPaddingTop(), view.getPaddingRight(), view.getPaddingBottom());
            view.setTag(R.id.insets_initial_padding, padding);
        }
        return padding;
    }

    private static int initialHeight(View view) {
        Integer height = (Integer)view.getTag(R.id.insets_initial_height);
        if (height == null) {
            height = view.getLayoutParams() != null ? view.getLayoutParams().height : 0;
            view.setTag(R.id.insets_initial_height, height);
        }
        return height;
    }

    private static boolean isFragmentHost(View view) {
        return view instanceof FragmentContainerView || view instanceof ViewPager || view instanceof ViewPager2;
    }

    private static boolean isScrollingView(View view) {
        return view instanceof ScrollView || view instanceof NestedScrollView
                || view instanceof AbsListView || view instanceof RecyclerView;
    }

    /**
     * Breadth-first search of the first view matching the condition, layout included, without entering fragment hosts.
     */
    @Nullable
    private static View find(View layout, Predicate<View> condition) {
        List<View> queue = new ArrayList<>();
        queue.add(layout);
        for (int i = 0; i < queue.size(); i++) {
            View view = queue.get(i);
            if (condition.test(view)) return view;
            if (view instanceof ViewGroup && !isFragmentHost(view)) {
                ViewGroup group = (ViewGroup)view;
                for (int c = 0; c < group.getChildCount(); c++)
                    queue.add(group.getChildAt(c));
            }
        }
        return null;
    }

    private static List<View> findAll(View view, Predicate<View> condition) {
        List<View> found = new ArrayList<>();
        if (condition.test(view)) found.add(view);
        else if (view instanceof ViewGroup && !isFragmentHost(view)) {
            ViewGroup group = (ViewGroup)view;
            for (int i = 0; i < group.getChildCount(); i++)
                found.addAll(findAll(group.getChildAt(i), condition));
        }
        return found;
    }
}
