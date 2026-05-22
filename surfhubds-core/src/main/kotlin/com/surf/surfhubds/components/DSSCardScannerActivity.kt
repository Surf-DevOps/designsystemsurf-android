package com.surf.surfhubds.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.view.Gravity
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
import com.surf.surfhubds.util.dpToPx

/**
 * Port do `DSSCardScannerViewController` do iOS.
 *
 * No iOS, este componente abre a câmera com overlay em formato de cartão e usa
 * Vision (OCR) para extrair PAN, validade, CVV e nome. Na primeira versão Android
 * usamos `journeyapps:zxing-android-embedded` para abrir a câmera com overlay
 * idêntico (cartão central + título). Para barras/QR a leitura é nativa do ZXing;
 * para OCR de PAN, exposemos o frame de cartão para que callers possam integrar
 * ML Kit Text Recognition em cima — mas por padrão devolvemos só [code] quando
 * ZXing reconhece um padrão.
 *
 * Resultado: o caller inicia via [Intent] e recebe em `onActivityResult` um
 * [DSSScannedCardData] (Parcelable) com o que foi reconhecido (geralmente só
 * `number` quando o scanner detecta um código).
 */
class DSSCardScannerActivity : AppCompatActivity() {

    private lateinit var barcodeView: DecoratedBarcodeView
    private var hasDelivered = false

    private val callback = object : BarcodeCallback {
        override fun barcodeResult(result: BarcodeResult?) {
            if (hasDelivered || result == null) return
            val text = result.text ?: return
            val digits = text.filter(Char::isDigit)
            if (digits.length in 13..19 && luhnCheck(digits)) {
                hasDelivered = true
                deliverResult(DSSScannedCardData(number = digits))
            }
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
            statusView.visibility = android.view.View.GONE
            setStatusText(null)
        }
        root.addView(
            barcodeView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ),
        )

        // Overlay: cartão branco no centro
        val cardFrame = android.view.View(this).apply {
            background = com.surf.surfhubds.util.DrawableFactory.rounded(
                context = this@DSSCardScannerActivity,
                backgroundColor = Color.TRANSPARENT,
                cornerRadiusDp = 12f,
                strokeColor = Color.WHITE,
                strokeWidthDp = 2f,
            )
        }
        // Cartão padrão: 1.586:1, ~80% da largura
        val screenWidth = resources.displayMetrics.widthPixels
        val cardWidth = (screenWidth * 0.85f).toInt() - (24f.dpToPx(this) * 2)
        val cardHeight = (cardWidth / 1.586f).toInt()
        root.addView(
            cardFrame,
            FrameLayout.LayoutParams(cardWidth, cardHeight, Gravity.CENTER),
        )

        val instructionLabel = TextView(this).apply {
            text = "Posicione o cartão dentro da área"
            setTextColor(Color.WHITE)
            textSize = 16f
            typeface = DSSFont.medium(this@DSSCardScannerActivity, 16f).typeface
            gravity = Gravity.CENTER
        }
        root.addView(
            instructionLabel,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL,
            ).apply {
                bottomMargin = 120f.dpToPx(this@DSSCardScannerActivity)
            },
        )

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
                topMargin = 32f.dpToPx(this@DSSCardScannerActivity)
                leftMargin = 16f.dpToPx(this@DSSCardScannerActivity)
            },
        )

        val flashButton = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_dialog_info)
            background = null
            setColorFilter(Color.WHITE)
            var torchOn = false
            setOnClickListener {
                torchOn = !torchOn
                if (torchOn) barcodeView.setTorchOn() else barcodeView.setTorchOff()
            }
        }
        root.addView(
            flashButton,
            FrameLayout.LayoutParams(
                36f.dpToPx(this), 36f.dpToPx(this),
                Gravity.TOP or Gravity.END,
            ).apply {
                topMargin = 32f.dpToPx(this@DSSCardScannerActivity)
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

    private fun deliverResult(data: DSSScannedCardData) {
        val intent = Intent().apply {
            putExtra(EXTRA_RESULT, data)
        }
        setResult(Activity.RESULT_OK, intent)
        finish()
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

    companion object {
        const val EXTRA_RESULT = "DSS_SCANNED_CARD_DATA"

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
 * Na versão Android atual normalmente só o [number] é preenchido pelo ZXing;
 * os outros campos ficam reservados para uma integração futura com OCR (ML Kit).
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
