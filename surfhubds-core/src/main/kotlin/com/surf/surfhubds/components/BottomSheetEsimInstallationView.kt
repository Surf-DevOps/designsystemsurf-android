package com.surf.surfhubds.components

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.surf.surfhubds.font.DSSFont
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.theme.Theme
import com.surf.surfhubds.theme.ThemeAware
import com.surf.surfhubds.theme.setupThemeObserver
import com.surf.surfhubds.util.AppStrings
import com.surf.surfhubds.util.dpToPx

/**
 * Port do `BottomSheetEsimInstallationView` do iOS — conteúdo do bottom sheet
 * de autorização de instalação do eSIM.
 *
 * Estrutura vertical: título + texto de autorização + botão "Concordar e continuar"
 * + link de "Não concordo". O sheet é apresentado por
 * [EsimInstallationSheetFragment].
 */
class BottomSheetEsimInstallationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr), ThemeAware {

    var onContinueTap: (() -> Unit)? = null
    var onCancelTap: (() -> Unit)? = null

    private val titleLabel = TextView(context).apply {
        textSize = 20f
        typeface = DSSFont.bold(context, 20f).typeface
        gravity = Gravity.CENTER
        text = AppStrings.brand(context, "esim_installation_title", "Instalação do seu eSIM")
    }

    private val authorizationLabel = TextView(context).apply {
        textSize = 16f
        typeface = DSSFont.regular(context, 16f).typeface
        gravity = Gravity.CENTER
        // iOS: `autorizationLabel` não define `numberOfLines`, então usa o default = 1.
        maxLines = 1
    }

    private val continueButton = DSSPrincipalButton(context).apply {
        text = AppStrings.brand(context, "esim_agree_continue", "Concordar e continuar")
        textSize = 16f
        typeface = DSSFont.regular(context, 16f).typeface
    }

    private val cancelLabel = TextWithActionLinkView(context).apply {
        gravity = Gravity.CENTER
    }

    private val column = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
    }

    init {
        addViews()
        applyContentConfig()
        refresh()
        setupThemeObserver()

        continueButton.onTap = { onContinueTap?.invoke() }
        cancelLabel.onLinkTap = { onCancelTap?.invoke() }
    }

    fun configure(text: String) {
        authorizationLabel.text = text
    }

    private fun applyContentConfig() {
        cancelLabel.configure(
            fullText = AppStrings.brand(context, "esim_disagree", "Não concordo"),
            linkText = AppStrings.brand(context, "esim_disagree", "Não concordo"),
            typeface = DSSFont.light(context, 14f).typeface,
            sizeSp = 14f,
            textColor = DSSColors.textPrimary(),
            linkColor = DSSColors.primary(),
        )
    }

    private fun addViews() {
        // iOS: somente o `autorizationLabel` tem leading/trailing de 24pt; título,
        // botão e link são apenas centralizados (centerX). Por isso as margens
        // laterais ficam por subview, não no container.
        addView(column, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            // Offset do topo reduzido (era 80dp) para subir todo o conteúdo — o link
            // "Não concordo" estava muito embaixo. Mantém folga abaixo do grabber.
            topMargin = 40f.dpToPx(context)
        })

        // iOS: titleLabel só tem centerX -> largura intrínseca centralizada.
        column.addView(titleLabel, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { gravity = Gravity.CENTER_HORIZONTAL })
        // iOS: autorizationLabel com leading +24 / trailing -24.
        column.addView(authorizationLabel, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply {
            // Gap título->texto reduzido (era 54dp) para o bloco ficar compacto.
            topMargin = 28f.dpToPx(context)
            leftMargin = 24f.dpToPx(context)
            rightMargin = 24f.dpToPx(context)
        })
        // iOS: continueButton só tem centerX -> usa defaultSize (320x50), centralizado.
        column.addView(continueButton, LinearLayout.LayoutParams(
            320f.dpToPx(context), LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply {
            topMargin = 24f.dpToPx(context)
            gravity = Gravity.CENTER_HORIZONTAL
        })
        // iOS: cancelLabel só tem centerX -> largura intrínseca centralizada.
        column.addView(cancelLabel, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply {
            topMargin = 24f.dpToPx(context)
            // Margem abaixo do "Não concordo" para não ficar colado na borda do sheet
            // (agora que o sheet abraça o conteúdo).
            bottomMargin = 32f.dpToPx(context)
            gravity = Gravity.CENTER_HORIZONTAL
        })
    }

    override fun applyTheme(theme: Theme) { refresh() }

    private fun refresh() {
        titleLabel.setTextColor(DSSColors.primary())
        authorizationLabel.setTextColor(DSSColors.textPrimary())
        // iOS: continueButton usa `backgroundColor: DSSColors.primaryButton` e
        // `textColor: DSSColors.buttonText` (não o token `primary` default do botão).
        continueButton.customBackgroundColor = DSSColors.primaryButton()
        continueButton.customTextColor = DSSColors.buttonText()
        applyContentConfig()
    }
}
