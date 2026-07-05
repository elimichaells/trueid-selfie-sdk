package com.trueid.sdk.selfie.overlay

import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
import android.view.View
import com.trueid.sdk.selfie.internal.FaceGuideStatus
import com.trueid.sdk.selfie.internal.MeshStroke
import com.trueid.sdk.selfie.util.BezierPath

/**
 * Draws face contour lines with glow effect over the camera preview.
 * Port of Flutter's _LiveFacePainter.
 */
internal class FaceMeshOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private var meshStrokes: List<MeshStroke> = emptyList()
    private var currentStatus: FaceGuideStatus = FaceGuideStatus.LOADING

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.8f * resources.displayMetrics.density
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f * resources.displayMetrics.density
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        maskFilter = BlurMaskFilter(3f * resources.displayMetrics.density, BlurMaskFilter.Blur.NORMAL)
    }

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    fun setMeshStrokes(strokes: List<MeshStroke>, status: FaceGuideStatus) {
        meshStrokes = strokes
        currentStatus = status
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (meshStrokes.isEmpty()) return

        val isCentered = currentStatus == FaceGuideStatus.FACE_CENTERED
        val color = if (isCentered) 0xFF22C55E.toInt() else 0xCC22C55E.toInt()
        val glowAlpha = (color ushr 24) * 35 / 100

        strokePaint.color = color
        glowPaint.color = (glowAlpha shl 24) or (color and 0x00FFFFFF)

        val w = width.toFloat()
        val h = height.toFloat()

        for (stroke in meshStrokes) {
            if (stroke.points.size < 2) continue

            val scaled = stroke.points.map { pt ->
                PointF(pt.x * w, pt.y * h)
            }

            val path = BezierPath.smoothed(scaled, stroke.closed)

            // Glow first, then stroke
            canvas.drawPath(path, glowPaint)
            canvas.drawPath(path, strokePaint)
        }
    }
}
