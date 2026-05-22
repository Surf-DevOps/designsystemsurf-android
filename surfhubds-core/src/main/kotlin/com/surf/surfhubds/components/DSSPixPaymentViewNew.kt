package com.surf.surfhubds.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.surf.surfhubds.font.DSSFont
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.theme.Theme
import com.surf.surfhubds.theme.ThemeAware
import com.surf.surfhubds.theme.setupThemeObserver
import com.surf.surfhubds.util.DrawableFactory
import com.surf.surfhubds.util.dpToPx

/**
 * Configuração de exibição da [DSSPixPaymentViewNew].
 */
data class PixPaymentConfig(
    val number: String,
    val offer: String,
    val priceInCents: Int,
    val pixCode: String,
    val title: String = "Quase lá!",
    val subtitle: String = "Faça o pagamento do código Pix para finalizar sua recarga",
)

/**
 * Callback equivalente ao `DSSPixPaymentViewDelegate` do iOS.
 */
interface DSSPixPaymentViewDelegate {
    fun pixPaymentViewDidTapCopyCode(view: DSSPixPaymentViewNew, code: String)
}

/**
 * Port do `DSSPixPaymentViewNew` do iOS — view rolável com:
 * título, subtítulo, resumo (DSSPixInfoCardView), container de QR code, código PIX em texto,
 * botão "Copiar código", instruções e label de alerta.
 *
 * Render do QR code usa ZXing via [BarcodeEncoder].
 */
class DSSPixPaymentViewNew @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr), ThemeAware {

    var delegate: DSSPixPaymentViewDelegate? = null

    private val scrollView = ScrollView(context).apply { isVerticalScrollBarEnabled = false }
    private val mainStack = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
    }

    private val titleLabel = TextView(context).apply {
        textSize = 24f
        typeface = DSSFont.bold(context, 24f).typeface
        setSingleLine(false)
    }
    private val subtitleLabel = TextView(context).apply {
        textSize = 16f
        typeface = DSSFont.regular(context, 16f).typeface
        setSingleLine(false)
    }

    private val resumeCard = DSSResumeCard(context)

    private val paymentDetailsLabel = TextView(context).apply {
        text = "Detalhes do pagamento"
        textSize = 18f
        typeface = DSSFont.medium(context, 18f).typeface
        gravity = Gravity.CENTER_HORIZONTAL
    }

    private val pixContainer = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
    }
    private val pixTitleLabel = TextView(context).apply {
        text = "Pague com Pix"
        textSize = 16f
        typeface = DSSFont.medium(context, 16f).typeface
        gravity = Gravity.CENTER_HORIZONTAL
    }
    private val qrCodeImageView = ImageView(context).apply {
        scaleType = ImageView.ScaleType.FIT_CENTER
        setBackgroundColor(Color.WHITE)
    }
    private val pixCodeLabel = TextView(context).apply {
        textSize = 12f
        typeface = DSSFont.regular(context, 12f).typeface
        gravity = Gravity.CENTER_HORIZONTAL
        setSingleLine(false)
    }

    private val copyButton = AppCompatButton(context).apply {
        text = "Copiar código"
        textSize = 16f
        typeface = DSSFont.medium(context, 16f).typeface
        isAllCaps = false
        setTextColor(Color.WHITE)
    }

    private val instructionsStack = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
    }

    private val warningLabel = TextView(context).apply {
        text = "Importante: O prazo limite para pagamento do Pix é de 3 horas, " +
                "após este prazo seu pedido será cancelado"
        textSize = 14f
        typeface = DSSFont.regular(context, 14f).typeface
        setSingleLine(false)
    }

    private var currentPixCode: String = ""
    private val handler = Handler(Looper.getMainLooper())

    init {
        addView(
            scrollView,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT),
        )
        scrollView.addView(
            mainStack,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
            ),
        )

        val hPad = 16f.dpToPx(context)
        val vPad = 20f.dpToPx(context)
        mainStack.setPadding(hPad, vPad, hPad, vPad)

        val spacing = 24f.dpToPx(context)

        mainStack.addView(titleLabel, spacedRow(0))
        mainStack.addView(subtitleLabel, spacedRow(spacing))
        mainStack.addView(resumeCard, spacedRow(spacing))
        mainStack.addView(paymentDetailsLabel, spacedRow(spacing))
        mainStack.addView(pixContainer, spacedRow(spacing))
        mainStack.addView(
            copyButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                48f.dpToPx(context),
            ).apply { topMargin = spacing },
        )
        mainStack.addView(instructionsStack, spacedRow(spacing))
        mainStack.addView(warningLabel, spacedRow(spacing))

        setupPixContainer()
        setupInstructions()

        copyButton.setOnClickListener { onCopyTapped() }

        refresh()
        setupThemeObserver()
    }

    fun configure(config: PixPaymentConfig) {
        titleLabel.text = config.title
        subtitleLabel.text = config.subtitle

        resumeCard.configure(
            number = config.number,
            offer = config.offer,
            priceInCents = config.priceInCents,
        )

        currentPixCode = config.pixCode
        pixCodeLabel.text = config.pixCode
        qrCodeImageView.setImageBitmap(generateQrCode(config.pixCode))
    }

    override fun applyTheme(theme: Theme) { refresh() }

    private fun setupPixContainer() {
        val pad = 16f.dpToPx(context)
        pixContainer.setPadding(pad, pad, pad, pad)

        pixContainer.addView(
            pixTitleLabel,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )
        pixContainer.addView(
            qrCodeImageView,
            LinearLayout.LayoutParams(
                200f.dpToPx(context),
                200f.dpToPx(context),
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = 16f.dpToPx(context)
            },
        )
        pixContainer.addView(
            pixCodeLabel,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = 16f.dpToPx(context) },
        )
    }

    private fun setupInstructions() {
        val instructions = listOf(
            "1- Abra o aplicativo ou internet banking no seu celular;",
            "2- Na opção Pix, escolher \"Ler QR Code\";",
            "3- Aponte a câmera do seu celular para o QR Code ao lado ou, se preferir, " +
                "copie o código para a Pix copia e cola;",
            "4- Revise as informações e confirme o pagamento. Pronto! " +
                "O status do pedido será atualizado na mesma hora;",
        )
        for ((i, line) in instructions.withIndex()) {
            val tv = TextView(context).apply {
                text = line
                textSize = 14f
                typeface = DSSFont.regular(context, 14f).typeface
                setSingleLine(false)
            }
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { if (i > 0) topMargin = 12f.dpToPx(context) }
            instructionsStack.addView(tv, lp)
        }
    }

    private fun onCopyTapped() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Pix", currentPixCode))
        delegate?.pixPaymentViewDidTapCopyCode(this, currentPixCode)

        val originalText = copyButton.text
        copyButton.text = "Código copiado!"
        val originalBg = copyButton.background
        copyButton.background = DrawableFactory.rounded(
            context = context,
            backgroundColor = DSSColors.success(),
            cornerRadiusDp = 8f,
        )

        handler.postDelayed({
            copyButton.text = originalText
            copyButton.background = originalBg
        }, 2_000L)
    }

    private fun refresh() {
        setBackgroundColor(DSSColors.background())

        titleLabel.setTextColor(DSSColors.textPrimary())
        subtitleLabel.setTextColor(DSSColors.textSecondary())
        paymentDetailsLabel.setTextColor(DSSColors.textPrimary())
        pixTitleLabel.setTextColor(DSSColors.textPrimary())
        pixCodeLabel.setTextColor(DSSColors.textSecondary())
        warningLabel.setTextColor(DSSColors.error())

        pixContainer.background = DrawableFactory.rounded(
            context = context,
            backgroundColor = DSSColors.backgroundSecondary(),
            cornerRadiusDp = 12f,
        )

        copyButton.background = DrawableFactory.rounded(
            context = context,
            backgroundColor = DSSColors.primary(),
            cornerRadiusDp = 8f,
        )
        copyButton.setTextColor(DSSColors.buttonText())

        instructionsStack.children().forEach { v ->
            if (v is TextView) v.setTextColor(DSSColors.textSecondary())
        }
    }

    private fun spacedRow(topPx: Int): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = topPx }

    private fun LinearLayout.children(): Sequence<View> =
        (0 until childCount).asSequence().map { getChildAt(it) }

    private fun generateQrCode(text: String): Bitmap? {
        if (text.isEmpty()) return null
        return try {
            val size = 400
            BarcodeEncoder().encodeBitmap(text, BarcodeFormat.QR_CODE, size, size)
        } catch (_: Throwable) {
            null
        }
    }
}
