package com.mystockmanager.app.core

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import androidx.core.graphics.set

object QRCodeGenerator {
    /**
     * Generates a QR code bitmap from the given [text].
     *
     * @param text The string to be encoded into the QR code.
     * @param size The width and height of the generated bitmap in pixels. Defaults to 512.
     * @return A [Bitmap] representing the QR code.
     */
    fun generate(text: String, size: Int = 512): Bitmap {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, size, size)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap[x, y] = if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE
            }
        }
        return bitmap
    }
}
