package com.surf.surfhubds.components

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatCheckBox
import com.surf.surfhubds.font.DSSFont
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.theme.Theme
import com.surf.surfhubds.theme.ThemeAware
import com.surf.surfhubds.theme.setupThemeObserver
import com.surf.surfhubds.util.dpToPx

/**
 * Port do `DSSCheckboxView` do iOS — checkbox + label.
 *
 * Estado público igual ao iOS: [State.UNCHECKED] / [State.CHECKED].
 */
class DSSCheckboxView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr), ThemeAware {

    enum class State { UNCHECKED, CHECKED }

    private val checkbox = AppCompatCheckBox(context)
    private val titleLabel = TextView(context)
    private val row = LinearLayout(context)

    var onStateChange: ((State) -> Unit)? = null

    var state: State = State.UNCHECKED
        set(value) {
            field = value
            checkbox.isChecked = value == State.CHECKED
            onStateChange?.invoke(value)
        }

    init {
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL

        val checkboxLp = LinearLayout.LayoutParams(24f.dpToPx(context), 24f.dpToPx(context)).apply {
            rightMargin = 8f.dpToPx(context)
        }
        row.addView(checkbox, checkboxLp)

        titleLabel.textSize = 14f
        titleLabel.typeface = DSSFont.light(context, 14f).typeface
        titleLabel.maxLines = Int.MAX_VALUE
        row.addView(titleLabel, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ))

        addView(row)

        checkbox.setOnCheckedChangeListener { _, isChecked ->
            state = if (isChecked) State.CHECKED else State.UNCHECKED
        }
        setOnClickListener { toggle() }

        refresh()
        setupThemeObserver()
    }

    /**
     * Construtor espelhando o `init(initialState:titleCheckbox:)` do iOS.
     * Aplica o estado inicial e o título após a inicialização padrão da View.
     * Sem defaults nos parâmetros para evitar ambiguidade com o construtor
     * `@JvmOverloads` `(Context)` da View.
     */
    constructor(
        context: Context,
        initialState: State,
        titleCheckbox: String,
    ) : this(context, null, 0) {
        titleLabel.text = titleCheckbox
        state = initialState
    }

    fun setTitle(title: CharSequence) {
        titleLabel.text = title
    }

    fun toggle() {
        state = if (state == State.UNCHECKED) State.CHECKED else State.UNCHECKED
    }

    override fun applyTheme(theme: Theme) { refresh() }

    private fun refresh() {
        titleLabel.setTextColor(DSSColors.textPrimary())
        androidx.core.widget.CompoundButtonCompat.setButtonTintList(
            checkbox,
            android.content.res.ColorStateList.valueOf(DSSColors.primary()),
        )
    }
}
