package com.surf.surfhubds.components

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.surf.surfhubds.R
import com.surf.surfhubds.util.ImageLoader

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

    /**
     * Ícone do Pix.
     *
     * Fiel ao iOS (`ImageLoader.image(named: "pix", brand: BrandResolver.current())`):
     * resolve o asset/recurso `pix` da brand atual via [ImageLoader], em vez de
     * apenas `R.drawable.pix`. Assim respeita a resolução por brand (assets em
     * `images/` e drawables com prefixo da brand).
     */
    fun pixIcon(context: Context): Drawable? = ImageLoader.image(context, "pix")

    /**
     * Ícone genérico de cartão.
     *
     * Espelha o SF Symbol "creditcard" do iOS — o fallback é o vetor [R.drawable.dss_creditcard]
     * do próprio DSS (antes caía no `ic_menu_edit` do sistema, um lápis).
     */
    fun creditCardIcon(context: Context): Drawable? =
        drawableByName(context, "ic_credit_card")
            ?: ContextCompat.getDrawable(context, R.drawable.dss_creditcard)

    /** Ícone preenchido de cartão (SF "creditcard.fill"). */
    fun creditCardFilledIcon(context: Context): Drawable? =
        drawableByName(context, "ic_credit_card_filled")
            ?: ContextCompat.getDrawable(context, R.drawable.dss_creditcard_fill)

    /** Ícone para adicionar cartão (iOS usa o SF "creditcard", contorno). */
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
     * Devolve o ícone para a bandeira (case-insensitive).
     *
     * Fiel ao iOS (`cardBrandIcon(for:)`): só faz `switch` sobre o valor em
     * minúsculas, tratando "visa", "mastercard"/"master" e
     * "amex"/"american express"; qualquer outro valor (incluindo "elo",
     * "hipercard", "pix", "boleto" e vazio) cai no [creditCardFilledIcon].
     * As funções [eloIcon]/[hipercardIcon]/[boletoIcon] continuam públicas para
     * uso direto, mas não são acionadas por este resolvedor (espelha o iOS).
     */
    fun cardBrandIcon(context: Context, brand: String): Drawable? {
        return when (brand.lowercase()) {
            "visa" -> visaIcon(context)
            "mastercard", "master" -> mastercardIcon(context)
            "amex", "american express" -> amexIcon(context)
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
