package com.surf.surfhubds.components

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * Port da extensão Swift `UIImage+QRCode` para Kotlin.
 *
 * Gera QR Codes via ZXing — substitui `CIQRCodeGenerator` do iOS. Resultado em
 * [Bitmap] preto/branco que pode ser exibido em um `ImageView` direto.
 */
object QRCodeImage {

    /**
     * Gera um QR Code padrão (mesma assinatura do iOS `generateQRCode(from:)`).
     *
     * @param content texto a codificar.
     * @param sizePx tamanho final do bitmap em pixels (padrão 512 — alta qualidade
     *   no Android porque telas têm densidade variada e o iOS usava 3x da
     *   matrix interna do CIFilter).
     */
    fun generate(content: String, sizePx: Int = 512): Bitmap? {
        if (content.isEmpty()) return null
        return runCatching {
            val writer = QRCodeWriter()
            val hints = mapOf(
                EncodeHintType.MARGIN to 1,
                EncodeHintType.CHARACTER_SET to "UTF-8",
            )
            val matrix = writer.encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
            val w = matrix.width
            val h = matrix.height
            val pixels = IntArray(w * h)
            for (y in 0 until h) {
                val offset = y * w
                for (x in 0 until w) {
                    pixels[offset + x] = if (matrix.get(x, y)) Color.BLACK else Color.WHITE
                }
            }
            Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply {
                setPixels(pixels, 0, w, 0, 0, w, h)
            }
        }.getOrNull()
    }

    /**
     * Gera um QR Code de alta qualidade com error correction level "H"
     * (equivalente ao `generateHighQualityQRCode(from:size:)` do iOS).
     *
     * @param content texto a codificar.
     * @param widthPx largura final em pixels.
     * @param heightPx altura final em pixels.
     */
    fun generateHighQuality(
        content: String,
        widthPx: Int = 512,
        heightPx: Int = 512,
    ): Bitmap? {
        if (content.isEmpty()) return null
        return runCatching {
            val writer = QRCodeWriter()
            val hints = mapOf(
                EncodeHintType.MARGIN to 1,
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H,
                EncodeHintType.CHARACTER_SET to "UTF-8",
            )
            val matrix = writer.encode(content, BarcodeFormat.QR_CODE, widthPx, heightPx, hints)
            val w = matrix.width
            val h = matrix.height
            val pixels = IntArray(w * h)
            for (y in 0 until h) {
                val offset = y * w
                for (x in 0 until w) {
                    pixels[offset + x] = if (matrix.get(x, y)) Color.BLACK else Color.WHITE
                }
            }
            Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply {
                setPixels(pixels, 0, w, 0, 0, w, h)
            }
        }.getOrNull()
    }
}
