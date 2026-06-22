package com.surf.surfhubds.components

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatEditText
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import com.surf.surfhubds.font.DSSFont
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.util.DSSBlur
import com.surf.surfhubds.util.DrawableFactory
import com.surf.surfhubds.util.dpToPx

/**
 * Dialog canônico do DSS: card arredondado centralizado sobre um blur do fundo, com
 * botões preenchidos na cor da brand (`DSSPrincipalButton`). Unifica três casos que
 * antes usavam `AlertDialog` do sistema (visual fora do design):
 *
 *  - [Mode.ALERT]   — um botão (ex.: erros / avisos). Vinha de `showSurfAlert` 1 botão.
 *  - [Mode.CONFIRM] — dois botões (cancelar/confirmar). Vinha do `AppConfirmDialog` do app.
 *  - [Mode.INPUT]   — campo de texto + validação (ex.: CVV). Vinha do `CVVAlertPresenter`.
 *
 * Use os helpers [alert], [confirm] e [input] do companion em vez de instanciar direto.
 * O fundo é o blur padrão do DSS ([DSSBlur]); o card respeita `surface`/`textPrimary`.
 */
class DSSAppDialog : DialogFragment() {

    enum class Mode { ALERT, CONFIRM, INPUT }

    private var mode: Mode = Mode.ALERT
    private var title: String = ""
    private var message: String = ""
    private var confirmText: String = ""
    private var cancelText: String = ""
    private var onConfirm: ((String?) -> Unit)? = null
    private var onCancel: (() -> Unit)? = null

    // Configuração do modo INPUT.
    private var inputHint: String = ""
    private var inputType: Int = InputType.TYPE_CLASS_TEXT
    private var inputSecure: Boolean = false
    private var inputMaxLength: Int = 0 // 0 = sem limite
    private var inputDigitsOnly: Boolean = false
    private var inputGravity: Int = Gravity.START
    /** Retorna a mensagem de erro a exibir, ou null se o texto for válido. */
    private var validate: ((String) -> String?)? = null

    private var backdrop: View? = null
    private var editText: EditText? = null
    private var errorLabel: TextView? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        val ctx = requireContext()

        // Fundo transparente: o scrim/blur fica a cargo do [DSSBlur] (adicionado em onStart),
        // por trás da janela do dialog, igual aos demais alerts do DSS.
        val root = FrameLayout(ctx).apply {
            isClickable = true
        }

        val card = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            isClickable = true // engole toques para não fechar ao tocar no card
            background = DrawableFactory.rounded(
                context = ctx, backgroundColor = DSSColors.surface(), cornerRadiusDp = 16f,
            )
            val pad = 24f.dpToPx(ctx)
            setPadding(pad, pad, pad, pad)
        }

        val titleLabel = TextView(ctx).apply {
            text = title
            gravity = Gravity.CENTER
            typeface = DSSFont.bold(ctx, 18f).typeface
            textSize = 18f
            setTextColor(DSSColors.textPrimary())
        }
        card.addView(titleLabel, matchWidth())

        if (message.isNotEmpty()) {
            val messageLabel = TextView(ctx).apply {
                text = message
                gravity = Gravity.CENTER
                typeface = DSSFont.regular(ctx, 15f).typeface
                textSize = 15f
                setTextColor(DSSColors.textPrimary())
            }
            card.addView(messageLabel, matchWidth().apply { topMargin = 12f.dpToPx(ctx) })
        }

        if (mode == Mode.INPUT) {
            addInputField(ctx, card)
        }

        addButtons(ctx, card)

        val sideMargin = 32f.dpToPx(ctx)
        root.addView(card, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT,
        ).apply {
            gravity = Gravity.CENTER
            leftMargin = sideMargin
            rightMargin = sideMargin
        })

        return root
    }

    private fun addInputField(ctx: android.content.Context, card: LinearLayout) {
        val field = AppCompatEditText(ctx).apply {
            hint = inputHint
            gravity = inputGravity
            setTextColor(DSSColors.textPrimary())
            setHintTextColor(DSSColors.textSecondary())
            typeface = DSSFont.regular(ctx, 16f).typeface
            textSize = 16f
            inputType = this@DSSAppDialog.inputType
            if (inputSecure) transformationMethod = PasswordTransformationMethod.getInstance()
            background = DrawableFactory.rounded(
                context = ctx,
                backgroundColor = Color.TRANSPARENT,
                cornerRadiusDp = 12f,
                strokeColor = DSSColors.borderDefault(),
                strokeWidthDp = 1.5f,
            )
            val h = 14f.dpToPx(ctx)
            setPadding(h, h, h, h)
            val builtFilters = mutableListOf<InputFilter>()
            if (inputDigitsOnly) builtFilters.add(digitsOnlyFilter())
            if (inputMaxLength > 0) builtFilters.add(InputFilter.LengthFilter(inputMaxLength))
            if (builtFilters.isNotEmpty()) filters = builtFilters.toTypedArray()
        }
        editText = field
        card.addView(field, matchWidth().apply { topMargin = 16f.dpToPx(ctx) })

        errorLabel = TextView(ctx).apply {
            gravity = Gravity.START
            typeface = DSSFont.regular(ctx, 13f).typeface
            textSize = 13f
            setTextColor(DSSColors.borderError())
            visibility = View.GONE
        }
        card.addView(errorLabel, matchWidth().apply { topMargin = 6f.dpToPx(ctx) })
    }

    private fun addButtons(ctx: android.content.Context, card: LinearLayout) {
        val topMargin = if (mode == Mode.INPUT) 20f.dpToPx(ctx) else 24f.dpToPx(ctx)

        if (mode == Mode.ALERT) {
            val confirmButton = DSSPrincipalButton(ctx).apply {
                defaultWidthDp = 0f
                configure(title = confirmText, action = { confirmTapped() })
            }
            card.addView(confirmButton, matchWidth().apply { this.topMargin = topMargin })
            return
        }

        val buttonsRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            weightSum = 2f
        }
        val gap = 6f.dpToPx(ctx)

        val cancelButton = DSSPrincipalButton(ctx).apply {
            defaultWidthDp = 0f
            configure(title = cancelText, action = {
                DSSBlur.removeBackdrop(backdrop)
                backdrop = null
                dismissAllowingStateLoss()
                onCancel?.invoke()
            })
        }
        val confirmButton = DSSPrincipalButton(ctx).apply {
            defaultWidthDp = 0f
            configure(title = confirmText, action = { confirmTapped() })
        }
        buttonsRow.addView(cancelButton, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { rightMargin = gap })
        buttonsRow.addView(confirmButton, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { leftMargin = gap })

        card.addView(buttonsRow, matchWidth().apply { this.topMargin = topMargin })
    }

    /** Trata o toque no botão de confirmar, validando o input quando há campo. */
    private fun confirmTapped() {
        if (mode == Mode.INPUT) {
            val text = editText?.text?.toString().orEmpty()
            val error = validate?.invoke(text)
            if (error != null) {
                errorLabel?.apply { this.text = error; visibility = View.VISIBLE }
                return
            }
            dismissAllowingStateLoss()
            onConfirm?.invoke(text)
        } else {
            dismissAllowingStateLoss()
            onConfirm?.invoke(null)
        }
    }

    private fun matchWidth() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
    )

    /**
     * Mantém apenas dígitos (descarta não numéricos inclusive em colagem). Port do
     * `limitToThreeDigits` do iOS, herdado do antigo `CVVAlertPresenter`.
     */
    private fun digitsOnlyFilter(): InputFilter = InputFilter { source, start, end, _, _, _ ->
        val filtered = StringBuilder()
        for (i in start until end) {
            val c = source[i]
            if (c.isDigit()) filtered.append(c)
        }
        if (filtered.length == end - start) null else filtered.toString()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { win: Window ->
            win.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            win.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            // O scrim/blur fica no backdrop do DSS; zera o dim p/ não escurecer duas vezes.
            win.setDimAmount(0f)
        }
        (activity as? FragmentActivity)?.let { backdrop = DSSBlur.addBlurBackdrop(it) }
    }

    override fun onDestroyView() {
        DSSBlur.removeBackdrop(backdrop)
        backdrop = null
        editText = null
        errorLabel = null
        super.onDestroyView()
    }

    companion object {
        private const val TAG = "DSSAppDialog"

        /** Alerta de um botão (ex.: erro/aviso). */
        fun alert(
            activity: FragmentActivity,
            title: String,
            message: String,
            buttonText: String = "OK",
            onDismiss: (() -> Unit)? = null,
        ) {
            DSSAppDialog().apply {
                this.mode = Mode.ALERT
                this.title = title
                this.message = message
                this.confirmText = buttonText
                this.onConfirm = { onDismiss?.invoke() }
            }.show(activity.supportFragmentManager, TAG)
        }

        /** Confirmação de dois botões (cancelar/confirmar). */
        fun confirm(
            activity: FragmentActivity,
            title: String,
            message: String,
            confirmText: String,
            cancelText: String,
            onConfirm: () -> Unit,
            onCancel: (() -> Unit)? = null,
        ) {
            DSSAppDialog().apply {
                this.mode = Mode.CONFIRM
                this.title = title
                this.message = message
                this.confirmText = confirmText
                this.cancelText = cancelText
                this.onConfirm = { onConfirm() }
                this.onCancel = onCancel
            }.show(activity.supportFragmentManager, TAG)
        }

        /**
         * Coleta de texto com validação (ex.: CVV). [validate] recebe o texto e retorna a
         * mensagem de erro (exibida abaixo do campo, sem fechar) ou null se válido — nesse
         * caso o dialog fecha e [onConfirm] recebe o texto.
         */
        fun input(
            activity: FragmentActivity,
            title: String,
            message: String,
            confirmText: String,
            cancelText: String,
            hint: String = "",
            inputType: Int = InputType.TYPE_CLASS_TEXT,
            secure: Boolean = false,
            maxLength: Int = 0,
            digitsOnly: Boolean = false,
            centered: Boolean = false,
            validate: ((String) -> String?)? = null,
            onConfirm: (String) -> Unit,
            onCancel: (() -> Unit)? = null,
        ) {
            DSSAppDialog().apply {
                this.mode = Mode.INPUT
                this.title = title
                this.message = message
                this.confirmText = confirmText
                this.cancelText = cancelText
                this.inputHint = hint
                this.inputType = inputType
                this.inputSecure = secure
                this.inputMaxLength = maxLength
                this.inputDigitsOnly = digitsOnly
                this.inputGravity = if (centered) Gravity.CENTER else Gravity.START
                this.validate = validate
                this.onConfirm = { text -> onConfirm(text.orEmpty()) }
                this.onCancel = onCancel
            }.show(activity.supportFragmentManager, TAG)
        }
    }
}
