package com.surf.surfhubds.components

import androidx.annotation.DrawableRes
import com.surf.surfhubds.R

/**
 * Tier do programa Uber Pro do motorista.
 *
 * Só muda a APARÊNCIA dos cards de plano ([DSSPlanCollectionView]): a tarja de validade
 * ganha o tom do tier e o valor de dados ganha o losango. O bônus de internet em si já
 * vem aplicado no catálogo pelo backend — o app não recalcula nada.
 *
 * Fora do app da Uber o tier é sempre `null` e os cards ficam com o visual padrão.
 */
enum class DSSPlanTier(
    /** Cor base do tier; a tarja de validade usa este tom com alpha. */
    val accentColor: Int,
    @DrawableRes val iconRes: Int,
) {
    OURO(0xFFE9A427.toInt(), R.drawable.dss_ic_tier_ouro),
    PLATINA(0xFF93A6B4.toInt(), R.drawable.dss_ic_tier_platina),
    DIAMANTE(0xFF111111.toInt(), R.drawable.dss_ic_tier_diamante),
    AZUL(0xFF1F5FD8.toInt(), R.drawable.dss_ic_tier_azul);

    companion object {
        /**
         * Converte o campo `tier` da API (`"Ouro"`, `"Platina"`, `"Diamante"`, `"Azul"`).
         *
         * Tolerante a caixa, espaços e acento (`"platina"`, `"PLATINA"`, `"Platina "`), e
         * devolve `null` para qualquer valor desconhecido ou vazio — nesse caso o card fica
         * com o visual padrão em vez de quebrar.
         */
        @JvmStatic
        fun from(raw: String?): DSSPlanTier? {
            val normalized = raw?.trim()?.uppercase()?.replace("Â", "A") ?: return null
            return entries.firstOrNull { it.name == normalized }
        }
    }
}
