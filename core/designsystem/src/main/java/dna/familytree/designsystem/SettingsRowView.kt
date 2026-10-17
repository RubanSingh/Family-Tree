package dna.familytree.designsystem

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.withStyledAttributes
import com.google.android.material.color.MaterialColors
import com.google.android.material.divider.MaterialDivider
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textview.MaterialTextView

/**
 * One row of a grouped settings list: tonal icon, title, optional subtitle and a trailing control.
 *
 * Trailing modes: none, chevron (navigates), switchControl (toggles), value (shows current value), lock (premium).
 * The whole row is the touch target; when the trailing control is a switch, tapping the row toggles it.
 *
 * ```xml
 * <dna.familytree.designsystem.SettingsRowView
 *     app:dsIcon="@drawable/ds_ic_save"
 *     app:dsTitle="@string/auto_save"
 *     app:dsTrailing="switchControl" />
 * ```
 */
class SettingsRowView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    enum class Trailing { NONE, CHEVRON, SWITCH, VALUE, LOCK }

    private val icon: ImageView
    private val title: MaterialTextView
    private val premiumBadge: MaterialTextView
    private val subtitle: MaterialTextView
    private val value: MaterialTextView
    private val chevron: ImageView
    private val lock: ImageView
    private val divider: MaterialDivider

    /** The switch shown when [trailing] is [Trailing.SWITCH]. */
    val switchView: MaterialSwitch

    private var onCheckedChange: ((Boolean) -> Unit)? = null

    var trailing: Trailing = Trailing.NONE
        set(mode) {
            field = mode
            chevron.visibility = if (mode == Trailing.CHEVRON) View.VISIBLE else View.GONE
            switchView.visibility = if (mode == Trailing.SWITCH) View.VISIBLE else View.GONE
            value.visibility = if (mode == Trailing.VALUE) View.VISIBLE else View.GONE
            lock.visibility = if (mode == Trailing.LOCK) View.VISIBLE else View.GONE
        }

    var isChecked: Boolean
        get() = switchView.isChecked
        set(checked) {
            switchView.isChecked = checked
        }

    /** Declared before init, so that its initializer doesn't run after it. */
    private var rowClickListener: OnClickListener? = null

    init {
        orientation = VERTICAL
        LayoutInflater.from(context).inflate(R.layout.ds_settings_row, this, true)
        icon = findViewById(R.id.ds_row_icon)
        title = findViewById(R.id.ds_row_title)
        premiumBadge = findViewById(R.id.ds_row_premium)
        subtitle = findViewById(R.id.ds_row_subtitle)
        value = findViewById(R.id.ds_row_value)
        chevron = findViewById(R.id.ds_row_chevron)
        lock = findViewById(R.id.ds_row_lock)
        switchView = findViewById(R.id.ds_row_switch)
        // Every row has a switch with the same id, so Android would restore one row's saved state into another
        // when the activity is recreated (e.g. after toggling dark mode), firing its listener with a wrong value.
        // Screens always bind the current value in onCreate, so the switch never needs to restore itself.
        switchView.isSaveEnabled = false
        divider = findViewById(R.id.ds_row_divider)

        val content: View = findViewById(R.id.ds_row_content)
        content.isClickable = true
        content.isFocusable = true
        content.setBackgroundResource(selectableBackground())
        content.setOnClickListener {
            if (trailing == Trailing.SWITCH) {
                switchView.toggle()
            } else {
                rowClickListener?.onClick(this)
            }
        }
        switchView.setOnCheckedChangeListener { _, checked -> onCheckedChange?.invoke(checked) }

        context.withStyledAttributes(attrs, R.styleable.SettingsRowView, defStyleAttr, 0) {
            getResourceId(R.styleable.SettingsRowView_dsIcon, 0).takeIf { it != 0 }?.let { setIcon(it) }
            when (getInt(R.styleable.SettingsRowView_dsIconStyle, 0)) {
                1 -> setIconStyle(IconStyle.PRIMARY)
                2 -> setIconStyle(IconStyle.PREMIUM)
                else -> setIconStyle(IconStyle.NEUTRAL)
            }
            getString(R.styleable.SettingsRowView_dsTitle)?.let { setTitle(it) }
            getString(R.styleable.SettingsRowView_dsSubtitle)?.let { setSubtitle(it) }
            getString(R.styleable.SettingsRowView_dsValue)?.let { setValue(it) }
            trailing = Trailing.entries.getOrElse(getInt(R.styleable.SettingsRowView_dsTrailing, 0)) { Trailing.NONE }
            setDividerVisible(getBoolean(R.styleable.SettingsRowView_dsShowDivider, false))
            setPremiumBadgeVisible(getBoolean(R.styleable.SettingsRowView_dsPremiumBadge, false))
        }
    }

    enum class IconStyle { NEUTRAL, PRIMARY, PREMIUM }

    /** Row click. For [Trailing.SWITCH] rows use [setOnCheckedChangeListener] instead. */
    override fun setOnClickListener(l: OnClickListener?) {
        rowClickListener = l
    }

    fun setOnCheckedChangeListener(listener: ((Boolean) -> Unit)?) {
        onCheckedChange = listener
    }

    /** Sets the checked state without firing the listener. */
    fun setCheckedSilently(checked: Boolean) {
        val saved = onCheckedChange
        onCheckedChange = null
        switchView.isChecked = checked
        onCheckedChange = saved
    }

    fun setIcon(@DrawableRes res: Int) {
        icon.setImageResource(res)
        icon.visibility = View.VISIBLE
    }

    fun setIconStyle(style: IconStyle) {
        val (bg, tint) = when (style) {
            IconStyle.NEUTRAL -> R.drawable.ds_bg_icon_container to com.google.android.material.R.attr.colorOnSecondaryContainer
            IconStyle.PRIMARY -> R.drawable.ds_bg_icon_container_primary to com.google.android.material.R.attr.colorOnPrimaryContainer
            IconStyle.PREMIUM -> R.drawable.ds_bg_icon_container_premium to com.google.android.material.R.attr.colorOnTertiaryContainer
        }
        icon.setBackgroundResource(bg)
        icon.imageTintList = ColorStateList.valueOf(
            MaterialColors.getColor(this, tint)
        )
    }

    fun setTitle(text: CharSequence) {
        title.text = text
    }

    fun setTitle(@StringRes res: Int) = setTitle(context.getString(res))

    fun setSubtitle(text: CharSequence?) {
        subtitle.text = text
        subtitle.visibility = if (text.isNullOrEmpty()) View.GONE else View.VISIBLE
    }

    fun setSubtitle(@StringRes res: Int) = setSubtitle(context.getString(res))

    fun setValue(text: CharSequence?) {
        value.text = text
        if (!text.isNullOrEmpty()) trailing = Trailing.VALUE
    }

    /** Shows the gold "Premium" badge above the title, for features the user hasn't unlocked yet. */
    fun setPremiumBadgeVisible(visible: Boolean) {
        premiumBadge.visibility = if (visible) View.VISIBLE else View.GONE
    }

    fun setDividerVisible(visible: Boolean) {
        divider.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private fun selectableBackground(): Int {
        val out = TypedValue()
        context.theme.resolveAttribute(android.R.attr.selectableItemBackground, out, true)
        return out.resourceId
    }
}
