package com.jarvish.gesture

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

class OverlayView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private var landmarks: List<NormalizedLandmark> = emptyList()
    private val dotPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.FILL
    }
    private val linePaint = Paint().apply {
        color = Color.YELLOW
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private val connections = listOf(
        0 to 1, 1 to 2, 2 to 3, 3 to 4,
        0 to 5, 5 to 6, 6 to 7, 7 to 8,
        0 to 9, 9 to 10, 10 to 11, 11 to 12,
        0 to 13, 13 to 14, 14 to 15, 15 to 16,
        0 to 17, 17 to 18, 18 to 19, 19 to 20
    )

    fun updateLandmarks(newLandmarks: List<NormalizedLandmark>) {
        landmarks = newLandmarks
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (landmarks.isEmpty()) return

        fun px(l: NormalizedLandmark) = (1f - l.x()) * width
        fun py(l: NormalizedLandmark) = l.y() * height

        for ((a, b) in connections) {
            if (a < landmarks.size && b < landmarks.size) {
                canvas.drawLine(
                    px(landmarks[a]), py(landmarks[a]),
                    px(landmarks[b]), py(landmarks[b]),
                    linePaint
                )
            }
        }
        for (l in landmarks) {
            canvas.drawCircle(px(l), py(l), 12f, dotPaint)
        }
    }
}
