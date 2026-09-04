package com.jarvish.gesture

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.abs

enum class Gesture {
    OPEN_PALM,
    FIST,
    SWIPE_UP,
    SWIPE_DOWN,
    SWIPE_LEFT,
    SWIPE_RIGHT,
    POINT,
    NONE
}

class GestureClassifier {

    private val positionHistory = ArrayDeque<Pair<Float, Float>>()
    private val HISTORY_SIZE = 6
    private val SWIPE_THRESHOLD = 0.18f

    fun classify(landmarks: List<NormalizedLandmark>): Gesture {
        if (landmarks.size < 21) return Gesture.NONE

        val wrist = landmarks[0]
        positionHistory.addLast(Pair(wrist.x(), wrist.y()))
        if (positionHistory.size > HISTORY_SIZE) positionHistory.removeFirst()

        if (positionHistory.size == HISTORY_SIZE) {
            val (startX, startY) = positionHistory.first()
            val (endX, endY) = positionHistory.last()
            val dx = endX - startX
            val dy = endY - startY

            if (abs(dx) > SWIPE_THRESHOLD && abs(dx) > abs(dy)) {
                positionHistory.clear()
                return if (dx > 0) Gesture.SWIPE_RIGHT else Gesture.SWIPE_LEFT
            }
            if (abs(dy) > SWIPE_THRESHOLD && abs(dy) > abs(dx)) {
                positionHistory.clear()
                return if (dy > 0) Gesture.SWIPE_DOWN else Gesture.SWIPE_UP
            }
        }

        val thumbExtended = landmarks[4].y() < landmarks[3].y()
        val indexExtended = landmarks[8].y() < landmarks[6].y()
        val middleExtended = landmarks[12].y() < landmarks[10].y()
        val ringExtended = landmarks[16].y() < landmarks[14].y()
        val pinkyExtended = landmarks[20].y() < landmarks[18].y()

        val extendedCount = listOf(thumbExtended, indexExtended, middleExtended, ringExtended, pinkyExtended)
            .count { it }

        return when {
            extendedCount >= 4 -> Gesture.OPEN_PALM
            extendedCount == 0 -> Gesture.FIST
            indexExtended && extendedCount == 1 -> Gesture.POINT
            else -> Gesture.NONE
        }
    }
}
