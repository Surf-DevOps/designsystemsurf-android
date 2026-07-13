package com.surf.surfhubds.components

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import com.surf.surfhubds.font.DSSFont
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.theme.Theme
import com.surf.surfhubds.theme.ThemeAware
import com.surf.surfhubds.theme.ThemeManager
import com.surf.surfhubds.theme.setupThemeObserver
import com.surf.surfhubds.tokens.ColorScheme
import com.surf.surfhubds.util.DrawableFactory
import com.surf.surfhubds.util.dpToPx

/**
 * Port do `DSSBonusRewardCard` do iOS — card de bônus resgatável
 * (ex.: Compre e Ganhe), na mesma linguagem visual dos cards de pacote:
 * canto 16, fundo system/secondarySystemBackground, título em destaque
 * bold(22) na cor primária e selo de elegibilidade em pill. Selecionado
 * ganha borda accent 2dp e fundo accent ~5%; tocar de novo desseleciona via
 * [toggleSelection]. Cards não elegíveis ficam esmaecidos e ignoram seleção.
 */
class DSSBonusRewardCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr), ThemeAware {

    private val titleLabel = TextView(context).apply {
        typeface = DSSFont.bold(context, 22f).typeface
        textSize = 22f
        maxLines = 2
        ellipsize = android.text.TextUtils.TruncateAt.END
    }
    private val badgeLabel = TextView(context).apply {
        typeface = DSSFont.medium(context, 12f).typeface
        textSize = 12f
        maxLines = 1
        gravity = Gravity.CENTER
    }
    private val requiredPointsLabel = TextView(context).apply {
        typeface = DSSFont.regular(context, 14f).typeface
        textSize = 14f
        maxLines = 1
    }
    private val availablePointsLabel = TextView(context).apply {
        typeface = DSSFont.light(context, 13f).typeface
        textSize = 13f
        maxLines = 1
    }
    private val column = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
    }
    private val headerRow = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }

    private var _selected = false
    private var _eligible = true

    val isCardSelected: Boolean get() = _selected
    val isEligible: Boolean get() = _eligible

    var cornerRadiusDp: Float = 16f
        set(value) { field = value; refresh() }

    /** Texto do selo quando elegível. */
    var eligibleBadgeText: String = "Elegível"
        set(value) { field = value; refresh() }

    /** Texto do selo quando não elegível. */
    var ineligibleBadgeText: String = "Inelegível"
        set(value) { field = value; refresh() }

    init {
        val pad = 16f.dpToPx(context)

        headerRow.addView(
            titleLabel,
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = 12f.dpToPx(context)
            },
        )
        headerRow.addView(
            badgeLabel,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )

        column.addView(
            headerRow,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )
        column.addView(
            requiredPointsLabel,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = 10f.dpToPx(context) },
        )
        column.addView(
            availablePointsLabel,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = 4f.dpToPx(context) },
        )

        addView(
            column,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                leftMargin = pad
                rightMargin = pad
                topMargin = pad
                bottomMargin = pad
            },
        )

        refresh()
        setupThemeObserver()
    }

    /** Construtor de conveniência alinhado ao iOS. */
    constructor(
        context: Context,
        title: String,
        requiredPointsText: String,
        availablePointsText: String,
        isEligible: Boolean,
    ) : this(context) {
        configure(title, requiredPointsText, availablePointsText, isEligible)
    }

    fun configure(
        title: String,
        requiredPointsText: String,
        availablePointsText: String,
        isEligible: Boolean,
    ) {
        titleLabel.text = title
        requiredPointsLabel.text = requiredPointsText
        availablePointsLabel.text = availablePointsText
        availablePointsLabel.visibility = if (availablePointsText.isEmpty()) GONE else VISIBLE
        _eligible = isEligible
        if (!isEligible) _selected = false
        refresh()
    }

    /** Seleção visual; ignorada quando o card não é elegível. */
    fun setSelectedState(selected: Boolean) {
        if (!_eligible && selected) return
        _selected = selected
        refresh()
    }

    /** Alterna a seleção (permite desselecionar tocando de novo). */
    fun toggleSelection(): Boolean {
        setSelectedState(!_selected)
        return _selected
    }

    override fun applyTheme(theme: Theme) { refresh() }

    private fun refresh() {
        val scheme = ThemeManager.colorScheme
        val isBlack = scheme == ColorScheme.BLACK
        val isDark = isBlack || scheme == ColorScheme.DARK
        val accent = if (isBlack) DSSColors.primaryButton() else DSSColors.primary()

        // iOS: systemBackground no claro, secondarySystemBackground no escuro;
        // borda systemGray3 no dark sem seleção (como o card de pacotes).
        val cardBackground = if (isDark) Color.rgb(28, 28, 30) else Color.WHITE
        background = when {
            _selected -> DrawableFactory.rounded(
                context = context,
                backgroundColor = withAlpha(accent, 0x0D), // ~5%
                cornerRadiusDp = cornerRadiusDp,
                strokeColor = accent,
                strokeWidthDp = 2f,
            )
            !isBlack && isDark -> DrawableFactory.rounded(
                context = context,
                backgroundColor = cardBackground,
                cornerRadiusDp = cornerRadiusDp,
                strokeColor = Color.rgb(72, 72, 74), // systemGray3 (dark)
                strokeWidthDp = 2f,
            )
            else -> DrawableFactory.rounded(
                context = context,
                backgroundColor = cardBackground,
                cornerRadiusDp = cornerRadiusDp,
            )
        }

        titleLabel.setTextColor(if (isBlack) Color.WHITE else DSSColors.primary())
        requiredPointsLabel.setTextColor(if (isBlack) Color.WHITE else DSSColors.textPrimary())
        availablePointsLabel.setTextColor(DSSColors.textSecondary())

        val badgeColor = if (_eligible) DSSColors.success() else DSSColors.error()
        badgeLabel.text = if (_eligible) eligibleBadgeText else ineligibleBadgeText
        badgeLabel.setTextColor(badgeColor)
        val hPad = 8f.dpToPx(context)
        val vPad = 4f.dpToPx(context)
        badgeLabel.setPadding(hPad, vPad, hPad, vPad)
        badgeLabel.background = DrawableFactory.rounded(
            context = context,
            backgroundColor = withAlpha(badgeColor, 0x1F), // ~12%
            cornerRadiusDp = 10f,
        )

        alpha = if (_eligible) 1f else 0.55f
    }

    private fun withAlpha(@ColorInt color: Int, alpha: Int): Int {
        val a = alpha and 0xFF
        return (color and 0x00FFFFFF) or (a shl 24)
    }
}
