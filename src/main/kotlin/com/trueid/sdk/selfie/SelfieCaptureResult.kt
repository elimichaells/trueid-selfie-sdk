package com.trueid.sdk.selfie

import android.graphics.RectF
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SelfieCaptureResult(
    val imageBytes: ByteArray?,
    val filePath: String?,
    val base64: String?,
    val burstFrames: List<String>?,
    val width: Int,
    val height: Int,
    val faceBounds: RectF?,
    val captureTimestamp: Long,
) : Parcelable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SelfieCaptureResult) return false
        return captureTimestamp == other.captureTimestamp &&
            width == other.width &&
            height == other.height &&
            filePath == other.filePath &&
            base64 == other.base64 &&
            burstFrames == other.burstFrames &&
            faceBounds == other.faceBounds &&
            imageBytes.contentEquals(other.imageBytes)
    }

    override fun hashCode(): Int {
        var result = imageBytes?.contentHashCode() ?: 0
        result = 31 * result + (filePath?.hashCode() ?: 0)
        result = 31 * result + (base64?.hashCode() ?: 0)
        result = 31 * result + (burstFrames?.hashCode() ?: 0)
        result = 31 * result + width
        result = 31 * result + height
        result = 31 * result + (faceBounds?.hashCode() ?: 0)
        result = 31 * result + captureTimestamp.hashCode()
        return result
    }
}
