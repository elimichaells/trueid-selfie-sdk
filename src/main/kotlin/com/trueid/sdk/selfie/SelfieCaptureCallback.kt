package com.trueid.sdk.selfie

interface SelfieCaptureCallback {
    fun onCaptured(result: SelfieCaptureResult)
    fun onCancelled()
    fun onError(error: SelfieCaptureError)
}

sealed class SelfieCaptureError(val message: String) {
    class CameraUnavailable(message: String = "Camera is not available") : SelfieCaptureError(message)
    class PermissionDenied(message: String = "Camera permission denied") : SelfieCaptureError(message)
    class ProcessingFailed(message: String = "Image processing failed") : SelfieCaptureError(message)
}
