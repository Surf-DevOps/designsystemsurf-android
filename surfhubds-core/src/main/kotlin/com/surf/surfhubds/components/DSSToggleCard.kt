package com.surf.surfhubds.components

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import androidx.appcompat.widget.SwitchCompat
import androidx.core.graphics.ColorUtils
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.theme.Theme
import com.surf.surfhubds.theme.ThemeAware
import com.surf.surfhubds.theme.setupThemeObserver
import com.surf.surfhubds.util.DSSWeight
import com.surf.surfhubds.util.DrawableFactory
import com.surf.surfhubds.util.dpToPx
import com.surf.surfhubds.util.dssText
import com.surf.surfhubds.util.weightParams
import com.surf.surfhubds.util.wrapParams

/**
 * Card com título, descrição e chave (Figma da recarga do grupo: "Mesmo plano para
 * todos" / "Desligue para escolher um plano para cada linha"). O card inteiro alterna a
 * chave. [onChange] só dispara por toque do usuário, não por [isChecked] do código.
 */
class DSSToggleCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr), ThemeAware {

    var onChange: ((Boolean) -> Unit)? = null

    var isChecked: Boolean
        get() = switch.isChecked
        set(value) {
            fromCode = true
            switch.isChecked = value
            fromCode = false
        }

    private var fromCode = false
    private val titleLabel = dssText(context, DSSWeight.BOLD, 15f, DSSColors.textPrimary())
    private val descriptionLabel = dssText(context, DSSWeight.REGULAR, 13f, DSSColors.textSecondary())
    private val switch = SwitchCompat(context).apply {
        setOnCheckedChangeListener { _, checked -> if (!fromCode) onChange?.invoke(checked) }
    }

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        val ctx = context
        val h = 22f.dpToPx(ctx)
        val v = 18f.dpToPx(ctx)
        setPadding(h, v, h, v)
        val texts = LinearLayout(ctx).apply {
            orientation = VERTICAL
            addView(titleLabel, wrapParams())
            addView(descriptionLabel, wrapParams().apply { topMargin = 6f.dpToPx(ctx) })
        }
        addView(texts, weightParams())
        addView(switch, wrapParams().apply { marginStart = 16f.dpToPx(ctx) })
        setOnClickListener { switch.toggle() }
        refresh()
        setupThemeObserver()
    }

    fun configure(title: CharSequence, description: CharSequence?, checked: Boolean) {
        titleLabel.text = title
        descriptionLabel.text = description
        descriptionLabel.visibility = if (description == null) GONE else VISIBLE
        isChecked = checked
        contentDescription = title
    }

    override fun applyTheme(theme: Theme) { refresh() }

    private fun refresh() {
        background = DrawableFactory.rippleOver(
            DrawableFactory.rounded(context, DSSColors.background(), 10f, DSSColors.divider(), 1.5f),
            ColorUtils.setAlphaComponent(DSSColors.textPrimary(), 24),
        )
        titleLabel.setTextColor(DSSColors.textPrimary())
        descriptionLabel.setTextColor(DSSColors.textSecondary())
        // Ligada = verde do Figma (success); desligada = trilho cinza.
        val states = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf())
        switch.thumbTintList = ColorStateList(states, intArrayOf(DSSColors.contrastOn(DSSColors.success()), DSSColors.background()))
        switch.trackTintList = ColorStateList(
            states,
            intArrayOf(DSSColors.success(), ColorUtils.setAlphaComponent(DSSColors.textPrimary(), 50)),
        )
    }
}
