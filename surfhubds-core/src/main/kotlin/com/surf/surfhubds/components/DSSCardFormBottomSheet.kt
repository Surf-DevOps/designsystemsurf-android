package com.surf.surfhubds.components

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.surf.surfhubds.font.DSSFont
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.util.dpToPx

/**
 * Dados retornados pelo `DSSCardFormBottomSheet` ao adicionar um cartão.
 */
data class DSSCardFormData(
    /** Número do cartão sem formatação (somente dígitos) */
    val number: String,
    /** Mês da validade, ex: "08" */
    val expiryMonth: String,
    /** Ano da validade, ex: "27" */
    val expiryYear: String,
    /** CVV exatamente como digitado */
    val cvv: String,
    /** Nome impresso no cartão */
    val holderName: String,
    /** CPF/CNPJ somente dígitos (sem pontos/traços) */
    val document: String,
)

/**
 * Port do `DSSCardFormBottomSheet` do iOS — bottom sheet de cadastro de cartão de crédito
 * com formatação automática, ícone de câmera e ilustração de bandeiras opcional.
 */
class DSSCardFormBottomSheet : BottomSheetDialogFragment() {

    var onAddCard: ((DSSCardFormData) -> Unit)? = null
    var onCameraTap: (() -> Unit)? = null

    /** Drawable das bandeiras (ilCardFlags) provido pelo módulo de brand. */
    var brandsImage: Drawable? = null
    /** Drawable do ícone de câmera (camera.fill no iOS) provido pelo módulo de brand. */
    var cameraIcon: Drawable? = null

    private lateinit var numberField: DSSLabelTextField
    private lateinit var expiryField: DSSLabelTextField
    private lateinit var cvvField: DSSLabelTextField
    private lateinit var holderField: DSSLabelTextField
    private lateinit var documentField: DSSLabelTextField

    private lateinit var numberErrorLabel: TextView
    private lateinit var expiryErrorLabel: TextView
    private lateinit var cvvErrorLabel: TextView
    private lateinit var holderErrorLabel: TextView
    private lateinit var documentErrorLabel: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        val ctx = requireContext()

        val scroll = ScrollView(ctx).apply {
            setBackgroundColor(DSSColors.background())
            isFillViewport = true
        }

        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24f.dpToPx(ctx), 16f.dpToPx(ctx), 24f.dpToPx(ctx), 24f.dpToPx(ctx))
        }

        val titleLabel = TextView(ctx).apply {
            text = "Novo cartão"
            typeface = DSSFont.bold(ctx, 22f).typeface
            textSize = 22f
            setTextColor(DSSColors.textPrimary())
        }
        root.addView(titleLabel, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { bottomMargin = 24f.dpToPx(ctx) })

        numberField = DSSLabelTextField(ctx).apply {
            configure(
                placeholder = "Número do cartão",
                type = DSSLabelTextField.Type.Numeric(19),
                rightIcon = cameraIcon,
                rightAction = { onCameraTap?.invoke() },
            )
        }
        numberErrorLabel = makeErrorLabel(ctx)
        // Primeiro campo: iOS posiciona o stack a 24 do título (sem spacing antes do 1º elemento),
        // então este campo não soma margem extra ao bottomMargin de 24 do título.
        root.addView(numberField, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ))
        root.addView(numberErrorLabel, fullWidthErrorLp(ctx))

        // Expiry + CVV row
        expiryField = DSSLabelTextField(ctx).apply {
            configure(placeholder = "Validade (MM/AA)", type = DSSLabelTextField.Type.Numeric(5))
        }
        expiryErrorLabel = makeErrorLabel(ctx)
        cvvField = DSSLabelTextField(ctx).apply {
            configure(placeholder = "CVV", type = DSSLabelTextField.Type.Numeric(3))
        }
        cvvErrorLabel = makeErrorLabel(ctx)

        val row = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        val leftCol = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        leftCol.addView(expiryField, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ))
        leftCol.addView(expiryErrorLabel, errorLp(ctx))

        val rightCol = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        rightCol.addView(cvvField, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ))
        rightCol.addView(cvvErrorLabel, errorLp(ctx))

        row.addView(leftCol, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            rightMargin = 6f.dpToPx(ctx)
        })
        row.addView(rightCol, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            leftMargin = 6f.dpToPx(ctx)
        })
        root.addView(row, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = 12f.dpToPx(ctx) })

        holderField = DSSLabelTextField(ctx).apply {
            configure(placeholder = "Nome impresso no cartão", type = DSSLabelTextField.Type.Text)
        }
        holderErrorLabel = makeErrorLabel(ctx)
        root.addView(holderField, defaultFieldLp(ctx))
        root.addView(holderErrorLabel, fullWidthErrorLp(ctx))

        documentField = DSSLabelTextField(ctx).apply {
            configure(placeholder = "CPF / CNPJ", type = DSSLabelTextField.Type.CpfOrCnpj)
        }
        documentErrorLabel = makeErrorLabel(ctx)
        root.addView(documentField, defaultFieldLp(ctx))
        root.addView(documentErrorLabel, fullWidthErrorLp(ctx))

        // Bandeiras (opcional via brand)
        if (brandsImage != null) {
            val brandsImageView = android.widget.ImageView(ctx).apply {
                scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
                setImageDrawable(brandsImage)
            }
            root.addView(brandsImageView, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 30f.dpToPx(ctx),
            ).apply { topMargin = 12f.dpToPx(ctx) })
        }

        val addButton = DSSPrincipalButton(ctx).apply {
            text = "Adicionar cartão"
            textSize = 18f
            typeface = DSSFont.bold(ctx, 18f).typeface
            cornerRadiusDp = 28f
            onTap = { handleAddCard() }
        }
        root.addView(addButton, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 56f.dpToPx(ctx),
        ).apply { topMargin = 12f.dpToPx(ctx) })

        // Formatadores
        attachCardNumberFormatter(numberField)
        attachExpiryFormatter(expiryField)
        attachCvvFormatter(cvvField)

        // Keyboard types
        numberField.editText.inputType = InputType.TYPE_CLASS_NUMBER
        expiryField.editText.inputType = InputType.TYPE_CLASS_NUMBER
        cvvField.editText.inputType = InputType.TYPE_CLASS_NUMBER
        holderField.editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PERSON_NAME

        scroll.addView(root, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
        ))
        return scroll
    }

    private fun defaultFieldLp(ctx: android.content.Context) = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
    ).apply { topMargin = 12f.dpToPx(ctx) }

    /** Gap field->error dos campos dentro da row expiry/cvv (iOS: inner stack spacing = 4). */
    private fun errorLp(ctx: android.content.Context) = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
    ).apply { topMargin = 4f.dpToPx(ctx) }

    /** Gap field->error dos campos full-width (iOS: formStack spacing = 12). */
    private fun fullWidthErrorLp(ctx: android.content.Context) = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
    ).apply { topMargin = 12f.dpToPx(ctx) }

    private fun makeErrorLabel(ctx: android.content.Context): TextView = TextView(ctx).apply {
        textSize = 12f
        // iOS usa DSSColors.primary (cor da brand) nos labels de erro deste sheet, não o token de erro.
        setTextColor(DSSColors.primary())
        visibility = View.GONE
    }

    /** Aceita o cartão lido por outro fluxo (ex.: scanner) populando os campos. */
    fun populateFromScan(
        number: String?, expiryMonth: String?, expiryYear: String?, cvv: String?, holderName: String?,
    ) {
        if (number != null) {
            val formatted = buildString {
                number.filter { it.isDigit() }.forEachIndexed { i, c ->
                    if (i > 0 && i % 4 == 0) append(' ')
                    append(c)
                }
            }
            numberField.editText.setText(formatted)
        }
        if (expiryMonth != null && expiryYear != null) {
            expiryField.editText.setText("$expiryMonth/$expiryYear")
        }
        cvv?.let { cvvField.editText.setText(it) }
        holderName?.let { holderField.editText.setText(it) }

        if (documentField.editText.text?.isNullOrEmpty() != false) {
            documentField.editText.requestFocus()
        }
    }

    private fun handleAddCard() {
        clearErrors()

        val cardDigits = (numberField.editText.text?.toString() ?: "").filter { it.isDigit() }
        val expiryRaw = (expiryField.editText.text?.toString() ?: "")
        val expiryDigits = expiryRaw.filter { it.isDigit() }
        val cvv = (cvvField.editText.text?.toString() ?: "").filter { it.isDigit() }
        val holder = (holderField.editText.text?.toString() ?: "").trim()
        val documentDigits = (documentField.editText.text?.toString() ?: "").filter { it.isDigit() }

        var hasError = false

        if (cardDigits.length != 16) {
            numberErrorLabel.visibility = View.VISIBLE
            numberErrorLabel.text = "Número do cartão é obrigatório."
            hasError = true
        }
        if (expiryDigits.length < 4) {
            expiryErrorLabel.visibility = View.VISIBLE
            expiryErrorLabel.text = "Validade é obrigatório."
            hasError = true
        }
        val month = if (expiryDigits.length >= 2) expiryDigits.take(2) else ""
        val year = if (expiryDigits.length >= 4) expiryDigits.takeLast(2) else ""

        if (month.isEmpty() || year.isEmpty()) {
            expiryErrorLabel.visibility = View.VISIBLE
            expiryErrorLabel.text = "Validade é obrigatório."
            hasError = true
        }
        if (cvv.isEmpty()) {
            cvvErrorLabel.visibility = View.VISIBLE
            cvvErrorLabel.text = "CVV é obrigatório."
            hasError = true
        }
        if (holder.isEmpty()) {
            holderErrorLabel.visibility = View.VISIBLE
            holderErrorLabel.text = "Nome impresso é obrigatório."
            hasError = true
        }
        if (documentDigits.isEmpty()) {
            documentErrorLabel.visibility = View.VISIBLE
            documentErrorLabel.text = "CPF / CNPJ é obrigatório."
            hasError = true
        }
        if (hasError) return

        val data = DSSCardFormData(
            number = cardDigits,
            expiryMonth = month,
            expiryYear = year,
            cvv = cvv,
            holderName = holder,
            document = documentDigits,
        )
        onAddCard?.invoke(data)
        dismiss()
    }

    private fun clearErrors() {
        listOf(numberErrorLabel, expiryErrorLabel, cvvErrorLabel, holderErrorLabel, documentErrorLabel)
            .forEach { it.visibility = View.GONE; it.text = null }
    }

    // MARK: - Formatters

    private fun attachCardNumberFormatter(field: DSSLabelTextField) {
        attachFormatter(field, maxDigits = 16) { digits ->
            buildString {
                digits.forEachIndexed { i, c ->
                    if (i > 0 && i % 4 == 0) append(' ')
                    append(c)
                }
            }
        }
    }

    private fun attachExpiryFormatter(field: DSSLabelTextField) {
        attachFormatter(field, maxDigits = 4) { digits ->
            buildString {
                digits.forEachIndexed { i, c ->
                    if (i == 2) append('/')
                    append(c)
                }
            }
        }
    }

    private fun attachCvvFormatter(field: DSSLabelTextField) {
        attachFormatter(field, maxDigits = 3) { it }
    }

    private fun attachFormatter(field: DSSLabelTextField, maxDigits: Int, format: (String) -> String) {
        val edit = field.editText
        edit.addTextChangedListener(object : TextWatcher {
            private var updating = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                if (updating || s == null) return
                val digits = s.toString().filter { it.isDigit() }.take(maxDigits)
                val formatted = format(digits)
                if (formatted != s.toString()) {
                    updating = true
                    edit.setText(formatted)
                    edit.setSelection(formatted.length)
                    updating = false
                }
            }
        })
    }

    companion object {
        fun present(
            activity: FragmentActivity,
            brandsImage: Drawable? = null,
            cameraIcon: Drawable? = null,
            onCameraTap: (() -> Unit)? = null,
            onAddCard: (DSSCardFormData) -> Unit,
        ): DSSCardFormBottomSheet {
            val sheet = DSSCardFormBottomSheet()
            sheet.brandsImage = brandsImage
            sheet.cameraIcon = cameraIcon
            sheet.onCameraTap = onCameraTap
            sheet.onAddCard = onAddCard
            sheet.show(activity.supportFragmentManager, "DSSCardFormBottomSheet")
            return sheet
        }
    }
}

