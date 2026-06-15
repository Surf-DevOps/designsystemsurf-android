package com.surf.surfhubds.components

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.appcompat.widget.AppCompatEditText
import com.surf.surfhubds.font.DSSFont
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.theme.Theme
import com.surf.surfhubds.theme.ThemeAware
import com.surf.surfhubds.theme.setupThemeObserver
import com.surf.surfhubds.util.DrawableFactory
import com.surf.surfhubds.util.ImageLoader
import com.surf.surfhubds.util.dpToPx

/**
 * Port do `DSSLabelTextField` do iOS — campo com label opcional, ícones laterais com ação,
 * formatação de CPF/CNPJ/telefone e validação de obrigatoriedade.
 *
 * Os tipos válidos vivem em [Type].
 */
class DSSLabelTextField @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr), ThemeAware {

    sealed class Type(val maxLength: Int?) {
        object Cpf : Type(11)
        object Cnpj : Type(14)
        object CpfOrCnpj : Type(14)
        // Formata como CPF enquanto houver até 11 dígitos; a partir daí vira ICCID
        // (sem máscara), até 20 dígitos. Útil para o campo de ativação.
        object CpfOrIccid : Type(20)
        // CEP brasileiro: 8 dígitos formatados como 00000-000.
        object Cep : Type(8)
        object Phone : Type(11)
        data class Numeric(val length: Int) : Type(length)
        object Text : Type(null)
        object Password : Type(4)
    }

    private val titleLabel = TextView(context)
    private val errorLabel = TextView(context)
    val editText: AppCompatEditText = AppCompatEditText(context)
    private val leftButton = ImageView(context)
    private val rightButton = ImageView(context)

    private var fieldType: Type = Type.Text
    private var hasLabel: Boolean = false
    private var showsErrorLabel: Boolean = false
    private var isSecure: Boolean = false
    private var isPasswordHidden: Boolean = false

    private var leftButtonAction: (() -> Unit)? = null
    private var rightButtonAction: (() -> Unit)? = null

    @ColorInt private var borderColorOverride: Int? = null
    @ColorInt private var backgroundColorOverride: Int? = null
    private var borderWidthOverride: Float? = null

    var nextField: DSSLabelTextField? = null
    var previousField: DSSLabelTextField? = null

    var text: CharSequence?
        get() = editText.text
        set(value) { editText.setText(value) }

    /** Texto sem máscara (apenas dígitos) — use para enviar à API. */
    val digitsOnly: String
        get() = editText.text?.toString()?.filter { it.isDigit() }.orEmpty()

    var hint: CharSequence?
        get() = editText.hint
        set(value) { editText.hint = value }

    init {
        orientation = VERTICAL

        titleLabel.textSize = 16f
        titleLabel.typeface = DSSFont.light(context, 16f).typeface
        titleLabel.visibility = View.GONE
        addView(titleLabel, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = 6f.dpToPx(context)
        })

        val fieldRow = FrameLayout(context)
        editText.background = null
        editText.setPadding(16f.dpToPx(context), 0, 16f.dpToPx(context), 0)
        editText.textSize = 16f
        editText.typeface = DSSFont.light(context, 16f).typeface
        editText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT || actionId == EditorInfo.IME_ACTION_DONE) {
                val next = nextField
                if (next != null) {
                    next.editText.requestFocus()
                } else {
                    editText.clearFocus()
                }
                true
            } else false
        }
        fieldRow.addView(editText, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, 50f.dpToPx(context),
        ).apply { gravity = Gravity.CENTER_VERTICAL })

        leftButton.visibility = View.GONE
        leftButton.setOnClickListener { leftButtonAction?.invoke() }
        fieldRow.addView(leftButton, FrameLayout.LayoutParams(24f.dpToPx(context), 24f.dpToPx(context)).apply {
            // iOS: ícone 24 centrado em container de 44 -> margem (44-24)/2 = 10.
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            leftMargin = 10f.dpToPx(context)
        })

        rightButton.visibility = View.GONE
        rightButton.setOnClickListener { onRightTapped() }
        fieldRow.addView(rightButton, FrameLayout.LayoutParams(24f.dpToPx(context), 24f.dpToPx(context)).apply {
            // iOS: ícone 24 centrado em container de 44 -> margem (44-24)/2 = 10.
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            rightMargin = 10f.dpToPx(context)
        })

        addView(fieldRow, LayoutParams(LayoutParams.MATCH_PARENT, 50f.dpToPx(context)))

        errorLabel.textSize = 12f
        errorLabel.setTextColor(Color.parseColor("#FF3B30"))
        errorLabel.maxLines = Int.MAX_VALUE
        errorLabel.visibility = View.GONE
        addView(errorLabel, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            topMargin = 4f.dpToPx(context)
            leftMargin = 8f.dpToPx(context)
            rightMargin = 8f.dpToPx(context)
        })

        refreshTheme()
        setupThemeObserver()
    }

    fun configure(
        label: String? = null,
        placeholder: String = "",
        type: Type = Type.Text,
        leftIcon: Drawable? = null,
        leftAction: (() -> Unit)? = null,
        rightIcon: Drawable? = null,
        rightAction: (() -> Unit)? = null,
        isSecureEntry: Boolean = false,
        showsErrorLabel: Boolean = false,
        @ColorInt backgroundColor: Int? = null,
        @ColorInt borderColor: Int? = null,
        fontSize: Float? = null,
        borderWidth: Float? = null,
    ) {
        this.fieldType = type
        this.hasLabel = label != null
        this.showsErrorLabel = showsErrorLabel
        this.isSecure = isSecureEntry
        this.leftButtonAction = leftAction
        this.rightButtonAction = rightAction
        this.backgroundColorOverride = backgroundColor
        this.borderColorOverride = borderColor
        this.borderWidthOverride = borderWidth

        if (label != null) {
            titleLabel.text = label
            titleLabel.typeface = DSSFont.light(context, fontSize ?: 16f).typeface
            titleLabel.textSize = fontSize ?: 16f
            titleLabel.visibility = View.VISIBLE
        } else titleLabel.visibility = View.GONE

        editText.hint = placeholder

        when (type) {
            Type.Cpf, Type.Cnpj, Type.CpfOrCnpj, Type.CpfOrIccid, Type.Cep, Type.Phone, is Type.Numeric, Type.Password -> {
                editText.inputType = InputType.TYPE_CLASS_NUMBER
            }
            Type.Text -> {
                editText.inputType = if (isSecure) {
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                } else InputType.TYPE_CLASS_TEXT
            }
        }
        if (type is Type.Password) {
            isPasswordHidden = true
            editText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }

        leftIcon?.let {
            leftButton.setImageDrawable(it)
            leftButton.visibility = View.VISIBLE
            // iOS: leftView é um container de 44pt -> inset de 44.
            editText.setPadding(44f.dpToPx(context), 0, editText.paddingRight, 0)
        }
        // Right button: ícone explícito OU olhinho padrão para campos de senha (espelha iOS).
        when {
            rightIcon != null -> {
                rightButton.setImageDrawable(rightIcon)
                rightButton.visibility = View.VISIBLE
                editText.setPadding(editText.paddingLeft, 0, 44f.dpToPx(context), 0)
            }
            type is Type.Password -> {
                ImageLoader.image(context, "eye_open")?.let { rightButton.setImageDrawable(it) }
                rightButton.visibility = View.VISIBLE
                editText.setPadding(editText.paddingLeft, 0, 44f.dpToPx(context), 0)
            }
        }

        editText.imeOptions = if (nextField != null) EditorInfo.IME_ACTION_NEXT else EditorInfo.IME_ACTION_DONE

        attachWatcher()
        refreshTheme()
    }

    /** Navegação entre campos (espelha `setupNavigation(previous:next:)` do iOS). */
    fun setupNavigation(previous: DSSLabelTextField? = null, next: DSSLabelTextField? = null) {
        this.previousField = previous
        this.nextField = next
        editText.imeOptions = if (next != null) EditorInfo.IME_ACTION_NEXT else EditorInfo.IME_ACTION_DONE
    }

    private fun onRightTapped() {
        if (fieldType is Type.Password) {
            isPasswordHidden = !isPasswordHidden
            editText.inputType = if (isPasswordHidden) {
                InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            } else InputType.TYPE_CLASS_NUMBER
            val iconName = if (isPasswordHidden) "eye_open" else "eye_closed"
            ImageLoader.image(context, iconName)?.let { rightButton.setImageDrawable(it) }
            refreshTheme()
            editText.setSelection(editText.text?.length ?: 0)
        } else rightButtonAction?.invoke()
    }

    fun setError(message: String?) {
        if (!showsErrorLabel) return
        errorLabel.text = message
        errorLabel.visibility = if (message == null) View.GONE else View.VISIBLE
        refreshTheme(error = message != null)
    }

    /** Retorna true se o campo está preenchido. Seta mensagem padrão senão. */
    fun validateRequired(fieldName: String? = null): Boolean {
        val v = editText.text?.toString().orEmpty().trim()
        return if (v.isEmpty()) {
            setError("${fieldName ?: "Campo"} obrigatório")
            false
        } else { setError(null); true }
    }

    override fun applyTheme(theme: Theme) { refreshTheme() }

    private fun refreshTheme(error: Boolean = false) {
        val borderColor = when {
            // iOS usa UIColor.systemRed literal no estado de erro -> #FF3B30.
            error -> Color.parseColor("#FF3B30")
            borderColorOverride != null -> borderColorOverride!!
            else -> DSSColors.borderDefault()
        }
        editText.background = DrawableFactory.rounded(
            context = context,
            backgroundColor = backgroundColorOverride ?: Color.TRANSPARENT,
            cornerRadiusDp = 25f,
            strokeColor = borderColor,
            strokeWidthDp = borderWidthOverride ?: 1f,
        )
        titleLabel.setTextColor(DSSColors.textPrimary())
        editText.setTextColor(DSSColors.textPrimary())
        editText.setHintTextColor(DSSColors.textTertiary())
        leftButton.setColorFilter(DSSColors.textTertiary(), android.graphics.PorterDuff.Mode.SRC_IN)
        rightButton.setColorFilter(DSSColors.textTertiary(), android.graphics.PorterDuff.Mode.SRC_IN)
    }

    private fun attachWatcher() {
        editText.filters = arrayOf<InputFilter>()
        val max = fieldType.maxLength
        if (max != null && fieldType !is Type.Phone && fieldType !is Type.Cpf && fieldType !is Type.Cnpj
            && fieldType !is Type.CpfOrCnpj && fieldType !is Type.CpfOrIccid && fieldType !is Type.Cep) {
            editText.filters = arrayOf(InputFilter.LengthFilter(max))
        }
        val watcher = MaskTextWatcher(editText, fieldType)
        editText.addTextChangedListener(watcher)
    }
}

/**
 * TextWatcher que reaplica formatação de CPF/CNPJ/Phone sem disparar loop infinito.
 */
internal class MaskTextWatcher(
    private val editText: AppCompatEditText,
    private val type: DSSLabelTextField.Type,
) : TextWatcher {
    private var updating = false
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
    override fun afterTextChanged(s: Editable?) {
        if (updating || s == null) return
        val digits = s.toString().filter { it.isDigit() }
        val formatted = when (type) {
            DSSLabelTextField.Type.Cpf -> formatCpf(digits.take(11))
            DSSLabelTextField.Type.Cnpj -> formatCnpj(digits.take(14))
            DSSLabelTextField.Type.CpfOrCnpj ->
                if (digits.length <= 11) formatCpf(digits.take(11)) else formatCnpj(digits.take(14))
            // CPF formatado até 11 dígitos; acima disso, ICCID cru (sem máscara), até 20 dígitos.
            DSSLabelTextField.Type.CpfOrIccid ->
                if (digits.length <= 11) formatCpf(digits.take(11)) else digits.take(20)
            DSSLabelTextField.Type.Cep -> formatCep(digits.take(8))
            DSSLabelTextField.Type.Phone -> formatPhone(digits.take(11))
            else -> return
        }
        if (formatted != s.toString()) {
            updating = true
            editText.setText(formatted)
            editText.setSelection(formatted.length)
            updating = false
        }
    }

    private fun formatCpf(d: String): String = buildString {
        d.forEachIndexed { i, c ->
            if (i == 3 || i == 6) append('.')
            if (i == 9) append('-')
            append(c)
        }
    }

    private fun formatCnpj(d: String): String = buildString {
        d.forEachIndexed { i, c ->
            if (i == 2 || i == 5) append('.')
            if (i == 8) append('/')
            if (i == 12) append('-')
            append(c)
        }
    }

    private fun formatCep(d: String): String = buildString {
        d.forEachIndexed { i, c ->
            if (i == 5) append('-')
            append(c)
        }
    }

    private fun formatPhone(d: String): String {
        if (d.isEmpty()) return ""
        return when {
            d.length <= 2 -> "(${d}"
            d.length <= 7 -> "(${d.take(2)}) ${d.drop(2)}"
            else -> "(${d.take(2)}) ${d.substring(2, 7)}-${d.drop(7)}"
        }
    }
}
