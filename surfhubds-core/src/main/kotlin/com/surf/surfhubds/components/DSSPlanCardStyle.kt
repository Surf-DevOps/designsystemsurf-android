package com.surf.surfhubds.components

/**
 * Densidade do card de plano do [DSSPlanCollectionView].
 *
 * [STANDARD] é o card histórico, usado por todos os apps. [COMPACT] encolhe o card para o
 * desenho do tier Uber Pro, onde a lista mostra mais planos sem rolagem: a tarja de validade
 * fica menor, o "Até" vira um rótulo discreto e as folgas verticais caem quase pela metade.
 *
 * Só o app que pede explicitamente muda de estilo — quem não mexe continua no [STANDARD].
 */
enum class DSSPlanCardStyle(
    /** Padding do bloco do header, em dp. */
    val headerPaddingDp: Float,
    /** Altura mínima do header, em dp — é ela que define a "cara" compacta ou folgada. */
    val headerMinHeightDp: Float,
    val validityTextSizeSp: Float,
    val validityPaddingHorizontalDp: Float,
    val validityPaddingVerticalDp: Float,
    val validityCornerRadiusDp: Float,
    val priceTextSizeSp: Float,
    val planNameTextSizeSp: Float,
    /** Folga entre a tarja de validade e o "Até", em dp. */
    val untilTopMarginDp: Float,
    val untilTextSizeSp: Float,
    /** Folga entre o "Até" e a franquia, em dp. */
    val dataTopMarginDp: Float,
    val dataTextSizeSp: Float,
    /** Lado do losango do tier, em dp. */
    val tierIconSizeDp: Float,
    /**
     * No compacto a tarja de validade usa o próprio tom do tier no texto, como no desenho.
     * No padrão o texto segue a cor de texto do tema, porque a tarja existe em app sem tier.
     */
    val tintsValidityTextWithTier: Boolean,
) {
    STANDARD(
        headerPaddingDp = 16f,
        headerMinHeightDp = 130f,
        validityTextSizeSp = 12f,
        validityPaddingHorizontalDp = 8f,
        validityPaddingVerticalDp = 4f,
        validityCornerRadiusDp = 10f,
        priceTextSizeSp = 20f,
        planNameTextSizeSp = 14f,
        untilTopMarginDp = 8f,
        untilTextSizeSp = 18f,
        dataTopMarginDp = 4f,
        dataTextSizeSp = 22f,
        tierIconSizeDp = 20f,
        tintsValidityTextWithTier = false,
    ),
    COMPACT(
        headerPaddingDp = 12f,
        headerMinHeightDp = 76f,
        validityTextSizeSp = 10f,
        validityPaddingHorizontalDp = 6f,
        validityPaddingVerticalDp = 2f,
        validityCornerRadiusDp = 8f,
        priceTextSizeSp = 17f,
        planNameTextSizeSp = 12f,
        untilTopMarginDp = 6f,
        untilTextSizeSp = 11f,
        dataTopMarginDp = 2f,
        dataTextSizeSp = 19f,
        tierIconSizeDp = 16f,
        tintsValidityTextWithTier = true,
    ),
}
