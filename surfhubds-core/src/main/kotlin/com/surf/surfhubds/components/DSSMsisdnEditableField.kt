package com.surf.surfhubds.components

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatEditText
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
 * Port do `DSSMsisdnEditableField` do iOS — exibe um MSISDN com botão de edit,
 * abre um inline editor, formata como BR e dispara uma validação externa.
 *
 * A validação contra a MVNO no iOS é feita por `APIClient.ConsolidatedQuery`. Aqui
 * o componente expõe um [validator] (callback async) pra que cada app/feature
 * forneça sua própria implementação — o core não conhece SurfAPIKit.
 */
class DSSMsisdnEditableField @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr), ThemeAware {

    /**
     * Resultado da validação esperado pelo componente.
     */
    data class ValidationResult(val valid: Boolean, val coMsisdn: Long, val mvno: String?)

    /**
     * Callback de validação: recebe o MSISDN normalizado (E.164: 55XXXXXXXXXXX)
     * e devolve em `done` o resultado. O componente desabilita o botão Confirmar
     * enquanto aguarda.
     */
    var validator: ((normalizedMsisdn: String, done: (Result<ValidationResult>) -> Unit) -> Unit)? = null

    interface Delegate {
        fun msisdnFieldDidUpdate(field: DSSMsisdnEditableField, newMsisdn: String, coMsisdn: Long)
    }

    var delegate: Delegate? = null

    /**
     * MVNO esperada — passada ao [validator] para conferência (e mensagem de erro).
     */
    var validMvno: String = "iFood"

    private var defaultMsisdn: String = ""
    private var isEditing: Boolean = false

    /**
     * Identifica a validação corrente. Incrementa a cada nova validação ou
     * cancelamento; callbacks de tokens antigos são ignorados — equivale ao
     * `validationTask?.cancel()` do iOS.
     */
    private var validationToken: Int = 0

    var confirmButtonTitle: String = "Confirmar"
        set(value) { field = value; confirmButton.text = value }

    var confirmButtonBackgroundColor: Int = DSSColors.primary()
        set(value) { field = value; refreshConfirmBg() }

    var showContainerShadow: Boolean = false
        set(value) { field = value; updateContainerAppearance() }

    var editButtonIcon: Drawable? = null
        set(value) { field = value; editButton.setImageDrawable(value) }

    // --- Views ---
    private val mainContainer = FrameLayout(context)
    private val displayContainer = FrameLayout(context)
    private val editContainer = FrameLayout(context).apply { visibility = View.GONE }
    private val editStackContainer = FrameLayout(context)

    private val msisdnLabel = TextView(context).apply {
        textSize = 16f
        typeface = DSSFont.regular(context, 16f).typeface
        gravity = Gravity.CENTER_VERTICAL or Gravity.END
    }
    private val editButton = ImageButton(context).apply {
        background = null
        // ImageButton herda padding do tema. Zera e aplica um padding pequeno
        // controlado pra o lápis ficar proporcional (nem minúsculo, nem ocupando
        // todo o botão de 32dp).
        val p = 5f.dpToPx(context)
        setPadding(p, p, p, p)
        minimumWidth = 0
        minimumHeight = 0
        scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
    }
    private val msisdnTextfield = AppCompatEditText(context).apply {
        // iOS usa .numberPad (apenas dígitos); TYPE_CLASS_NUMBER é o equivalente.
        inputType = InputType.TYPE_CLASS_NUMBER
        textSize = 16f
        typeface = DSSFont.regular(context, 16f).typeface
        setPadding(12f.dpToPx(context), 0, 12f.dpToPx(context), 0)
    }
    private val confirmButton = AppCompatButton(context).apply {
        isAllCaps = false
        textSize = 16f
        typeface = DSSFont.regular(context, 16f).typeface
        // buttonText (conteúdo sobre a primary), não branco fixo: em brand com primary
        // clara (Uber) o branco some no botão de fundo primary.
        setTextColor(DSSColors.buttonText())
        // AppCompatButton herda padding horizontal e minWidth do tema, que comem
        // a largura de 95dp e cortam "Confirmar". Zera pra o texto caber centrado.
        setPadding(0, 0, 0, 0)
        minWidth = 0
        minimumWidth = 0
        gravity = Gravity.CENTER
        maxLines = 1
    }

    init {
        setupViews()
        refresh()
        setupThemeObserver()
    }

    /**
     * Define o MSISDN inicial. Aceita formatos brutos ou já com DDI; armazena
     * o normalizado como [defaultMsisdn] para restauração.
     */
    fun setMsisdn(value: String) {
        val normalized = normalizeBrazilianPhone(value)
        if (normalized != null) {
            // iOS atribui o mesmo `formatPhoneNumber()` ao label e ao textfield.
            val formatted = formatBrazilianPhone(normalized)
            msisdnLabel.text = formatted
            msisdnTextfield.setText(formatted)
            defaultMsisdn = normalized
        } else {
            msisdnLabel.text = value
            msisdnTextfield.setText(value)
            defaultMsisdn = value
        }
    }

    /**
     * Retorna o MSISDN atual já normalizado em E.164 (sem máscara, com DDI 55).
     */
    fun getCurrentMsisdn(): String =
        normalizeBrazilianPhone(msisdnLabel.text?.toString() ?: "") ?: ""

    fun getDefaultMsisdn(): String = defaultMsisdn

    fun setDefaultMsisdn(msisdn: String) {
        defaultMsisdn = normalizeBrazilianPhone(msisdn) ?: msisdn
    }

    fun setEditingEnabled(enabled: Boolean) {
        editButton.isEnabled = enabled
        editButton.visibility = if (enabled) View.VISIBLE else View.GONE
    }

    /**
     * Dispara a validação do MSISDN default via [validator].
     */
    fun validateDefaultMsisdn() {
        if (defaultMsisdn.isEmpty()) return
        runValidation(defaultMsisdn)
    }

    /**
     * Cancela qualquer validação em andamento (espelha `cancelValidation()` do
     * iOS). Marca o resultado pendente como obsoleto e reabilita o botão
     * Confirmar.
     */
    fun cancelValidation() {
        validationToken++
        confirmButton.isEnabled = true
        confirmButton.alpha = 1.0f
    }

    private fun setupViews() {
        // Hierarquia
        addView(mainContainer, LayoutParams(LayoutParams.MATCH_PARENT, 50f.dpToPx(context)))

        mainContainer.addView(displayContainer, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        displayContainer.addView(msisdnLabel, LayoutParams(
            LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
        ).apply {
            gravity = Gravity.CENTER_VERTICAL or Gravity.END
            rightMargin = (16 + 32 + 8).dpToPx(context) // mesma posição: editButton + spacing
        })
        displayContainer.addView(editButton, LayoutParams(
            32f.dpToPx(context), 32f.dpToPx(context),
        ).apply {
            gravity = Gravity.CENTER_VERTICAL or Gravity.END
            rightMargin = 16f.dpToPx(context)
        })

        mainContainer.addView(editContainer, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        editContainer.addView(editStackContainer, LayoutParams(
            LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
        ).apply { gravity = Gravity.CENTER })

        editStackContainer.addView(msisdnTextfield, LayoutParams(
            230f.dpToPx(context), 36f.dpToPx(context),
        ).apply { gravity = Gravity.CENTER_VERTICAL or Gravity.START })

        editStackContainer.addView(confirmButton, LayoutParams(
            95f.dpToPx(context), 36f.dpToPx(context),
        ).apply {
            gravity = Gravity.CENTER_VERTICAL or Gravity.END
            // Sobrepõe levemente o campo: leading = textfield.trailing-78 (do iOS).
            // Com gravity END, o leftMargin garante a largura (stack vira 277dp:
            // 230 + 95 - 78 + 30 de trailing) e o rightMargin reproduz o
            // editStackContainer.trailing = confirmButton.trailing - 30.
            leftMargin = (230 - 78).dpToPx(context)
            rightMargin = 30f.dpToPx(context)
        })

        confirmButton.text = confirmButtonTitle
        refreshConfirmBg()

        editButton.setOnClickListener { toggleEditMode() }
        confirmButton.setOnClickListener { onConfirmTapped() }
        msisdnTextfield.addTextChangedListener(PhoneMaskWatcher(msisdnTextfield))
    }

    private fun toggleEditMode() {
        isEditing = !isEditing
        updateUIForEditingState()
        if (isEditing) {
            val current = msisdnLabel.text?.toString().orEmpty()
            val digits = current.filter(Char::isDigit)
            msisdnTextfield.setText(formatBrazilianPhoneNumbersOnly(stripDDI(digits)))
            msisdnTextfield.requestFocus()
            msisdnTextfield.setSelection(msisdnTextfield.text?.length ?: 0)
            showKeyboard()
        } else {
            hideKeyboard()
        }
    }

    private fun updateUIForEditingState() {
        displayContainer.visibility = if (isEditing) View.GONE else View.VISIBLE
        editContainer.visibility = if (isEditing) View.VISIBLE else View.GONE
    }

    private fun onConfirmTapped() {
        val text = msisdnTextfield.text?.toString().orEmpty()
        if (text.isEmpty()) return

        val numbersOnly = text.filter(Char::isDigit)
        val normalized = normalizeBrazilianPhone(numbersOnly)
        if (normalized == null) {
            showErrorAlert("Número de telefone inválido. Por favor, verifique e tente novamente.")
            return
        }
        runValidation(normalized)
    }

    private fun runValidation(normalized: String) {
        val v = validator
        if (v == null) {
            // Sem validator, apenas aceita o número como válido
            applyAcceptedMsisdn(normalized, coMsisdn = 0L)
            return
        }

        // Cancela requisição anterior se existir (espelha validationTask?.cancel()).
        val token = ++validationToken

        confirmButton.isEnabled = false
        confirmButton.alpha = 0.6f

        v(normalized) { result ->
            post {
                // Ignora callbacks de validações canceladas/substituídas.
                if (token != validationToken) return@post

                confirmButton.isEnabled = true
                confirmButton.alpha = 1.0f

                result.fold(
                    onSuccess = { res ->
                        if (res.valid && (res.mvno == null || res.mvno == validMvno)) {
                            applyAcceptedMsisdn(normalized, res.coMsisdn)
                        } else {
                            restoreDefaultMsisdn()
                            showErrorAlert("Esse número não pertence a sua operadora ($validMvno), confira o número e tente novamente.")
                        }
                    },
                    onFailure = { err ->
                        restoreDefaultMsisdn()
                        showErrorAlert("Erro de conexão: ${err.message ?: "tente novamente"}")
                    },
                )
            }
        }
    }

    private fun applyAcceptedMsisdn(normalized: String, coMsisdn: Long) {
        msisdnLabel.text = formatBrazilianPhone(normalized)
        delegate?.msisdnFieldDidUpdate(this, normalized, coMsisdn)
        isEditing = false
        updateUIForEditingState()
        hideKeyboard()
    }

    private fun restoreDefaultMsisdn() {
        if (defaultMsisdn.isNotEmpty()) {
            val normalized = normalizeBrazilianPhone(defaultMsisdn) ?: defaultMsisdn
            msisdnLabel.text = formatBrazilianPhone(normalized)
            msisdnTextfield.setText(formatBrazilianPhoneNumbersOnly(stripDDI(normalized)))
        }
        isEditing = false
        updateUIForEditingState()
        hideKeyboard()
    }

    private fun showKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.showSoftInput(msisdnTextfield, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(windowToken, 0)
        msisdnTextfield.clearFocus()
    }

    private fun showErrorAlert(message: String) {
        AlertDialog.Builder(context)
            .setTitle("Atenção")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    override fun applyTheme(theme: Theme) { refresh() }

    private fun refresh() {
        val isDark = ThemeManager.colorScheme == ColorScheme.DARK ||
            ThemeManager.colorScheme == ColorScheme.BLACK
        msisdnLabel.setTextColor(if (isDark) Color.WHITE else Color.BLACK)
        editButton.setColorFilter(
            if (isDark) Color.WHITE else Color.BLACK,
            android.graphics.PorterDuff.Mode.SRC_IN,
        )
        msisdnTextfield.background = DrawableFactory.rounded(
            context = context,
            backgroundColor = Color.WHITE,
            cornerRadiusDp = 18f,
            strokeColor = Color.argb(255, 229, 229, 234), // iOS systemGray5 (light)
            strokeWidthDp = 1f,
        )
        msisdnTextfield.setTextColor(Color.BLACK)
        refreshConfirmBg()
        updateContainerAppearance()
    }

    private fun refreshConfirmBg() {
        confirmButton.background = DrawableFactory.rounded(
            context = context,
            backgroundColor = confirmButtonBackgroundColor,
            cornerRadiusDp = 18f,
        )
    }

    private fun updateContainerAppearance() {
        if (showContainerShadow) {
            mainContainer.background = DrawableFactory.rounded(
                context = context,
                backgroundColor = Color.WHITE,
                cornerRadiusDp = 16f,
            )
            mainContainer.elevation = 4f.dpToPx(context).toFloat()
        } else {
            mainContainer.background = null
            mainContainer.elevation = 0f
        }
    }

    // ---------- Phone normalization helpers ----------

    /**
     * Converte um telefone brasileiro em E.164 (5511989795250). Espelha
     * `String.normalizeBrazilianPhone` do iOS: remove não-dígitos, descarta
     * prefixo internacional com zeros (`00`/`000`) e zeros à esquerda, garante o
     * prefixo `55` e só aceita se o resultado tiver 12 ou 13 dígitos.
     */
    private fun normalizeBrazilianPhone(raw: String): String? {
        val digits = raw.trim().filter(Char::isDigit)
        if (digits.isEmpty()) return null

        var cleaned = digits
        // Remove prefixo internacional com zeros (ex.: 0055..., 00055...)
        if (cleaned.startsWith("00")) {
            cleaned = cleaned.dropWhile { it == '0' }
        }
        // Remove zeros à esquerda restantes
        while (cleaned.startsWith("0")) {
            cleaned = cleaned.drop(1)
        }
        // Garante prefixo do Brasil
        if (!cleaned.startsWith("55")) {
            cleaned = "55$cleaned"
        }
        // E.164 BR: 55 + DDD(2) + número(8 ou 9) => 12 ou 13 dígitos
        return if (cleaned.length == 12 || cleaned.length == 13) cleaned else null
    }

    private fun stripDDI(e164: String): String {
        return if (e164.startsWith("55") && e164.length > 11) e164.removePrefix("55") else e164
    }

    /**
     * Espelha `String.formatPhoneNumber()` do iOS (usado no label): só formata
     * quando o número tem prefixo `55` e exatamente 11 dígitos locais
     * (`(DD) 9XXXX-XXXX`); caso contrário devolve a string inalterada.
     */
    private fun formatBrazilianPhone(e164: String): String {
        if (!e164.startsWith("55")) return e164
        val trimmed = e164.drop(2)
        if (trimmed.length != 11) return e164
        val ddd = trimmed.take(2)
        val prefix = trimmed.drop(2).take(5)
        val suffix = trimmed.takeLast(4)
        return "($ddd) $prefix-$suffix"
    }

    private fun formatBrazilianPhoneNumbersOnly(numbers: String): String {
        val clean = numbers.filter(Char::isDigit)
        return when {
            clean.isEmpty() -> ""
            clean.length <= 2 -> "($clean"
            clean.length <= 6 -> "(${clean.take(2)}) ${clean.drop(2)}"
            clean.length <= 10 -> {
                val area = clean.take(2)
                val prefix = clean.drop(2).take(4)
                val suffix = clean.drop(6)
                "($area) $prefix-$suffix"
            }
            clean.length == 11 -> {
                val area = clean.take(2)
                val prefix = clean.drop(2).take(5)
                val suffix = clean.drop(7)
                "($area) $prefix-$suffix"
            }
            else -> clean
        }
    }

    /**
     * TextWatcher que reaplica a máscara brasileira no AppCompatEditText.
     */
    private inner class PhoneMaskWatcher(private val editText: AppCompatEditText) : TextWatcher {
        private var updating = false
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        override fun afterTextChanged(s: Editable?) {
            if (updating || s == null) return
            val digits = s.toString().filter(Char::isDigit).take(11)
            val formatted = formatBrazilianPhoneNumbersOnly(digits)
            if (formatted != s.toString()) {
                updating = true
                editText.setText(formatted)
                editText.setSelection(formatted.length)
                updating = false
            }
        }
    }
}
