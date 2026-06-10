package com.surf.surfhubds.components

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.Gravity
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
 * Modelo de configuração para [DSSOptionCardView].
 *
 * Espelha `DSSOptionCardConfiguration` do iOS.
 */
data class DSSOptionCardConfiguration(
    val icon: Drawable?,
    val title: String,
    val description: String,
    val buttonTitle: String,
    /** Cor de fundo do botão. `null` usa o token padrão (primary). */
    val buttonBackgroundColor: Int? = null,
    /** Cor do texto do botão. `null` usa o token padrão (buttonText). */
    val buttonTextColor: Int? = null,
)

/**
 * Port do `DSSOptionCardView` do iOS — card genérico de opção com ícone,
 * título, descrição e botão de ação.
 *
 * O tap no botão chama [onTapButton]. Use [configure] para definir conteúdo.
 */
class DSSOptionCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr), ThemeAware {

    /** Callback do tap no botão de ação. */
    var onTapButton: (() -> Unit)? = null

    private var buttonBackgroundColor: Int? = null
    private var buttonTextColor: Int? = null

    private val iconImageView = ImageView(context).apply {
        scaleType = ImageView.ScaleType.FIT_CENTER
    }
    private val titleLabel = TextView(context).apply {
        typeface = DSSFont.bold(context, 18f).typeface
        textSize = 18f
        setSingleLine(false)
    }
    private val descriptionLabel = TextView(context).apply {
        typeface = DSSFont.regular(context, 14f).typeface
        textSize = 14f
        setSingleLine(false)
    }
    private val actionButton = DSSPrincipalButton(context).apply {
        onTap = { this@DSSOptionCardView.onTapButton?.invoke() }
    }

    init {
        orientation = VERTICAL
        val pad = 20f.dpToPx(context)
        setPadding(pad, pad, pad, pad)

        // Icon: 40x40
        addView(
            iconImageView,
            LayoutParams(40f.dpToPx(context), 40f.dpToPx(context)),
        )

        // Title (topo + 12)
        addView(
            titleLabel,
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = 12f.dpToPx(context) },
        )

        // Description (topo + 8)
        addView(
            descriptionLabel,
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = 8f.dpToPx(context) },
        )

        // Action button (topo + 16, altura 48)
        actionButton.gravity = Gravity.CENTER
        actionButton.textSize = 16f
        actionButton.typeface = DSSFont.regular(context, 16f).typeface
        addView(
            actionButton,
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                48f.dpToPx(context),
            ).apply { topMargin = 16f.dpToPx(context) },
        )

        refresh()
        setupThemeObserver()
    }

    /**
     * Construtor de conveniência alinhado com a configure do iOS.
     */
    constructor(
        context: Context,
        config: DSSOptionCardConfiguration,
        onTapButton: (() -> Unit)? = null,
    ) : this(context) {
        this.onTapButton = onTapButton
        configure(config)
    }

    fun configure(config: DSSOptionCardConfiguration) {
        iconImageView.setImageDrawable(config.icon)
        titleLabel.text = config.title
        descriptionLabel.text = config.description
        actionButton.text = config.buttonTitle
        buttonBackgroundColor = config.buttonBackgroundColor
        buttonTextColor = config.buttonTextColor
        refresh()
    }

    /**
     * Carrega um ícone da brand pelo nome (resources do app), sem falhar se ausente.
     */
    fun loadIcon(name: String): Drawable? {
        val resId = resources.getIdentifier(
            name.lowercase(),
            "drawable",
            context.packageName,
        )
        return if (resId != 0) AppCompatResources.getDrawable(context, resId) else null
    }

    override fun applyTheme(theme: Theme) { refresh() }

    private fun refresh() {
        background = DrawableFactory.rounded(
            context = context,
            backgroundColor = DSSColors.surface(),
            cornerRadiusDp = 12f,
            strokeColor = DSSColors.borderDefault(),
            strokeWidthDp = 1f,
        )

        // Ícone renderizado como template, tingido com primary.
        iconImageView.setColorFilter(DSSColors.primary(), PorterDuff.Mode.SRC_IN)

        titleLabel.setTextColor(DSSColors.primary())
        descriptionLabel.setTextColor(DSSColors.textPrimary())

        // Botão de ação: usa cores custom se fornecidas, senão os tokens padrão.
        val bg = buttonBackgroundColor ?: DSSColors.primary()
        val txt = buttonTextColor ?: DSSColors.buttonText()
        actionButton.background = DrawableFactory.rounded(
            context = context,
            backgroundColor = bg,
            cornerRadiusDp = actionButton.cornerRadiusDp,
        )
        actionButton.setTextColor(txt)
    }
}
