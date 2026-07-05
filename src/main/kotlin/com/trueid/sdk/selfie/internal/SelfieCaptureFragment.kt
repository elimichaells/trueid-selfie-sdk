package com.trueid.sdk.selfie.internal

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.RectF
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.trueid.sdk.selfie.CaptureMode
import com.trueid.sdk.selfie.R
import com.trueid.sdk.selfie.ResultFormat
import com.trueid.sdk.selfie.SelfieCaptureConfig
import com.trueid.sdk.selfie.SelfieCaptureError
import com.trueid.sdk.selfie.SelfieCaptureResult
import com.trueid.sdk.selfie.overlay.CircularPreviewLayout
import com.trueid.sdk.selfie.overlay.FaceMeshOverlayView
import com.trueid.sdk.selfie.overlay.GuidanceOverlayView
import com.trueid.sdk.selfie.overlay.SegmentRingView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

internal class SelfieCaptureFragment : Fragment() {

    companion object {
        const val RESULT_KEY = "selfie_capture_result"
        const val RESULT_DATA = "result"
        private const val ARG_CONFIG = "config"

        fun newInstance(config: SelfieCaptureConfig): SelfieCaptureFragment {
            return SelfieCaptureFragment().apply {
                arguments = bundleOf(ARG_CONFIG to config)
            }
        }
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    private lateinit var config: SelfieCaptureConfig
    private var cameraManager: CameraManager? = null
    private var faceAnalyzer: FaceAnalyzer? = null
    private lateinit var stateMachine: CaptureStateMachine

    private lateinit var ringView: SegmentRingView
    private lateinit var previewLayout: CircularPreviewLayout
    private lateinit var meshOverlay: FaceMeshOverlayView
    private lateinit var guidanceView: GuidanceOverlayView
    private lateinit var guidanceText: TextView
    private var captureButton: Button? = null
    private var switchCameraButton: ImageButton? = null
    private var closeButton: ImageButton? = null

    private var lastFaceBounds: RectF? = null
    private var isCapturing = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startCamera()
        } else {
            deliverError(SelfieCaptureError.PermissionDenied())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        config = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable(ARG_CONFIG, SelfieCaptureConfig::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getParcelable(ARG_CONFIG)
        } ?: SelfieCaptureConfig()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_selfie_capture, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ringView = view.findViewById(R.id.ringView)
        previewLayout = view.findViewById(R.id.previewLayout)
        meshOverlay = view.findViewById(R.id.meshOverlay)
        guidanceView = view.findViewById(R.id.guidanceView)
        guidanceText = view.findViewById(R.id.guidanceText)
        captureButton = view.findViewById(R.id.captureButton)
        switchCameraButton = view.findViewById(R.id.switchCameraButton)
        closeButton = view.findViewById(R.id.closeButton)

        ringView.setColors(
            config.theme.ringActiveColor,
            config.theme.ringInactiveColor,
            config.theme.ringGlowColor
        )
        ringView.segmentCount = config.ringSegmentCount

        captureButton?.visibility =
            if (config.captureMode == CaptureMode.MANUAL) View.VISIBLE else View.GONE

        captureButton?.setOnClickListener { triggerCapture() }
        switchCameraButton?.visibility =
            if (config.allowCameraSwitch) View.VISIBLE else View.GONE
        switchCameraButton?.setOnClickListener { cameraManager?.switchCamera() }
        closeButton?.setOnClickListener { deliverCancelled() }

        stateMachine = CaptureStateMachine(
            config = config,
            onCapture = { triggerCapture() },
            onStateChanged = { state ->
                activity?.runOnUiThread {
                    ringView.litSegments = state.litSegments
                    guidanceView.setStatus(state.faceStatus)
                    guidanceText.setText(state.guidanceResId)

                    val textColor = if (state.faceStatus == FaceGuideStatus.FACE_CENTERED) {
                        config.theme.ringActiveColor
                    } else {
                        resources.getColor(R.color.trueid_guidance_text, null)
                    }
                    guidanceText.setTextColor(textColor)

                    if (config.captureMode == CaptureMode.MANUAL) {
                        captureButton?.isEnabled =
                            state.faceStatus == FaceGuideStatus.FACE_CENTERED ||
                                state.faceStatus == FaceGuideStatus.FACE_DETECTED
                    }
                }
            }
        )

        checkCameraPermission()
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> startCamera()
            else -> permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val analyzer = FaceAnalyzer(
            showMesh = config.showFaceMesh,
            validator = FaceAlignmentValidator(config),
        ) { result ->
            lastFaceBounds = result.displayBounds
            stateMachine.onFaceAnalysisResult(result)

            activity?.runOnUiThread {
                if (config.showFaceMesh) {
                    meshOverlay.setMeshStrokes(result.meshStrokes, result.status)
                }
            }
        }
        faceAnalyzer = analyzer
        analyzer.setFrontCamera(config.initialCamera == com.trueid.sdk.selfie.CameraFacing.FRONT)

        cameraManager = CameraManager(
            lifecycleOwner = viewLifecycleOwner,
            previewContainer = previewLayout,
            facing = config.initialCamera,
            analyzer = analyzer,
        )
        cameraManager?.start()
    }

    private fun triggerCapture() {
        if (isCapturing) return
        isCapturing = true

        val totalFrames = 1 + config.burstFrameCount.coerceAtLeast(0)
        captureSequence(
            totalFrames = totalFrames,
            delayMs = config.burstFrameDelayMs.coerceAtLeast(0L),
            onComplete = { frames ->
                if (frames.isEmpty()) {
                    isCapturing = false
                    deliverError(SelfieCaptureError.ProcessingFailed())
                    return@captureSequence
                }

                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        val croppedFrames = withContext(Dispatchers.Default) {
                            frames.map { jpegBytes ->
                                ImageCropper.cropToPassportStyle(
                                    jpegBytes = jpegBytes,
                                    faceBoundsHint = lastFaceBounds,
                                    outputWidth = config.outputWidth,
                                    outputHeight = config.outputHeight,
                                    faceTopRatio = config.faceTopPositionRatio,
                                    jpegQuality = config.jpegQuality,
                                )
                            }
                        }

                        val principal = croppedFrames.first()
                        val bursts = croppedFrames.drop(1)
                        val result = buildResult(principal, bursts)
                        deliverResult(result)
                    } catch (_: Exception) {
                        isCapturing = false
                        deliverError(SelfieCaptureError.ProcessingFailed())
                    }
                }
            },
            onFailure = {
                isCapturing = false
                deliverError(SelfieCaptureError.ProcessingFailed())
            }
        )
    }

    private fun captureSequence(
        totalFrames: Int,
        delayMs: Long,
        onComplete: (List<ByteArray>) -> Unit,
        onFailure: () -> Unit,
    ) {
        val manager = cameraManager ?: run {
            onFailure()
            return
        }
        val frames = ArrayList<ByteArray>(totalFrames)

        fun captureNext(index: Int) {
            manager.capturePhoto { jpegBytes ->
                if (jpegBytes == null) {
                    onFailure()
                    return@capturePhoto
                }
                frames.add(jpegBytes)
                if (index + 1 >= totalFrames) {
                    onComplete(frames)
                    return@capturePhoto
                }
                if (delayMs > 0L) {
                    mainHandler.postDelayed({ captureNext(index + 1) }, delayMs)
                } else {
                    captureNext(index + 1)
                }
            }
        }

        captureNext(0)
    }

    private suspend fun buildResult(
        imageBytes: ByteArray,
        burstFramesBytes: List<ByteArray>,
    ): SelfieCaptureResult {
        val format = config.resultFormat
        val includeBytes = format == ResultFormat.BYTE_ARRAY || format == ResultFormat.ALL
        val includeFile = format == ResultFormat.FILE_PATH || format == ResultFormat.ALL
        val includeBase64 = format == ResultFormat.BASE64 || format == ResultFormat.ALL

        var filePath: String? = null
        var base64: String? = null
        var burstFrames: List<String>? = null

        if (includeFile) {
            filePath = withContext(Dispatchers.IO) {
                val file = File(requireContext().cacheDir, "trueid_selfie_${System.currentTimeMillis()}.jpg")
                FileOutputStream(file).use { it.write(imageBytes) }
                file.absolutePath
            }
        }

        if (includeBase64) {
            base64 = withContext(Dispatchers.Default) {
                Base64.encodeToString(imageBytes, Base64.NO_WRAP)
            }
            if (burstFramesBytes.isNotEmpty()) {
                burstFrames = withContext(Dispatchers.Default) {
                    burstFramesBytes.map { Base64.encodeToString(it, Base64.NO_WRAP) }
                }
            }
        }

        return SelfieCaptureResult(
            imageBytes = if (includeBytes) imageBytes else null,
            filePath = filePath,
            base64 = base64,
            burstFrames = burstFrames,
            width = config.outputWidth,
            height = config.outputHeight,
            faceBounds = lastFaceBounds,
            captureTimestamp = System.currentTimeMillis(),
        )
    }

    private fun deliverResult(result: SelfieCaptureResult) {
        parentFragmentManager.setFragmentResult(
            RESULT_KEY,
            bundleOf(RESULT_DATA to result)
        )
    }

    private fun deliverCancelled() {
        parentFragmentManager.setFragmentResult(
            RESULT_KEY,
            bundleOf()
        )
    }

    private fun deliverError(error: SelfieCaptureError) {
        (activity as? SelfieCaptureActivity)?.let {
            SelfieCaptureActivity.pendingCallback?.onError(error)
            SelfieCaptureActivity.pendingCallback = null
            it.finish()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mainHandler.removeCallbacksAndMessages(null)
        stateMachine.release()
        cameraManager?.release()
        faceAnalyzer?.close()
    }
}
