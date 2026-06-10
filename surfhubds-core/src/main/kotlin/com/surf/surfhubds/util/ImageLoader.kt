package com.surf.surfhubds.util

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
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
        return if (res != 0) ContextCompat.getDrawable(context, res) else null
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
