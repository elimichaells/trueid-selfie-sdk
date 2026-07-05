package com.trueid.sdk.selfie.internal

import android.os.Handler
import android.os.Looper
import com.trueid.sdk.selfie.CaptureMode
import com.trueid.sdk.selfie.R
import com.trueid.sdk.selfie.SelfieCaptureConfig

internal data class CaptureState(
    val phase: CapturePhase = CapturePhase.LIVE,
    val faceStatus: FaceGuideStatus = FaceGuideStatus.LOADING,
    val litSegments: Int = 0,
    val guidance: String = "",
    val guidanceResId: Int = R.string.trueid_guidance_position,
)

internal enum class CapturePhase { LIVE, REVIEW }

/**
 * Manages the selfie capture lifecycle: face status debouncing,
 * ring fill animation, and auto-capture triggering.
 *
 * Port of Flutter's ring fill timer + debounce logic.
 */
internal class CaptureStateMachine(
    private val config: SelfieCaptureConfig,
    private val onCapture: () -> Unit,
    private val onStateChanged: (CaptureState) -> Unit,
) {
    private val handler = Handler(Looper.getMainLooper())

    private var phase = CapturePhase.LIVE
    private var faceStatus = FaceGuideStatus.LOADING
    private var litSegments = 0
    private var pendingStatus: FaceGuideStatus? = null
    private var debounceCount = 0
    private var ringFillRunnable: Runnable? = null
    private var hasTriggeredCapture = false

    companion object {
        private const val DEBOUNCE_FRAMES = 2
    }

    fun onFaceAnalysisResult(result: FaceAnalysisResult) {
        if (phase != CapturePhase.LIVE || hasTriggeredCapture) return

        val newStatus = result.status

        // Debounce status changes over 2 frames to prevent flickering
        if (newStatus != faceStatus) {
            if (pendingStatus == newStatus) {
                debounceCount++
                if (debounceCount >= DEBOUNCE_FRAMES) {
                    faceStatus = newStatus
                    pendingStatus = null
                    debounceCount = 0
                    onStatusChanged(result.guidanceResId)
                }
            } else {
                pendingStatus = newStatus
                debounceCount = 1
            }
        } else {
            pendingStatus = null
            debounceCount = 0
        }

        emitState(result.guidanceResId)
    }

    private fun onStatusChanged(guidanceResId: Int) {
        if (faceStatus == FaceGuideStatus.FACE_CENTERED) {
            startRingFill()
        } else {
            stopRingFill()
            // Reset ring when face leaves centered
            litSegments = 0
        }
    }

    private fun startRingFill() {
        if (ringFillRunnable != null) return

        val runnable = object : Runnable {
            override fun run() {
                if (faceStatus != FaceGuideStatus.FACE_CENTERED || phase != CapturePhase.LIVE) {
                    stopRingFill()
                    return
                }

                litSegments = minOf(config.ringSegmentCount, litSegments + config.ringFillPerTick)
                emitState(R.string.trueid_guidance_hold_still)

                if (litSegments >= config.ringSegmentCount) {
                    // Ring full — trigger capture
                    stopRingFill()
                    if (config.captureMode == CaptureMode.AUTO && !hasTriggeredCapture) {
                        hasTriggeredCapture = true
                        onCapture()
                    }
                } else {
                    handler.postDelayed(this, config.ringTickMs)
                }
            }
        }

        ringFillRunnable = runnable
        handler.postDelayed(runnable, config.ringTickMs)
    }

    private fun stopRingFill() {
        ringFillRunnable?.let { handler.removeCallbacks(it) }
        ringFillRunnable = null
    }

    private fun emitState(guidanceResId: Int) {
        onStateChanged(
            CaptureState(
                phase = phase,
                faceStatus = faceStatus,
                litSegments = litSegments,
                guidanceResId = guidanceResId,
            )
        )
    }

    fun reset() {
        stopRingFill()
        phase = CapturePhase.LIVE
        faceStatus = FaceGuideStatus.LOADING
        litSegments = 0
        hasTriggeredCapture = false
        pendingStatus = null
        debounceCount = 0
    }

    fun release() {
        stopRingFill()
        handler.removeCallbacksAndMessages(null)
    }
}
