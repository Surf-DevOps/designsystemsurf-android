package com.surf.surfhubds.components

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import com.surf.surfhubds.font.DSSFont
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.theme.Theme
import com.surf.surfhubds.theme.ThemeAware
import com.surf.surfhubds.theme.ThemeManager
import com.surf.surfhubds.theme.setupThemeObserver
import com.surf.surfhubds.tokens.ColorScheme
import com.surf.surfhubds.util.AppStrings
import com.surf.surfhubds.util.DrawableFactory
import com.surf.surfhubds.util.Utility
import com.surf.surfhubds.util.dpToPx
import com.surf.surfhubds.util.formatPhoneNumber

/** Benefício do plano — um bullet. */
data class PlanBenefit(val text: String)

/** Método de pagamento — espelha `DSSPaymentMethodType` do iOS. */
enum class DSSPaymentMethodType { CREDIT_CARD, PIX }

/** Configuração da [RechargeCompletedCardView]. */
data class PlanInfoCardConfig(
    val msisdn: String,
    val totalDataPlan: String,
    val benefits: List<PlanBenefit>,
    val value: String,
    val lastDigitsCard: String,
    val dateNextRecharge: String,
    val hasScheduledRecharge: Boolean = true,
    val paymentMethod: DSSPaymentMethodType = DSSPaymentMethodType.CREDIT_CARD,
)

/**
 * Port do `RechargeCompletedCardView` do iOS — card de resumo de recarga concluída
 * com 4 blocos: número, plano (com bullets), valor, pagamento.
 *
 * Formatadores ficam externos. A app pode formatar antes de passar via [configure],
 * mas a view aplica:
 *  - número de telefone via `formatPhoneNumber`
 *  - "R$ X,XX" no valor
 */
class RechargeCompletedCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr), ThemeAware {

    private val stack = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }

    private val numberTitleLabel = makeTitleLabel()
    private val numberValueLabel = makeValueLabel(size = 16f)
    private val sep1 = View(context)

    private val planTitleLabel = makeTitleLabel()
    private val totalPlanLabel = TextView(context).apply {
        textSize = 34f
        typeface = DSSFont.bold(context, 34f).typeface
    }

    private val bulletsStack = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
    // iOS reconstrói os bullets dinamicamente a cada configure() — sem limite fixo.
    private val bullets = mutableListOf<BulletRow>()

    private val sep2 = View(context)

    private val valueTitleLabel = makeTitleLabel()
    private val valueValueLabel = makeValueLabel(size = 16f)
    private val sep3 = View(context)

    private val paymentTitleLabel = makeTitleLabel()
    private val paymentMethodLabel = TextView(context).apply {
        textSize = 14f; typeface = DSSFont.regular(context, 14f).typeface
    }
    private val paymentRightLabel = TextView(context).apply {
        textSize = 14f; typeface = DSSFont.regular(context, 14f).typeface
    }
    private val paymentMethodRow = TextView(context).apply {
        textSize = 14f; typeface = DSSFont.regular(context, 14f).typeface
    }
    private val paymentStatusLabel = TextView(context).apply {
        textSize = 14f; typeface = DSSFont.regular(context, 14f).typeface
        gravity = Gravity.END
    }
    private val bottomInfoLabel = TextView(context).apply {
        textSize = 13f; typeface = DSSFont.regular(context, 13f).typeface
    }

    /** Drawable do bullet (check ícone) — opcional, app pode injetar. */
    var bulletIcon: Drawable? = null
        set(value) {
            field = value
            bullets.forEach { it.setIcon(value) }
        }

    init {
        val pad = 20f.dpToPx(context)
        stack.setPadding(pad, pad, pad, pad)

        addView(stack, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        // iOS: UIStackView.spacing = 16 — gap uniforme de 16 entre TODOS os arranged subviews.
        stack.addView(numberTitleLabel, columnLp(0))
        stack.addView(numberValueLabel, columnLp(16f.dpToPx(context)))
        stack.addView(sep1, sepLp())

        stack.addView(planTitleLabel, columnLp(16f.dpToPx(context)))
        stack.addView(totalPlanLabel, columnLp(16f.dpToPx(context)))
        stack.addView(bulletsStack, columnLp(16f.dpToPx(context)))
        stack.addView(sep2, sepLp())

        stack.addView(valueTitleLabel, columnLp(16f.dpToPx(context)))
        stack.addView(valueValueLabel, columnLp(16f.dpToPx(context)))
        stack.addView(sep3, sepLp())

        stack.addView(paymentTitleLabel, columnLp(16f.dpToPx(context)))
        stack.addView(makePaymentRow(), columnLp(16f.dpToPx(context)))
        stack.addView(makeSecondPaymentRow(), columnLp(16f.dpToPx(context)))

        stack.addView(bottomInfoLabel, columnLp(16f.dpToPx(context)))

        // Try to load a default check icon
        bulletIcon = loadCheckCircle()

        refresh()
        setupThemeObserver()
    }

    fun configure(config: PlanInfoCardConfig) {
        numberTitleLabel.text = AppStrings.brand(context, "resume_card_number", "Número")
        numberValueLabel.text = config.msisdn.formatPhoneNumber()

        planTitleLabel.text = AppStrings.brand(context, "pix_info_plan_header", "Plano")
        totalPlanLabel.text = Utility.formatMBToGB(config.totalDataPlan)

        // iOS: remove todos e recria os bullets a partir de config.benefits (sem limite).
        bulletsStack.removeAllViews()
        bullets.clear()
        config.benefits.forEachIndexed { i, b ->
            val bullet = BulletRow(context)
            bullet.setIcon(bulletIcon)
            bullet.setText(b.text)
            bullets.add(bullet)
            bulletsStack.addView(
                bullet,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply { topMargin = if (i == 0) 0 else 8f.dpToPx(context) },
            )
        }

        valueTitleLabel.text = AppStrings.brand(context, "resume_card_price", "Valor")
        valueValueLabel.text = "R$ ${Utility.formatPrice(config.value.toIntOrNull() ?: 0)}"

        paymentTitleLabel.text = AppStrings.brand(context, "recharge_sheet_payment_label", "Pagamento")

        when (config.paymentMethod) {
            DSSPaymentMethodType.CREDIT_CARD -> {
                paymentMethodLabel.text = AppStrings.brand(context, "recharge_completed_credit_card", "Cartão de Crédito")
                paymentRightLabel.text = "Final ${config.lastDigitsCard}"
                paymentRightLabel.visibility = View.VISIBLE
            }
            DSSPaymentMethodType.PIX -> {
                paymentMethodLabel.text = AppStrings.brand(context, "card_plan_pix", "Pix")
                paymentRightLabel.text = null
                paymentRightLabel.visibility = View.GONE
            }
        }

        if (config.hasScheduledRecharge) {
            paymentMethodRow.text = AppStrings.brand(context, "payment_scheduled_recharge_title", "Recarga Programada")
            paymentStatusLabel.text = AppStrings.brand(context, "recharge_completed_active", "Ativa")
            paymentMethodRow.visibility = View.VISIBLE
            paymentStatusLabel.visibility = View.VISIBLE
            bottomInfoLabel.text =
                AppStrings.brand(
                    context,
                    "recharge_completed_next_recharge_format",
                    "Próxima recarga %1\$s",
                    Utility.formatDateToBrazilianFormatPlus30Days(config.dateNextRecharge),
                )
        } else {
            paymentMethodRow.visibility = View.GONE
            paymentStatusLabel.visibility = View.GONE
        }
    }

    override fun applyTheme(theme: Theme) { refresh() }

    private fun refresh() {
        // iOS: light = sombra (sem borda); dark/black = borda 1pt (white / white-alpha-0.4).
        val scheme = ThemeManager.colorScheme
        val borderColor: Int? = when (scheme) {
            ColorScheme.BLACK -> Color.WHITE
            ColorScheme.DARK -> Color.argb((0.4f * 255).toInt(), 255, 255, 255)
            ColorScheme.LIGHT -> null
        }
        background = DrawableFactory.rounded(
            context = context,
            backgroundColor = DSSColors.surface(),
            cornerRadiusDp = 16f,
            strokeColor = borderColor,
            strokeWidthDp = if (borderColor != null) 1f else 0f,
        )
        // iOS light: shadowOffset (0,2), shadowRadius 8, opacity 0.1 — aproximado via elevation.
        elevation = if (scheme == ColorScheme.LIGHT) 8f.dpToPx(context).toFloat() else 0f

        listOf(numberTitleLabel, planTitleLabel, valueTitleLabel, paymentTitleLabel)
            .forEach { it.setTextColor(DSSColors.textPrimary()) }
        numberValueLabel.setTextColor(DSSColors.textPrimary())
        totalPlanLabel.setTextColor(DSSColors.primary())
        valueValueLabel.setTextColor(DSSColors.textPrimary())
        paymentMethodLabel.setTextColor(DSSColors.textPrimary())
        paymentRightLabel.setTextColor(DSSColors.textPrimary())
        paymentMethodRow.setTextColor(DSSColors.textPrimary())
        paymentStatusLabel.setTextColor(DSSColors.success())
        bottomInfoLabel.setTextColor(DSSColors.textPrimary())

        listOf(sep1, sep2, sep3).forEach { it.setBackgroundColor(DSSColors.divider()) }
        bullets.forEach { it.applyThemeColors() }
    }

    private fun makeTitleLabel(): TextView = TextView(context).apply {
        textSize = 14f
        typeface = DSSFont.light(context, 14f).typeface
    }

    private fun makeValueLabel(size: Float): TextView = TextView(context).apply {
        textSize = size
        typeface = DSSFont.regular(context, size).typeface
    }

    private fun makePaymentRow(): View {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        row.addView(
            paymentMethodLabel,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f,
            ),
        )
        row.addView(
            paymentRightLabel,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )
        return row
    }

    private fun makeSecondPaymentRow(): View {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        row.addView(
            paymentMethodRow,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f,
            ),
        )
        row.addView(
            paymentStatusLabel,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )
        return row
    }

    private fun columnLp(top: Int): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = top }

    private fun sepLp(): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            1f.dpToPx(context),
        ).apply { topMargin = 16f.dpToPx(context) }

    private fun loadCheckCircle(): Drawable? {
        val resId = resources.getIdentifier(
            "ic_check_circle", "drawable", context.packageName,
        )
        return if (resId != 0) AppCompatResources.getDrawable(context, resId) else null
    }
}

/**
 * Linha de bullet com ícone à esquerda e label à direita.
 * Usada pelas listas de benefícios do plano em [RechargeCompletedCardView].
 */
private class BulletRow(context: Context) : LinearLayout(context) {
    private val icon = ImageView(context).apply {
        scaleType = ImageView.ScaleType.FIT_CENTER
    }
    private val label = TextView(context).apply {
        textSize = 14f
        typeface = DSSFont.regular(context, 14f).typeface
    }

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        addView(
            icon,
            LayoutParams(14f.dpToPx(context), 14f.dpToPx(context)).apply {
                marginEnd = 8f.dpToPx(context)
            },
        )
        addView(
            label,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT),
        )
        applyThemeColors()
    }

    fun setIcon(d: Drawable?) {
        icon.setImageDrawable(d)
        icon.setColorFilter(if (d == null) Color.TRANSPARENT else DSSColors.success())
    }

    fun setText(text: String) { label.text = text }

    fun applyThemeColors() {
        label.setTextColor(DSSColors.textPrimary())
        icon.setColorFilter(DSSColors.success())
    }
}
