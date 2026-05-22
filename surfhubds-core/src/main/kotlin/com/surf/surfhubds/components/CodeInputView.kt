package com.surf.surfhubds.components

import android.content.Context
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatEditText
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

    val code: String get() = textFields.joinToString(separator = "") { it.text?.toString() ?: "" }

    init {
        orientation = HORIZONTAL
        gravity = android.view.Gravity.CENTER
        rebuild()
        setupThemeObserver()
    }

    private fun rebuild() {
        removeAllViews()
        textFields.clear()
        repeat(digits) { index ->
            val field = makeTextField(index)
            val lp = LayoutParams(56f.dpToPx(context), 48f.dpToPx(context))
            if (index > 0) lp.leftMargin = 16f.dpToPx(context)
            addView(field, lp)
            textFields.add(field)
        }
    }

    private fun makeTextField(index: Int): AppCompatEditText {
        val field = AppCompatEditText(context).apply {
            setBackgroundResource(android.R.color.transparent)
            gravity = android.view.Gravity.CENTER
            textSize = 28f
            inputType = InputType.TYPE_CLASS_NUMBER
            imeOptions = if (index == digits - 1) EditorInfo.IME_ACTION_DONE else EditorInfo.IME_ACTION_NEXT
            filters = arrayOf(android.text.InputFilter.LengthFilter(1))
            setTextColor(DSSColors.textPrimary())
        }

        // Underline drawn via a child view? simplest: bottom border via background drawable.
        val container = field
        container.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DEL) {
                if ((field.text?.isEmpty() == true) && index > 0) {
                    textFields[index - 1].apply {
                        text?.clear()
                        requestFocus()
                    }
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
                val txt = s?.toString().orEmpty()
                if (txt.length > 1) field.setText(txt.takeLast(1))
                if (txt.isNotEmpty() && index < textFields.size - 1) {
                    textFields[index + 1].requestFocus()
                } else if (txt.isNotEmpty()) {
                    field.clearFocus()
                }
                codeChanged?.invoke(code)
            }
        })
        return field
    }

    fun beginEditing() {
        textFields.firstOrNull()?.requestFocus()
    }

    fun clear() {
        textFields.forEach { it.setText("") }
        codeChanged?.invoke(code)
    }

    override fun applyTheme(theme: Theme) {
        textFields.forEach { it.setTextColor(DSSColors.textPrimary()) }
    }
}
