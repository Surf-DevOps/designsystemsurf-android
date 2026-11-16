package com.surf.surfhubds.theme

import android.content.Context
import android.graphics.drawable.Drawable

/**
 * Implementado pelos [Theme]s de brand que precisam de um fundo diferente do preenchimento
 * sólido padrão no header da Home (ex.: o "glow" radial da Chip da Saúde, que reproduz o
 * gradiente "Fundo de marca" do guia dela — ver [RadialGradientDrawable]). Brands que não
 * implementam caem no fallback de [DSSColors.homeHeaderBackground] (preenchimento sólido,
 * comportamento idêntico ao que já existia antes desse hook).
 */
interface HomeHeaderBackground {
    fun homeHeaderBackground(context: Context): Drawable

    /**
     * Cor da status bar enquanto esse header está visível (precisa combinar com o topo do
     * [homeHeaderBackground], que pode não ser mais [DSSColors.primary] — ex.: a Chip da
     * Saúde usa o Elétrico do início do glow). Default: [DSSColors.primary], igual a sempre.
     */
    fun homeHeaderStatusBarColor(context: Context): Int = DSSColors.primary()
}
