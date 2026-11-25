package com.surf.surfhubds.components

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.theme.Theme
import com.surf.surfhubds.theme.ThemeAware
import com.surf.surfhubds.theme.setupThemeObserver
import com.surf.surfhubds.util.DSSWeight
import com.surf.surfhubds.util.dpToPx
import com.surf.surfhubds.util.dssInitial
import com.surf.surfhubds.util.dssText
import com.surf.surfhubds.util.styleText

/**
 * Avatares sobrepostos com a inicial de cada pessoa e um "+N" no fim (Figma do Grupo de
 * Recarga: "G A +3"). Cada círculo tem uma borda da cor do fundo onde o componente está,
 * para separar os vizinhos; ajuste [ringColor] quando ele ficar sobre um card cinza.
 */
class DSSAvatarStack @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr), ThemeAware {

    /** Diâmetro de cada avatar. */
    var avatarSizeDp: Float = 26f
        set(value) { field = value; rebuild() }

    /** Quanto cada avatar entra por baixo do anterior. */
    var overlapDp: Float = 7f
        set(value) { field = value; rebuild() }

    /** Cor da borda que separa os avatares; `null` usa a cor de fundo da tela. */
    @ColorInt var ringColor: Int? = null
        set(value) { field = value; rebuild() }

    private var initials: List<String> = emptyList()
    private var extraCount: Int = 0

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setupThemeObserver()
    }

    /**
     * @param initials iniciais na ordem de exibição (ex.: `listOf("G", "A")`).
     * @param extraCount pessoas além das mostradas; vira o último círculo "+N" (0 esconde).
     */
    fun configure(initials: List<String>, extraCount: Int = 0) {
        this.initials = initials
        this.extraCount = extraCount.coerceAtLeast(0)
        rebuild()
    }

    /** Mostra até [maxVisible] iniciais dos [names] e resume o resto em "+N". */
    fun configureNames(names: List<String>, maxVisible: Int) {
        val visible = names.take(maxVisible).map { it.dssInitial() }
        configure(visible, names.size - visible.size)
    }

    override fun applyTheme(theme: Theme) { rebuild() }

    private fun rebuild() {
        removeAllViews()
        val labels = initials + if (extraCount > 0) listOf("+$extraCount") else emptyList()
        val size = avatarSizeDp.dpToPx(context)
        val overlap = overlapDp.dpToPx(context)
        labels.forEachIndexed { index, label ->
            val circle = FrameLayout(context).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(avatarFill())
                    setStroke(2f.dpToPx(context), ringColor ?: DSSColors.background())
                }
            }
            val text: TextView = dssText(context, DSSWeight.BOLD, labelSizeSp(), DSSColors.textPrimary()).apply {
                text = label
                gravity = Gravity.CENTER
            }
            circle.addView(text, FrameLayout.LayoutParams(size, size, Gravity.CENTER))
            addView(circle, LayoutParams(size, size).apply {
                if (index > 0) marginStart = -overlap
            })
        }
        contentDescription = labels.joinToString(" ")
    }

    private fun labelSizeSp(): Float = (avatarSizeDp * 0.42f).coerceIn(10f, 16f)

    companion object {
        /** Fundo dos avatares: cinza neutro que acompanha o tema (claro/escuro). */
        @ColorInt
        internal fun avatarFill(): Int {
            val base = DSSColors.textPrimary()
            return ColorUtils.setAlphaComponent(base, 26) // ~10% do texto: cinza no claro e no escuro
        }

        /** Avatar único (linha de integrante): mesmo estilo, com tamanho próprio. */
        internal fun single(context: Context, initial: String, sizeDp: Float): FrameLayout {
            val size = sizeDp.dpToPx(context)
            return FrameLayout(context).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(avatarFill())
                }
                val text = TextView(context).apply {
                    styleText(DSSWeight.BOLD, (sizeDp * 0.36f).coerceIn(10f, 18f), DSSColors.textPrimary())
                    includeFontPadding = false
                    gravity = Gravity.CENTER
                    this.text = initial
                }
                addView(text, FrameLayout.LayoutParams(size, size, Gravity.CENTER))
            }
        }
    }
}
