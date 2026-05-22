package com.surf.surfhubds.components

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.theme.ThemeManager
import com.surf.surfhubds.tokens.ColorScheme
import com.surf.surfhubds.util.dpToPx

/**
 * Port do `DarkModeExampleViewController` do iOS — tela de exemplo demonstrando
 * todos os tipos de [DSSLabelTextField] e validação manual em dark mode.
 */
class DarkModeExampleFragment : Fragment() {

    private val textFields = mutableListOf<DSSLabelTextField>()
    private lateinit var rootContainer: ScrollView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        val ctx = requireContext()

        rootContainer = ScrollView(ctx).apply {
            setBackgroundColor(DSSColors.background())
        }
        val stack = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20f.dpToPx(ctx), 20f.dpToPx(ctx), 20f.dpToPx(ctx), 20f.dpToPx(ctx))
        }

        val name = DSSLabelTextField(ctx).apply {
            configure(label = "Nome Completo", placeholder = "Digite seu nome completo",
                type = DSSLabelTextField.Type.Text)
        }
        addRow(stack, name)

        val cpf = DSSLabelTextField(ctx).apply {
            configure(label = "CPF", placeholder = "000.000.000-00", type = DSSLabelTextField.Type.Cpf)
        }
        addRow(stack, cpf)

        val phone = DSSLabelTextField(ctx).apply {
            configure(label = "Telefone", placeholder = "(00) 00000-0000", type = DSSLabelTextField.Type.Phone)
        }
        addRow(stack, phone)

        val password = DSSLabelTextField(ctx).apply {
            configure(label = "Senha", placeholder = "Digite sua senha",
                type = DSSLabelTextField.Type.Password, isSecureEntry = true, showsErrorLabel = true)
        }
        addRow(stack, password)

        val email = DSSLabelTextField(ctx).apply {
            configure(label = "Email", placeholder = "seu@email.com",
                type = DSSLabelTextField.Type.Text, showsErrorLabel = true)
        }
        addRow(stack, email)

        val validateButton = DSSPrincipalButton(ctx).apply {
            text = "Validar Campos"
            onTap = { validateFields() }
        }
        stack.addView(validateButton, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 50f.dpToPx(ctx),
        ).apply { topMargin = 20f.dpToPx(ctx) })

        val themeToggleButton = DSSPrincipalButton(ctx).apply {
            text = "Alternar Tema (Teste)"
            onTap = { toggleTheme() }
        }
        stack.addView(themeToggleButton, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 44f.dpToPx(ctx),
        ).apply { topMargin = 12f.dpToPx(ctx) })

        textFields.clear()
        textFields.addAll(listOf(name, cpf, phone, password, email))
        for (i in textFields.indices) {
            val prev = if (i > 0) textFields[i - 1] else null
            val next = if (i < textFields.size - 1) textFields[i + 1] else null
            textFields[i].previousField = prev
            textFields[i].nextField = next
        }

        rootContainer.addView(stack, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
        ))

        return rootContainer
    }

    private fun addRow(stack: LinearLayout, field: DSSLabelTextField) {
        stack.addView(field, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = 20f.dpToPx(stack.context) })
    }

    private fun validateFields() {
        var isValid = true
        isValid = textFields[0].validateRequired(fieldName = "Nome") && isValid
        isValid = textFields[1].validateRequired(fieldName = "CPF") && isValid
        isValid = textFields[2].validateRequired(fieldName = "Telefone") && isValid
        isValid = textFields[3].validateRequired(fieldName = "Senha") && isValid

        val emailText = textFields[4].text?.toString().orEmpty()
        when {
            emailText.isEmpty() -> { textFields[4].setError("Email é obrigatório"); isValid = false }
            !emailText.contains("@") -> { textFields[4].setError("Email deve conter @"); isValid = false }
            else -> textFields[4].setError(null)
        }
        if (isValid) showAlert("Sucesso", "Todos os campos são válidos!")
    }

    private fun toggleTheme() {
        val current = ThemeManager.colorScheme
        val next = if (current == ColorScheme.LIGHT) ColorScheme.DARK else ColorScheme.LIGHT
        ThemeManager.setColorScheme(next)
        // ThemeAware observers irão atualizar automaticamente.
        rootContainer.setBackgroundColor(DSSColors.background())
    }

    private fun showAlert(title: String, message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
}

