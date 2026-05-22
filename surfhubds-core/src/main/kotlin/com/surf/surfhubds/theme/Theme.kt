package com.surf.surfhubds.theme

import com.surf.surfhubds.tokens.ColorScheme
import com.surf.surfhubds.tokens.ComponentStyles
import com.surf.surfhubds.tokens.DesignTokens

/**
 * Equivalente ao `ThemeProtocol` do iOS — toda brand exporta um Theme com tokens e componentStyles.
 */
interface Theme {
    val tokens: DesignTokens
    val components: ComponentStyles
    val colorScheme: ColorScheme
}
