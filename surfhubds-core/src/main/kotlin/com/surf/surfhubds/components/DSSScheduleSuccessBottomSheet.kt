package com.surf.surfhubds.components

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Base64
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.surf.surfhubds.font.DSSFont
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.theme.ThemeManager
import com.surf.surfhubds.tokens.ColorScheme
import com.surf.surfhubds.util.DrawableFactory
import com.surf.surfhubds.util.ImageLoader
import com.surf.surfhubds.util.dpToPx
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Port do `DSSScheduleSuccessBottomSheet` do iOS — bottom sheet de sucesso após
 * agendar/alterar plano com card de "Nova oferta", lista de benefícios e SVAs.
 *
 * O modelo `CatalogSuccess.CustomerResult` do iOS vive em SurfAPIKit; aqui usamos um
 * [Content] simples montado pelo app.
 */
class DSSScheduleSuccessBottomSheet : BottomSheetDialogFragment() {

    interface Delegate {
        fun scheduleSuccessBottomSheetDidDismiss(sheet: DSSScheduleSuccessBottomSheet) {}
    }

    data class BenefitItem(val name: String, val quantity: String? = null)
    data class IconItem(val name: String, val imageNameOrUrl: String? = null)

    /**
     * Conteúdo a renderizar no card de "Nova oferta".
     *
     * Se [iconResolver] é fornecido, ele é chamado com o `imageNameOrUrl` para devolver
     * um Drawable local (ex.: vindo do módulo de brand). Caso contrário, URLs são carregadas
     * via Glide e data-URIs base64 são decodificadas inline.
     */
    data class Content(
        val planName: String,
        val priceInCents: Int,
        val dateText: String,
        val internetSemCortes: String,
        val detalhamento: List<BenefitItem>,
        val ratingGroups: List<IconItem>,
        val svaText: String?,
        val svaImageName: String?,
        /**
         * Equivalente ao `includeUnlimitedCalls` do iOS (derivado de `qtVoz.contains("ilimitad")`).
         * Quando `true`, renderiza a linha "Ligações ilimitadas (código 41)" na seção Ilimitados.
         */
        val includeUnlimitedCalls: Boolean = false,
        val iconResolver: ((String) -> Drawable?)? = null,
    )

    var delegate: Delegate? = null
    private var content: Content? = null

    private var didFireDismiss = false

    fun configure(content: Content) {
        this.content = content
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        val ctx = requireContext()
        // iOS: containerView cornerRadius 24 nos cantos de topo + borderLayer (lineWidth 1)
        // só em black/dark (black -> branco; dark -> branco @40%; light -> sem borda).
        val scroll = ScrollView(ctx).apply { background = roundedTopBorderedBackground(ctx, DSSColors.background()) }

        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24f.dpToPx(ctx), 16f.dpToPx(ctx), 24f.dpToPx(ctx), 16f.dpToPx(ctx))
        }

        // Header "Plano alterado com sucesso!"
        val headerRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val icon = ImageView(ctx).apply {
            setImageResource(android.R.drawable.checkbox_on_background)
            setColorFilter(DSSColors.success(), PorterDuff.Mode.SRC_IN)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        headerRow.addView(icon, LinearLayout.LayoutParams(32f.dpToPx(ctx), 32f.dpToPx(ctx)))
        val title = TextView(ctx).apply {
            text = "Plano alterado com sucesso!"
            typeface = DSSFont.bold(ctx, 18f).typeface
            textSize = 18f
            setTextColor(DSSColors.textPrimary())
        }
        headerRow.addView(title, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { leftMargin = 10f.dpToPx(ctx) })

        root.addView(headerRow, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { bottomMargin = 20f.dpToPx(ctx) })

        // Offer card
        val content = this.content
        if (content != null) {
            root.addView(buildOfferCard(ctx, content), LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
            ))
        }

        scroll.addView(root, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
        ))
        return scroll
    }

    private fun buildOfferCard(ctx: Context, c: Content): View {
        val card = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = DrawableFactory.rounded(
                context = ctx, backgroundColor = DSSColors.backgroundSecondary(), cornerRadiusDp = 16f,
            )
            setPadding(20f.dpToPx(ctx), 20f.dpToPx(ctx), 20f.dpToPx(ctx), 20f.dpToPx(ctx))
        }

        val novaOfertaLabel = TextView(ctx).apply {
            text = "Nova oferta"
            typeface = DSSFont.bold(ctx, 18f).typeface
            textSize = 18f
            setTextColor(DSSColors.success())
        }
        card.addView(novaOfertaLabel, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { bottomMargin = 8f.dpToPx(ctx) })

        // Plan row
        val planRow = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        val planLabel = TextView(ctx).apply {
            text = c.planName
            typeface = DSSFont.bold(ctx, 20f).typeface
            textSize = 20f
            setTextColor(DSSColors.textPrimary())
        }
        val priceLabel = TextView(ctx).apply {
            val reais = c.priceInCents / 100
            text = "R$$reais/mês"
            typeface = DSSFont.bold(ctx, 16f).typeface
            textSize = 16f
            setTextColor(DSSColors.textPrimary())
            gravity = Gravity.END
        }
        planRow.addView(planLabel, LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f,
        ))
        planRow.addView(priceLabel, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ))
        card.addView(planRow, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { bottomMargin = 12f.dpToPx(ctx) })

        // Badge + date row
        val badgeRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val badge = TextView(ctx).apply {
            text = "Inicio da programada"
            typeface = DSSFont.light(ctx, 13f).typeface
            textSize = 13f
            // buttonText (conteúdo sobre a primary), não branco fixo: em brand com primary
            // clara (Uber) o branco some no badge de fundo primary.
            setTextColor(DSSColors.buttonText())
            background = DrawableFactory.rounded(
                context = ctx, backgroundColor = DSSColors.primary(), cornerRadiusDp = 14f,
            )
            // iOS: badge tem altura fixa de 28pt e padding horizontal de 14pt (vertical preenche os 28).
            gravity = Gravity.CENTER
            setPadding(14f.dpToPx(ctx), 0, 14f.dpToPx(ctx), 0)
        }
        val dateLabel = TextView(ctx).apply {
            text = c.dateText
            typeface = DSSFont.light(ctx, 14f).typeface
            textSize = 14f
            setTextColor(DSSColors.textPrimary())
            gravity = Gravity.END
        }
        badgeRow.addView(badge, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, 28f.dpToPx(ctx),
        ))
        // spacer
        badgeRow.addView(View(ctx), LinearLayout.LayoutParams(0, 1, 1f))
        badgeRow.addView(dateLabel, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ))
        card.addView(badgeRow, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { bottomMargin = 20f.dpToPx(ctx) })

        // Separator
        val sep = View(ctx).apply { setBackgroundColor(DSSColors.divider()) }
        // iOS: separador com 0.5pt de altura.
        card.addView(sep, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0.5f.dpToPx(ctx),
        ).apply { bottomMargin = 16f.dpToPx(ctx) })

        if (c.internetSemCortes.isNotEmpty()) {
            card.addView(makeBenefitRow(ctx, c.internetSemCortes), LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { bottomMargin = 8f.dpToPx(ctx) })
        }
        c.detalhamento.forEach { item ->
            val text = if (!item.quantity.isNullOrEmpty()) "${item.quantity} ${item.name}" else item.name
            card.addView(makeBenefitRow(ctx, text), LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { bottomMargin = 8f.dpToPx(ctx) })
        }

        if (c.ratingGroups.isNotEmpty() || c.includeUnlimitedCalls) {
            val ilimitadosLabel = TextView(ctx).apply {
                text = "Ilimitados"
                typeface = DSSFont.bold(ctx, 15f).typeface
                textSize = 15f
                setTextColor(DSSColors.textPrimary())
            }
            card.addView(ilimitadosLabel, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = 8f.dpToPx(ctx)
                bottomMargin = 12f.dpToPx(ctx)
            })

            c.ratingGroups.forEach { rg ->
                card.addView(makeIconRow(ctx, rg.name, rg.imageNameOrUrl, c.iconResolver), LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply { bottomMargin = 8f.dpToPx(ctx) })
            }

            // Ligações ilimitadas (código 41) quando a voz é ilimitada
            if (c.includeUnlimitedCalls) {
                card.addView(
                    makeIconRow(ctx, "Ligações usando o código 41", null, c.iconResolver, systemPhone = true),
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).apply { bottomMargin = 8f.dpToPx(ctx) },
                )
            }
        }

        if (!c.svaText.isNullOrEmpty()) {
            val assinaturasLabel = TextView(ctx).apply {
                text = "Assinaturas"
                typeface = DSSFont.bold(ctx, 15f).typeface
                textSize = 15f
                setTextColor(DSSColors.textPrimary())
            }
            card.addView(assinaturasLabel, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = 8f.dpToPx(ctx)
                bottomMargin = 12f.dpToPx(ctx)
            })
            card.addView(makeIconRow(ctx, c.svaText, c.svaImageName ?: "icAppSkeelo", c.iconResolver),
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
                ),
            )
        }

        return card
    }

    private fun makeBenefitRow(ctx: Context, text: String): View {
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
        }
        row.addView(label, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { leftMargin = 8f.dpToPx(ctx) })
        return row
    }

    private fun makeIconRow(
        ctx: Context,
        text: String,
        imageNameOrUrl: String?,
        resolver: ((String) -> Drawable?)?,
        systemPhone: Boolean = false,
    ): View {
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
            imageNameOrUrl != null && imageNameOrUrl.startsWith("data:image") -> {
                val base64 = imageNameOrUrl
                    .removePrefix("data:image/png;base64,")
                    .removePrefix("data:image/jpeg;base64,")
                    .removePrefix("data:image/jpg;base64,")
                try {
                    val bytes = Base64.decode(base64, Base64.DEFAULT)
                    val bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (bmp != null) {
                        icon.setImageBitmap(bmp)
                        resolved = true
                    }
                } catch (_: Exception) {}
            }
            imageNameOrUrl != null && imageNameOrUrl.startsWith("http") -> {
                Glide.with(ctx).load(imageNameOrUrl).into(icon)
                resolved = true
            }
            imageNameOrUrl != null -> {
                // iOS resolve nomes locais via ImageLoader.image(named:brand:).
                val drawable = resolver?.invoke(imageNameOrUrl) ?: ImageLoader.image(ctx, imageNameOrUrl)
                if (drawable != null) {
                    icon.setImageDrawable(drawable)
                    resolved = true
                }
            }
        }

        // iOS: se nenhuma imagem resolveu, cai para SF Symbol "app.fill" tingido com primary.
        if (!resolved) {
            icon.setImageResource(android.R.drawable.ic_menu_compass)
            icon.setColorFilter(DSSColors.primary(), PorterDuff.Mode.SRC_IN)
        }

        val label = TextView(ctx).apply {
            this.text = text
            typeface = DSSFont.regular(ctx, 14f).typeface
            textSize = 14f
            setTextColor(DSSColors.textSecondary())
        }
        row.addView(label, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { leftMargin = 10f.dpToPx(ctx) })
        return row
    }

    override fun onDismiss(dialog: android.content.DialogInterface) {
        super.onDismiss(dialog)
        if (!didFireDismiss) {
            didFireDismiss = true
            delegate?.scheduleSuccessBottomSheetDidDismiss(this)
        }
    }

    companion object {
        // iOS: SF Symbol "phone.fill" usa tint .systemBlue (#007AFF) na linha de ligações.
        private val SYSTEM_BLUE = Color.parseColor("#007AFF")

        private val displayFormatter: SimpleDateFormat by lazy {
            SimpleDateFormat("dd MMM. yyyy", Locale("pt", "BR"))
        }
        private val isoFormatter: SimpleDateFormat by lazy {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
        }
        private val isoFormatterNoFraction: SimpleDateFormat by lazy {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
        }

        /** Conveniência: converte uma string ISO 8601 para o formato exibido "dd MMM. yyyy". */
        fun displayDate(iso: String): String {
            // iOS tenta primeiro com segundos fracionados (isoFormatter) e depois sem (isoFormatterNoFraction).
            val parsed: Date? = parseOrNull(isoFormatter, iso) ?: parseOrNull(isoFormatterNoFraction, iso)
            return if (parsed != null) displayFormatter.format(parsed) else iso
        }

        private fun parseOrNull(formatter: SimpleDateFormat, iso: String): Date? =
            try { formatter.parse(iso) } catch (_: Exception) { null }

        fun present(
            activity: FragmentActivity,
            content: Content,
            delegate: Delegate? = null,
        ): DSSScheduleSuccessBottomSheet {
            val sheet = DSSScheduleSuccessBottomSheet()
            sheet.delegate = delegate
            sheet.configure(content)
            sheet.show(activity.supportFragmentManager, "DSSScheduleSuccessBottomSheet")
            return sheet
        }
    }
}

/**
 * Fundo com cantos superiores arredondados (24dp) + borda do iOS.
 * iOS: borderLayer (lineWidth 1) só em black/dark — black -> branco; dark -> branco @40%.
 */
private fun roundedTopBorderedBackground(ctx: Context, @androidx.annotation.ColorInt color: Int): Drawable {
    val r = 24f.dpToPx(ctx).toFloat()
    return android.graphics.drawable.GradientDrawable().apply {
        shape = android.graphics.drawable.GradientDrawable.RECTANGLE
        setColor(color)
        cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
        when (ThemeManager.colorScheme) {
            ColorScheme.BLACK -> setStroke(1f.dpToPx(ctx), Color.WHITE)
            ColorScheme.DARK -> setStroke(1f.dpToPx(ctx), Color.argb(102, 255, 255, 255))
            else -> {}
        }
    }
}
