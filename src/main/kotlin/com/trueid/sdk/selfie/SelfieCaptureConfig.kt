package com.trueid.sdk.selfie

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

enum class CaptureMode { AUTO, MANUAL }
enum class CameraFacing { FRONT, BACK }
enum class ResultFormat { BYTE_ARRAY, FILE_PATH, BASE64, ALL }

@Parcelize
data class SelfieCaptureConfig(
    val captureMode: CaptureMode = CaptureMode.AUTO,
    val initialCamera: CameraFacing = CameraFacing.FRONT,
    val allowCameraSwitch: Boolean = true,
    val theme: SelfieCaptureTheme = SelfieCaptureTheme(),

    // Face alignment thresholds
    val minFaceHeightRatio: Float = 0.30f,
    val maxFaceHeightRatio: Float = 0.82f,
    val centerTolerance: Float = 0.22f,

    // Ring animation
    val ringSegmentCount: Int = 40,
    val ringTickMs: Long = 40L,
    val ringFillPerTick: Int = 3,

    // Output tuned for server-side liveness and face comparison
    val outputWidth: Int = 600,
    val outputHeight: Int = 800,
    val jpegQuality: Int = 94,
    val faceTopPositionRatio: Float = 0.38f,

    // Additional frames captured around the principal selfie for liveness
    val burstFrameCount: Int = 4,
    val burstFrameDelayMs: Long = 90L,

    // Result format
    val resultFormat: ResultFormat = ResultFormat.BYTE_ARRAY,

    // Contour mesh overlay
    val showFaceMesh: Boolean = true,
) : Parcelable
