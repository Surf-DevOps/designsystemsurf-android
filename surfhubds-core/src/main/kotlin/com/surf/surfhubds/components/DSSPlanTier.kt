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
         * Converte o campo `tier` de `spec-mobile/v1/tier/uber/...`.
         *
         * O endpoint fala três vocabulários para a mesma coisa, e todos são aceitos:
         *  - código cru: `"TIER_1"`..`"TIER_4"`
         *  - `display_name` em inglês: `"Blue"`, `"Gold"`, `"Platinum"`, `"Diamond"`
         *  - português, como este enum nomeia: `"Azul"`, `"Ouro"`, `"Platina"`, `"Diamante"`
         *
         * Antes só o português era reconhecido, e como o endpoint responde em inglês na
         * maior parte dos ambientes, a recarga ficava com o visual padrão mesmo com tier
         * válido. Os três seguem aceitos porque nada garante que todos os ambientes
         * respondam igual.
         *
         * Tolerante a caixa, espaços e acento (`"platina"`, `"PLATINA"`, `"Platina "`), e
         * devolve `null` para qualquer valor desconhecido ou vazio — nesse caso o card fica
         * com o visual padrão em vez de quebrar.
         */
        @JvmStatic
        fun from(raw: String?): DSSPlanTier? =
            when (raw?.trim()?.uppercase()?.replace("Â", "A")) {
                "TIER_1", "BLUE", "AZUL" -> AZUL
                "TIER_2", "GOLD", "OURO" -> OURO
                "TIER_3", "PLATINUM", "PLATINA" -> PLATINA
                "TIER_4", "DIAMOND", "DIAMANTE" -> DIAMANTE
                else -> null
            }
    }
}
