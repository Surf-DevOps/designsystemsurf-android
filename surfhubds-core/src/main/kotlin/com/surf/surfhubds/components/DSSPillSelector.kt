package com.surf.surfhubds.components

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import com.surf.surfhubds.font.DSSFont
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.theme.Theme
import com.surf.surfhubds.theme.ThemeAware
import com.surf.surfhubds.theme.setupThemeObserver
import com.surf.surfhubds.util.DrawableFactory
import com.surf.surfhubds.util.dpToPx

/**
 * Opções em pílulas separadas (Figma do Compre e Ganhe): a selecionada fica cheia em
 * `primaryButton`, as demais contornadas sobre `surface`.
 *
 * - `fillWidth = true`: as pílulas dividem a largura (abas "QR Code / Digitar código");
 * - `fillWidth = false`: cada pílula tem a largura do texto — ponha dentro de um
 *   HorizontalScrollView para rolar (chips de filtro do extrato).
 *
 * A altura vem do layout (36dp no Figma). [onSelectionChanged] só dispara em toque do usuário.
 */
class DSSPillSelector @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr), ThemeAware {

    var onSelectionChanged: ((Int) -> Unit)? = null

    var selectedIndex: Int = 0
        private set

    private val pills = mutableListOf<TextView>()

    init {
        orientation = HORIZONTAL
        setupThemeObserver()
    }

    fun configure(
        titles: List<String>,
        selectedIndex: Int = 0,
        fillWidth: Boolean = false,
        textSizeSp: Float = 13f,
    ) {
        removeAllViews()
        pills.clear()
        titles.forEachIndexed { index, title ->
            val pill = TextView(context).apply {
                text = title
                gravity = Gravity.CENTER
                textSize = textSizeSp
                typeface = DSSFont.bold(context, textSizeSp).typeface
                maxLines = 1
                if (!fillWidth) {
                    val padH = 16f.dpToPx(context)
                    setPadding(padH, 0, padH, 0)
                }
                setOnClickListener {
                    if (index != this@DSSPillSelector.selectedIndex) {
                        select(index)
                        onSelectionChanged?.invoke(index)
                    }
                }
            }
            val lp = if (fillWidth) {
                LayoutParams(0, LayoutParams.MATCH_PARENT, 1f)
            } else {
                LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT)
            }
            if (index > 0) lp.marginStart = 8f.dpToPx(context)
            pills.add(pill)
            addView(pill, lp)
        }
        select(selectedIndex)
    }

    /** Muda a seleção sem disparar [onSelectionChanged]. */
    fun select(index: Int) {
        selectedIndex = index
        refresh()
    }

    override fun applyTheme(theme: Theme) { refresh() }

    private fun refresh() {
        pills.forEachIndexed { index, pill ->
            val selected = index == selectedIndex
            // Raio grande o bastante para qualquer altura: vira pílula.
            pill.background = if (selected) {
                DrawableFactory.rounded(context, DSSColors.primaryButton(), PILL_RADIUS_DP)
            } else {
                DrawableFactory.rounded(
                    context = context,
                    backgroundColor = DSSColors.surface(),
                    cornerRadiusDp = PILL_RADIUS_DP,
                    strokeColor = DSSColors.borderSubtle(),
                    strokeWidthDp = 1f,
                )
            }
            pill.setTextColor(if (selected) DSSColors.buttonText() else DSSColors.textSecondary())
        }
    }

    private companion object {
        const val PILL_RADIUS_DP = 100f
    }
}
