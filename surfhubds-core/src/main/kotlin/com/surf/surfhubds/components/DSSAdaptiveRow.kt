package com.surf.surfhubds.components

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout

/**
 * Linha que vira coluna quando os filhos não caberiam lado a lado.
 *
 * Vários blocos do DS eram linhas com largura/altura fixas em dp (ou dois filhos
 * posicionados por gravity dentro de um `FrameLayout`). Com a fonte do sistema ampliada o
 * conteúdo era cortado ou se sobrepunha. Aqui a quebra é decidida pela MEDIDA real dos
 * filhos, não por um limiar de `fontScale`, então o comportamento é contínuo e correto em
 * qualquer escala — inclusive nas de acessibilidade.
 *
 * Na escala 1.0 o resultado é idêntico ao da linha original: como tudo cabe, nada quebra.
 */
class DSSAdaptiveRow @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    /** Espaço vertical entre os filhos quando a linha quebra em coluna. */
    var stackedGap: Int = 0

    private var stacked = false
    private var measuredOnce = false

    /** Largura e peso declarados de cada filho no modo linha, para restaurar ao voltar. */
    private val rowWidths = HashMap<View, Int>()
    private val rowWeights = HashMap<View, Float>()

    init {
        orientation = HORIZONTAL
    }

    /**
     * Espaçador flexível: empurra os filhos para as pontas no modo linha e desaparece no
     * modo coluna (onde cada filho já ocupa a largura inteira).
     *
     * @param minWidth folga MÍNIMA entre os filhos. Sem ela, quando os filhos couberem por
     *   pouco o espaçador colapsa a zero e os textos ficam encostados um no outro. Essa
     *   folga entra na conta de caber, então a linha quebra antes de os textos se tocarem.
     */
    @JvmOverloads
    fun addSpacer(minWidth: Int = 0) {
        addView(View(context).apply { tag = SPACER_TAG }, LayoutParams(minWidth, 1, 1f))
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.UNSPECIFIED) {
            val available = MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight
            applyStacked(naturalRowWidth(heightMeasureSpec) > available)
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    /**
     * Soma da largura natural dos filhos de conteúdo. O espaçador não conta: ele existe só
     * para distribuir a folga, e é justamente a folga que estamos testando.
     */
    private fun naturalRowWidth(heightMeasureSpec: Int): Int {
        var total = 0
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val lp = child.layoutParams as LayoutParams
            if (child.tag == SPACER_TAG) {
                total += lp.width  // folga mínima exigida
                continue
            }
            if (child.visibility == GONE) continue
            // Largura exata declarada manda. Medir com UNSPECIFIED um filho de tamanho fixo
            // devolve o tamanho INTRÍNSECO do conteúdo — para um ImageView isso é o tamanho do
            // drawable, muito maior que os poucos dp que ele realmente ocupa, e a linha
            // quebrava mesmo com espaço sobrando.
            val width = if (lp.width > 0) {
                lp.width
            } else {
                child.measure(
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                    MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(heightMeasureSpec), MeasureSpec.UNSPECIFIED),
                )
                child.measuredWidth
            }
            total += width + lp.leftMargin + lp.rightMargin
        }
        return total
    }

    private fun applyStacked(value: Boolean) {
        if (measuredOnce && stacked == value) return
        stacked = value
        measuredOnce = true

        var firstContent = true
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val lp = child.layoutParams as LayoutParams
            if (child.tag == SPACER_TAG) {
                // Sem setVisibility(): mexer no campo evita um requestLayout durante o measure.
                child.visibility = if (value) GONE else VISIBLE
                continue
            }
            rowWidths.getOrPut(child) { lp.width }
            rowWeights.getOrPut(child) { lp.weight }
            // Mutação direta dos campos do LayoutParams (em vez de setLayoutParams) para não
            // disparar requestLayout no meio do measure.
            if (value) {
                // Filho de largura FIXA (ícone, chevron) mantém a largura: esticar para
                // MATCH_PARENT fazia o ImageView com FIT_CENTER centralizar o desenho.
                lp.width = if ((rowWidths[child] ?: 0) > 0) rowWidths[child]!! else LayoutParams.MATCH_PARENT
                // Em coluna o peso passaria a distribuir ALTURA e esticaria os filhos.
                lp.weight = 0f
                lp.topMargin = if (firstContent) 0 else stackedGap
            } else {
                lp.width = rowWidths[child] ?: LayoutParams.WRAP_CONTENT
                lp.weight = rowWeights[child] ?: 0f
                lp.topMargin = 0
            }
            firstContent = false
        }
        // Só aqui a orientação muda, e apenas quando de fato mudou.
        orientation = if (value) VERTICAL else HORIZONTAL
    }

    private companion object {
        const val SPACER_TAG = "dss_adaptive_row_spacer"
    }
}
