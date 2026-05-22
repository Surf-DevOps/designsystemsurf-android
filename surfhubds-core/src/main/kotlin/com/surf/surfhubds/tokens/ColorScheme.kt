package com.surf.surfhubds.tokens

import android.content.Context
import android.content.res.Configuration

enum class ColorScheme {
    LIGHT, DARK, BLACK;

    companion object {
        fun fromContext(context: Context): ColorScheme {
            val mode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            return if (mode == Configuration.UI_MODE_NIGHT_YES) DARK else LIGHT
        }
    }
}
