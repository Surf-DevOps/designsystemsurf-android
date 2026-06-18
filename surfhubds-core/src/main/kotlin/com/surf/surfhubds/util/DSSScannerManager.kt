package com.surf.surfhubds.util

import com.google.zxing.BarcodeFormat
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.journeyapps.barcodescanner.DefaultDecoderFactory

/**
 * Port do `DSSScannerManager.swift` — wrapper sobre a `DecoratedBarcodeView` da
 * journeyapps/zxing-android-embedded.
 *
 * Uso típico:
 * ```
 * val scanner = DSSScannerManager(decoratedBarcodeView)
 * scanner.delegate = object : DSSScannerManager.Delegate {
 *     override fun didFind(code: String) { ... }
 * }
 * scanner.validator = { code -> code.length in 19..20 }
 * scanner.startScanning(DSSScannerType.BarCode)
 * ```
 */
class DSSScannerManager(private val view: DecoratedBarcodeView) {

    enum class DSSScannerType { QrCode, BarCode }

    interface Delegate {
        fun didFind(code: String)
    }

    var delegate: Delegate? = null

    /** Closure de validação. Retorna true pra aceitar o código. */
    var validator: ((String) -> Boolean)? = null

    /** Transformação aplicada no payload antes de validar. Default: remove letras. */
    var payloadTransform: ((String) -> String)? = { it.filter { c -> !c.isLetter() } }

    private var isReading = false

    fun startScanning(type: DSSScannerType, onError: ((String) -> Unit)? = null) {
        val formats = when (type) {
            DSSScannerType.QrCode -> listOf(BarcodeFormat.QR_CODE)
            DSSScannerType.BarCode -> listOf(
                BarcodeFormat.EAN_13,
                BarcodeFormat.CODE_128,
                BarcodeFormat.CODE_39,
            )
        }
        view.barcodeView.decoderFactory = DefaultDecoderFactory(formats)
        view.decodeContinuous(object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult) {
                if (!isReading) return
                val raw = result.text ?: return
                val transformed = payloadTransform?.invoke(raw) ?: raw
                if (validator?.invoke(transformed) == false) return
                isReading = false
                delegate?.didFind(transformed)
                view.pause()
            }
            override fun possibleResultPoints(resultPoints: MutableList<ResultPoint>?) = Unit
        })
        try {
            view.resume()
            isReading = true
        } catch (e: Exception) { onError?.invoke(e.localizedMessage ?: "Falha ao abrir câmera.") }
    }

    fun resetScanner() {
        isReading = true
        view.resume()
    }

    fun stopScanning() {
        isReading = false
        view.pause()
    }
}
