package com.surf.surfhubds.components

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.surf.surfhubds.R
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.theme.Theme
import com.surf.surfhubds.theme.ThemeAware
import com.surf.surfhubds.theme.setupThemeObserver
import com.surf.surfhubds.util.DSSWeight
import com.surf.surfhubds.util.DrawableFactory
import com.surf.surfhubds.util.dpToPx
import com.surf.surfhubds.util.dssText
import com.surf.surfhubds.util.matchWidthParams
import com.surf.surfhubds.util.weightParams
import com.surf.surfhubds.util.wrapParams

/**
 * Card de um grupo na lista "Grupos cadastrados" (Figma do Grupo de Recarga):
 * nome + status da programada, avatares + "5 integrantes", caixa com duas informações
 * ("Próxima recarga 25/11" · "Valor da recarga R$ 235,00") e o rodapé de ações.
 *
 * Rodapé:
 * - com [Model.primaryAction]: link à esquerda ([Model.secondaryAction], ex.: "Ver grupo")
 *   e botão escuro à direita (ex.: "Recarregar →"), como na visão do administrador;
 * - sem [Model.primaryAction]: só o link, centralizado (visão do integrante: "Sair do grupo").
 *
 * Os textos vêm prontos do app; o componente não formata valores nem datas.
 */
class DSSGroupCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr), ThemeAware {

    data class Info(val label: CharSequence, val value: CharSequence)

    data class Model(
        val name: CharSequence,
        val memberNames: List<String>,
        val membersText: CharSequence,
        val statusText: CharSequence? = null,
        val statusTone: DSSStatusLabel.Tone = DSSStatusLabel.Tone.NEUTRAL,
        val infos: List<Info> = emptyList(),
        val secondaryAction: CharSequence? = null,
        val primaryAction: CharSequence? = null,
    )

    var onSecondaryAction: (() -> Unit)? = null
    var onPrimaryAction: (() -> Unit)? = null

    private val nameLabel = dssText(context, DSSWeight.SEMIBOLD, 17f, DSSColors.textPrimary(), maxLines = 1)
    private val status = DSSStatusLabel(context)
    private val avatars = DSSAvatarStack(context)
    private val membersLabel = dssText(context, DSSWeight.REGULAR, 13f, DSSColors.textSecondary(), maxLines = 1)
    private val infoBox = LinearLayout(context).apply { orientation = HORIZONTAL }
    private val footerDivider = View(context)
    private val footer = LinearLayout(context).apply { orientation = HORIZONTAL }
    private val secondaryButton = dssText(context, DSSWeight.MEDIUM, 14f, DSSColors.textPrimary(), maxLines = 1).apply {
        gravity = Gravity.CENTER
        setOnClickListener { onSecondaryAction?.invoke() }
    }
    private val verticalDivider = View(context)
    private val primaryButton = LinearLayout(context).apply {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER
        setOnClickListener { onPrimaryAction?.invoke() }
    }
    private val primaryLabel = dssText(context, DSSWeight.SEMIBOLD, 14f, DSSColors.background(), maxLines = 1)
    private val primaryArrow = ImageView(context).apply {
        setImageDrawable(ContextCompat.getDrawable(context, R.drawable.dss_ic_arrow_right))
    }

    private var model: Model? = null

    init {
        orientation = VERTICAL
        val ctx = context
        val pad = 17f.dpToPx(ctx)

        val content = LinearLayout(ctx).apply {
            orientation = VERTICAL
            setPadding(pad, 16f.dpToPx(ctx), pad, 14f.dpToPx(ctx))
        }
        val titleRow = LinearLayout(ctx).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(nameLabel, weightParams())
            addView(status, wrapParams().apply { marginStart = 12f.dpToPx(ctx) })
        }
        val membersRow = LinearLayout(ctx).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(avatars, wrapParams())
            addView(membersLabel, wrapParams().apply { marginStart = 14f.dpToPx(ctx) })
        }
        content.addView(titleRow, matchWidthParams())
        content.addView(membersRow, matchWidthParams().apply { topMargin = 8f.dpToPx(ctx) })
        content.addView(infoBox, matchWidthParams().apply { topMargin = 15f.dpToPx(ctx) })
        addView(content, matchWidthParams())

        addView(footerDivider, LayoutParams(LayoutParams.MATCH_PARENT, 1f.dpToPx(ctx)))
        primaryButton.addView(primaryLabel, wrapParams())
        val arrowSize = 14f.dpToPx(ctx)
        primaryButton.addView(primaryArrow, LayoutParams(arrowSize, arrowSize).apply { marginStart = 6f.dpToPx(ctx) })
        val footerHeight = 52f.dpToPx(ctx)
        footer.addView(secondaryButton, LayoutParams(0, footerHeight, 1f))
        footer.addView(verticalDivider, LayoutParams(1f.dpToPx(ctx), footerHeight))
        footer.addView(primaryButton, LayoutParams(0, footerHeight, 1f))
        addView(footer, matchWidthParams())

        refresh()
        setupThemeObserver()
    }

    fun configure(model: Model) {
        this.model = model
        nameLabel.text = model.name
        status.visibility = if (model.statusText == null) GONE else VISIBLE
        model.statusText?.let { status.configure(it, model.statusTone, DSSStatusLabel.Style.DOT) }
        avatars.configureNames(model.memberNames, maxVisible = 2)
        membersLabel.text = model.membersText

        infoBox.removeAllViews()
        infoBox.visibility = if (model.infos.isEmpty()) GONE else VISIBLE
        model.infos.forEachIndexed { index, info ->
            val column = LinearLayout(context).apply {
                orientation = VERTICAL
                gravity = if (index == 0) Gravity.START else Gravity.END
                addView(dssText(context, DSSWeight.REGULAR, 12f, DSSColors.textSecondary(), maxLines = 1).apply { text = info.label })
                addView(
                    dssText(context, DSSWeight.BOLD, 15f, DSSColors.textPrimary(), maxLines = 1).apply { text = info.value },
                    wrapParams().apply { topMargin = 4f.dpToPx(context) },
                )
            }
            infoBox.addView(column, weightParams())
        }

        secondaryButton.text = model.secondaryAction
        primaryLabel.text = model.primaryAction
        val hasPrimary = model.primaryAction != null
        val hasSecondary = model.secondaryAction != null
        footer.visibility = if (hasPrimary || hasSecondary) VISIBLE else GONE
        footerDivider.visibility = footer.visibility
        primaryButton.visibility = if (hasPrimary) VISIBLE else GONE
        verticalDivider.visibility = if (hasPrimary && hasSecondary) VISIBLE else GONE
        secondaryButton.visibility = if (hasSecondary) VISIBLE else GONE
        refresh()
    }

    override fun applyTheme(theme: Theme) { refresh() }

    private fun refresh() {
        val radius = 12f
        val border = DSSColors.divider()
        background = DrawableFactory.rounded(context, DSSColors.background(), radius, border, 1f)
        clipToOutline = true
        nameLabel.setTextColor(DSSColors.textPrimary())
        membersLabel.setTextColor(DSSColors.textSecondary())
        infoBox.background = DrawableFactory.rounded(context, DSSColors.backgroundSecondary(), 8f)
        val boxPadH = 14f.dpToPx(context)
        val boxPadV = 11f.dpToPx(context)
        infoBox.setPadding(boxPadH, boxPadV, boxPadH, boxPadV)
        footerDivider.setBackgroundColor(border)
        verticalDivider.setBackgroundColor(border)
        secondaryButton.setTextColor(DSSColors.textPrimary())
        secondaryButton.background = DrawableFactory.rippleOver(
            android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT),
            ColorUtils.setAlphaComponent(DSSColors.textPrimary(), 30),
        )

        // Botão escuro do Figma (#111): fundo = cor do texto principal, conteúdo em contraste.
        // Só o canto inferior direito acompanha o card.
        val dark = DSSColors.textPrimary()
        val r = radius.dpToPxFloat()
        // Sem o link ao lado, o botão ocupa o rodapé todo e arredonda os dois cantos de baixo.
        val bottomLeft = if (model?.secondaryAction == null) r else 0f
        primaryButton.background = android.graphics.drawable.GradientDrawable().apply {
            setColor(dark)
            cornerRadii = floatArrayOf(0f, 0f, 0f, 0f, r, r, bottomLeft, bottomLeft)
        }
        val onDark = DSSColors.contrastOn(dark)
        primaryLabel.setTextColor(onDark)
        primaryArrow.setColorFilter(onDark)
    }

    private fun Float.dpToPxFloat(): Float = this * resources.displayMetrics.density
}
