package com.surf.surfhubds.components

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import com.surf.surfhubds.font.DSSFont
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.theme.Theme
import com.surf.surfhubds.theme.ThemeAware
import com.surf.surfhubds.theme.setupThemeObserver
import com.surf.surfhubds.util.DrawableFactory
import com.surf.surfhubds.util.dpToPx

/**
 * Port do `DSSHelpCardView` do iOS — card de ajuda com ícone, título, descrição e chevron.
 *
 * Tap chama [action]. Use [configure] para definir conteúdo.
 */
class DSSHelpCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr), ThemeAware {

    /** Callback do tap no card. */
    var action: (() -> Unit)? = null

    private val iconImageView = ImageView(context).apply {
        scaleType = ImageView.ScaleType.FIT_CENTER
    }
    private val titleLabel = TextView(context).apply {
        typeface = DSSFont.light(context, 14f).typeface
        textSize = 14f
        maxLines = 1
    }
    private val descriptionLabel = TextView(context).apply {
        typeface = DSSFont.light(context, 14f).typeface
        textSize = 14f
        setSingleLine(false)
    }
    private val arrowImageView = ImageView(context).apply {
        scaleType = ImageView.ScaleType.FIT_CENTER
    }
    private val textColumn = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
    }

    init {
        // Layout horizontal: [icon] [title/description column] [arrow]
        val pad = 16f.dpToPx(context)
        setPadding(pad, pad, pad, pad)

        // Icon
        val iconParams = LayoutParams(35f.dpToPx(context), 40f.dpToPx(context)).apply {
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
        }
        addView(iconImageView, iconParams)

        // Text column (title on top, description below)
        textColumn.addView(
            titleLabel,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )
        textColumn.addView(
            descriptionLabel,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = 4f.dpToPx(context) },
        )
        val textParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT,
        ).apply {
            gravity = Gravity.CENTER_VERTICAL
            leftMargin = 35f.dpToPx(context) + 16f.dpToPx(context)
            rightMargin = 15f.dpToPx(context) + 16f.dpToPx(context)
        }
        addView(textColumn, textParams)

        // Arrow
        val arrowParams = LayoutParams(
            15f.dpToPx(context),
            20f.dpToPx(context),
        ).apply { gravity = Gravity.END or Gravity.CENTER_VERTICAL }
        addView(arrowImageView, arrowParams)

        // Fallback chevron drawable (vectors expected in app's R.drawable).
        // Tenta carregar um `ic_chevron_right` da brand; se faltar, deixa vazio.
        arrowImageView.setImageDrawable(loadChevronRight())

        isClickable = true
        isFocusable = true
        setOnClickListener { action?.invoke() }

        refresh()
        setupThemeObserver()
    }

    /**
     * Construtor de conveniência alinhado com a init do iOS.
     */
    constructor(
        context: Context,
        icon: Drawable?,
        title: String,
        description: String,
        action: (() -> Unit)? = null,
    ) : this(context) {
        this.action = action
        configure(icon, title, description)
    }

    fun configure(icon: Drawable?, title: String, description: String) {
        iconImageView.setImageDrawable(icon)
        titleLabel.text = title
        descriptionLabel.text = description
    }

    override fun applyTheme(theme: Theme) { refresh() }

    private fun refresh() {
        // Background + border
        background = DrawableFactory.rounded(
            context = context,
            backgroundColor = DSSColors.surface(),
            cornerRadiusDp = 8f,
            strokeColor = DSSColors.borderDefault(),
            strokeWidthDp = 1f,
        )

        val title = DSSColors.primary()
        val description = DSSColors.textPrimary()
        val tint = DSSColors.primary()

        titleLabel.setTextColor(title)
        descriptionLabel.setTextColor(description)
        arrowImageView.setColorFilter(tint, PorterDuff.Mode.SRC_IN)
        iconImageView.setColorFilter(tint, PorterDuff.Mode.SRC_IN)
    }

    private fun loadChevronRight(): Drawable? {
        // Procura `ic_chevron_right` em recursos do app, sem falhar se ausente.
        val resId = resources.getIdentifier("ic_chevron_right", "drawable", context.packageName)
        return if (resId != 0) AppCompatResources.getDrawable(context, resId) else null
    }
}
