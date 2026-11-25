package com.surf.surfhubds.components

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
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
import com.surf.surfhubds.util.dssInitial
import com.surf.surfhubds.util.dssText
import com.surf.surfhubds.util.weightParams
import com.surf.surfhubds.util.wrapParams

/**
 * Linha de integrante (Figma do Grupo de Recarga): avatar com a inicial, nome com selo
 * opcional ("ADMIN"), número embaixo, valor à direita ("R$30" ou "Até 20GB · R$ 30,00")
 * e seta. Com [onTap] nulo a seta some e a linha não é clicável.
 *
 * [Style.PLAIN] é a linha usada dentro de [DSSGroupMemberList]; [Style.CARD] tem borda
 * própria e serve para listas soltas, como o "plano por linha" da recarga do grupo.
 */
class DSSGroupMemberRow @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr), ThemeAware {

    enum class Style { PLAIN, CARD }

    data class Model(
        val name: CharSequence,
        val phone: CharSequence,
        val value: CharSequence? = null,
        val badge: CharSequence? = null,
        /** Mostra o avatar com a inicial do [name]. */
        val showsAvatar: Boolean = true,
        /** Nome usado só para a inicial do avatar, quando [name] é um rótulo como "Você". */
        val avatarName: String? = null,
    )

    var onTap: (() -> Unit)? = null
        set(value) {
            field = value
            chevron.visibility = if (value == null) GONE else VISIBLE
            isClickable = value != null
            refresh()
        }

    var style: Style = Style.PLAIN
        set(value) { field = value; refresh() }

    private val avatarHolder = FrameLayout(context)
    private val nameLabel = dssText(context, DSSWeight.BOLD, 14f, DSSColors.textPrimary(), maxLines = 1)
    private val badgeLabel = dssText(context, DSSWeight.BOLD, 10f, DSSColors.textPrimary(), maxLines = 1)
    private val phoneLabel = dssText(context, DSSWeight.REGULAR, 12f, DSSColors.textSecondary(), maxLines = 1)
    private val valueLabel = dssText(context, DSSWeight.MEDIUM, 16f, DSSColors.textPrimary(), maxLines = 1)
    private val chevron = ImageView(context).apply {
        setImageDrawable(ContextCompat.getDrawable(context, R.drawable.dss_ic_chevron_right))
        visibility = GONE
    }
    private var model: Model? = null

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        val ctx = context
        val avatarSize = 36f.dpToPx(ctx)
        addView(avatarHolder, LayoutParams(avatarSize, avatarSize).apply { marginEnd = 12f.dpToPx(ctx) })

        val nameRow = LinearLayout(ctx).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(nameLabel, wrapParams())
            addView(badgeLabel, wrapParams().apply { marginStart = 12f.dpToPx(ctx) })
        }
        val texts = LinearLayout(ctx).apply {
            orientation = VERTICAL
            addView(nameRow, wrapParams())
            addView(phoneLabel, wrapParams().apply { topMargin = 2f.dpToPx(ctx) })
        }
        addView(texts, weightParams())
        addView(valueLabel, wrapParams().apply { marginStart = 12f.dpToPx(ctx) })
        val chevronSize = 23f.dpToPx(ctx)
        addView(chevron, LayoutParams(chevronSize, chevronSize).apply { marginStart = 10f.dpToPx(ctx) })
        setOnClickListener { onTap?.invoke() }
        isClickable = false
        refresh()
        setupThemeObserver()
    }

    fun configure(model: Model) {
        this.model = model
        nameLabel.text = model.name
        badgeLabel.text = model.badge
        badgeLabel.visibility = if (model.badge == null) GONE else VISIBLE
        phoneLabel.text = model.phone
        phoneLabel.visibility = if (model.phone.isBlank()) GONE else VISIBLE
        valueLabel.text = model.value
        valueLabel.visibility = if (model.value == null) GONE else VISIBLE
        avatarHolder.visibility = if (model.showsAvatar) VISIBLE else GONE
        avatarHolder.removeAllViews()
        if (model.showsAvatar) {
            avatarHolder.addView(DSSAvatarStack.single(context, (model.avatarName ?: model.name.toString()).dssInitial(), 36f))
        }
        contentDescription = listOfNotNull(model.name, model.badge, model.phone, model.value).filter { it.isNotBlank() }.joinToString(", ")
    }

    override fun applyTheme(theme: Theme) {
        refresh()
        model?.let(::configure)
    }

    private fun refresh() {
        nameLabel.setTextColor(DSSColors.textPrimary())
        badgeLabel.setTextColor(DSSColors.textPrimary())
        phoneLabel.setTextColor(DSSColors.textSecondary())
        valueLabel.setTextColor(DSSColors.textPrimary())
        chevron.setColorFilter(DSSColors.textPrimary())
        val ripple = androidx.core.graphics.ColorUtils.setAlphaComponent(DSSColors.textPrimary(), 24)
        when (style) {
            Style.PLAIN -> {
                background = if (isClickable) {
                    DrawableFactory.rippleOver(DrawableFactory.rounded(context, android.graphics.Color.TRANSPARENT, 8f), ripple)
                } else {
                    null
                }
                setPadding(0, 0, 0, 0)
                valueLabel.textSize = 16f
            }
            Style.CARD -> {
                val base = DrawableFactory.rounded(context, DSSColors.background(), 10f, DSSColors.divider(), 1f)
                background = if (isClickable) DrawableFactory.rippleOver(base, ripple) else base
                val h = 16f.dpToPx(context)
                val v = 14f.dpToPx(context)
                setPadding(h, v, h * 3 / 4, v)
                valueLabel.textSize = 13f
            }
        }
        minimumHeight = 36f.dpToPx(context)
    }
}

/**
 * Lista de integrantes dentro de um card com borda (Figma do "Meu grupo"): uma
 * [DSSGroupMemberRow] por pessoa, separadas por divisória fina.
 */
class DSSGroupMemberList @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr), ThemeAware {

    /** Toque numa linha, com a posição na lista. Defina antes de [configure]; `null` deixa as linhas sem seta. */
    var onMemberTap: ((Int) -> Unit)? = null

    private val dividers = mutableListOf<View>()

    init {
        orientation = VERTICAL
        val p = 20f.dpToPx(context)
        setPadding(p, 8f.dpToPx(context), p, 8f.dpToPx(context))
        refresh()
        setupThemeObserver()
    }

    fun configure(members: List<DSSGroupMemberRow.Model>) {
        removeAllViews()
        dividers.clear()
        members.forEachIndexed { index, member ->
            if (index > 0) {
                val divider = View(context)
                dividers += divider
                addView(divider, LayoutParams(LayoutParams.MATCH_PARENT, 1f.dpToPx(context)))
            }
            val row = DSSGroupMemberRow(context).apply {
                configure(member)
                onTap = onMemberTap?.let { tap -> { tap(index) } }
            }
            val v = 12f.dpToPx(context)
            addView(row, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = v
                bottomMargin = v
            })
        }
        visibility = if (members.isEmpty()) GONE else VISIBLE
        refresh()
    }

    override fun applyTheme(theme: Theme) { refresh() }

    private fun refresh() {
        background = DrawableFactory.rounded(context, DSSColors.background(), 10f, DSSColors.divider(), 1f)
        dividers.forEach { it.setBackgroundColor(DSSColors.divider()) }
    }
}
