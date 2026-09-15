package com.surf.surfhubds.components

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.surf.surfhubds.R
import com.surf.surfhubds.font.DSSFont
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.theme.Theme
import com.surf.surfhubds.theme.ThemeAware
import com.surf.surfhubds.theme.setupThemeObserver
import com.surf.surfhubds.util.DrawableFactory
import com.surf.surfhubds.util.dpToPx

/**
 * Componente de card de benefício horizontal.
 * Exibe um ícone à esquerda, título e descrição ao centro e um chevron à direita.
 */
class DSSBenefitCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr), ThemeAware {

    /** Callback acionado ao tocar no card. */
    var onTap: (() -> Unit)? = null

    private val iconContainer = FrameLayout(context)
    private val iconImageView = ImageView(context).apply {
        scaleType = ImageView.ScaleType.CENTER_INSIDE
    }

    private val textContainer = LinearLayout(context).apply {
        orientation = VERTICAL
    }

    private val titleLabel = TextView(context).apply {
        typeface = DSSFont.bold(context, 12f).typeface
        textSize = 12f
        isAllCaps = true
    }

    private val descriptionLabel = TextView(context).apply {
        typeface = DSSFont.medium(context, 16f).typeface
        textSize = 16f
        setSingleLine(false)
    }

    private val chevronImageView = ImageView(context).apply {
        setImageResource(R.drawable.dss_ic_chevron_right)
        scaleType = ImageView.ScaleType.CENTER_INSIDE
    }

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        val padH = 16f.dpToPx(context)
        val padV = 16f.dpToPx(context)
        setPadding(padH, padV, padH, padV)

        // Container do Ícone (48x48 arredondado)
        val iconSize = 48f.dpToPx(context)
        iconContainer.addView(
            iconImageView,
            FrameLayout.LayoutParams(24f.dpToPx(context), 24f.dpToPx(context), Gravity.CENTER)
        )
        addView(iconContainer, LayoutParams(iconSize, iconSize))

        // Textos (Meio)
        val middleParams = LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginStart = 16f.dpToPx(context)
            marginEnd = 8f.dpToPx(context)
        }
        textContainer.addView(titleLabel)
        textContainer.addView(descriptionLabel, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            topMargin = 2f.dpToPx(context)
        })
        addView(textContainer, middleParams)

        // Chevron (Direita)
        addView(chevronImageView, LayoutParams(20f.dpToPx(context), 20f.dpToPx(context)))

        setOnClickListener { onTap?.invoke() }

        refresh()
        setupThemeObserver()
    }

    /**
     * Configura o conteúdo do card.
     */
    fun configure(
        title: String,
        description: String,
        icon: Drawable? = null,
        action: (() -> Unit)? = null
    ) {
        titleLabel.text = title
        descriptionLabel.text = description
        iconImageView.setImageDrawable(icon)
        onTap = action
        refresh()
    }

    override fun applyTheme(theme: Theme) { refresh() }

    private fun refresh() {
        val ctx = context
        
        // Background do Card
        background = DrawableFactory.rounded(
            context = ctx,
            backgroundColor = DSSColors.surface(),
            cornerRadiusDp = 12f,
            strokeColor = DSSColors.borderDefault(),
            strokeWidthDp = 1f
        )

        // Background do Ícone (Círculo ou arredondado suave com cor primária translúcida)
        iconContainer.background = DrawableFactory.rounded(
            context = ctx,
            backgroundColor = DSSColors.primary(),
            cornerRadiusDp = 10f
        )
        iconContainer.background.alpha = 25 // 10% opacidade

        // Cores dos Textos
        titleLabel.setTextColor(DSSColors.primary())
        descriptionLabel.setTextColor(DSSColors.textPrimary())

        // Cor do Chevron e Ícone (Tingidos com primary)
        chevronImageView.setColorFilter(DSSColors.primary(), PorterDuff.Mode.SRC_IN)
        iconImageView.setColorFilter(DSSColors.primary(), PorterDuff.Mode.SRC_IN)
    }
}
