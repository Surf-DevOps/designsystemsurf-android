package com.surf.surfhubds.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.appcompat.widget.AppCompatButton
import com.surf.surfhubds.font.DSSFont
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.theme.Theme
import com.surf.surfhubds.theme.ThemeAware
import com.surf.surfhubds.theme.ThemeManager
import com.surf.surfhubds.theme.setupThemeObserver
import com.surf.surfhubds.tokens.ColorScheme
import com.surf.surfhubds.util.DrawableFactory
import com.surf.surfhubds.util.ImageLoader
import com.surf.surfhubds.util.dpToPx

/**
 * Port do `DSSRecurrencyFailureCardView.swift` (iOS) — card de "falha na recorrência" com
 * header vermelho-escuro (ícone `empty_recurrency` + título), descrição, um [DSSResumeCard]
 * embutido (Número / Plano / Valor) e um botão outlined "Copiar código" que copia o código
 * PIX para a área de transferência e mostra "Pix copiado" por 2s antes de reverter.
 *
 * Espelha a estrutura de [DSSPixSimpleCardView] (clipboard + botão de copiar + resume card)
 * e o estilo dos demais cards `ThemeAware` do módulo.
 */
class DSSRecurrencyFailureCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr), ThemeAware {

    /** Disparado ao tocar no card. */
    var onCardTapped: (() -> Unit)? = null

    /** Disparado ao tocar no botão de copiar código. */
    var onCopyCodeTapped: (() -> Unit)? = null

    private val container = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
    }

    // iOS: headerStackView horizontal, alignment .center, spacing 5.
    private val headerRow = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }
    private val iconImageView = ImageView(context).apply {
        scaleType = ImageView.ScaleType.FIT_CENTER
        // iOS: ImageLoader.image(named: "empty_recurrency", brand: BrandResolver.current()).
        setImageDrawable(ImageLoader.image(context, "empty_recurrency"))
    }
    private val titleLabel = TextView(context).apply {
        text = "FALHA NA RECORRÊNCIA"
        textSize = 12f
        typeface = DSSFont.bold(context, 12f).typeface
        maxLines = 1
    }
    private val descriptionLabel = TextView(context).apply {
        text = "Realize a recarga via PIX e não fique sem internet!"
        textSize = 14f
        typeface = DSSFont.regular(context, 14f).typeface
        gravity = Gravity.START
    }

    private val resumeCard = DSSResumeCard(context).apply {
        // iOS: setCategoryLabels(number: "Número", offer: "Plano", price: "Valor").
        setCategoryLabels(number = "Número", offer = "Plano", price = "Valor")
        // iOS: setTitleFont(DSSFont.medium(14)).
        titleFont = DSSFont.medium(context, 14f).typeface
        // iOS: borderWidth = 0.
        borderWidthDp = 0f
    }

    private val copyButton = AppCompatButton(context).apply {
        text = "Copiar código"
        textSize = 16f
        typeface = DSSFont.medium(context, 16f).typeface
        isAllCaps = false
    }

    /** Override opcional do ícone. null => usa o asset `empty_recurrency` da brand (como o iOS). */
    var iconDrawable: android.graphics.drawable.Drawable? = null
        set(value) {
            field = value
            iconImageView.setImageDrawable(value ?: ImageLoader.image(context, "empty_recurrency"))
        }

    var cornerRadiusDp: Float = 12f
        set(value) { field = value; refresh() }

    private var pixCode: String? = null

    // Overrides de estilo (configureStyle). iOS preserva essas cores em troca de tema.
    @ColorInt private var titleColorOverride: Int? = null
    @ColorInt private var buttonColorOverride: Int? = null

    private val handler = Handler(Looper.getMainLooper())

    init {
        addView(
            container,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT),
        )
        val pad = 16f.dpToPx(context)
        container.setPadding(pad, pad, pad, pad)

        // Header row (ícone 24x24 + título), spacing 5.
        headerRow.addView(
            iconImageView,
            LinearLayout.LayoutParams(24f.dpToPx(context), 24f.dpToPx(context)).apply {
                marginEnd = 5f.dpToPx(context)
            },
        )
        headerRow.addView(
            titleLabel,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )
        container.addView(
            headerRow,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )

        // iOS: description topAnchor = header.bottom + 8.
        container.addView(
            descriptionLabel,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = 8f.dpToPx(context) },
        )

        // iOS: resumeCard topAnchor = description.bottom (sem margem extra; o próprio
        // DSSResumeCard já aplica padding interno de 16 + topMargin do row).
        container.addView(
            resumeCard,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )

        // iOS: copyButton top = resumeCard.bottom + 20, altura 50, cornerRadius 25.
        container.addView(
            copyButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                50f.dpToPx(context),
            ).apply { topMargin = 20f.dpToPx(context) },
        )

        container.setOnClickListener { onCardTapped?.invoke() }
        copyButton.setOnClickListener { onCopyTapped() }

        refresh()
        setupThemeObserver()
    }

    /**
     * Configura o card com os dados da falha de recorrência (espelha `configure` do iOS).
     * @param priceInCents valor em centavos.
     * @param pixCode código PIX que será copiado pelo botão "Copiar código".
     */
    fun configure(
        msisdn: String,
        planName: String,
        priceInCents: Int,
        pixCode: String? = null,
    ) {
        this.pixCode = pixCode
        // iOS reconfigura o resume card com title vazio ("").
        resumeCard.configure(title = "", number = msisdn, offer = planName, priceInCents = priceInCents)
    }

    /**
     * Configura o card com estilo customizado (espelha `configureStyle` do iOS).
     * Cada parâmetro nulo é ignorado, preservando o valor atual.
     * @param titleColor cor do título (ColorInt).
     * @param buttonColor cor do texto e da borda do botão de copiar (ColorInt).
     */
    fun configureStyle(
        titleText: String? = null,
        @ColorInt titleColor: Int? = null,
        descriptionText: String? = null,
        buttonTitle: String? = null,
        @ColorInt buttonColor: Int? = null,
    ) {
        if (titleText != null) titleLabel.text = titleText
        if (titleColor != null) {
            titleColorOverride = titleColor
            titleLabel.setTextColor(titleColor)
        }
        if (descriptionText != null) descriptionLabel.text = descriptionText
        if (buttonTitle != null) copyButton.text = buttonTitle
        if (buttonColor != null) {
            buttonColorOverride = buttonColor
            copyButton.setTextColor(buttonColor)
            copyButton.background = DrawableFactory.rounded(
                context = context,
                backgroundColor = Color.TRANSPARENT,
                cornerRadiusDp = 25f,
                strokeColor = buttonColor,
                strokeWidthDp = 2f,
            )
        }
    }

    /** Mostra ou esconde o botão de copiar (iOS: setCopyButtonVisible). */
    fun setCopyButtonVisible(visible: Boolean) {
        copyButton.visibility = if (visible) View.VISIBLE else View.GONE
    }

    /** Mostra ou esconde o card de informações (iOS: setResumeCardVisible). */
    fun setResumeCardVisible(visible: Boolean) {
        resumeCard.visibility = if (visible) View.VISIBLE else View.GONE
    }

    override fun applyTheme(theme: Theme) { refresh() }

    private fun refresh() {
        // iOS applyContainerColors():
        //  .black  -> backgroundColor secundário, SEM borda
        //  .dark   -> backgroundColor secundário, borda 2pt systemGray3 (#48484A)
        //  .light  -> backgroundColor padrão, SEM borda
        val isDark = ThemeManager.colorScheme == ColorScheme.DARK
        background = DrawableFactory.rounded(
            context = context,
            backgroundColor = DSSColors.surface(),
            cornerRadiusDp = cornerRadiusDp,
            // iOS: borda só no DARK (systemGray3 = #48484A, 2pt). LIGHT/BLACK sem borda.
            strokeColor = if (isDark) SYSTEM_GRAY3_DARK else null,
            strokeWidthDp = if (isDark) 2f else 0f,
        )

        // Título: vermelho-escuro literal do iOS UIColor(red:0.65, green:0.16, blue:0.16) = #A62929
        // (cor literal, não semântica). Override de configureStyle tem prioridade.
        titleLabel.setTextColor(titleColorOverride ?: DARK_RED)
        // iOS: description fica branca em dark/black, preta em light → token textPrimary.
        descriptionLabel.setTextColor(DSSColors.textPrimary())

        val buttonColor = buttonColorOverride ?: DARK_RED
        copyButton.setTextColor(buttonColor)
        copyButton.background = DrawableFactory.rounded(
            context = context,
            backgroundColor = Color.TRANSPARENT,
            cornerRadiusDp = 25f,
            strokeColor = buttonColor,
            strokeWidthDp = 2f,
        )
    }

    private fun onCopyTapped() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Pix", pixCode ?: "Código PIX copiado"))

        val originalText = copyButton.text
        copyButton.text = "Pix copiado"
        copyButton.isEnabled = false
        handler.postDelayed({
            copyButton.text = originalText
            copyButton.isEnabled = true
        }, 2_000L)

        onCopyCodeTapped?.invoke()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        handler.removeCallbacksAndMessages(null)
    }

    companion object {
        // iOS UIColor(red: 0.65, green: 0.16, blue: 0.16, alpha: 1.0) = #A62929 — literal, não token.
        @ColorInt private val DARK_RED: Int = Color.rgb(166, 41, 41)

        // iOS UIColor.systemGray3 no dark mode = #48484A — borda do container só no scheme DARK.
        @ColorInt private val SYSTEM_GRAY3_DARK: Int = Color.rgb(72, 72, 74)

        /** Card de falha de recorrência com estilo padrão (iOS: defaultStyle()). */
        @JvmStatic
        fun defaultStyle(context: Context): DSSRecurrencyFailureCardView =
            DSSRecurrencyFailureCardView(context).apply { cornerRadiusDp = 12f }
    }
}
