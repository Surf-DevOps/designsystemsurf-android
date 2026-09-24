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
        typeface = DSSFont.medium(context, 11f).typeface
        textSize = 11f
        isAllCaps = true
        letterSpacing = 0.02f
    }

    private val descriptionLabel = TextView(context).apply {
        typeface = DSSFont.regular(context, 14f).typeface
        textSize = 14f
        setLineSpacing(0f, 1.1f)
        setSingleLine(false)
    }

    /**
     * Quando true (padrão) o ícone é tingido com `textOnPrimary` — serve para ícones
     * monocromáticos. Ilustrações coloridas (ex.: estrela do Compre e Ganhe) passam false.
     */
    private var tintIcon: Boolean = true

    private val chevronImageView = ImageView(context).apply {
        setImageResource(R.drawable.dss_ic_chevron_right)
        scaleType = ImageView.ScaleType.CENTER_INSIDE
    }

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        val padH = 20f.dpToPx(context)
        val padV = 20f.dpToPx(context)
        setPadding(padH, padV, padH, padV)

        // Container do Ícone (40x40 arredondado, fundo primary sólido)
        val iconSize = 40f.dpToPx(context)
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

        // Sombra suave no lugar da borda (Figma: card flutuante sem stroke)
        elevation = 4f.dpToPx(context).toFloat()
        outlineProvider = android.view.ViewOutlineProvider.BACKGROUND

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
        tintIcon: Boolean = true,
        action: (() -> Unit)? = null
    ) {
        titleLabel.text = title
        descriptionLabel.text = description
        iconImageView.setImageDrawable(icon)
        this.tintIcon = tintIcon
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
            cornerRadiusDp = 12f
        )

        // Background do Ícone (quadrado arredondado com primary sólido)
        iconContainer.background = DrawableFactory.rounded(
            context = ctx,
            backgroundColor = DSSColors.primary(),
            cornerRadiusDp = 8f
        )

        // Cores dos Textos
        titleLabel.setTextColor(DSSColors.primary())
        descriptionLabel.setTextColor(DSSColors.textPrimary())

        chevronImageView.setColorFilter(DSSColors.primary(), PorterDuff.Mode.SRC_IN)
        if (tintIcon) {
            iconImageView.setColorFilter(DSSColors.textOnPrimary(), PorterDuff.Mode.SRC_IN)
        } else {
            iconImageView.clearColorFilter()
        }
    }
}
