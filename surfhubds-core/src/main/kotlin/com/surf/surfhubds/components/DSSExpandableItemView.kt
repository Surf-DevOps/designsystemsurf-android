package com.surf.surfhubds.components

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
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
import com.surf.surfhubds.util.dpToPx

/**
 * Port do `DSSExpandableItemView` do iOS — card de FAQ (pergunta + resposta) com toggle +/−.
 *
 * O título fica à esquerda, o botão de toggle à direita; o detalhe fica oculto por padrão.
 */
class DSSExpandableItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr), ThemeAware {

    private val titleLabel = TextView(context)
    private val toggleButton = AppCompatButton(context)
    private val detailsLabel = TextView(context)

    private var isExpanded = false

    init {
        orientation = VERTICAL
        setPadding(
            12f.dpToPx(context),
            8f.dpToPx(context),
            12f.dpToPx(context),
            8f.dpToPx(context),
        )

        val titleRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        titleLabel.textSize = 16f
        titleLabel.typeface = DSSFont.medium(context, 16f).typeface
        titleLabel.maxLines = Int.MAX_VALUE

        toggleButton.isAllCaps = false
        toggleButton.background = null
        toggleButton.text = "+"
        toggleButton.textSize = 22f
        toggleButton.minWidth = 20f.dpToPx(context)
        toggleButton.setPadding(0, 0, 0, 0)
        toggleButton.setOnClickListener { toggleDetails() }

        titleRow.addView(titleLabel, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        titleRow.addView(toggleButton, LayoutParams(
            LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
        ).apply { leftMargin = 8f.dpToPx(context) })

        detailsLabel.textSize = 14f
        detailsLabel.typeface = DSSFont.regular(context, 14f).typeface
        detailsLabel.visibility = View.GONE
        detailsLabel.maxLines = Int.MAX_VALUE

        addView(titleRow, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        addView(detailsLabel, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            topMargin = 4f.dpToPx(context)
        })

        setOnClickListener { toggleDetails() }
        applyColors()
        setupThemeObserver()
    }

    constructor(context: Context, question: String, answer: String) : this(context) {
        configure(question, answer)
    }

    fun configure(question: String, answer: String) {
        titleLabel.text = question
        detailsLabel.text = answer
    }

    private fun toggleDetails() {
        isExpanded = !isExpanded
        detailsLabel.visibility = if (isExpanded) View.VISIBLE else View.GONE
        toggleButton.text = if (isExpanded) "−" else "+"
    }

    override fun applyTheme(theme: Theme) { applyColors() }

    private fun applyColors() {
        val isDarkOrBlack = ThemeManager.colorScheme == ColorScheme.DARK ||
            ThemeManager.colorScheme == ColorScheme.BLACK
        val borderColor = if (isDarkOrBlack) {
            Color.argb((0.4f * 255).toInt(), 255, 255, 255)
        } else Color.LTGRAY
        background = DrawableFactory.rounded(
            context = context,
            backgroundColor = Color.TRANSPARENT,
            cornerRadiusDp = 10f,
            strokeColor = borderColor,
            strokeWidthDp = 1f,
        )
        titleLabel.setTextColor(if (isDarkOrBlack) Color.WHITE else Color.DKGRAY)
        toggleButton.setTextColor(
            if (isDarkOrBlack) Color.YELLOW else DSSColors.primary(),
        )
        detailsLabel.setTextColor(
            if (isDarkOrBlack) Color.argb((0.7f * 255).toInt(), 255, 255, 255) else Color.GRAY,
        )
    }
}
