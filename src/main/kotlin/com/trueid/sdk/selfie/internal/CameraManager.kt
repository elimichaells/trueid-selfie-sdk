package com.trueid.sdk.selfie.internal

import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.trueid.sdk.selfie.CameraFacing
import com.trueid.sdk.selfie.overlay.CircularPreviewLayout
import java.util.concurrent.Executors

internal class CameraManager(
    private val lifecycleOwner: LifecycleOwner,
    private val previewContainer: CircularPreviewLayout,
    private var facing: CameraFacing,
    private val analyzer: ImageAnalysis.Analyzer,
) {
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private val analysisExecutor = Executors.newSingleThreadExecutor()
    private val previewView: PreviewView

    val isFrontCamera: Boolean get() = facing == CameraFacing.FRONT

    init {
        previewView = PreviewView(previewContainer.context).apply {
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            )
            implementationMode = PreviewView.ImplementationMode.PERFORMANCE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
        previewContainer.addView(previewView, 0)
    }

    fun start() {
        val context = previewContainer.context
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindUseCases()
        }, ContextCompat.getMainExecutor(context))
    }

    fun switchCamera() {
        facing = if (facing == CameraFacing.FRONT) CameraFacing.BACK else CameraFacing.FRONT
        (analyzer as? FaceAnalyzer)?.setFrontCamera(isFrontCamera)
        bindUseCases()
    }

    fun capturePhoto(callback: (ByteArray?) -> Unit) {
        val capture = imageCapture ?: run {
            callback(null)
            return
        }
        capture.takePicture(
            ContextCompat.getMainExecutor(previewContainer.context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val buffer = image.planes[0].buffer
                    val bytes = ByteArray(buffer.remaining())
                    buffer.get(bytes)
                    image.close()
                    callback(bytes)
                }

                override fun onError(exception: ImageCaptureException) {
                    callback(null)
                }
            }
        )
    }

    fun release() {
        cameraProvider?.unbindAll()
        analysisExecutor.shutdown()
    }

    private fun bindUseCases() {
        val provider = cameraProvider ?: return

        provider.unbindAll()

        val cameraSelector = if (facing == CameraFacing.FRONT) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }

        val preview = Preview.Builder()
            .build()
            .also { it.surfaceProvider = previewView.surfaceProvider }

        val analysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(480, 640))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { it.setAnalyzer(analysisExecutor, analyzer) }

        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()

        provider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview,
            analysis,
            imageCapture!!
        )
    }
}
