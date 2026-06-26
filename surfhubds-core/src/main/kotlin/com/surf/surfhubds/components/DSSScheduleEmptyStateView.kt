package com.surf.surfhubds.components

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.surf.surfhubds.font.DSSFont
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.theme.Theme
import com.surf.surfhubds.theme.ThemeAware
import com.surf.surfhubds.theme.ThemeManager
import com.surf.surfhubds.theme.setupThemeObserver
import com.surf.surfhubds.tokens.ColorScheme
import com.surf.surfhubds.util.AppStrings
import com.surf.surfhubds.util.ImageLoader
import com.surf.surfhubds.util.dpToPx

/**
 * Port do `DSSScheduleEmptyStateView.swift` (iOS) — empty state da tela de recarga programada.
 */
class DSSScheduleEmptyStateView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr), ThemeAware {

    var onScheduleRechargeTapped: (() -> Unit)? = null

    /** Override opcional do ícone. null => usa o asset `empty_recurrency` da brand (como o iOS). */
    var iconDrawable: android.graphics.drawable.Drawable? = null
        set(value) {
            field = value
            iconImage.setImageDrawable(value ?: ImageLoader.image(context, "empty_recurrency"))
        }

    private val column = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
    }
    private val iconImage = ImageView(context).apply {
        scaleType = ImageView.ScaleType.FIT_CENTER
        setImageDrawable(ImageLoader.image(context, "empty_recurrency"))
    }
    private val titleLabel = TextView(context).apply {
        text = AppStrings.brand(context, "schedule_empty_message", "Você não possui uma recarga \nprogramada na sua linha.\nPrograme agora e aproveite!")
        textSize = 16f
        typeface = DSSFont.regular(context, 16f).typeface
        gravity = Gravity.START
    }
    private val benefitsStack = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.START
    }
    private val scheduleButton = DSSPrincipalButton(context).apply {
        text = AppStrings.brand(context, "schedule_empty_button", "Programar recarga")
        textSize = 17f
        // iOS: `.boldSystemFont(ofSize: 17)` e `cornerRadius = 28`.
        typeface = DSSFont.bold(context, 17f).typeface
        cornerRadiusDp = 28f
        // Botão um pouco menor que o bloco de texto (320). O minWidth padrão é 320,
        // então baixamos o defaultWidthDp p/ a largura do layout (280) valer de fato.
        defaultWidthDp = 280f
    }

    init {
        // WRAP_CONTENT + gravity CENTER centraliza o bloco (ícone/título/benefícios/
        // botão) vertical e horizontalmente no FrameLayout. Com MATCH_PARENT o column
        // preenchia tudo e o conteúdo colava no topo (não centralizava).
        column.layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.CENTER
        }
        // iOS não aplica padding no container; título/botão têm largura fixa 320 centralizados.
        column.addView(
            iconImage,
            LinearLayout.LayoutParams(100f.dpToPx(context), 100f.dpToPx(context)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            },
        )
        // iOS pina leading/trailing do título ao botão (largura 320).
        column.addView(
            titleLabel,
            LinearLayout.LayoutParams(
                320f.dpToPx(context),
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = 24f.dpToPx(context)
                gravity = Gravity.CENTER_HORIZONTAL
            },
        )

        for (text in listOf("GB bônus", "Renovações automáticas", "Mais praticidade")) {
            val l = TextView(context).apply {
                this.text = "✓ $text"
                textSize = 18f
                typeface = DSSFont.regular(context, 18f).typeface
            }
            benefitsStack.addView(l)
        }
        // iOS: stack centralizado horizontalmente, labels alinhadas à esquerda (wrap content).
        // Largura fixa igual ao título (320dp) para que a borda esquerda coincida.
        column.addView(
            benefitsStack,
            LinearLayout.LayoutParams(
                320f.dpToPx(context),
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = 24f.dpToPx(context)
                gravity = Gravity.CENTER_HORIZONTAL
            },
        )
        scheduleButton.onTap = { onScheduleRechargeTapped?.invoke() }
        column.addView(
            scheduleButton,
            LinearLayout.LayoutParams(
                280f.dpToPx(context),
                50f.dpToPx(context),
            ).apply {
                topMargin = 32f.dpToPx(context)
                gravity = Gravity.CENTER_HORIZONTAL
            },
        )

        addView(column)
        refresh()
        setupThemeObserver()
    }

    override fun applyTheme(theme: Theme) { refresh() }

    private fun refresh() {
        val scheme = ThemeManager.colorScheme
        val isBlack = scheme == ColorScheme.BLACK

        // iOS: `.black ? .black : .secondarySystemBackground`.
        setBackgroundColor(if (isBlack) Color.BLACK else DSSColors.backgroundSecondary())

        titleLabel.setTextColor(DSSColors.textPrimary())

        // iOS: `.black ? .systemRed : DSSColors.primary`  (.systemRed -> #FF3B30).
        val benefitsColor = if (isBlack) Color.parseColor("#FF3B30") else DSSColors.primary()
        for (i in 0 until benefitsStack.childCount) {
            (benefitsStack.getChildAt(i) as? TextView)?.setTextColor(benefitsColor)
        }

        scheduleButton.customBackgroundColor = DSSColors.primaryButton()
        scheduleButton.customTextColor = DSSColors.buttonText()
    }
}
