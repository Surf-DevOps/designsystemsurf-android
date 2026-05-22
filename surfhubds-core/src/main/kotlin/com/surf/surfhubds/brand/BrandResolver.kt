package com.surf.surfhubds.brand

import android.content.Context
import android.content.pm.PackageManager

/**
 * Equivalente ao `BrandResolver` do iOS. Detecta a brand a partir de:
 * 1. `<meta-data android:name="BRAND_IDENTIFIER" android:value="uber" />` no AndroidManifest;
 * 2. fallback: padrões no `applicationId`.
 */
object BrandResolver {

    private const val META_KEY = "BRAND_IDENTIFIER"

    fun current(context: Context): Brand {
        return readMetaData(context)?.let(Brand::from) ?: detectFromPackage(context)
    }

    private fun readMetaData(context: Context): String? {
        return try {
            val info = context.packageManager.getApplicationInfo(
                context.packageName,
                PackageManager.GET_META_DATA,
            )
            info.metaData?.get(META_KEY)?.toString()
        } catch (e: Exception) { null }
    }

    private fun detectFromPackage(context: Context): Brand {
        val pkg = context.packageName.lowercase()
        return when {
            "bandsports" in pkg -> Brand.BANDSPORTS
            "ifood" in pkg -> Brand.IFOOD
            "matizconecta" in pkg -> Brand.MATIZCONECTA
            "flachip" in pkg -> Brand.FLACHIP
            "conecta" in pkg -> Brand.CONECTA
            "mega" in pkg -> Brand.MEGA
            "fluxo" in pkg -> Brand.FLUXO
            "pafer" in pkg -> Brand.PAFER
            "paguemenos" in pkg -> Brand.PAGUEMENOS
            "carrefourchip" in pkg -> Brand.CARREFOURCHIP
            "correios" in pkg -> Brand.CORREIOSCELULAR
            "uber" in pkg -> Brand.UBER
            else -> Brand.DEFAULT
        }
    }
}
