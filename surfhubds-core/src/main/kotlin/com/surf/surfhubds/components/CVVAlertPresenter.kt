package com.surf.surfhubds.components

import android.app.Activity
import android.text.InputFilter
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.view.Gravity
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatEditText
import com.surf.surfhubds.util.dpToPx

/**
 * Port do `CVVAlertPresenter` do iOS — alerta simples para coleta de CVV (3 dígitos).
 */
object CVVAlertPresenter {

    /**
     * Apresenta um alerta para coletar CVV (3 dígitos) e retorna via callback.
     */
    fun present(activity: Activity, completion: (String) -> Unit) {
        showCvvDialog(activity, completion)
    }

    private fun showCvvDialog(activity: Activity, completion: (String) -> Unit) {
        val ctx = activity
        val editText = AppCompatEditText(ctx).apply {
            hint = "CVV (3 dígitos)"
            inputType = InputType.TYPE_CLASS_NUMBER
            transformationMethod = PasswordTransformationMethod.getInstance()
            filters = arrayOf<InputFilter>(InputFilter.LengthFilter(3))
        }
        val container = FrameLayout(ctx).apply {
            val pad = 24f.dpToPx(ctx)
            setPadding(pad, pad / 2, pad, 0)
            addView(editText, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
            ).apply { gravity = Gravity.CENTER })
        }

        AlertDialog.Builder(ctx)
            .setTitle("Digite o CVV")
            .setMessage("Informe o código de segurança do cartão (3 dígitos).")
            .setView(container)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Confirmar") { _, _ ->
                val raw = editText.text?.toString().orEmpty()
                val digits = raw.filter { it.isDigit() }
                if (digits.length != 3) {
                    presentValidationError(activity, "CVV inválido. Digite exatamente 3 dígitos.") {
                        showCvvDialog(activity, completion)
                    }
                } else {
                    completion(digits)
                }
            }
            .show()
    }

    private fun presentValidationError(
        activity: Activity, message: String, retry: () -> Unit,
    ) {
        AlertDialog.Builder(activity)
            .setTitle("Atenção")
            .setMessage(message)
            .setPositiveButton("Tentar novamente") { _, _ -> retry() }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
