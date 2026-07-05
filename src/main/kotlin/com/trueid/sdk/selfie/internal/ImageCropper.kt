package com.trueid.sdk.selfie.internal

import android.graphics.Bitmap
import android.graphics.RectF
import com.trueid.sdk.selfie.util.ImageUtils
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Passport-style face cropping.
 *
 * Produces a 3:4 portrait crop centered on the detected face,
 * resized to [outputWidth]x[outputHeight]. The crop is intentionally
 * slightly tighter than the original mobile implementation so the face
 * occupies a safer portion of the final frame for server-side liveness.
 */
internal object ImageCropper {

    fun cropToPassportStyle(
        jpegBytes: ByteArray,
        faceBoundsHint: RectF?,
        outputWidth: Int = 600,
        outputHeight: Int = 800,
        faceTopRatio: Float = 0.38f,
        jpegQuality: Int = 94,
    ): ByteArray {
        val decoded = ImageUtils.decodeWithOrientation(jpegBytes)
        val imgW = decoded.width.toFloat()
        val imgH = decoded.height.toFloat()

        var faceCX = imgW / 2f
        var faceCY = imgH * faceTopRatio
        var faceH = imgH * 0.32f

        if (faceBoundsHint != null &&
            faceBoundsHint.width() > 0 && faceBoundsHint.height() > 0
        ) {
            val scaledLeft = faceBoundsHint.left * imgW
            val scaledTop = faceBoundsHint.top * imgH
            val scaledW = faceBoundsHint.width() * imgW
            val scaledH = faceBoundsHint.height() * imgH

            faceCX = (scaledLeft + scaledW / 2f).coerceIn(0f, imgW)
            faceCY = (scaledTop + scaledH / 2f).coerceIn(0f, imgH)
            faceH = scaledH.coerceIn(imgH * 0.12f, imgH * 0.82f)
        }

        val cropH = (faceH * 2.2f).coerceIn(imgH * 0.24f, imgH)
        val cropW = min(cropH * 3f / 4f, imgW)
        val adjustedCropH = min(cropW * 4f / 3f, imgH)

        var cropX = faceCX - cropW / 2f
        var cropY = faceCY - adjustedCropH * faceTopRatio

        cropX = cropX.coerceIn(0f, max(0f, imgW - cropW))
        cropY = cropY.coerceIn(0f, max(0f, imgH - adjustedCropH))

        val cw = max(1, min(cropW, imgW - cropX).roundToInt())
        val ch = max(1, min(adjustedCropH, imgH - cropY).roundToInt())

        val cropped = Bitmap.createBitmap(
            decoded,
            cropX.roundToInt().coerceIn(0, (decoded.width - cw).coerceAtLeast(0)),
            cropY.roundToInt().coerceIn(0, (decoded.height - ch).coerceAtLeast(0)),
            cw,
            ch
        )
        if (cropped !== decoded) decoded.recycle()

        val scaled = Bitmap.createScaledBitmap(cropped, outputWidth, outputHeight, true)
        if (scaled !== cropped) cropped.recycle()

        val outputStream = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, jpegQuality, outputStream)
        scaled.recycle()

        return outputStream.toByteArray()
    }
}
