package com.surf.surfhubds.components

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.UnderlineSpan
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt
import androidx.appcompat.widget.AppCompatTextView
import com.surf.surfhubds.font.DSSFont
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.theme.Theme
import com.surf.surfhubds.theme.ThemeAware
import com.surf.surfhubds.theme.setupThemeObserver

/**
 * Port do `TextWithActionLinkView` do iOS.
 *
 * TextView que renderiza um texto completo com um trecho clicável (link de ação).
 * O toque no link aciona [onLinkTap]. Não há navegação real — o link é só um gancho
 * para a callback.
 */
class TextWithActionLinkView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AppCompatTextView(context, attrs, defStyleAttr), ThemeAware {

    /** Callback acionada ao tocar no link. */
    var onLinkTap: (() -> Unit)? = null

    /**
     * Fonte padrão do componente, equivalente ao `resolvedFont` do iOS
     * (`DSSFont.light(14)`). Usada quando [configure] é chamado sem `typeface`.
     */
    val resolvedFont: Typeface
        get() = DSSFont.light(context, 14f).typeface

    private var fullText: String = ""
    private var linkText: String = ""
    @ColorInt private var customTextColor: Int? = null
    @ColorInt private var customLinkColor: Int? = null
    private var customTypeface: Typeface? = null
    private var customSizeSp: Float = 14f

    init {
        movementMethod = LinkMovementMethod.getInstance()
        highlightColor = 0x00000000 // sem highlight cinza
        // iOS: textContainerInset = .zero + lineFragmentPadding = 0 (UITextView sem padding, parece label).
        setPadding(0, 0, 0, 0)
        includeFontPadding = false
        // iOS bloqueia seleção/handles (selectedTextRange = nil, long press desativado).
        setTextIsSelectable(false)
        setupThemeObserver()
    }

    /**
     * Configura o texto e o range do link.
     *
     * @param fullText texto completo.
     * @param linkText substring do [fullText] que vira link.
     * @param typeface fonte aplicada ao texto inteiro. Se null usa `DSSFont.light(14)`.
     * @param sizeSp tamanho de fonte em SP.
     * @param textColor cor do texto normal (default = [DSSColors.textPrimary]).
     * @param linkColor cor do link (default = [DSSColors.primary]).
     */
    fun configure(
        fullText: String,
        linkText: String,
        typeface: Typeface? = null,
        sizeSp: Float = 14f,
        @ColorInt textColor: Int? = null,
        @ColorInt linkColor: Int? = null,
    ) {
        this.fullText = fullText
        this.linkText = linkText
        this.customTypeface = typeface
        this.customSizeSp = sizeSp
        this.customTextColor = textColor
        this.customLinkColor = linkColor

        textSize = sizeSp
        setTypeface(typeface ?: DSSFont.light(context, sizeSp).typeface)
        // iOS força múltiplas linhas (textContainer.maximumNumberOfLines = 0).
        maxLines = Int.MAX_VALUE
        refresh()
    }

    override fun applyTheme(theme: Theme) {
        refresh()
    }

    private fun refresh() {
        val resolvedTextColor = customTextColor ?: DSSColors.textPrimary()
        val resolvedLinkColor = customLinkColor ?: DSSColors.primary()

        setTextColor(resolvedTextColor)

        if (fullText.isEmpty()) {
            text = ""
            return
        }

        val span = SpannableString(fullText)
        val start = fullText.indexOf(linkText)
        if (start >= 0 && linkText.isNotEmpty()) {
            val end = start + linkText.length
            span.setSpan(
                object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        onLinkTap?.invoke()
                    }
                },
                start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
            span.setSpan(
                ForegroundColorSpan(resolvedLinkColor),
                start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
            span.setSpan(UnderlineSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        text = span
    }
}
