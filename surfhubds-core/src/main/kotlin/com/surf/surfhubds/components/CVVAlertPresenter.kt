package com.surf.surfhubds.components

import android.app.Activity
import android.text.InputFilter
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.view.Gravity
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatEditText
import androidx.fragment.app.FragmentActivity
import com.surf.surfhubds.util.AppStrings
import com.surf.surfhubds.util.applyDssTheme
import com.surf.surfhubds.util.dpToPx

/**
 * Coleta de CVV (3 dígitos). Renderiza pelo [DSSAppDialog] (card do DSS) quando a
 * [Activity] é uma [FragmentActivity]; caso contrário cai no [AlertDialog] legado.
 */
object CVVAlertPresenter {

    /**
     * Apresenta um alerta para coletar CVV (3 dígitos) e retorna via callback.
     */
    fun present(activity: Activity, completion: (String) -> Unit) {
        val fragmentActivity = activity as? FragmentActivity
        if (fragmentActivity != null) {
            DSSAppDialog.input(
                activity = fragmentActivity,
                title = AppStrings.brand(activity, "cvv_title", "Digite o CVV"),
                message = AppStrings.brand(activity, "cvv_message", "Informe o código de segurança do cartão (3 dígitos)."),
                confirmText = "Confirmar",
                cancelText = "Cancelar",
                hint = AppStrings.brand(activity, "cvv_placeholder", "CVV (3 dígitos)"),
                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD,
                secure = true,
                maxLength = 3,
                digitsOnly = true,
                centered = true,
                validate = { text ->
                    val digits = text.filter { it.isDigit() }
                    if (digits.length != 3) {
                        AppStrings.brand(activity, "cvv_invalid", "CVV inválido. Digite exatamente 3 dígitos.")
                    } else {
                        null
                    }
                },
                onConfirm = { text -> completion(text.filter { it.isDigit() }) },
            )
            return
        }
        showCvvDialog(activity, completion)
    }

    private fun showCvvDialog(activity: Activity, completion: (String) -> Unit) {
        val ctx = activity
        val editText = AppCompatEditText(ctx).apply {
            hint = AppStrings.brand(ctx, "cvv_placeholder", "CVV (3 dígitos)")
            // iOS: keyboardType = .numberPad + isSecureTextEntry = true
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            transformationMethod = PasswordTransformationMethod.getInstance()
            // iOS limitToThreeDigits: mantém apenas números e limita a 3 caracteres (ao vivo)
            filters = arrayOf<InputFilter>(digitsOnlyFilter(), InputFilter.LengthFilter(3))
        }
        val container = FrameLayout(ctx).apply {
            val pad = 24f.dpToPx(ctx)
            setPadding(pad, pad / 2, pad, 0)
            addView(editText, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
            ).apply { gravity = Gravity.CENTER })
        }

        val dialog = AlertDialog.Builder(ctx)
            .setTitle(AppStrings.brand(ctx, "cvv_title", "Digite o CVV"))
            .setMessage(AppStrings.brand(ctx, "cvv_message", "Informe o código de segurança do cartão (3 dígitos)."))
            .setView(container)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Confirmar") { _, _ ->
                val raw = editText.text?.toString().orEmpty()
                val digits = raw.filter { it.isDigit() }
                if (digits.length != 3) {
                    presentValidationError(activity, AppStrings.brand(activity, "cvv_invalid", "CVV inválido. Digite exatamente 3 dígitos.")) {
                        showCvvDialog(activity, completion)
                    }
                } else {
                    completion(digits)
                }
            }
            .create()
        showWithBlur(activity, dialog)
    }

    /** Mostra o alert com blur do fundo (backdrop borrado atrás), removendo no dismiss. */
    private fun showWithBlur(activity: Activity, dialog: AlertDialog) {
        val backdrop = com.surf.surfhubds.util.DSSBlur.addBlurBackdrop(activity)
        dialog.setOnDismissListener { com.surf.surfhubds.util.DSSBlur.removeBackdrop(backdrop) }
        dialog.show()
        dialog.window?.setDimAmount(0f)
        // Tematiza pelo DSS (sem isso o dialog fica branco e o texto some no dark/black).
        dialog.applyDssTheme()
    }

    /**
     * Replica o filtro `limitToThreeDigits` do iOS: mantém apenas dígitos numéricos
     * (descarta qualquer caractere não numérico inclusive em colagem).
     */
    private fun digitsOnlyFilter(): InputFilter = InputFilter { source, start, end, _, _, _ ->
        val filtered = StringBuilder()
        for (i in start until end) {
            val c = source[i]
            if (c.isDigit()) filtered.append(c)
        }
        // null = aceita o trecho como está; caso contrário substitui pelo filtrado
        if (filtered.length == end - start) null else filtered.toString()
    }

    private fun presentValidationError(
        activity: Activity, message: String, retry: () -> Unit,
    ) {
        val dialog = AlertDialog.Builder(activity)
            .setTitle(AppStrings.brand(activity, "common_attention", "Atenção"))
            .setMessage(message)
            .setPositiveButton(AppStrings.brand(activity, "common_try_again", "Tentar novamente")) { _, _ -> retry() }
            .setNegativeButton("Cancelar", null)
            .create()
        showWithBlur(activity, dialog)
    }
}
