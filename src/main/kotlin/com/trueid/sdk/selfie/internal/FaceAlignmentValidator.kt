package com.trueid.sdk.selfie.internal

import com.trueid.sdk.selfie.CaptureMode
import com.trueid.sdk.selfie.R
import com.trueid.sdk.selfie.SelfieCaptureConfig

internal enum class FaceGuideStatus {
    LOADING, NO_FACE, FACE_DETECTED, FACE_CENTERED, ERROR
}

internal data class AlignmentResult(
    val status: FaceGuideStatus,
    val guidanceResId: Int,
)

internal class FaceAlignmentValidator(private val config: SelfieCaptureConfig) {

    fun evaluate(
        faceHeightRatio: Float,
        centerXRatio: Float,
        centerYRatio: Float,
    ): AlignmentResult {
        val centered =
            (centerXRatio - 0.5f).let { kotlin.math.abs(it) } < config.centerTolerance &&
            (centerYRatio - 0.5f).let { kotlin.math.abs(it) } < config.centerTolerance

        val tooFar = faceHeightRatio < config.minFaceHeightRatio
        val tooClose = faceHeightRatio > config.maxFaceHeightRatio

        if (!centered && tooFar) {
            return AlignmentResult(FaceGuideStatus.FACE_DETECTED, R.string.trueid_guidance_closer_center)
        }
        if (!centered && tooClose) {
            return AlignmentResult(FaceGuideStatus.FACE_DETECTED, R.string.trueid_guidance_back_center)
        }
        if (tooFar) {
            return AlignmentResult(FaceGuideStatus.FACE_DETECTED, R.string.trueid_guidance_closer)
        }
        if (tooClose) {
            return AlignmentResult(FaceGuideStatus.FACE_DETECTED, R.string.trueid_guidance_back)
        }
        if (!centered) {
            return AlignmentResult(FaceGuideStatus.FACE_DETECTED, R.string.trueid_guidance_center)
        }

        // Face is centered and at correct distance
        val guidanceRes = if (config.captureMode == CaptureMode.AUTO) {
            R.string.trueid_guidance_hold_still
        } else {
            R.string.trueid_guidance_capture_ready
        }
        return AlignmentResult(FaceGuideStatus.FACE_CENTERED, guidanceRes)
    }
}
