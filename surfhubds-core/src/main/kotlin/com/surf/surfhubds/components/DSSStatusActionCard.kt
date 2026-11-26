package com.surf.surfhubds.components

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.theme.Theme
import com.surf.surfhubds.theme.ThemeAware
import com.surf.surfhubds.theme.setupThemeObserver
import com.surf.surfhubds.util.DSSWeight
import com.surf.surfhubds.util.DrawableFactory
import com.surf.surfhubds.util.dpToPx
import com.surf.surfhubds.util.dssText
import com.surf.surfhubds.util.matchWidthParams
import com.surf.surfhubds.util.weightParams
import com.surf.surfhubds.util.wrapParams

/**
 * Card de estado com ação (Figma do "Meu grupo"): ícone num quadrado cinza, título,
 * subtítulo, status em pílula no canto ("Inativa", "Vinculado") e um botão contornado
 * embaixo ("Programar recarga", "Trocar cartão"). Sem [Model.actionTitle] o botão some.
 */
class DSSStatusActionCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr), ThemeAware {

    data class Model(
        val icon: Drawable?,
        val title: CharSequence,
        val subtitle: CharSequence? = null,
        val statusText: CharSequence? = null,
        val statusTone: DSSStatusLabel.Tone = DSSStatusLabel.Tone.NEUTRAL,
        val actionTitle: CharSequence? = null,
    )

    var onAction: (() -> Unit)? = null

    private val iconBox = FrameLayout(context)
    private val iconView = ImageView(context)
    private val titleLabel = dssText(context, DSSWeight.SEMIBOLD, 14f, DSSColors.textPrimary(), maxLines = 1)
    private val subtitleLabel = dssText(context, DSSWeight.REGULAR, 11f, DSSColors.textSecondary(), maxLines = 2)
    private val status = DSSStatusLabel(context)
    private val actionButton = DSSSecondaryButton(context).apply {
        applyPillStyle(heightDp = 38f, textSizeSp = 13f)
        borderWidthDp = 1f
        onTap = { onAction?.invoke() }
    }

    init {
        orientation = VERTICAL
        val ctx = context
        setPadding(9f.dpToPx(ctx), 7f.dpToPx(ctx), 7f.dpToPx(ctx), 13f.dpToPx(ctx))

        val iconSize = 28f.dpToPx(ctx)
        val glyph = 14f.dpToPx(ctx)
        iconBox.addView(iconView, FrameLayout.LayoutParams(glyph, glyph, Gravity.CENTER))

        val texts = LinearLayout(ctx).apply {
            orientation = VERTICAL
            addView(titleLabel, wrapParams())
            addView(subtitleLabel, wrapParams().apply { topMargin = 2f.dpToPx(ctx) })
        }
        val top = LinearLayout(ctx).apply {
            orientation = HORIZONTAL
            gravity = Gravity.TOP
            addView(iconBox, LayoutParams(iconSize, iconSize).apply { topMargin = 8f.dpToPx(ctx) })
            addView(texts, weightParams().apply {
                marginStart = 10f.dpToPx(ctx)
                topMargin = 8f.dpToPx(ctx)
            })
            addView(status, wrapParams().apply { marginStart = 6f.dpToPx(ctx) })
        }
        addView(top, matchWidthParams())
        addView(actionButton, LayoutParams(LayoutParams.MATCH_PARENT, 38f.dpToPx(ctx)).apply {
            topMargin = 16f.dpToPx(ctx)
            val inset = 27f.dpToPx(ctx)
            marginStart = inset
            marginEnd = inset
        })
        refresh()
        setupThemeObserver()
    }

    fun configure(model: Model) {
        iconView.setImageDrawable(model.icon?.mutate())
        iconBox.visibility = if (model.icon == null) GONE else VISIBLE
        titleLabel.text = model.title
        subtitleLabel.text = model.subtitle
        subtitleLabel.visibility = if (model.subtitle == null) GONE else VISIBLE
        status.visibility = if (model.statusText == null) GONE else VISIBLE
        model.statusText?.let { status.configure(it, model.statusTone, DSSStatusLabel.Style.PILL) }
        actionButton.text = model.actionTitle
        actionButton.visibility = if (model.actionTitle == null) GONE else VISIBLE
        refresh()
    }

    override fun applyTheme(theme: Theme) { refresh() }

    private fun refresh() {
        background = DrawableFactory.rounded(context, DSSColors.background(), 10f, DSSColors.divider(), 1f)
        iconBox.background = DrawableFactory.rounded(context, DSSColors.backgroundSecondary(), 6f)
        iconView.setColorFilter(DSSColors.textPrimary())
        titleLabel.setTextColor(DSSColors.textPrimary())
        subtitleLabel.setTextColor(DSSColors.textSecondary())
    }
}
