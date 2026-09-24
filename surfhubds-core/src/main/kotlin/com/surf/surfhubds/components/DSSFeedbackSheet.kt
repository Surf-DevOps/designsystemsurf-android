package com.surf.surfhubds.components

import android.content.Context
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.core.view.children
import androidx.core.view.isVisible
import com.surf.surfhubds.font.DSSFont
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.theme.Theme
import com.surf.surfhubds.theme.ThemeAware
import com.surf.surfhubds.theme.setupThemeObserver
import com.surf.surfhubds.util.DrawableFactory
import com.surf.surfhubds.util.dpToPx

/**
 * Aviso em sheet sobre a tela (Figma dos erros do Compre e Ganhe): véu sobre a tela
 * desfocada (Android 12+), puxador, ícone em [DSSIconBadge] de erro, título, descrição,
 * botão principal em pílula e link opcional.
 *
 * Coloque como último filho da raiz da tela, ocupando tudo e `gone`. [show] desfoca os
 * irmãos (o resto da tela) e [hide] desfaz. O véu consome toques atrás do sheet.
 */
class DSSFeedbackSheet @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr), ThemeAware {

    private val handle = View(context)
    private val badge = DSSIconBadge(context)
    private val titleLabel = TextView(context).apply {
        gravity = Gravity.CENTER
        textSize = 18f
        typeface = DSSFont.bold(context, 18f).typeface
    }
    private val descriptionLabel = TextView(context).apply {
        gravity = Gravity.CENTER
        textSize = 14f
        typeface = DSSFont.regular(context, 14f).typeface
    }
    private val primaryButton = DSSPrincipalButton(context).apply { applyPillStyle(42f, 13f) }
    private val secondaryLink = TextView(context).apply {
        gravity = Gravity.CENTER
        textSize = 13f
        typeface = DSSFont.regular(context, 13f).typeface
        val pad = 6f.dpToPx(context)
        setPadding(pad * 2, pad, pad * 2, pad)
    }
    private val sheet = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        elevation = 8f.dpToPx(context).toFloat()
        val padH = 24f.dpToPx(context)
        setPadding(padH, 8f.dpToPx(context), padH, 40f.dpToPx(context))
    }

    init {
        isClickable = true
        isFocusable = true
        visibility = GONE

        val ctx = context
        sheet.addView(handle, LinearLayout.LayoutParams(150f.dpToPx(ctx), 4f.dpToPx(ctx)))
        sheet.addView(badge, LinearLayout.LayoutParams(52f.dpToPx(ctx), 52f.dpToPx(ctx)).apply {
            topMargin = 56f.dpToPx(ctx)
        })
        sheet.addView(titleLabel, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            topMargin = 20f.dpToPx(ctx)
        })
        sheet.addView(descriptionLabel, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            topMargin = 10f.dpToPx(ctx)
            marginStart = 28f.dpToPx(ctx)
            marginEnd = 28f.dpToPx(ctx)
        })
        sheet.addView(primaryButton, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            topMargin = 24f.dpToPx(ctx)
            marginStart = 36f.dpToPx(ctx)
            marginEnd = 36f.dpToPx(ctx)
        })
        sheet.addView(secondaryLink, LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            topMargin = 6f.dpToPx(ctx)
        })
        addView(sheet, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.BOTTOM))

        refresh()
        setupThemeObserver()
    }

    /**
     * Mostra o aviso. [secondaryTitle] nulo esconde o link. As ações não fecham o sheet
     * sozinhas: quem usa decide (ex.: [hide] para tentar de novo, ou navegar).
     */
    fun show(
        icon: Drawable?,
        title: String,
        description: String,
        primaryTitle: String,
        onPrimary: () -> Unit,
        secondaryTitle: String? = null,
        onSecondary: (() -> Unit)? = null,
    ) {
        badge.configure(icon, DSSIconBadge.Style.ERROR)
        titleLabel.text = title
        descriptionLabel.text = description
        primaryButton.text = primaryTitle
        primaryButton.setOnClickListener { onPrimary() }
        secondaryLink.isVisible = secondaryTitle != null
        secondaryLink.text = secondaryTitle
        secondaryLink.setOnClickListener { onSecondary?.invoke() }
        visibility = VISIBLE
        setBlurBehind(true)
    }

    fun hide() {
        visibility = GONE
        setBlurBehind(false)
    }

    val isShowing: Boolean get() = visibility == VISIBLE

    /** Desfoca os irmãos (Android 12+); antes disso fica só o véu. */
    private fun setBlurBehind(blurred: Boolean) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val parent = parent as? ViewGroup ?: return
        val effect = if (blurred) RenderEffect.createBlurEffect(BLUR_RADIUS, BLUR_RADIUS, Shader.TileMode.CLAMP) else null
        parent.children.filter { it !== this }.forEach { it.setRenderEffect(effect) }
    }

    override fun applyTheme(theme: Theme) { refresh() }

    private fun refresh() {
        setBackgroundColor(ColorUtils.setAlphaComponent(DSSColors.surface(), VEIL_ALPHA))
        sheet.background = GradientDrawable().apply {
            val r = 16f.dpToPx(context).toFloat()
            cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
            setColor(DSSColors.surface())
        }
        handle.background = DrawableFactory.rounded(
            context,
            androidx.core.graphics.ColorUtils.setAlphaComponent(DSSColors.textTertiary(), 0x66),
            2f,
        )
        titleLabel.setTextColor(DSSColors.textPrimary())
        descriptionLabel.setTextColor(DSSColors.textSecondary())
        secondaryLink.setTextColor(DSSColors.textLink())
        primaryButton.customBackgroundColor = DSSColors.primaryButton()
        primaryButton.customTextColor = DSSColors.buttonText()
    }

    private companion object {
        const val BLUR_RADIUS = 24f
        const val VEIL_ALPHA = 0x99
    }
}
