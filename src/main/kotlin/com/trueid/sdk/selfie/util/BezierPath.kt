package com.trueid.sdk.selfie.util

import android.graphics.Path
import android.graphics.PointF

internal object BezierPath {

    /**
     * Creates a smoothed path through [points] using quadratic Bezier curves.
     * Ported from the Flutter `_smoothedPath` method in selfie_verification_screen.dart.
     */
    fun smoothed(points: List<PointF>, closed: Boolean): Path {
        val path = Path()
        if (points.isEmpty()) return path

        path.moveTo(points.first().x, points.first().y)

        if (points.size == 2) {
            path.lineTo(points.last().x, points.last().y)
            if (closed) path.close()
            return path
        }

        for (i in 1 until points.size - 1) {
            val current = points[i]
            val next = points[i + 1]
            val midX = (current.x + next.x) / 2f
            val midY = (current.y + next.y) / 2f
            path.quadTo(current.x, current.y, midX, midY)
        }

        path.lineTo(points.last().x, points.last().y)
        if (closed) path.close()

        return path
    }
}
