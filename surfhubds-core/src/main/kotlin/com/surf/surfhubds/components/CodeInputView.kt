package com.surf.surfhubds.components

import android.content.Context
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatEditText
import com.surf.surfhubds.font.DSSFont
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.theme.Theme
import com.surf.surfhubds.theme.ThemeAware
import com.surf.surfhubds.theme.setupThemeObserver
import com.surf.surfhubds.util.dpToPx

/**
 * Port do `CodeInputView` do iOS — N campos para PIN/OTP, com auto-advance e backspace inteligente.
 */
class CodeInputView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr), ThemeAware {

    var codeChanged: ((String) -> Unit)? = null
    var digits: Int = 4
        set(value) { field = value; rebuild() }

    private val textFields = mutableListOf<AppCompatEditText>()
    private val underlines = mutableListOf<View>()

    /** Suprime o callback do [TextWatcher] durante alterações programáticas (mantém 1 disparo por gesto, como no iOS). */
    private var suppressWatcher = false

    val code: String get() = textFields.joinToString(separator = "") { it.text?.toString() ?: "" }

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER
        rebuild()
        setupThemeObserver()
    }

    private fun rebuild() {
        removeAllViews()
        textFields.clear()
        underlines.clear()
        repeat(digits) { index ->
            val container = makeTextField(index)
            val lp = LayoutParams(56f.dpToPx(context), 48f.dpToPx(context))
            if (index > 0) lp.leftMargin = 16f.dpToPx(context)
            addView(container, lp)
        }
    }

    /** Cria um container 56x48 com o EditText (preenchendo) e a underline (2dp, 4dp acima da base), espelhando o iOS. */
    private fun makeTextField(index: Int): FrameLayout {
        val field = AppCompatEditText(context).apply {
            setBackgroundResource(android.R.color.transparent)
            gravity = Gravity.CENTER
            textSize = 28f
            typeface = DSSFont.medium(context, 28f).typeface
            inputType = InputType.TYPE_CLASS_NUMBER
            filters = arrayOf(InputFilter.LengthFilter(1))
            setTextColor(DSSColors.textPrimary())
        }

        field.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DEL) {
                val current = field.text?.toString().orEmpty()
                // iOS: backspace em campo preenchido -> limpa o próprio campo, dispara codeChanged e volta o foco.
                if (current.isNotEmpty()) {
                    suppressWatcher = true
                    field.setText("")
                    suppressWatcher = false
                    codeChanged?.invoke(code)
                    if (index > 0) textFields[index - 1].requestFocus()
                    return@setOnKeyListener true
                }
                // iOS: backspace em campo vazio -> limpa o anterior, volta o foco e dispara codeChanged.
                if (index > 0) {
                    suppressWatcher = true
                    textFields[index - 1].setText("")
                    suppressWatcher = false
                    textFields[index - 1].requestFocus()
                    codeChanged?.invoke(code)
                    return@setOnKeyListener true
                }
            }
            false
        }

        field.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                if (suppressWatcher) return
                val txt = s?.toString().orEmpty()
                if (txt.length > 1) {
                    suppressWatcher = true
                    field.setText(txt.takeLast(1))
                    suppressWatcher = false
                }
                if (txt.isNotEmpty() && index < textFields.size - 1) {
                    textFields[index + 1].requestFocus()
                } else if (txt.isNotEmpty()) {
                    field.clearFocus()
                }
                codeChanged?.invoke(code)
            }
        })

        textFields.add(field)

        // Underline: 2dp de altura, 4dp acima da base, largura total do campo, cor textTertiary (iOS).
        val underline = View(context).apply {
            setBackgroundColor(DSSColors.textTertiary())
        }
        underlines.add(underline)

        return FrameLayout(context).apply {
            addView(
                field,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                ),
            )
            addView(
                underline,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    2f.dpToPx(context),
                    Gravity.BOTTOM,
                ).apply { bottomMargin = 4f.dpToPx(context) },
            )
        }
    }

    fun beginEditing() {
        textFields.firstOrNull()?.requestFocus()
    }

    fun clear() {
        suppressWatcher = true
        textFields.forEach { it.setText("") }
        suppressWatcher = false
        codeChanged?.invoke(code)
    }

    override fun applyTheme(theme: Theme) {
        textFields.forEach { it.setTextColor(DSSColors.textPrimary()) }
        underlines.forEach { it.setBackgroundColor(DSSColors.textTertiary()) }
    }
}
