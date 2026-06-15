package com.surf.surfhubds.components

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import com.surf.surfhubds.font.DSSFont
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.theme.Theme
import com.surf.surfhubds.theme.ThemeAware
import com.surf.surfhubds.theme.ThemeManager
import com.surf.surfhubds.theme.setupThemeObserver
import com.surf.surfhubds.tokens.ColorScheme
import com.surf.surfhubds.util.DrawableFactory
import com.surf.surfhubds.util.ImageLoader
import com.surf.surfhubds.util.dpToPx

/**
 * Port do `CardsEmptyStateView` do iOS — estado vazio para a lista de cartões,
 * com ícone, título, subtítulo, benefícios e CTA "Adicionar cartão".
 */
class CardsEmptyStateView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr), ThemeAware {

    var onAddCardTapped: (() -> Unit)? = null

    /**
     * Override para fornecer o ícone do empty-state.
     * Default espelha o iOS: `ImageLoader.image(named: "no_cards_image", brand: BrandResolver.current())`.
     */
    var iconResolver: () -> android.graphics.drawable.Drawable? = {
        ImageLoader.image(context, "no_cards_image")
    }

    private val iconView = ImageView(context).apply { scaleType = ImageView.ScaleType.FIT_CENTER }
    private val titleView = TextView(context).apply {
        gravity = Gravity.CENTER
        textSize = 20f
        typeface = DSSFont.bold(context, 20f).typeface
        text = "Você ainda não tem um cartão cadastrado"
    }
    private val subtitleView = TextView(context).apply {
        gravity = Gravity.CENTER
        textSize = 15f
        typeface = DSSFont.regular(context, 15f).typeface
        text = "Adicione um cartão agora para ter mais facilidades."
    }
    private val benefitsStack = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
    }
    private val addCardButton = AppCompatButton(context).apply {
        isAllCaps = false
        text = "Adicionar cartão"
        textSize = 17f
        typeface = DSSFont.bold(context, 17f).typeface
    }

    init {
        setupTree()
        setupFixedBenefits()
        refresh()
        addCardButton.setOnClickListener { onAddCardTapped?.invoke() }
        setupThemeObserver()
    }

    override fun applyTheme(theme: Theme) { refresh() }

    private fun setupTree() {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            // CENTER (e não só CENTER_HORIZONTAL) pra centralizar o conteúdo verticalmente
            // no espaço disponível (a view de "sem cartão" fica no meio da tela).
            gravity = Gravity.CENTER
        }
        val pad32 = 32f.dpToPx(context)

        container.addView(
            iconView,
            LinearLayout.LayoutParams(80f.dpToPx(context), 60f.dpToPx(context)).apply {
                topMargin = 0
                gravity = Gravity.CENTER_HORIZONTAL
            },
        )
        container.addView(
            titleView,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = 24f.dpToPx(context)
                leftMargin = pad32
                rightMargin = pad32
            },
        )
        container.addView(
            subtitleView,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = 12f.dpToPx(context)
                leftMargin = pad32
                rightMargin = pad32
            },
        )
        container.addView(
            benefitsStack,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = 24f.dpToPx(context)
                gravity = Gravity.CENTER_HORIZONTAL
            },
        )
        container.addView(
            addCardButton,
            LinearLayout.LayoutParams(320f.dpToPx(context), 50f.dpToPx(context)).apply {
                topMargin = 32f.dpToPx(context)
                gravity = Gravity.CENTER_HORIZONTAL
            },
        )

        val lp = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        lp.gravity = Gravity.CENTER
        addView(container, lp)
    }

    private fun setupFixedBenefits() {
        val items = listOf(
            "Recarga programada",
            "Mais agilidade",
            "Mais praticidade",
        )
        items.forEachIndexed { index, text ->
            val label = TextView(context).apply {
                textSize = 15f
                typeface = DSSFont.regular(context, 15f).typeface
                this.text = "✓ $text"
            }
            benefitsStack.addView(
                label,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    // iOS UIStackView.spacing = 8 is inter-item only; no spacing before the first.
                ).apply { topMargin = if (index == 0) 0 else 8f.dpToPx(context) },
            )
        }
    }

    private fun refresh() {
        val scheme = ThemeManager.colorScheme
        val isBlack = scheme == ColorScheme.BLACK

        // iOS: colorScheme == .black ? .black : .secondarySystemBackground
        setBackgroundColor(if (isBlack) Color.BLACK else DSSColors.backgroundSecondary())
        iconView.setImageDrawable(iconResolver())

        titleView.setTextColor(DSSColors.textPrimary())
        subtitleView.setTextColor(DSSColors.textSecondary())

        // iOS: colorScheme == .black ? DSSColors.primaryButton : DSSColors.primary
        addCardButton.background = DrawableFactory.rounded(
            context = context,
            backgroundColor = if (isBlack) DSSColors.primaryButton() else DSSColors.primary(),
            cornerRadiusDp = 28f,
        )
        // iOS: .black -> .white ; senão (dark -> .black, light -> .white)
        val buttonTitleColor = when (scheme) {
            ColorScheme.BLACK -> Color.WHITE
            ColorScheme.DARK -> Color.BLACK
            ColorScheme.LIGHT -> Color.WHITE
        }
        addCardButton.setTextColor(buttonTitleColor)

        // iOS: colorScheme == .black ? .systemRed : DSSColors.primary  (.systemRed -> #FF3B30)
        val benefitColor = if (isBlack) Color.parseColor("#FF3B30") else DSSColors.primary()
        for (i in 0 until benefitsStack.childCount) {
            (benefitsStack.getChildAt(i) as? TextView)?.setTextColor(benefitColor)
        }
    }
}
