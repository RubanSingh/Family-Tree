package dna.familytree.designsystem

import android.content.Context
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ImageSpan
import androidx.appcompat.content.res.AppCompatResources

/**
 * Inline premium marker for plain text (menu items, button labels):
 * appends a gold diamond after the text.
 *
 * ```java
 * menu.add(0, ID, 0, PremiumBadge.withDiamond(context, getText(R.string.merge_tree)));
 * ```
 */
object PremiumBadge {

    @JvmStatic
    fun withDiamond(context: Context, text: CharSequence): CharSequence {
        val icon = AppCompatResources.getDrawable(context, R.drawable.ds_ic_diamond_small)?.mutate() ?: return text
        icon.setTint(com.google.android.material.color.MaterialColors.getColor(
            context, com.google.android.material.R.attr.colorTertiary, 0xFF725C00.toInt()))
        icon.setBounds(0, 0, icon.intrinsicWidth, icon.intrinsicHeight)
        val builder = SpannableStringBuilder(text).append("  ")
        val start = builder.length
        builder.append("◆") // replaced by the icon; also read by accessibility as a diamond
        builder.setSpan(ImageSpan(icon, ImageSpan.ALIGN_BASELINE), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        return builder
    }
}
