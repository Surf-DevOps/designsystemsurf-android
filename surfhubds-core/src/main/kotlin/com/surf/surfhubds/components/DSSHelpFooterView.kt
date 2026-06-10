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
import com.surf.surfhubds.util.dpToPx

/**
 * Port do `DSSHelpFooterView` do iOS — rodapé com ícone + texto + ação de tap.
 * Reutilizável em qualquer tela.
 *
 * Tap chama [onTap]. Use [configure] para definir conteúdo.
 */
class DSSHelpFooterView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr), ThemeAware {

    /** Callback do tap no rodapé. */
    var onTap: (() -> Unit)? = null

    private val iconImageView = ImageView(context).apply {
        scaleType = ImageView.ScaleType.FIT_CENTER
    }
    private val titleLabel = TextView(context).apply {
        typeface = DSSFont.regular(context, 14f).typeface
        textSize = 14f
    }

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL

        // Icon 18x18, com 6dp de espaçamento até o texto.
        addView(
            iconImageView,
            LayoutParams(18f.dpToPx(context), 18f.dpToPx(context)),
        )
        addView(
            titleLabel,
            LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT,
            ).apply { leftMargin = 6f.dpToPx(context) },
        )

        // Fallback do ícone: tenta `questionmark_circle` da brand; se faltar, deixa vazio.
        iconImageView.setImageDrawable(loadQuestionMarkIcon())

        isClickable = true
        isFocusable = true
        setOnClickListener { onTap?.invoke() }

        refresh()
        setupThemeObserver()
    }

    /**
     * Construtor de conveniência alinhado com a init do iOS.
     */
    constructor(
        context: Context,
        title: String,
        icon: Drawable? = null,
        onTap: (() -> Unit)? = null,
    ) : this(context) {
        this.onTap = onTap
        configure(title, icon)
    }

    fun configure(title: String, icon: Drawable? = null) {
        titleLabel.text = title
        if (icon != null) iconImageView.setImageDrawable(icon)
    }

    override fun applyTheme(theme: Theme) { refresh() }

    private fun refresh() {
        titleLabel.setTextColor(DSSColors.textPrimary())
        iconImageView.setColorFilter(DSSColors.primary(), PorterDuff.Mode.SRC_IN)
    }

    private fun loadQuestionMarkIcon(): Drawable? {
        val resId = resources.getIdentifier("questionmark_circle", "drawable", context.packageName)
        return if (resId != 0) AppCompatResources.getDrawable(context, resId) else null
    }
}
