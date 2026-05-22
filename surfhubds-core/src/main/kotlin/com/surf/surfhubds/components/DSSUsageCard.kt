package com.surf.surfhubds.components

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.graphics.drawable.DrawableCompat
import com.surf.surfhubds.font.DSSFont
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.theme.Theme
import com.surf.surfhubds.theme.ThemeAware
import com.surf.surfhubds.theme.setupThemeObserver
import com.surf.surfhubds.util.DrawableFactory
import com.surf.surfhubds.util.dpToPx
import java.util.Locale

/**
 * Port do `DSSUsageCard.swift` (iOS) — card de consumo (Internet / Ligações / SMS) com
 * label de disponível/total, percentual usado, progressbar colorida e validade.
 */
class DSSUsageCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr), ThemeAware {

    enum class Type { INTERNET, CALLS, SMS }

    data class Configuration(
        val type: Type,
        val available: Int,
        val total: Int,
        val validUntil: String,
    )

    /** Drawables opcionais para os ícones (Internet/Calls/SMS). */
    var internetIcon: Drawable? = null
    var callsIcon: Drawable? = null
    var smsIcon: Drawable? = null

    private val container = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
    private val headerRow = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
    private val iconView = ImageView(context).apply { scaleType = ImageView.ScaleType.FIT_CENTER }
    private val titleLabel = TextView(context).apply { textSize = 16f; typeface = DSSFont.light(context, 16f).typeface }
    private val divider = View(context)
    private val infoRow = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
    private val availableLabel = TextView(context).apply { textSize = 16f; typeface = DSSFont.light(context, 16f).typeface }
    private val totalLabel = TextView(context).apply { textSize = 14f; typeface = DSSFont.light(context, 14f).typeface }
    private val usedLabel = TextView(context).apply { textSize = 14f; typeface = DSSFont.light(context, 14f).typeface; gravity = Gravity.END }
    private val progressBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply { max = 100 }
    private val validUntilLabel = TextView(context).apply { textSize = 13f; typeface = DSSFont.light(context, 13f).typeface }

    init {
        val hPad = 16f.dpToPx(context); val vPad = 16f.dpToPx(context)
        container.setPadding(hPad, vPad, hPad, vPad)

        headerRow.addView(iconView, LinearLayout.LayoutParams(20f.dpToPx(context), 20f.dpToPx(context)))
        headerRow.addView(
            titleLabel,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                .apply { leftMargin = 8f.dpToPx(context) },
        )
        container.addView(headerRow)

        container.addView(
            divider,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                1f.dpToPx(context),
            ).apply { topMargin = 12f.dpToPx(context) },
        )

        // Available + total on left, used on right
        val leftCol = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        leftCol.addView(availableLabel)
        leftCol.addView(totalLabel)
        infoRow.addView(leftCol, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        infoRow.addView(usedLabel, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        container.addView(
            infoRow,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = 12f.dpToPx(context) },
        )

        container.addView(
            progressBar,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                8f.dpToPx(context),
            ).apply { topMargin = 12f.dpToPx(context) },
        )
        container.addView(
            validUntilLabel,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = 12f.dpToPx(context) },
        )

        addView(container, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        refresh()
        setupThemeObserver()
    }

    fun configure(config: Configuration) {
        when (config.type) {
            Type.INTERNET -> {
                iconView.setImageDrawable(internetIcon)
                titleLabel.text = "Internet base do plano"
                availableLabel.text = "${formatMBToGB(config.available)}GB disponíveis"
                totalLabel.text = "de ${formatMBToGB(config.total)}GB"
                validUntilLabel.text = "Válido até: ${config.validUntil} 00:01"
                val used = (config.total - config.available).coerceAtLeast(0)
                val pct = percent(used, config.total)
                usedLabel.text = "$pct% utilizados"
                applyProgress(pct)
            }
            Type.CALLS -> {
                iconView.setImageDrawable(callsIcon)
                titleLabel.text = "Ligações"
                val usedMinutes = (config.total - config.available).coerceAtLeast(0)
                val totalSecondsUsed = usedMinutes * 60
                val h = totalSecondsUsed / 3600
                val m = (totalSecondsUsed % 3600) / 60
                val s = totalSecondsUsed % 60
                usedLabel.text = String.format(Locale.US, "%02d:%02d:%02d utilizados", h, m, s)
                val formattedTotal = if (config.total >= 1000) "Ilimitado" else "${config.total} min"
                val formattedAvailable = if (config.total >= 1000) "Ilimitado" else "${config.available} min"
                availableLabel.text = "$formattedAvailable disponíveis"
                totalLabel.text = "de $formattedTotal"
                validUntilLabel.text = "Válido até: ${config.validUntil} 00:01"
                if (config.total >= 1000) {
                    progressBar.progress = 0
                } else {
                    val pct = percent(usedMinutes, config.total)
                    applyProgress(pct)
                }
            }
            Type.SMS -> {
                iconView.setImageDrawable(smsIcon)
                titleLabel.text = "SMS"
                val used = (config.total - config.available).coerceAtLeast(0)
                val pct = percent(used, config.total)
                availableLabel.text = "${config.available} disponíveis"
                totalLabel.text = "de ${config.total}SMS"
                usedLabel.text = "$pct% utilizados"
                validUntilLabel.text = "Válido até: ${config.validUntil} 00:01"
                applyProgress(pct)
            }
        }
    }

    private fun percent(used: Int, total: Int): Int {
        if (total <= 0) return 0
        return ((used.toDouble() / total.toDouble()) * 100).toInt()
    }

    private fun applyProgress(percent: Int) {
        val clamped = percent.coerceIn(0, 100)
        progressBar.progress = clamped
        val color: Int = when {
            clamped < 50 -> Color.parseColor("#34C759")
            clamped < 70 -> Color.parseColor("#FFCC00")
            else -> DSSColors.error()
        }
        val drawable = progressBar.progressDrawable
        if (drawable != null) {
            val wrapped = DrawableCompat.wrap(drawable.mutate())
            DrawableCompat.setTint(wrapped, color)
            progressBar.progressDrawable = wrapped
        }
    }

    private fun formatMBToGB(mb: Int): String {
        val gb = mb / 1024.0
        return String.format(Locale.US, "%.1f", gb)
    }

    override fun applyTheme(theme: Theme) { refresh() }

    private fun refresh() {
        background = DrawableFactory.rounded(
            context = context,
            backgroundColor = DSSColors.surface(),
            cornerRadiusDp = 12f,
            strokeColor = DSSColors.borderDefault(),
            strokeWidthDp = 1f,
        )
        iconView.setColorFilter(DSSColors.textPrimary(), PorterDuff.Mode.SRC_IN)
        titleLabel.setTextColor(DSSColors.textPrimary())
        availableLabel.setTextColor(DSSColors.textPrimary())
        totalLabel.setTextColor(DSSColors.textSecondary())
        usedLabel.setTextColor(DSSColors.primary())
        validUntilLabel.setTextColor(DSSColors.textSecondary())
        divider.setBackgroundColor(DSSColors.divider())
    }
}
