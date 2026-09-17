package com.surf.surfhubds.util

import android.content.Context
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.surf.surfhubds.brand.Brand
import com.surf.surfhubds.brand.BrandResolver
import java.io.IOException

/**
 * Port do `ImageLoader` do iOS (SurfHubDS/BrandsConfig/UIHelpers.swift).
 *
 * Resolve uma imagem por nome:
 * 1. tenta `assets/images/<name>.<ext>` (convenção dos módulos `surfhubds-brand-*`);
 * 2. tenta `assets/<name>.<ext>` (assets no root, ex. carouselImage1.png);
 * 3. cai para `R.drawable.<brand>_<name>` se a brand não for DEFAULT;
 * 4. cai para `R.drawable.<name>`.
 */
object ImageLoader {

    private val ASSET_EXTENSIONS = listOf("png", "webp", "jpg", "jpeg")
    private val ASSET_PREFIXES = listOf("images/", "")

    fun image(
        context: Context,
        named: String,
        brand: Brand = BrandResolver.current(context),
    ): Drawable? {
        for (prefix in ASSET_PREFIXES) {
            for (ext in ASSET_EXTENSIONS) {
                val path = "$prefix$named.$ext"
                try {
                    context.assets.open(path).use { stream ->
                        Drawable.createFromStream(stream, path)?.let { return it }
                    }
                } catch (_: IOException) {
                    // asset não existe nessa combinação — segue tentando
                }
            }
        }
        val res = imageRes(context, named, brand)
        return if (res != 0) ContextCompat.getDrawable(themedContext(context), res) else null
    }

    /**
     * Contexto cujo `uiMode` reflete o tema do APP (o que o `AppTheme` definiu via
     * [AppCompatDelegate]) e nao o tema do APARELHO.
     *
     * `AppCompatDelegate.setDefaultNightMode` so reescreve a configuracao de contextos
     * de Activity. Um `@ApplicationContext` — o que os ViewModels recebem via Hilt —
     * mantem o `uiMode` do sistema, entao resolver um drawable por ali escolhe a pasta
     * `-night` pelo modo do APARELHO enquanto os tokens de cor do DS seguem o tema do
     * APP. Num aparelho no escuro com o app no claro isso servia o PNG branco sobre o
     * fundo branco (era o caso do `carouselimage1` da brand Uber).
     */
    private fun themedContext(context: Context): Context {
        val nightFlag = when (AppCompatDelegate.getDefaultNightMode()) {
            AppCompatDelegate.MODE_NIGHT_YES -> Configuration.UI_MODE_NIGHT_YES
            AppCompatDelegate.MODE_NIGHT_NO -> Configuration.UI_MODE_NIGHT_NO
            // FOLLOW_SYSTEM / UNSPECIFIED: o modo do aparelho ja e o modo do app.
            else -> return context
        }
        val current = context.resources.configuration
        if ((current.uiMode and Configuration.UI_MODE_NIGHT_MASK) == nightFlag) return context
        val config = Configuration(current).apply {
            uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or nightFlag
        }
        return context.createConfigurationContext(config)
    }

    @DrawableRes
    fun imageRes(
        context: Context,
        named: String,
        brand: Brand = BrandResolver.current(context),
    ): Int {
        val pkg = context.packageName
        val normalized = named.lowercase()
        if (brand != Brand.DEFAULT) {
            val branded = context.resources.getIdentifier(
                "${brand.raw}_$normalized", "drawable", pkg,
            )
            if (branded != 0) return branded
        }
        return context.resources.getIdentifier(normalized, "drawable", pkg)
    }
}
