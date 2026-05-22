package com.surf.surfhubds.font

import android.content.Context
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.surf.surfhubds.tokens.FontSpec

/**
 * Equivalente ao `DSSFont` do iOS.
 *
 * Brand registra a família via [setFontFamily] passando um [FontFamily] com refs a `R.font.*`.
 * Em runtime, [thin] / [light] / [regular] / [medium] / [bold] / [black] retornam um [FontSpec].
 *
 * Padrão sem registro: usa `Typeface.SANS_SERIF` com `Typeface.NORMAL/BOLD`.
 */
object DSSFont {

    sealed interface FontFamily {
        object SystemDefault : FontFamily
        data class Custom(
            val thin: Int? = null,
            val light: Int? = null,
            val regular: Int? = null,
            val medium: Int? = null,
            val bold: Int? = null,
            val black: Int? = null,
        ) : FontFamily
    }

    @Volatile
    private var currentFamily: FontFamily = FontFamily.SystemDefault

    @Volatile
    private var cachedTypefaces: MutableMap<Int, Typeface?> = mutableMapOf()

    fun setFontFamily(family: FontFamily) {
        currentFamily = family
        cachedTypefaces = mutableMapOf()
    }

    fun thin(context: Context, sizeSp: Float): FontSpec =
        FontSpec(loadOrSystem(context, (currentFamily as? FontFamily.Custom)?.thin, Typeface.NORMAL), sizeSp)

    fun light(context: Context, sizeSp: Float): FontSpec =
        FontSpec(loadOrSystem(context, (currentFamily as? FontFamily.Custom)?.light, Typeface.NORMAL), sizeSp)

    fun regular(context: Context, sizeSp: Float): FontSpec =
        FontSpec(loadOrSystem(context, (currentFamily as? FontFamily.Custom)?.regular, Typeface.NORMAL), sizeSp)

    fun medium(context: Context, sizeSp: Float): FontSpec =
        FontSpec(loadOrSystem(context, (currentFamily as? FontFamily.Custom)?.medium, Typeface.NORMAL), sizeSp)

    fun bold(context: Context, sizeSp: Float): FontSpec =
        FontSpec(loadOrSystem(context, (currentFamily as? FontFamily.Custom)?.bold, Typeface.BOLD), sizeSp)

    fun black(context: Context, sizeSp: Float): FontSpec =
        FontSpec(loadOrSystem(context, (currentFamily as? FontFamily.Custom)?.black, Typeface.BOLD), sizeSp)

    private fun loadOrSystem(context: Context, resId: Int?, fallback: Int): Typeface {
        if (resId == null) return Typeface.create(Typeface.SANS_SERIF, fallback)
        return cachedTypefaces.getOrPut(resId) {
            try { ResourcesCompat.getFont(context.applicationContext, resId) } catch (e: Exception) { null }
        } ?: Typeface.create(Typeface.SANS_SERIF, fallback)
    }
}
