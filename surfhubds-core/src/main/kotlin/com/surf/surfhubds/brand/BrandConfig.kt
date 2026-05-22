package com.surf.surfhubds.brand

import android.content.Context
import org.json.JSONObject

/**
 * Equivalente ao `BrandConfig.plist` do iOS.
 *
 * Cada brand module embarca um `assets/brand_config.json` com chaves arbitrárias
 * (apiBaseURL, enableChat, primaryColorHex etc). Loaders abaixo expõem leitura tipada.
 */
class BrandConfig private constructor(private val data: JSONObject) {

    fun getString(key: String): String? = data.optString(key, null)
    fun getBoolean(key: String, default: Boolean = false): Boolean = data.optBoolean(key, default)
    fun getInt(key: String, default: Int = 0): Int = data.optInt(key, default)
    fun getDouble(key: String, default: Double = 0.0): Double = data.optDouble(key, default)

    fun toJson(): String = data.toString()

    companion object {
        private const val ASSET_PATH = "brand_config.json"

        fun load(context: Context): BrandConfig {
            return try {
                val json = context.assets.open(ASSET_PATH).bufferedReader().use { it.readText() }
                BrandConfig(JSONObject(json))
            } catch (e: Exception) {
                BrandConfig(JSONObject())
            }
        }
    }
}
