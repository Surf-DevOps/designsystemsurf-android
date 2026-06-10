package com.surf.surfhubds.components

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import com.surf.surfhubds.font.DSSFont
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.util.DrawableFactory
import com.surf.surfhubds.util.dpToPx

/**
 * Port do `DoubtsPopupViewController` do iOS — popup centralizado com perguntas e respostas
 * sobre portabilidade. Usa [DialogFragment] com janela transparente para reproduzir o card
 * arredondado central com overlay escuro.
 */
class DoubtsPopupDialogFragment : DialogFragment() {

    /** Seção (título + corpo) exibida no popup. */
    data class Section(val title: String, val body: String)

    private val sections: MutableList<Section> = mutableListOf(
        Section(
            title = "Condições para realizar a portabilidade",
            body = "Sua linha Surf deve estar atrelada ao mesmo CPF da linha da outra operadora; " +
                "Certifique-se de que o número a ser portado esteja ativo e que não existam débitos " +
                "pendentes na outra operadora.",
        ),
        Section(
            title = "Quanto tempo leva a portabilidade?",
            body = "O prazo para efetuar a portabilidade é de até 72 horas úteis, e tanto o seu número " +
                "Surf quanto o da outra operadora vão funcionar normalmente nesse período.",
        ),
    )

    /** Substitui o conteúdo padrão por uma lista custom de seções. */
    fun setSections(custom: List<Section>) {
        sections.clear()
        sections.addAll(custom)
    }

    /** Referência ao card central para reproduzir a animação de entrada do iOS. */
    private var cardView: View? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        val ctx = requireContext()

        // Root with dim overlay (iOS: blur de fundo -> token de overlay)
        val root = FrameLayout(ctx).apply {
            setBackgroundColor(DSSColors.overlay())
            isClickable = true
            setOnClickListener { dismiss() }
        }

        // Container card (320x420)
        val cardWidth = 320f.dpToPx(ctx)
        val cardHeight = 420f.dpToPx(ctx)

        val card = FrameLayout(ctx).apply {
            background = DrawableFactory.rounded(
                context = ctx, backgroundColor = DSSColors.surface(), cornerRadiusDp = 12f,
            )
            isClickable = true // swallow taps so they don't dismiss
        }
        root.addView(card, FrameLayout.LayoutParams(cardWidth, cardHeight).apply {
            gravity = Gravity.CENTER
        })
        cardView = card

        // Header
        val titleLabel = TextView(ctx).apply {
            text = "Dúvidas"
            gravity = Gravity.CENTER
            // iOS: .systemFont(ofSize: 16, weight: .semibold) -> medium
            typeface = DSSFont.medium(ctx, 16f).typeface
            textSize = 16f
            maxLines = 1
            // iOS: .systemBlue (// DSSColors.brandPrimary) -> primary
            setTextColor(DSSColors.primary())
        }
        card.addView(titleLabel, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = 38f.dpToPx(ctx) })

        val closeButton = ImageButton(ctx).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setBackgroundColor(Color.TRANSPARENT)
            // iOS: tintColor = .systemBlue (// DSSColors.brandPrimary) -> primary
            setColorFilter(DSSColors.primary())
            setOnClickListener { dismiss() }
        }
        card.addView(closeButton, FrameLayout.LayoutParams(
            28f.dpToPx(ctx), 28f.dpToPx(ctx),
        ).apply {
            gravity = Gravity.END or Gravity.TOP
            topMargin = 32f.dpToPx(ctx)
            rightMargin = 16f.dpToPx(ctx)
        })

        // Scrollable content
        val scroll = ScrollView(ctx)
        val stack = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
        }

        // iOS: stackView.spacing = 16 (espaçamento uniforme entre TODAS as subviews;
        // o primeiro elemento não tem margem superior).
        val spacing = 16f.dpToPx(ctx)
        sections.forEachIndexed { index, section ->
            val sectionTitle = TextView(ctx).apply {
                text = section.title
                // iOS: .systemFont(ofSize: 16, weight: .bold) ; .black -> textPrimary
                typeface = DSSFont.bold(ctx, 16f).typeface
                textSize = 16f
                setTextColor(DSSColors.textPrimary())
            }
            val sectionBody = TextView(ctx).apply {
                text = section.body
                // iOS: .systemFont(ofSize: 14) ; .systemGray -> textSecondary
                typeface = DSSFont.regular(ctx, 14f).typeface
                textSize = 14f
                setTextColor(DSSColors.textSecondary())
            }
            stack.addView(sectionTitle, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = if (index == 0) 0 else spacing })
            stack.addView(sectionBody, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = spacing })
        }

        scroll.addView(stack, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
        ))

        card.addView(scroll, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT,
        ).apply {
            topMargin = 80f.dpToPx(ctx)
            leftMargin = 18f.dpToPx(ctx)
            rightMargin = 18f.dpToPx(ctx)
            bottomMargin = 18f.dpToPx(ctx)
        })

        return root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { win: Window ->
            win.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
            win.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
        }
        animateIn()
    }

    /**
     * Reproduz `animateIn` do iOS: card surge com alpha 0 -> 1 e escala 0.95 -> 1.0
     * em 0.18s (UIView.animate withDuration: 0.18).
     */
    private fun animateIn() {
        cardView?.let { card ->
            card.alpha = 0f
            card.scaleX = 0.95f
            card.scaleY = 0.95f
            card.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(180L)
                .start()
        }
    }

    companion object {
        fun present(activity: FragmentActivity, sections: List<Section>? = null): DoubtsPopupDialogFragment {
            val popup = DoubtsPopupDialogFragment()
            if (sections != null) popup.setSections(sections)
            popup.show(activity.supportFragmentManager, "DoubtsPopupDialogFragment")
            return popup
        }
    }
}
