package dna.familytree.theme;

import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.recyclerview.widget.RecyclerView;


import dna.familytree.R;

/**
 * Created by rubansingh.john on 9/28/2021.
 */
public class ThemeAdapter extends RecyclerView.Adapter<ThemeAdapter.ViewHolder> {

    int[] themeArray;
    int[] secondaryArray;
    String[] themeName;
    private final int selectedIndex;
    public ThemeSelectionInterface themeSelectionInterface;
    static final boolean[] premiumThemeArray = new boolean[]{true, true, true, true, true, true, false, false, true, false, false, true, true, true, true, true, false, false, false, true};


    public ThemeAdapter(int[] themeArray, int[] secondaryArray, String[] themeName, int selectedIndex, ThemeSelectionInterface themeSelectionInterface) {
        this.themeArray = themeArray;
        this.secondaryArray = secondaryArray;
        this.themeName = themeName;
        this.selectedIndex = selectedIndex;
        this.themeSelectionInterface = themeSelectionInterface;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Create a new view, which defines the UI of the list item
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.theme_row_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        boolean isDarkTheme = position == themeArray.length - 1;
        holder.themeLayout.setBackgroundColor(themeArray[position]);
        holder.secondaryColor.setBackgroundColor(isDarkTheme
                ? holder.themeLayout.getContext().getResources().getColor(R.color.ghost_black)
                : holder.themeLayout.getContext().getResources().getColor(R.color.ghost_white));

        holder.themeName.setText(themeName[position]);
        holder.themeName.setTextColor(secondaryArray[position]);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            holder.circleIcon.setImageTintMode(PorterDuff.Mode.SRC_ATOP);
            holder.circleIcon.setImageTintList(ColorStateList.valueOf(themeArray[position]));
            holder.tickIcon.setImageTintList(isDarkTheme
                    ? ColorStateList.valueOf(holder.themeLayout.getContext().getResources().getColor(R.color.ghost_white))
                    : ColorStateList.valueOf(holder.themeLayout.getContext().getResources().getColor(R.color.ghost_black)));
        }
//
//        if (AppConfig.getInstance().isPremiumThemeEnabled()) {
//            holder.premiumTheme.setVisibility(View.INVISIBLE);
//        } else {
//            if (premiumThemeArray[position]) {
//                holder.premiumTheme.setVisibility(View.VISIBLE);
//            } else {
//                holder.premiumTheme.setVisibility(View.INVISIBLE);
//            }
//        }

        if (position == selectedIndex) {
            holder.tickIcon.setVisibility(View.VISIBLE);
        } else
            holder.tickIcon.setVisibility(View.INVISIBLE);
    }

    @Override
    public int getItemCount() {
        return themeArray.length;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        public LinearLayoutCompat themeLayout;
        public LinearLayoutCompat secondaryColor;
        public AppCompatTextView themeName;
        public AppCompatImageView premiumTheme;
        public AppCompatImageView circleIcon;
        public AppCompatImageView tickIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            themeLayout = itemView.findViewById(R.id.themeLayout);
            secondaryColor = itemView.findViewById(R.id.secondaryColor);
            themeName = itemView.findViewById(R.id.themName);
            premiumTheme = itemView.findViewById(R.id.premiumIcon);
            circleIcon = itemView.findViewById(R.id.circleIcon);
            tickIcon = itemView.findViewById(R.id.tickIcon);

            itemView.setOnClickListener(view -> {
//                AppUtils.vibrate(view.getContext(),10);
                int position = getBindingAdapterPosition();
                themeSelectionInterface.onThemeSelected(themeArray[position], position == themeArray.length - 1, premiumThemeArray[position]);
            });
        }
    }
}