package com.surf.surfhubds

import android.content.Context
import com.surf.surfhubds.brand.Brand
import com.surf.surfhubds.brand.BrandConfig
import com.surf.surfhubds.brand.BrandResolver
import com.surf.surfhubds.theme.DefaultTheme
import com.surf.surfhubds.theme.Theme
import com.surf.surfhubds.theme.ThemeManager
import com.surf.surfhubds.theme.brands.BandSportsTheme
import com.surf.surfhubds.theme.brands.CarrefourChipTheme
import com.surf.surfhubds.theme.brands.ConectaTheme
import com.surf.surfhubds.theme.brands.CorreiosCelularTheme
import com.surf.surfhubds.theme.brands.FlachipTheme
import com.surf.surfhubds.theme.brands.FluxoTheme
import com.surf.surfhubds.theme.brands.IfoodTheme
import com.surf.surfhubds.theme.brands.MatizConectaTheme
import com.surf.surfhubds.theme.brands.MegaTheme
import com.surf.surfhubds.theme.brands.PaferTheme
import com.surf.surfhubds.theme.brands.PagueMenosTheme
import com.surf.surfhubds.theme.brands.UberTheme
import com.surf.surfhubds.tokens.ColorScheme

/**
 * Ponto único de inicialização do DS. Chame em `Application.onCreate()`:
 *
 * ```
 * SurfHubDS.initialize(this) { brand ->
 *     when (brand) {
 *         Brand.UBER -> UberTheme()
 *         Brand.IFOOD -> IfoodTheme()
 *         // ...
 *         else -> DefaultTheme()
 *     }
 * }
 * ```
 *
 * Resolve a brand via [BrandResolver], carrega o [BrandConfig] do assets,
 * e seta o [Theme] no [ThemeManager].
 */
object SurfHubDS {

    @Volatile
    var brand: Brand = Brand.DEFAULT
        private set

    @Volatile
    lateinit var config: BrandConfig
        private set

    fun initialize(context: Context, themeFor: (Brand) -> Theme = ::defaultThemeFor) {
        val resolved = BrandResolver.current(context)
        brand = resolved
        config = BrandConfig.load(context)
        ThemeManager.setTheme(themeFor(resolved))
        if (resolved == Brand.FLUXO) ThemeManager.setColorScheme(ColorScheme.BLACK)
    }

    /**
     * Mapeamento padrão Brand → Theme, equivalente ao switch do `SurfHubDS.swift`.
     * Use [initialize] sem passar `themeFor` pra obter este comportamento.
     */
    fun defaultThemeFor(brand: Brand): Theme = when (brand) {
        Brand.MATIZCONECTA -> MatizConectaTheme()
        Brand.UBER -> UberTheme()
        Brand.IFOOD -> IfoodTheme()
        Brand.BANDSPORTS -> BandSportsTheme()
        Brand.FLACHIP -> FlachipTheme()
        Brand.CONECTA -> ConectaTheme()
        Brand.MEGA -> MegaTheme()
        Brand.FLUXO -> FluxoTheme()
        Brand.PAFER -> PaferTheme()
        Brand.PAGUEMENOS -> PagueMenosTheme()
        Brand.CARREFOURCHIP -> CarrefourChipTheme()
        Brand.CORREIOSCELULAR -> CorreiosCelularTheme()
        Brand.DEFAULT -> DefaultTheme()
    }
}
