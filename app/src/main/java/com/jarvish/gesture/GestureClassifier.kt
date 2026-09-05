package com.jarvish.gesture

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.abs

enum class Gesture {
    OPEN_PALM,      // fingers extended -> Play/Pause
    FIST,           // closed hand -> no-op / stop
    SWIPE_UP,       // hand moved up quickly -> scroll up
    SWIPE_DOWN,     // hand moved down quickly -> scroll down
    SWIPE_LEFT,     // hand moved left quickly -> previous / back
    SWIPE_RIGHT,    // hand moved right quickly -> next / forward
    POINT,          // only index finger extended -> tap/select
    NONE
}

/**
 * Takes MediaPipe's 21 hand landmarks per frame and turns them into a Gesture.
 * Landmark indices (MediaPipe Hand Landmarker):
 * 0 = wrist, 4 = thumb tip, 8 = index tip, 12 = middle tip, 16 = ring tip, 20 = pinky tip
 */
class GestureClassifier {

    // Keep track of recent wrist positions to detect swipes
    private val positionHistory = ArrayDeque<Pair<Float, Float>>()
    private val HISTORY_SIZE = 6
    private val SWIPE_THRESHOLD = 0.18f // normalized (0-1) screen-fraction movement

    fun classify(landmarks: List<NormalizedLandmark>): Gesture {
        if (landmarks.size < 21) return Gesture.NONE

        val wrist = landmarks[0]
        positionHistory.addLast(Pair(wrist.x(), wrist.y()))
        if (positionHistory.size > HISTORY_SIZE) positionHistory.removeFirst()

        // 1. Check for swipe first (fast directional movement)
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

        // 2. Check finger states (extended vs curled) for static gestures
        val thumbExtended = landmarks[4].y() < landmarks[3].y()
        val indexExtended = landmarks[8].y() < landmarks[6].y()
        val middleExtended = landmarks[12].y() < landmarks[10].y()
        val ringExtended = landmarks[16].y() < landmarks[14].y()
        val pinkyExtended = landmarks[20].y() < landmarks[18].y()

        val extendedCount = listOf(thumbExtended, indexExtended, middleExtended, ringExtended, pinkyExtended)
            .count { it }

        return when {
            extendedCount >= 3 -> Gesture.OPEN_PALM
            extendedCount == 0 -> Gesture.FIST
            indexExtended && extendedCount == 1 -> Gesture.POINT
            else -> Gesture.NONE
        }
    }
}
