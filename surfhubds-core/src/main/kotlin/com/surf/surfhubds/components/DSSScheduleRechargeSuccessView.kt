package com.surf.surfhubds.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Base64
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import com.bumptech.glide.Glide
import com.surf.surfhubds.font.DSSFont
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.theme.Theme
import com.surf.surfhubds.theme.ThemeAware
import com.surf.surfhubds.theme.ThemeManager
import com.surf.surfhubds.theme.setupThemeObserver
import com.surf.surfhubds.tokens.ColorScheme
import com.surf.surfhubds.util.ImageLoader
import com.surf.surfhubds.util.dpToPx

/**
 * Configuração da [DSSScheduleRechargeSuccessView].
 *
 * Espelha o `DSSScheduleRechargeSuccessConfig` do iOS. `ratingGroups` são pares
 * (nome, imagem) onde a imagem pode ser nome de recurso brand, URL http(s) ou data-URI base64.
 */
data class DSSScheduleRechargeSuccessConfig(
    val title: String,
    val subtitle: String,
    val dueDateText: String,
    val nextRechargeText: String,
    val planInfo: PlanInfoCardConfig,
    val internetSemCortes: String = "",
    val ratingGroups: List<Pair<String, String?>> = emptyList(),
    val includeUnlimitedCalls: Boolean = false,
    val svaText: String? = null,
    val svaImageName: String? = null,
    /**
     * Resolve um nome de imagem brand para um Drawable local. Se não fornecido,
     * tenta `resources.getIdentifier` no pacote do app.
     */
    val iconResolver: ((String) -> Drawable?)? = null,
)

/**
 * Port do `DSSScheduleRechargeSuccessView` do iOS — tela de sucesso do fluxo de recarga
 * programada. Cabeçalho (título, subtítulo, datas) + [RechargeCompletedCardView] + seções
 * de catálogo (Internet sem cortes, Ilimitados, Assinaturas) e botão de fechar (X).
 */
class DSSScheduleRechargeSuccessView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr), ThemeAware {

    /** Handler chamado ao tocar no botão de fechar (X). */
    var onClose: (() -> Unit)? = null

    private val scrollView = ScrollView(context).apply {
        isVerticalScrollBarEnabled = false
    }
    private val contentStack = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
    }

    private val closeButton = ImageView(context).apply {
        scaleType = ImageView.ScaleType.FIT_CENTER
        contentDescription = "Fechar"
        isClickable = true
        isFocusable = true
    }

    private val titleLabel = TextView(context).apply {
        typeface = DSSFont.bold(context, 28f).typeface
        textSize = 28f
        setSingleLine(false)
    }
    private val subtitleLabel = TextView(context).apply {
        typeface = DSSFont.regular(context, 15f).typeface
        textSize = 15f
        setSingleLine(false)
    }
    private val dueDateLabel = TextView(context).apply {
        typeface = DSSFont.regular(context, 15f).typeface
        textSize = 15f
        setSingleLine(false)
    }
    private val nextRechargeLabel = TextView(context).apply {
        typeface = DSSFont.regular(context, 15f).typeface
        textSize = 15f
        setSingleLine(false)
    }

    private val card = RechargeCompletedCardView(context)
    private val catalogStack = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        visibility = View.GONE
    }

    init {
        val side = 24f.dpToPx(context)

        // Close button (top-right)
        closeButton.setImageDrawable(loadCloseIcon())
        closeButton.setOnClickListener { onClose?.invoke() }
        addView(
            closeButton,
            LayoutParams(28f.dpToPx(context), 28f.dpToPx(context)).apply {
                gravity = Gravity.TOP or Gravity.END
                topMargin = 12f.dpToPx(context)
                rightMargin = side
            },
        )

        // Content stack inside scroll
        contentStack.addView(
            titleLabel,
            mp().apply { bottomMargin = 12f.dpToPx(context) },
        )
        contentStack.addView(
            subtitleLabel,
            mp().apply { bottomMargin = 8f.dpToPx(context) },
        )
        contentStack.addView(
            dueDateLabel,
            mp().apply { bottomMargin = 8f.dpToPx(context) },
        )
        contentStack.addView(
            nextRechargeLabel,
            mp().apply { bottomMargin = 24f.dpToPx(context) },
        )
        contentStack.addView(
            card,
            mp().apply { bottomMargin = 16f.dpToPx(context) },
        )
        contentStack.addView(catalogStack, mp())

        scrollView.addView(
            contentStack,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                leftMargin = side
                rightMargin = side
                bottomMargin = 24f.dpToPx(context)
            },
        )
        addView(
            scrollView,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT).apply {
                topMargin = 12f.dpToPx(context) + 28f.dpToPx(context) + 16f.dpToPx(context)
            },
        )

        refresh()
        setupThemeObserver()
    }

    private fun mp() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT,
    )

    fun configure(config: DSSScheduleRechargeSuccessConfig) {
        titleLabel.text = config.title
        subtitleLabel.text = config.subtitle
        dueDateLabel.text = config.dueDateText
        nextRechargeLabel.text = config.nextRechargeText
        card.configure(config.planInfo)
        buildCatalogSections(config)
    }

    private fun buildCatalogSections(config: DSSScheduleRechargeSuccessConfig) {
        catalogStack.removeAllViews()
        val ctx = context

        // Internet sem cortes
        if (config.internetSemCortes.isNotEmpty()) {
            catalogStack.addView(
                makeBenefitRow(config.internetSemCortes),
                mp().apply { bottomMargin = 8f.dpToPx(ctx) },
            )
        }

        // Ilimitados
        if (config.ratingGroups.isNotEmpty() || config.includeUnlimitedCalls) {
            addSectionHeader("Ilimitados")

            config.ratingGroups.forEach { (nome, imageName) ->
                catalogStack.addView(
                    makeIconRow(nome, imageName, config.iconResolver),
                    mp().apply { bottomMargin = 8f.dpToPx(ctx) },
                )
            }

            if (config.includeUnlimitedCalls) {
                catalogStack.addView(
                    makeIconRow("Ligações usando o código 41", null, config.iconResolver, systemPhone = true),
                    mp().apply { bottomMargin = 8f.dpToPx(ctx) },
                )
            }
        }

        // Assinaturas
        val svaText = config.svaText
        if (!svaText.isNullOrEmpty()) {
            addSectionHeader("Assinaturas")
            catalogStack.addView(
                makeIconRow(svaText, config.svaImageName ?: "icAppSkeelo", config.iconResolver),
                mp().apply { bottomMargin = 8f.dpToPx(ctx) },
            )
        }

        catalogStack.visibility = if (catalogStack.childCount == 0) View.GONE else View.VISIBLE
    }

    private fun addSectionHeader(text: String) {
        val ctx = context
        // top spacer (8) before section
        catalogStack.addView(
            View(ctx),
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 8f.dpToPx(ctx)),
        )
        val label = TextView(ctx).apply {
            this.text = text
            typeface = DSSFont.bold(ctx, 15f).typeface
            textSize = 15f
            setTextColor(DSSColors.textPrimary())
        }
        catalogStack.addView(
            label,
            mp().apply { bottomMargin = 12f.dpToPx(ctx) },
        )
    }

    override fun applyTheme(theme: Theme) {
        refresh()
        // Re-tinta cabeçalho conforme novo tema.
        titleLabel.setTextColor(DSSColors.textPrimary())
        subtitleLabel.setTextColor(DSSColors.textSecondary())
        dueDateLabel.setTextColor(DSSColors.primary())
        nextRechargeLabel.setTextColor(DSSColors.primary())
    }

    private fun refresh() {
        setBackgroundColor(DSSColors.background())
        closeButton.setColorFilter(DSSColors.textPrimary(), PorterDuff.Mode.SRC_IN)
        titleLabel.setTextColor(DSSColors.textPrimary())
        subtitleLabel.setTextColor(DSSColors.textSecondary())
        dueDateLabel.setTextColor(DSSColors.primary())
        nextRechargeLabel.setTextColor(DSSColors.primary())
    }

    // MARK: - Row builders

    private fun makeBenefitRow(text: String): View {
        val ctx = context
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val checkTint = if (ThemeManager.colorScheme == ColorScheme.BLACK) {
            DSSColors.primaryButton()
        } else {
            DSSColors.primary()
        }
        val check = ImageView(ctx).apply {
            setImageResource(android.R.drawable.checkbox_on_background)
            setColorFilter(checkTint, PorterDuff.Mode.SRC_IN)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        row.addView(check, LinearLayout.LayoutParams(20f.dpToPx(ctx), 20f.dpToPx(ctx)))
        val label = TextView(ctx).apply {
            this.text = text
            typeface = DSSFont.regular(ctx, 14f).typeface
            textSize = 14f
            setTextColor(DSSColors.textSecondary())
            setSingleLine(false)
        }
        row.addView(
            label,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { leftMargin = 8f.dpToPx(ctx) },
        )
        return row
    }

    private fun makeIconRow(
        text: String,
        imageName: String?,
        resolver: ((String) -> Drawable?)?,
        systemPhone: Boolean = false,
    ): View {
        val ctx = context
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val icon = ImageView(ctx).apply { scaleType = ImageView.ScaleType.FIT_CENTER }
        row.addView(icon, LinearLayout.LayoutParams(24f.dpToPx(ctx), 24f.dpToPx(ctx)))

        var resolved = false
        when {
            systemPhone -> {
                // iOS: SF Symbol "phone.fill" tingido com .systemBlue (#007AFF).
                icon.setImageResource(android.R.drawable.sym_action_call)
                icon.setColorFilter(SYSTEM_BLUE, PorterDuff.Mode.SRC_IN)
                resolved = true
            }
            imageName != null && imageName.startsWith("data:image") -> {
                val base64 = imageName
                    .removePrefix("data:image/png;base64,")
                    .removePrefix("data:image/jpeg;base64,")
                    .removePrefix("data:image/jpg;base64,")
                try {
                    val bytes = Base64.decode(base64, Base64.DEFAULT)
                    val bmp: Bitmap? = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (bmp != null) {
                        icon.setImageBitmap(bmp)
                        resolved = true
                    }
                } catch (_: Exception) {
                }
            }
            imageName != null && imageName.startsWith("http") -> {
                Glide.with(ctx).load(imageName).into(icon)
                resolved = true
            }
            imageName != null -> {
                // iOS: ImageLoader.image(named: imageName, brand: BrandResolver.current()).
                val d = resolver?.invoke(imageName) ?: ImageLoader.image(ctx, imageName)
                if (d != null) {
                    icon.setImageDrawable(d)
                    resolved = true
                }
            }
        }

        if (!resolved) {
            icon.setImageResource(android.R.drawable.ic_menu_compass)
            icon.setColorFilter(DSSColors.primary(), PorterDuff.Mode.SRC_IN)
        }

        val label = TextView(ctx).apply {
            this.text = text
            typeface = DSSFont.regular(ctx, 14f).typeface
            textSize = 14f
            setTextColor(DSSColors.textSecondary())
            setSingleLine(false)
        }
        row.addView(
            label,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { leftMargin = 10f.dpToPx(ctx) },
        )
        return row
    }

    private fun loadCloseIcon(): Drawable? {
        // Tenta um recurso brand `xmark_circle_fill`; senão usa o close nativo. Não falha se ausente.
        val brandId = resources.getIdentifier("xmark_circle_fill", "drawable", context.packageName)
        if (brandId != 0) return AppCompatResources.getDrawable(context, brandId)
        return AppCompatResources.getDrawable(context, android.R.drawable.ic_menu_close_clear_cancel)
    }

    private companion object {
        // Equivalente ao UIColor.systemBlue do iOS (#007AFF).
        const val SYSTEM_BLUE = 0xFF007AFF.toInt()
    }
}
