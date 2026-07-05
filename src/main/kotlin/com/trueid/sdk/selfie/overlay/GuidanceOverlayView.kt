package com.trueid.sdk.selfie.overlay

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.trueid.sdk.selfie.R
import com.trueid.sdk.selfie.internal.FaceGuideStatus

/**
 * Status pill showing current face detection status with animated color transitions.
 */
internal class GuidanceOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : TextView(context, attrs, defStyleAttr) {

    private var currentColor: Int = ContextCompat.getColor(context, R.color.trueid_status_loading)
    private val pillBackground = GradientDrawable().apply {
        cornerRadius = 20f * resources.displayMetrics.density
        setColor(currentColor)
    }

    init {
        background = pillBackground
        gravity = Gravity.CENTER
        val hPad = (12 * resources.displayMetrics.density).toInt()
        val vPad = (4 * resources.displayMetrics.density).toInt()
        setPadding(hPad, vPad, hPad, vPad)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        setTextColor(0xFFFFFFFF.toInt())
        setText(R.string.trueid_status_loading)
    }

    fun setStatus(status: FaceGuideStatus, guidanceResIdOrText: String? = null) {
        val (textResId, colorResId) = when (status) {
            FaceGuideStatus.LOADING -> R.string.trueid_status_loading to R.color.trueid_status_loading
            FaceGuideStatus.NO_FACE -> R.string.trueid_status_no_face to R.color.trueid_status_no_face
            FaceGuideStatus.FACE_DETECTED -> R.string.trueid_status_detected to R.color.trueid_status_detected
            FaceGuideStatus.FACE_CENTERED -> R.string.trueid_status_centered to R.color.trueid_status_centered
            FaceGuideStatus.ERROR -> R.string.trueid_status_error to R.color.trueid_status_error
        }

        setText(textResId)
        val targetColor = ContextCompat.getColor(context, colorResId)
        animateColor(targetColor)
    }

    fun setStatus(status: FaceGuideStatus, guidanceResId: Int) {
        setStatus(status, null as String?)
    }

    private fun animateColor(targetColor: Int) {
        if (targetColor == currentColor) return

        val animator = ValueAnimator.ofObject(ArgbEvaluator(), currentColor, targetColor)
        animator.duration = 200
        animator.addUpdateListener { anim ->
            val color = anim.animatedValue as Int
            pillBackground.setColor(color)
            currentColor = color
        }
        animator.start()
    }
}
