package com.surf.surfhubds.util

import android.content.Context
import android.text.TextUtils
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import com.surf.surfhubds.font.DSSFont

/** Peso de fonte usado pelos builders internos (mapeia para [DSSFont]). */
internal enum class DSSWeight { REGULAR, MEDIUM, SEMIBOLD, BOLD }

/** TextView já com fonte, tamanho e cor do DS. Usado pelos componentes programáticos. */
internal fun dssText(
    context: Context,
    weight: DSSWeight,
    sizeSp: Float,
    @ColorInt color: Int,
    maxLines: Int = Int.MAX_VALUE,
): TextView = TextView(context).apply {
    styleText(weight, sizeSp, color)
    if (maxLines != Int.MAX_VALUE) {
        this.maxLines = maxLines
        ellipsize = TextUtils.TruncateAt.END
    }
    includeFontPadding = false
}

internal fun TextView.styleText(weight: DSSWeight, sizeSp: Float, @ColorInt color: Int) {
    typeface = when (weight) {
        DSSWeight.REGULAR -> DSSFont.regular(context, sizeSp)
        DSSWeight.MEDIUM -> DSSFont.medium(context, sizeSp)
        DSSWeight.SEMIBOLD -> DSSFont.semibold(context, sizeSp)
        DSSWeight.BOLD -> DSSFont.bold(context, sizeSp)
    }.typeface
    textSize = sizeSp
    setTextColor(color)
}

internal fun wrapParams(): LinearLayout.LayoutParams =
    LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)

internal fun matchWidthParams(): LinearLayout.LayoutParams =
    LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

/** Ocupa o espaço que sobrar na linha (peso 1, largura 0). */
internal fun weightParams(): LinearLayout.LayoutParams =
    LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)

/** Inicial maiúscula do nome para avatares ("ana souza" → "A"); vazio vira "?". */
fun String.dssInitial(): String =
    trim().firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "?"
