package com.surf.surfhubds.components

import android.content.Context
import android.graphics.Color
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
import com.surf.surfhubds.util.DrawableFactory
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
        val scroll = ScrollView(ctx).apply { setBackgroundColor(DSSColors.background()) }

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
            setColorFilter(android.graphics.Color.parseColor("#34C759"))
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
                context = ctx, backgroundColor = Color.WHITE, cornerRadiusDp = 16f,
            )
            setPadding(20f.dpToPx(ctx), 20f.dpToPx(ctx), 20f.dpToPx(ctx), 20f.dpToPx(ctx))
        }

        val novaOfertaLabel = TextView(ctx).apply {
            text = "Nova oferta"
            typeface = DSSFont.bold(ctx, 18f).typeface
            textSize = 18f
            setTextColor(Color.parseColor("#34C759"))
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
            setTextColor(Color.WHITE)
            background = DrawableFactory.rounded(
                context = ctx, backgroundColor = DSSColors.primary(), cornerRadiusDp = 14f,
            )
            setPadding(14f.dpToPx(ctx), 4f.dpToPx(ctx), 14f.dpToPx(ctx), 4f.dpToPx(ctx))
        }
        val dateLabel = TextView(ctx).apply {
            text = c.dateText
            typeface = DSSFont.light(ctx, 14f).typeface
            textSize = 14f
            setTextColor(DSSColors.textPrimary())
            gravity = Gravity.END
        }
        badgeRow.addView(badge, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT,
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
        card.addView(sep, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 1,
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

        if (c.ratingGroups.isNotEmpty()) {
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
        val check = ImageView(ctx).apply {
            setImageResource(android.R.drawable.checkbox_on_background)
            setColorFilter(Color.parseColor("#34C759"))
        }
        row.addView(check, LinearLayout.LayoutParams(20f.dpToPx(ctx), 20f.dpToPx(ctx)))
        val label = TextView(ctx).apply {
            this.text = text
            typeface = DSSFont.regular(ctx, 14f).typeface
            textSize = 14f
            setTextColor(DSSColors.textPrimary())
        }
        row.addView(label, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { leftMargin = 8f.dpToPx(ctx) })
        return row
    }

    private fun makeIconRow(ctx: Context, text: String, imageNameOrUrl: String?, resolver: ((String) -> Drawable?)?): View {
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val icon = ImageView(ctx).apply { scaleType = ImageView.ScaleType.FIT_CENTER }
        row.addView(icon, LinearLayout.LayoutParams(24f.dpToPx(ctx), 24f.dpToPx(ctx)))

        if (imageNameOrUrl != null) {
            when {
                imageNameOrUrl.startsWith("data:image") -> {
                    val base64 = imageNameOrUrl
                        .removePrefix("data:image/png;base64,")
                        .removePrefix("data:image/jpeg;base64,")
                        .removePrefix("data:image/jpg;base64,")
                    try {
                        val bytes = Base64.decode(base64, Base64.DEFAULT)
                        val bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        icon.setImageBitmap(bmp)
                    } catch (_: Exception) {}
                }
                imageNameOrUrl.startsWith("http") -> {
                    Glide.with(ctx).load(imageNameOrUrl).into(icon)
                }
                else -> {
                    val drawable = resolver?.invoke(imageNameOrUrl)
                    if (drawable != null) icon.setImageDrawable(drawable)
                    else {
                        icon.setImageResource(android.R.drawable.ic_menu_compass)
                        icon.setColorFilter(DSSColors.primary())
                    }
                }
            }
        }

        val label = TextView(ctx).apply {
            this.text = text
            typeface = DSSFont.regular(ctx, 14f).typeface
            textSize = 14f
            setTextColor(DSSColors.textPrimary())
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
        private val displayFormatter: SimpleDateFormat by lazy {
            SimpleDateFormat("dd MMM. yyyy", Locale("pt", "BR"))
        }
        private val isoFormatter: SimpleDateFormat by lazy {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
        }

        /** Conveniência: converte uma string ISO 8601 para o formato exibido "dd MMM. yyyy". */
        fun displayDate(iso: String): String {
            return try {
                val d: Date? = isoFormatter.parse(iso)
                if (d != null) displayFormatter.format(d) else iso
            } catch (_: Exception) { iso }
        }

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
