package com.surf.surfhubds.components

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
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
 * Cabeçalho do "Meu grupo" (Figma do Grupo de Recarga): nome com lápis opcional,
 * avatares + "5/100 integrantes", valor à direita ("Valor da recarga" / "R$ 235,00") e
 * o botão principal em pílula ("Recarregar grupo" para o administrador, "Sair do grupo"
 * para o integrante). Sem [onEdit] o lápis some; sem [Model.actionTitle] o botão some.
 */
class DSSGroupHeaderCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr), ThemeAware {

    data class Model(
        val name: CharSequence,
        val memberNames: List<String>,
        val membersText: CharSequence,
        val valueLabel: CharSequence? = null,
        val value: CharSequence? = null,
        val actionTitle: CharSequence? = null,
    )

    /** Toque no lápis ao lado do nome. `null` esconde o lápis. */
    var onEdit: (() -> Unit)? = null
        set(value) { field = value; editIcon.visibility = if (value == null) GONE else VISIBLE }

    var onAction: (() -> Unit)? = null

    private val nameLabel = dssText(context, DSSWeight.BOLD, 20f, DSSColors.textPrimary(), maxLines = 1)
    private val editIcon = ImageView(context).apply {
        setImageDrawable(ContextCompat.getDrawable(context, R.drawable.dss_ic_edit))
        val p = 4f.dpToPx(context)
        setPadding(p, p, p, p)
        visibility = GONE
        setOnClickListener { onEdit?.invoke() }
    }
    private val avatars = DSSAvatarStack(context)
    private val membersLabel = dssText(context, DSSWeight.REGULAR, 13f, DSSColors.textSecondary(), maxLines = 1)
    private val valueLabel = dssText(context, DSSWeight.REGULAR, 12f, DSSColors.textSecondary(), maxLines = 1)
    private val valueText = dssText(context, DSSWeight.BOLD, 20f, DSSColors.textPrimary(), maxLines = 1)
    private val valueColumn = LinearLayout(context).apply {
        orientation = VERTICAL
        gravity = Gravity.END
    }
    private val actionButton = DSSPrincipalButton(context).apply {
        applyPillStyle(heightDp = 40f, textSizeSp = 14f)
        onTap = { onAction?.invoke() }
    }

    init {
        orientation = VERTICAL
        val ctx = context
        setPadding(22f.dpToPx(ctx), 21f.dpToPx(ctx), 22f.dpToPx(ctx), 18f.dpToPx(ctx))

        val titleRow = LinearLayout(ctx).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(nameLabel, wrapParams())
            val iconSize = 22f.dpToPx(ctx)
            addView(editIcon, LayoutParams(iconSize, iconSize).apply { marginStart = 10f.dpToPx(ctx) })
        }
        val left = LinearLayout(ctx).apply {
            orientation = VERTICAL
            addView(avatars, wrapParams())
            addView(membersLabel, wrapParams().apply { topMargin = 7f.dpToPx(ctx) })
        }
        valueColumn.addView(valueLabel, wrapParams())
        valueColumn.addView(valueText, wrapParams().apply { topMargin = 1f.dpToPx(ctx) })
        val infoRow = LinearLayout(ctx).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(left, weightParams())
            addView(valueColumn, wrapParams())
        }
        addView(titleRow, matchWidthParams())
        addView(infoRow, matchWidthParams().apply { topMargin = 12f.dpToPx(ctx) })
        addView(actionButton, LayoutParams(LayoutParams.MATCH_PARENT, 40f.dpToPx(ctx)).apply {
            topMargin = 12f.dpToPx(ctx)
            val inset = 12f.dpToPx(ctx)
            marginStart = inset
            marginEnd = inset
        })

        refresh()
        setupThemeObserver()
    }

    fun configure(model: Model) {
        nameLabel.text = model.name
        avatars.configureNames(model.memberNames, maxVisible = 4)
        membersLabel.text = model.membersText
        valueLabel.text = model.valueLabel
        valueText.text = model.value
        valueColumn.visibility = if (model.value == null) GONE else VISIBLE
        actionButton.text = model.actionTitle
        actionButton.visibility = if (model.actionTitle == null) GONE else VISIBLE
    }

    override fun applyTheme(theme: Theme) { refresh() }

    private fun refresh() {
        background = DrawableFactory.rounded(context, DSSColors.backgroundSecondary(), 10f)
        // Os avatares ficam sobre o cinza do card: a borda que separa os círculos usa essa cor.
        avatars.ringColor = DSSColors.backgroundSecondary()
        editIcon.setColorFilter(DSSColors.textSecondary())
        nameLabel.setTextColor(DSSColors.textPrimary())
        valueText.setTextColor(DSSColors.textPrimary())
        membersLabel.setTextColor(DSSColors.textSecondary())
        valueLabel.setTextColor(DSSColors.textSecondary())
    }
}
