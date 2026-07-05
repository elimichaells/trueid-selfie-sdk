package com.trueid.sdk.selfie.internal

import android.graphics.PointF
import android.graphics.RectF
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

internal data class MeshStroke(
    val points: List<PointF>,
    val closed: Boolean,
)

internal data class FaceAnalysisResult(
    val rawBounds: RectF?,
    val displayBounds: RectF?,
    val meshStrokes: List<MeshStroke>,
    val status: FaceGuideStatus,
    val guidanceResId: Int,
)

internal class FaceAnalyzer(
    private val showMesh: Boolean,
    private val validator: FaceAlignmentValidator,
    private val onResult: (FaceAnalysisResult) -> Unit,
) : ImageAnalysis.Analyzer {

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setContourMode(
                if (showMesh) FaceDetectorOptions.CONTOUR_MODE_ALL
                else FaceDetectorOptions.CONTOUR_MODE_NONE
            )
            .build()
    )

    private var isProcessing = false
    private var lastProcessTimeMs = 0L
    private var isFrontCamera = true

    fun setFrontCamera(front: Boolean) {
        isFrontCamera = front
    }

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val now = System.currentTimeMillis()
        if (isProcessing || now - lastProcessTimeMs < 15) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        isProcessing = true
        lastProcessTimeMs = now

        val inputImage = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )

        val imgWidth = inputImage.width.toFloat()
        val imgHeight = inputImage.height.toFloat()

        detector.process(inputImage)
            .addOnSuccessListener { faces ->
                if (faces.isEmpty()) {
                    onResult(
                        FaceAnalysisResult(
                            rawBounds = null,
                            displayBounds = null,
                            meshStrokes = emptyList(),
                            status = FaceGuideStatus.NO_FACE,
                            guidanceResId = com.trueid.sdk.selfie.R.string.trueid_guidance_position,
                        )
                    )
                } else {
                    // Pick the largest face
                    val face = faces.maxByOrNull {
                        it.boundingBox.width() * it.boundingBox.height()
                    }!!

                    val box = face.boundingBox

                    // Normalize to 0..1
                    var nx = box.centerX() / imgWidth
                    var ny = box.centerY() / imgHeight
                    var nw = box.width() / imgWidth
                    var nh = box.height() / imgHeight

                    // Mirror X for front camera
                    if (isFrontCamera) {
                        nx = 1.0f - nx
                    }

                    val displayBounds = RectF(
                        nx - nw / 2f, ny - nh / 2f,
                        nx + nw / 2f, ny + nh / 2f
                    )

                    val rawBounds = RectF(
                        box.left / imgWidth,
                        box.top / imgHeight,
                        box.right / imgWidth,
                        box.bottom / imgHeight,
                    )

                    // Evaluate alignment
                    val alignment = validator.evaluate(
                        faceHeightRatio = nh,
                        centerXRatio = nx,
                        centerYRatio = ny,
                    )

                    // Extract contour mesh strokes
                    val meshStrokes = if (showMesh) {
                        extractMeshStrokes(face, imgWidth, imgHeight)
                    } else {
                        emptyList()
                    }

                    onResult(
                        FaceAnalysisResult(
                            rawBounds = rawBounds,
                            displayBounds = displayBounds,
                            meshStrokes = meshStrokes,
                            status = alignment.status,
                            guidanceResId = alignment.guidanceResId,
                        )
                    )
                }
            }
            .addOnFailureListener {
                onResult(
                    FaceAnalysisResult(
                        rawBounds = null,
                        displayBounds = null,
                        meshStrokes = emptyList(),
                        status = FaceGuideStatus.ERROR,
                        guidanceResId = com.trueid.sdk.selfie.R.string.trueid_guidance_unavailable,
                    )
                )
            }
            .addOnCompleteListener {
                imageProxy.close()
                isProcessing = false
            }
    }

    private fun extractMeshStrokes(
        face: com.google.mlkit.vision.face.Face,
        imgWidth: Float,
        imgHeight: Float,
    ): List<MeshStroke> {
        val strokes = mutableListOf<MeshStroke>()

        for (contourType in MESH_CONTOUR_TYPES) {
            val contour = face.getContour(contourType) ?: continue
            val points = contour.points.map { pt ->
                var x = pt.x / imgWidth
                if (isFrontCamera) x = 1.0f - x
                val y = pt.y / imgHeight
                PointF(x, y)
            }
            if (points.size >= 2) {
                strokes.add(
                    MeshStroke(
                        points = points,
                        closed = contourType in CLOSED_CONTOUR_TYPES,
                    )
                )
            }
        }

        return strokes
    }

    fun close() {
        detector.close()
    }

    companion object {
        private val MESH_CONTOUR_TYPES = listOf(
            FaceContour.FACE,
            FaceContour.LEFT_EYE,
            FaceContour.RIGHT_EYE,
            FaceContour.LEFT_EYEBROW_TOP,
            FaceContour.LEFT_EYEBROW_BOTTOM,
            FaceContour.RIGHT_EYEBROW_TOP,
            FaceContour.RIGHT_EYEBROW_BOTTOM,
            FaceContour.NOSE_BRIDGE,
            FaceContour.NOSE_BOTTOM,
            FaceContour.UPPER_LIP_TOP,
            FaceContour.UPPER_LIP_BOTTOM,
            FaceContour.LOWER_LIP_TOP,
            FaceContour.LOWER_LIP_BOTTOM,
        )

        private val CLOSED_CONTOUR_TYPES = setOf(
            FaceContour.FACE,
            FaceContour.LEFT_EYE,
            FaceContour.RIGHT_EYE,
        )
    }
}
