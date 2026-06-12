package com.surf.surfhubds.components

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
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
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.surf.surfhubds.font.DSSFont
import com.surf.surfhubds.util.DrawableFactory
import com.surf.surfhubds.util.dpToPx
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.regex.Pattern

/**
 * Port do `DSSCardScannerViewController` do iOS.
 *
 * No iOS este componente abre a câmera com overlay em formato de cartão e usa
 * Vision (`VNRecognizeTextRequest`) para fazer OCR do texto do cartão, extraindo
 * PAN, validade, CVV e nome a partir das *bounding boxes* de cada linha. Na versão
 * Android usamos o equivalente: `CameraX` (preview + análise de frames) +
 * `ML Kit Text Recognition` (OCR no dispositivo). O ML Kit também entrega o texto
 * e a bounding box de cada linha, então conseguimos reproduzir **toda** a lógica do
 * iOS — inclusive o filtro espacial do nome (nome deve ficar abaixo do número).
 *
 * Cada frame da câmera é convertido para [InputImage] e processado pelo OCR. As
 * linhas reconhecidas viram [OcrObject]s (texto + retângulo "em pé" + confiança) e
 * passam pelos mesmos extratores ([extractCardNumber] com Luhn, [extractExpiry],
 * [extractCVVFromLine], [likelyName]) e pela mesma máquina de estabilização por
 * votação ([requiredConfirmations] confirmações consecutivas do mesmo número,
 * votação de nome por frequência). Quando confirmado, a borda fica verde, o título
 * vira "Cartão detectado!" e o resultado é entregue após 0.8s — igual iOS.
 *
 * Resultado: o caller inicia via [Intent] e recebe em `onActivityResult` um
 * [DSSScannedCardData] (Parcelable) com o que foi reconhecido.
 */
class DSSCardScannerActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var cardFrame: View
    private lateinit var instructionLabel: TextView

    private val recognizer: TextRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }
    private lateinit var cameraExecutor: ExecutorService
    private var camera: Camera? = null

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

    /** Objeto OCR com texto, bounding box "em pé" e confiança (igual OcrObject do iOS). */
    private data class OcrObject(val text: String, val rect: RectF, val confidence: Float)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()

        val root = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            setBackgroundColor(Color.BLACK)
        }

        previewView = PreviewView(this).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
        root.addView(
            previewView,
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
                val cam = camera ?: return@setOnClickListener
                if (cam.cameraInfo.hasFlashUnit().not()) return@setOnClickListener
                torchOn = !torchOn
                cam.cameraControl.enableTorch(torchOn)
                setImageResource(
                    if (torchOn) android.R.drawable.ic_lock_idle_charging
                    else android.R.drawable.ic_lock_idle_low_battery,
                )
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

        // Permissão de câmera: se já concedida, inicia; senão pede (iOS mostra alerta de permissão).
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera()
            } else {
                setResult(Activity.RESULT_CANCELED)
                finish()
            }
        }
    }

    private fun startCamera() {
        val providerFuture = ProcessCameraProvider.getInstance(this)
        providerFuture.addListener({
            val cameraProvider = providerFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { it.setAnalyzer(cameraExecutor, CardAnalyzer()) }

            cameraProvider.unbindAll()
            camera = cameraProvider.bindToLifecycle(
                this,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                analysis,
            )
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onDestroy() {
        mainHandler.removeCallbacksAndMessages(null)
        if (this::cameraExecutor.isInitialized) cameraExecutor.shutdown()
        recognizer.close()
        super.onDestroy()
    }

    private fun statusBarHeightPx(): Int {
        val id = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (id > 0) resources.getDimensionPixelSize(id) else 24f.dpToPx(this)
    }

    // MARK: - Analyzer (frames da câmera → OCR)

    private inner class CardAnalyzer : ImageAnalysis.Analyzer {
        @ExperimentalGetImage
        override fun analyze(imageProxy: ImageProxy) {
            val mediaImage = imageProxy.image
            if (mediaImage == null || hasDeliveredResult) {
                imageProxy.close()
                return
            }
            val rotation = imageProxy.imageInfo.rotationDegrees
            val input = InputImage.fromMediaImage(mediaImage, rotation)
            // Dimensões do buffer (sem rotação) — usadas para colocar as boxes "em pé".
            val bufferW = mediaImage.width
            val bufferH = mediaImage.height
            recognizer.process(input)
                .addOnSuccessListener { text ->
                    handleOcrResult(text, rotation, bufferW, bufferH)
                }
                .addOnCompleteListener { imageProxy.close() }
        }
    }

    /** Converte o resultado do ML Kit em [OcrObject]s e roda a análise do iOS. */
    private fun handleOcrResult(text: Text, rotation: Int, bufferW: Int, bufferH: Int) {
        if (hasDeliveredResult) return
        val objects = ArrayList<OcrObject>()
        for (block in text.textBlocks) {
            for (line in block.lines) {
                val box = line.boundingBox ?: continue
                objects.add(
                    OcrObject(
                        text = line.text,
                        rect = uprightRect(box, rotation, bufferW, bufferH),
                        confidence = line.confidence ?: 1f,
                    ),
                )
            }
        }
        analyzeWithBoundingBoxes(objects)
    }

    /**
     * Converte uma bounding box do espaço do buffer (sem rotação) para o espaço
     * "em pé" (origem no topo-esquerda, y crescendo para baixo) — mesma orientação
     * que o iOS usa após converter as coordenadas normalizadas do Vision. Sem isso,
     * em sensores rotacionados 90°/270° o eixo Y trocaria com o X e o filtro espacial
     * do nome ("abaixo do número") não funcionaria.
     */
    private fun uprightRect(r: Rect, rotation: Int, w: Int, h: Int): RectF = when (rotation) {
        90 -> RectF(
            (h - r.bottom).toFloat(), r.left.toFloat(),
            (h - r.top).toFloat(), r.right.toFloat(),
        )
        180 -> RectF(
            (w - r.right).toFloat(), (h - r.bottom).toFloat(),
            (w - r.left).toFloat(), (h - r.top).toFloat(),
        )
        270 -> RectF(
            r.top.toFloat(), (w - r.right).toFloat(),
            r.bottom.toFloat(), (w - r.left).toFloat(),
        )
        else -> RectF(r)
    }

    // MARK: - OCR Processing (mesma lógica do iOS analyzeWithBoundingBoxes)

    /**
     * Análise principal usando bounding boxes (idêntica ao iOS): encontra número
     * (com Luhn), validade (mantendo a maior), CVV (por keyword) e filtra o nome por
     * posição espacial — o nome deve estar abaixo do número do cartão.
     */
    private fun analyzeWithBoundingBoxes(objects: List<OcrObject>) {
        if (hasDeliveredResult) return

        var foundNumber: String? = null
        var numberBox: RectF? = null
        var foundExpiryMonth: String? = null
        var foundExpiryYear: String? = null
        var expiryBox: RectF? = null
        var foundCVV: String? = null
        val nameCandidates = ArrayList<OcrObject>()

        // 1. Primeiro pass: número, data e CVV + coletar candidatos a nome
        for (obj in objects) {
            val text = obj.text

            if (foundNumber == null) {
                extractCardNumber(text)?.let {
                    foundNumber = it
                    numberBox = obj.rect
                }
            }

            val expiry = extractExpiry(text)
            if (expiry != null) {
                if (foundExpiryMonth == null) {
                    foundExpiryMonth = expiry.first
                    foundExpiryYear = expiry.second
                    expiryBox = obj.rect
                } else {
                    // Se encontrou outra data, pegar a maior (validade real) — igual iOS
                    val oldValue =
                        (foundExpiryYear?.toIntOrNull() ?: 0) * 100 + (foundExpiryMonth?.toIntOrNull() ?: 0)
                    val newValue =
                        (expiry.second.toIntOrNull() ?: 0) * 100 + (expiry.first.toIntOrNull() ?: 0)
                    if (newValue > oldValue) {
                        foundExpiryMonth = expiry.first
                        foundExpiryYear = expiry.second
                        expiryBox = obj.rect
                    }
                }
            }

            if (foundCVV == null) {
                extractCVVFromLine(text)?.let { foundCVV = it }
            }

            if (likelyName(text) != null) {
                nameCandidates.add(obj)
            }
        }

        // 2. Filtrar nome por posição espacial (CHAVE do Stripe/iOS!)
        // O nome deve estar ABAIXO do número (minY = topo do número - altura, ou topo da data).
        val minY: Float? = numberBox?.let { it.top - it.height() } ?: expiryBox?.top
        var foundName: String? = null

        val validNames = nameCandidates.filter { candidate ->
            val isInExpectedLocation = minY?.let { candidate.rect.top >= (it - 5f) } ?: true
            candidate.confidence >= 0.5f && isInExpectedLocation
        }
        validNames.firstOrNull()?.let { foundName = likelyName(it.text) }

        // Pelo menos o número do cartão precisa ser encontrado
        val number = foundNumber ?: return

        // Verificar estabilidade: mesmo número por N frames consecutivos (igual iOS)
        if (number == candidateNumber) {
            confirmationCount += 1
        } else {
            candidateNumber = number
            candidateExpiry = null
            candidateCVV = null
            nameVotes.clear()
            confirmationCount = 1
        }

        // Atualizar dados extras
        if (foundExpiryMonth != null && foundExpiryYear != null) {
            candidateExpiry = foundExpiryMonth!! to foundExpiryYear!!
        }
        if (foundCVV != null) candidateCVV = foundCVV

        // Votação de nomes por frequência (igual iOS ErrorCorrection)
        foundName?.let { nameVotes[it] = (nameVotes[it] ?: 0) + 1 }

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
        mainHandler.post {
            cardFrame.background = DrawableFactory.rounded(
                context = this,
                backgroundColor = Color.TRANSPARENT,
                cornerRadiusDp = 12f,
                strokeColor = SYSTEM_GREEN,
                strokeWidthDp = 2f,
            )
            instructionLabel.text = "Cartão detectado!"
            mainHandler.postDelayed({ deliverResult(scannedData) }, 800L)
        }
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
        private const val REQUEST_CAMERA = 4711

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
 * Dados extraídos do cartão via OCR (paralelo iOS `DSSScannedCardData`).
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
