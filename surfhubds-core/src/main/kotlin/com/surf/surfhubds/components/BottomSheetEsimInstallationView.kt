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
        text = "Instalação do seu eSIM"
    }

    private val authorizationLabel = TextView(context).apply {
        textSize = 16f
        typeface = DSSFont.regular(context, 16f).typeface
        gravity = Gravity.CENTER
        maxLines = Int.MAX_VALUE
    }

    private val continueButton = DSSPrincipalButton(context).apply {
        text = "Concordar e continuar"
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
            fullText = "Não concordo",
            linkText = "Não concordo",
            typeface = DSSFont.light(context, 14f).typeface,
            sizeSp = 14f,
            textColor = DSSColors.textPrimary(),
            linkColor = DSSColors.primary(),
        )
    }

    private fun addViews() {
        addView(column, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            topMargin = 80f.dpToPx(context)
            leftMargin = 24f.dpToPx(context)
            rightMargin = 24f.dpToPx(context)
        })

        column.addView(titleLabel, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ))
        column.addView(authorizationLabel, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = 54f.dpToPx(context) })
        column.addView(continueButton, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = 24f.dpToPx(context) })
        column.addView(cancelLabel, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply {
            topMargin = 24f.dpToPx(context)
            gravity = Gravity.CENTER_HORIZONTAL
        })
    }

    override fun applyTheme(theme: Theme) { refresh() }

    private fun refresh() {
        titleLabel.setTextColor(DSSColors.primary())
        authorizationLabel.setTextColor(DSSColors.textPrimary())
        applyContentConfig()
    }
}
