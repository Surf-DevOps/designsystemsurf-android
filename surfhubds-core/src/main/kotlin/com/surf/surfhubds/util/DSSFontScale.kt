package com.surf.surfhubds.util

import android.content.Context
import android.content.res.Configuration

/**
 * Teto para o tamanho de fonte do sistema (Configurações > Tamanho da fonte).
 *
 * Acima de ~1.3 vários layouts quebram (texto sobrepondo, componentes deformados). Em vez de
 * ignorar a preferência do usuário (acessibilidade), o app respeita a escala até [MAX] e
 * trava nela dali para cima. No Android 14+ a escala não linear parte do `fontScale` da
 * config, então o teto vale lá também.
 *
 * Uso — em toda Activity do app:
 * ```
 * override fun attachBaseContext(newBase: Context) {
 *     super.attachBaseContext(DSSFontScale.wrap(newBase))
 * }
 * ```
 * Views infladas com `applicationContext` NÃO recebem o teto: inflar sempre com o contexto
 * da Activity. Se a Activity declarar `fontScale` em `configChanges`, ela não é recriada ao
 * trocar a fonte e o teto deixa de ser reaplicado — não declarar.
 */
object DSSFontScale {

    const val MAX = 1.3f

    fun wrap(base: Context, max: Float = MAX): Context {
        if (base.resources.configuration.fontScale <= max) return base
        // Override só do fontScale: uma cópia da config inteira congelaria uiMode/locale etc.,
        // e Activities com `uiMode` em configChanges deixariam de acompanhar o tema do sistema.
        val override = Configuration().apply { fontScale = max }
        return base.createConfigurationContext(override)
    }
}
