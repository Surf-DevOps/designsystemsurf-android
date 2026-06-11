package com.surf.surfhubds.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Parcel
import android.os.Parcelable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.BarcodeFormat
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import com.surf.surfhubds.font.DSSFont
import com.surf.surfhubds.util.DrawableFactory
import com.surf.surfhubds.util.dpToPx
import java.util.regex.Pattern

/**
 * Port do `DSSCardScannerViewController` do iOS.
 *
 * No iOS, este componente abre a câmera com overlay em formato de cartão e usa
 * Vision (OCR) para extrair PAN, validade, CVV e nome via bounding boxes. Na
 * versão Android usamos `journeyapps:zxing-android-embedded` para abrir a câmera
 * com o mesmo overlay (cartão central + título + máscara escurecida com recorte).
 *
 * O ZXing entrega o texto decodificado de um frame por vez. Para manter a lógica
 * de negócio fiel ao iOS, cada texto decodificado é tratado como um "frame" e
 * passa pelos mesmos extratores ([extractCardNumber] com Luhn, [extractExpiry],
 * [extractCVVFromLine], [likelyName]) e pela mesma máquina de estabilização por
 * votação ([requiredConfirmations] confirmações consecutivas do mesmo número,
 * votação de nome por frequência). Quando confirmado, a borda fica verde, o
 * título vira "Cartão detectado!" e o resultado é entregue após 0.8s — igual iOS.
 *
 * Resultado: o caller inicia via [Intent] e recebe em `onActivityResult` um
 * [DSSScannedCardData] (Parcelable) com o que foi reconhecido.
 */
class DSSCardScannerActivity : AppCompatActivity() {

    private lateinit var barcodeView: DecoratedBarcodeView
    private lateinit var cardFrame: View
    private lateinit var instructionLabel: TextView

    // MARK: - OCR state (igual iOS)
    private var hasDeliveredResult = false

    // Acumuladores para estabilizar a leitura (votação por frames, igual Stripe ErrorCorrection)
    private var candidateNumber: String? = null
    private var candidateExpiry: Pair<String, String>? = null // (month, year)
    private var candidateCVV: String? = null
    private val nameVotes = HashMap<String, Int>()
    private var confirmationCount = 0
    private val requiredConfirmations = 4

    private val mainHandler = Handler(Looper.getMainLooper())

    private val callback = object : BarcodeCallback {
        override fun barcodeResult(result: BarcodeResult?) {
            if (hasDeliveredResult || result == null) return
            val text = result.text ?: return
            analyzeText(text)
        }

        override fun possibleResultPoints(resultPoints: MutableList<ResultPoint>?) = Unit
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            setBackgroundColor(Color.BLACK)
        }

        barcodeView = DecoratedBarcodeView(this).apply {
            barcodeView.decoderFactory = DefaultDecoderFactory(
                listOf(
                    BarcodeFormat.CODE_39,
                    BarcodeFormat.CODE_128,
                    BarcodeFormat.EAN_13,
                    BarcodeFormat.ITF,
                    BarcodeFormat.QR_CODE,
                    BarcodeFormat.PDF_417,
                ),
            )
            statusView.text = ""
            statusView.visibility = View.GONE
            setStatusText(null)
        }
        root.addView(
            barcodeView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ),
        )

        // Cartão padrão: proporção 1.586:1, largura = tela - 24dp de cada lado (igual iOS leading/trailing 24)
        val screenWidth = resources.displayMetrics.widthPixels
        val cardWidth = screenWidth - (24f.dpToPx(this) * 2)
        val cardHeight = (cardWidth / 1.586f).toInt()
        // iOS: centerY = view.centerY - 40 → desloca o cartão 40pt para cima
        val cardVerticalOffset = -(40f.dpToPx(this))

        // Máscara escurecida (preto 0.6) com recorte do cartão (igual iOS updateOverlayMask)
        val overlay = OverlayMaskView(
            context = this,
            cardWidthPx = cardWidth,
            cardHeightPx = cardHeight,
            cardVerticalOffsetPx = cardVerticalOffset,
            cornerRadiusPx = 12f.dpToPx(this).toFloat(),
        )
        root.addView(
            overlay,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ),
        )

        // Overlay: borda branca do cartão no centro
        cardFrame = View(this).apply {
            background = DrawableFactory.rounded(
                context = this@DSSCardScannerActivity,
                backgroundColor = Color.TRANSPARENT,
                cornerRadiusDp = 12f,
                strokeColor = Color.WHITE,
                strokeWidthDp = 2f,
            )
        }
        root.addView(
            cardFrame,
            FrameLayout.LayoutParams(cardWidth, cardHeight, Gravity.CENTER).apply {
                topMargin = cardVerticalOffset
            },
        )

        instructionLabel = TextView(this).apply {
            text = "Posicione o cartão dentro da área"
            setTextColor(Color.WHITE)
            textSize = 16f
            typeface = DSSFont.medium(this@DSSCardScannerActivity, 16f).typeface
            gravity = Gravity.CENTER
        }
        // iOS: instructionLabel.top = cardFrame.bottom + 24 (e centrado em X).
        // bottom do cartão = centerY do root + offset + cardHeight/2.
        val instructionTopMargin =
            (resources.displayMetrics.heightPixels / 2) + cardVerticalOffset +
                (cardHeight / 2) + 24f.dpToPx(this)
        root.addView(
            instructionLabel,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP or Gravity.CENTER_HORIZONTAL,
            ).apply {
                topMargin = instructionTopMargin
            },
        )

        // iOS: top = safeArea.top + 16. Aproximamos com status bar + 16dp.
        val topInset = statusBarHeightPx() + 16f.dpToPx(this)

        val closeButton = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            background = null
            setColorFilter(Color.WHITE)
            setOnClickListener {
                setResult(Activity.RESULT_CANCELED)
                finish()
            }
        }
        root.addView(
            closeButton,
            FrameLayout.LayoutParams(
                36f.dpToPx(this), 36f.dpToPx(this),
                Gravity.TOP or Gravity.START,
            ).apply {
                topMargin = topInset
                leftMargin = 16f.dpToPx(this@DSSCardScannerActivity)
            },
        )

        // iOS: começa com "bolt.slash.fill" (flash desligado) e alterna para "bolt.fill" ao ligar.
        val flashButton = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_lock_idle_low_battery)
            background = null
            setColorFilter(Color.WHITE)
            var torchOn = false
            setOnClickListener {
                torchOn = !torchOn
                if (torchOn) {
                    barcodeView.setTorchOn()
                    setImageResource(android.R.drawable.ic_lock_idle_charging)
                } else {
                    barcodeView.setTorchOff()
                    setImageResource(android.R.drawable.ic_lock_idle_low_battery)
                }
                setColorFilter(Color.WHITE)
            }
        }
        root.addView(
            flashButton,
            FrameLayout.LayoutParams(
                36f.dpToPx(this), 36f.dpToPx(this),
                Gravity.TOP or Gravity.END,
            ).apply {
                topMargin = topInset
                rightMargin = 16f.dpToPx(this@DSSCardScannerActivity)
            },
        )

        setContentView(root)

        barcodeView.decodeContinuous(callback)
    }

    override fun onResume() {
        super.onResume()
        barcodeView.resume()
    }

    override fun onPause() {
        super.onPause()
        barcodeView.pause()
    }

    override fun onDestroy() {
        mainHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private fun statusBarHeightPx(): Int {
        val id = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (id > 0) resources.getDimensionPixelSize(id) else 24f.dpToPx(this)
    }

    // MARK: - OCR Processing (mesma lógica do iOS analyzeWithBoundingBoxes, sobre o texto do ZXing)

    /**
     * Aplica os mesmos extratores do iOS ao texto decodificado e roda a máquina de
     * estabilização por votação. O ZXing não fornece bounding boxes, então o filtro
     * espacial do nome do iOS não se aplica; o restante da lógica (número + Luhn,
     * validade maior, CVV por keyword, votação de nome, N confirmações) é fiel.
     */
    private fun analyzeText(text: String) {
        if (hasDeliveredResult) return

        val foundNumber = extractCardNumber(text)
        var foundExpiry = extractExpiry(text)
        val foundCVV = extractCVVFromLine(text)
        val foundName = likelyName(text)

        // Pelo menos o número do cartão precisa ser encontrado
        if (foundNumber == null) return

        // Verificar estabilidade: mesmo número por N frames consecutivos (igual iOS)
        if (foundNumber == candidateNumber) {
            confirmationCount += 1
        } else {
            candidateNumber = foundNumber
            candidateExpiry = null
            candidateCVV = null
            nameVotes.clear()
            confirmationCount = 1
        }

        // Atualizar dados extras
        if (foundExpiry != null) {
            // Se já havia uma data, manter a maior (validade real) — igual iOS
            val current = candidateExpiry
            if (current != null) {
                val oldValue = (current.second.toIntOrNull() ?: 0) * 100 + (current.first.toIntOrNull() ?: 0)
                val newValue = (foundExpiry.second.toIntOrNull() ?: 0) * 100 + (foundExpiry.first.toIntOrNull() ?: 0)
                if (newValue <= oldValue) foundExpiry = current
            }
            candidateExpiry = foundExpiry
        }
        if (foundCVV != null) candidateCVV = foundCVV

        // Votação de nomes por frequência (igual iOS ErrorCorrection)
        if (foundName != null) {
            nameVotes[foundName] = (nameVotes[foundName] ?: 0) + 1
        }

        if (confirmationCount < requiredConfirmations) return

        hasDeliveredResult = true

        // Nome mais votado (igual iOS: max by count)
        val bestName = nameVotes.maxByOrNull { it.value }?.key

        val scannedData = DSSScannedCardData(
            number = candidateNumber,
            expiryMonth = candidateExpiry?.first,
            expiryYear = candidateExpiry?.second,
            cvv = candidateCVV,
            holderName = bestName,
        )

        // Feedback de sucesso (igual iOS): borda verde + título + delay de 0.8s
        // iOS usa UIColor.systemGreen (literal), não token semântico → #34C759.
        cardFrame.background = DrawableFactory.rounded(
            context = this,
            backgroundColor = Color.TRANSPARENT,
            cornerRadiusDp = 12f,
            strokeColor = SYSTEM_GREEN,
            strokeWidthDp = 2f,
        )
        instructionLabel.text = "Cartão detectado!"

        mainHandler.postDelayed({
            deliverResult(scannedData)
        }, 800L)
    }

    private fun deliverResult(data: DSSScannedCardData) {
        val intent = Intent().apply {
            putExtra(EXTRA_RESULT, data)
        }
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    // MARK: - Extraction helpers (portados do iOS)

    /** Número do cartão: 13-19 dígitos + validação Luhn (igual iOS extractCardNumber). */
    private fun extractCardNumber(text: String): String? {
        val digits = text.filter(Char::isDigit)
        if (digits.length !in 13..19) return null
        if (!luhnCheck(digits)) return null
        return digits
    }

    /** Validade: extrai MM/YY de uma linha (igual iOS extractExpiry). Retorna (month, year). */
    private fun extractExpiry(text: String): Pair<String, String>? {
        val matcher = EXPIRY_PATTERN.matcher(text)
        if (!matcher.find()) return null
        val month = matcher.group(1) ?: return null
        var year = matcher.group(2) ?: return null
        if (year.length == 4) year = year.substring(year.length - 2)
        return month to year
    }

    /** CVV: 3-4 dígitos SOMENTE de linhas com keywords de segurança (igual iOS extractCVVFromLine). */
    private fun extractCVVFromLine(text: String): String? {
        val lowerText = text.lowercase()

        // Ignorar linhas com "service"
        if (lowerText.contains("service")) return null

        val hasKeyword = CVV_KEYWORDS.any { lowerText.contains(it) }
        if (!hasKeyword) return null

        val matcher = CVV_DIGIT_PATTERN.matcher(text)
        while (matcher.find()) {
            val candidate = matcher.group(1) ?: continue
            if (candidate.length == 4 && (candidate.startsWith("20") || candidate.startsWith("19"))) continue
            val allDigits = text.filter(Char::isDigit)
            if (allDigits.length > 6) continue
            return candidate
        }
        return null
    }

    /**
     * Nome: filtra por palavra (igual iOS likelyName + NonNameWords).
     * Cada palavra deve ser SOMENTE letras maiúsculas (ou '.'), não pode ser uma
     * non-name word, e precisa ter 2+ palavras válidas.
     */
    private fun likelyName(text: String): String? {
        val words = text.split(" ").filter { it.isNotEmpty() }

        val validWords = words.filter { word ->
            if (NON_NAME_WORDS.contains(word.lowercase())) return@filter false
            val isAllUppercase = word.all { (it in 'A'..'Z') || it == ' ' || it == '.' }
            isAllUppercase && word.length >= 2
        }

        if (validWords.size < 2) return null
        return validWords.joinToString(" ")
    }

    private fun luhnCheck(number: String): Boolean {
        var sum = 0
        for ((i, ch) in number.reversed().withIndex()) {
            val d = ch.digitToIntOrNull() ?: return false
            if (i % 2 == 1) {
                val doubled = d * 2
                sum += if (doubled > 9) doubled - 9 else doubled
            } else {
                sum += d
            }
        }
        return sum % 10 == 0
    }

    /** View que escurece a tela inteira com um recorte do cartão (igual iOS updateOverlayMask). */
    private class OverlayMaskView(
        context: Context,
        private val cardWidthPx: Int,
        private val cardHeightPx: Int,
        private val cardVerticalOffsetPx: Int,
        private val cornerRadiusPx: Float,
    ) : View(context) {

        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb((0.6f * 255).toInt(), 0, 0, 0)
        }

        init {
            isClickable = false
            isFocusable = false
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val path = Path().apply { fillType = Path.FillType.EVEN_ODD }
            // Retângulo de tela inteira
            path.addRect(0f, 0f, width.toFloat(), height.toFloat(), Path.Direction.CW)
            // Recorte do cartão centralizado, deslocado verticalmente igual iOS
            val left = (width - cardWidthPx) / 2f
            val centerY = height / 2f + cardVerticalOffsetPx
            val top = centerY - cardHeightPx / 2f
            val cardRect = RectF(left, top, left + cardWidthPx, top + cardHeightPx)
            path.addRoundRect(cardRect, cornerRadiusPx, cornerRadiusPx, Path.Direction.CW)
            canvas.drawPath(path, paint)
        }
    }

    companion object {
        const val EXTRA_RESULT = "DSS_SCANNED_CARD_DATA"

        // iOS UIColor.systemGreen (borda do cartão ao detectar)
        private val SYSTEM_GREEN = Color.parseColor("#34C759")

        // Validade MM/YY (igual iOS regex)
        private val EXPIRY_PATTERN: Pattern =
            Pattern.compile("(0[1-9]|1[0-2])\\s?[/\\-.]\\s?(\\d{2,4})")

        // CVV: 3-4 dígitos
        private val CVV_DIGIT_PATTERN: Pattern = Pattern.compile("(\\d{3,4})")

        private val CVV_KEYWORDS = listOf(
            "cvv", "cvc", "cód. segurança", "cod. segurança", "cód segurança",
            "cod seguranca", "código de segurança", "segurança", "seguranca",
            "security code", "security",
        )

        // Non-name words (baseado no Stripe NonNameWords.swift) — igual iOS
        private val NON_NAME_WORDS: Set<String> = setOf(
            "customer", "debit", "visa", "mastercard", "navy", "american", "express", "thru", "good",
            "authorized", "signature", "wells", "credit", "federal", "union", "bank", "valid",
            "validfrom", "validthru", "llc", "business", "netspend", "goodthru", "chase", "fargo",
            "hsbc", "usaa", "commerce", "last", "of", "lastdayof", "check", "card", "inc", "first",
            "member", "since", "republic", "bmo", "capital", "one", "capitalone", "platinum",
            "expiry", "date", "expiration", "cash", "back", "td", "access", "international", "interac",
            "nterac", "entreprise", "enterprise", "fifth", "third", "fifththird", "world", "rewards",
            "citi", "cardmember", "cardholder", "valued", "membersince", "cardmembersince",
            "cardholdersince", "freedom", "quicksilver", "penfed", "use", "this", "is", "subject",
            "to", "the", "not", "transferable", "sign", "gold", "black", "classic", "standard",
            "premium", "infinite", "desde", "validade", "mes", "ano", "month", "year", "from",
            "expires", "end", "contactless", "chip", "maestro", "elo", "hipercard", "amex",
            "carrefour", "nubank", "bradesco", "itau", "santander", "safra", "original", "pan",
            "caixa", "inter", "pague", "band", "sports", "celular",
        )

        fun newIntent(context: Context): Intent {
            return Intent(context, DSSCardScannerActivity::class.java)
        }

        fun extractResult(intent: Intent?): DSSScannedCardData? {
            if (intent == null) return null
            @Suppress("DEPRECATION")
            return intent.getParcelableExtra(EXTRA_RESULT)
        }
    }
}

/**
 * Dados extraídos do cartão (paralelo iOS `DSSScannedCardData`).
 *
 * Na versão Android atual o [number] é preenchido pelo ZXing; validade, CVV e nome
 * são preenchidos quando o texto decodificado contém esses padrões (mesma lógica iOS).
 */
data class DSSScannedCardData(
    val number: String? = null,
    val expiryMonth: String? = null,
    val expiryYear: String? = null,
    val cvv: String? = null,
    val holderName: String? = null,
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(number)
        parcel.writeString(expiryMonth)
        parcel.writeString(expiryYear)
        parcel.writeString(cvv)
        parcel.writeString(holderName)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<DSSScannedCardData> {
        override fun createFromParcel(parcel: Parcel): DSSScannedCardData = DSSScannedCardData(parcel)
        override fun newArray(size: Int): Array<DSSScannedCardData?> = arrayOfNulls(size)
    }
}
