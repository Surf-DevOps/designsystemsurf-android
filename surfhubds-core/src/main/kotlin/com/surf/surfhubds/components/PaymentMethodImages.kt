package com.surf.surfhubds.components

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat

/**
 * Port da extensão Swift `UIImage+PaymentMethods`.
 *
 * No iOS, alguns ícones vinham de `ImageLoader` (asset por brand) e outros do
 * `SF Symbols` (`creditcard.fill`). No Android, retornamos um [Drawable]
 * carregado de resources do app host (cada brand pode incluir
 * `pix`, `card_visa`, `card_mastercard`, `card_amex` em `res/drawable`); caso
 * o resource não exista, devolvemos `null` — quem consome pode usar um
 * fallback de `android.R.drawable.ic_menu_*`.
 */
object PaymentMethodImages {

    /** Ícone do Pix. Carrega `R.drawable.pix` se existir. */
    fun pixIcon(context: Context): Drawable? = drawableByName(context, "pix")

    /** Ícone genérico de cartão. */
    fun creditCardIcon(context: Context): Drawable? =
        drawableByName(context, "ic_credit_card")
            ?: ContextCompat.getDrawable(context, android.R.drawable.ic_menu_edit)

    /** Ícone preenchido de cartão. */
    fun creditCardFilledIcon(context: Context): Drawable? =
        drawableByName(context, "ic_credit_card_filled") ?: creditCardIcon(context)

    /** Ícone para adicionar cartão. */
    fun addCardIcon(context: Context): Drawable? =
        drawableByName(context, "ic_add_card") ?: creditCardIcon(context)

    // ---- Brand-specific ----

    fun visaIcon(context: Context): Drawable? =
        drawableByName(context, "card_visa") ?: creditCardFilledIcon(context)

    fun mastercardIcon(context: Context): Drawable? =
        drawableByName(context, "card_mastercard") ?: creditCardFilledIcon(context)

    fun amexIcon(context: Context): Drawable? =
        drawableByName(context, "card_amex") ?: creditCardFilledIcon(context)

    fun eloIcon(context: Context): Drawable? =
        drawableByName(context, "card_elo") ?: creditCardFilledIcon(context)

    fun hipercardIcon(context: Context): Drawable? =
        drawableByName(context, "card_hipercard") ?: creditCardFilledIcon(context)

    fun boletoIcon(context: Context): Drawable? =
        drawableByName(context, "boleto") ?: creditCardFilledIcon(context)

    /**
     * Devolve o ícone para a bandeira (case-insensitive). Aceita "visa",
     * "mastercard"/"master", "amex"/"american express", "elo", "hipercard",
     * "pix", "boleto". Default: cartão genérico.
     */
    fun cardBrandIcon(context: Context, brand: String): Drawable? {
        return when (brand.lowercase().trim()) {
            "visa" -> visaIcon(context)
            "mastercard", "master" -> mastercardIcon(context)
            "amex", "american express" -> amexIcon(context)
            "elo" -> eloIcon(context)
            "hipercard" -> hipercardIcon(context)
            "pix" -> pixIcon(context)
            "boleto" -> boletoIcon(context)
            else -> creditCardFilledIcon(context)
        }
    }

    /**
     * Resolve `R.drawable.<name>` por reflection do `Resources` do app host.
     */
    private fun drawableByName(context: Context, name: String): Drawable? {
        val resId = context.resources.getIdentifier(name, "drawable", context.packageName)
        if (resId == 0) return null
        return ContextCompat.getDrawable(context, resId)
    }
}
