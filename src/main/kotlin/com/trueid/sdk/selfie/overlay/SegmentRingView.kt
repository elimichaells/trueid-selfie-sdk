package com.trueid.sdk.selfie.overlay

import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

/**
 * Draws an animated segmented ring around the circular camera preview.
 * Port of Flutter's _SegmentRingPainter.
 *
 * 40 rounded segments arranged in a circle. Lit segments glow green;
 * unlit segments are translucent.
 */
internal class SegmentRingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    var segmentCount: Int = 40
        set(value) {
            field = value
            invalidate()
        }

    var litSegments: Int = 0
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    private var activeColor = 0xFF22C55E.toInt()
    private var inactiveColor = 0x3722C55E
    private var glowColor = 0xAA22C55E.toInt()

    private val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val inactivePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        maskFilter = BlurMaskFilter(6f * resources.displayMetrics.density, BlurMaskFilter.Blur.NORMAL)
    }

    init {
        // Needed for BlurMaskFilter to work
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        updatePaintColors()
    }

    fun setColors(active: Int, inactive: Int, glow: Int) {
        activeColor = active
        inactiveColor = inactive
        glowColor = glow
        updatePaintColors()
        invalidate()
    }

    private fun updatePaintColors() {
        activePaint.color = activeColor
        inactivePaint.color = inactiveColor
        glowPaint.color = glowColor
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f
        val cy = height / 2f
        val density = resources.displayMetrics.density

        val radius = minOf(cx, cy) - 12f * density
        val segWidth = 2f * density
        val segHeight = 22f * density
        val segCornerRadius = segWidth / 2f

        for (i in 0 until segmentCount) {
            val angle = (i.toFloat() / segmentCount) * 360f - 90f
            val radians = Math.toRadians(angle.toDouble())

            val segCenterX = cx + radius * cos(radians).toFloat()
            val segCenterY = cy + radius * sin(radians).toFloat()

            canvas.save()
            canvas.translate(segCenterX, segCenterY)
            canvas.rotate(angle + 90f)

            val rect = RectF(
                -segWidth / 2f,
                -segHeight / 2f,
                segWidth / 2f,
                segHeight / 2f
            )

            val isLit = i < litSegments
            if (isLit) {
                // Glow behind
                canvas.drawRoundRect(rect, segCornerRadius, segCornerRadius, glowPaint)
                canvas.drawRoundRect(rect, segCornerRadius, segCornerRadius, activePaint)
            } else {
                canvas.drawRoundRect(rect, segCornerRadius, segCornerRadius, inactivePaint)
            }

            canvas.restore()
        }
    }
}
