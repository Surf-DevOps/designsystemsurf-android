package com.surf.surfhubds.components

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Port do `FAQLoader.swift`.
 *
 * No iOS o loader buscava por brand em vários módulos `SurfHubBrand*` que expõem um enum
 * `FAQList` com `hasCategories` + `categories`. No Android multi-brand, fica mais
 * pragmático carregar de um JSON embutido em `assets/`. Cada brand inclui um arquivo
 * (ex. `faq_uber.json`) e a aplicação resolve o nome correto a partir de
 * `BrandResolver`.
 *
 * Formato esperado do JSON:
 * ```json
 * {
 *   "hasCategories": true,
 *   "categories": [
 *     {
 *       "title": "Geral",
 *       "items": [
 *         { "question": "...", "answer": "..." }
 *       ]
 *     }
 *   ]
 * }
 * ```
 */
data class DSSFAQItem(
    val question: String,
    val answer: String,
)

data class DSSFAQCategory(
    val title: String,
    val items: List<DSSFAQItem>,
)

data class DSSFAQ(
    val hasCategories: Boolean,
    val categories: List<DSSFAQCategory>,
) {
    /** Retorna um dicionário título → lista de pares (question, answer). */
    fun asDictionary(): Map<String, List<Pair<String, String>>> =
        categories.associate { cat -> cat.title to cat.items.map { it.question to it.answer } }
}

object FAQLoader {

    /**
     * Carrega o FAQ de um arquivo JSON em `assets/`.
     *
     * @param context contexto para resolver `assets`.
     * @param filename nome do arquivo dentro de `assets/` (ex.: "faq_uber.json").
     * @return [DSSFAQ] vazio se o arquivo não existir ou falhar o parse.
     */
    @JvmStatic
    fun loadFromAssets(context: Context, filename: String): DSSFAQ {
        return try {
            val json = context.assets.open(filename).bufferedReader().use { it.readText() }
            parse(json)
        } catch (_: Throwable) {
            DSSFAQ(hasCategories = false, categories = emptyList())
        }
    }

    /** Faz parse a partir de uma string JSON. Util para testes ou fontes alternativas. */
    @JvmStatic
    fun parse(json: String): DSSFAQ {
        return try {
            val root = JSONObject(json)
            val hasCategories = root.optBoolean("hasCategories", true)
            val catsJson = root.optJSONArray("categories") ?: JSONArray()
            val categories = (0 until catsJson.length()).map { i ->
                val cat = catsJson.getJSONObject(i)
                val title = cat.optString("title", "")
                val itemsJson = cat.optJSONArray("items") ?: JSONArray()
                val items = (0 until itemsJson.length()).map { j ->
                    val item = itemsJson.getJSONObject(j)
                    DSSFAQItem(
                        question = item.optString("question", ""),
                        answer = item.optString("answer", ""),
                    )
                }
                DSSFAQCategory(title = title, items = items)
            }
            DSSFAQ(hasCategories = hasCategories, categories = categories)
        } catch (_: Throwable) {
            DSSFAQ(hasCategories = false, categories = emptyList())
        }
    }
}
