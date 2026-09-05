package com.jarvish.gesture

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast

class GestureAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "GestureA11yService"
        var instance: GestureAccessibilityService? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d(TAG, "Accessibility service connected")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility service interrupted")
    }

    private fun toast(msg: String) {
        android.os.Handler(mainLooper).post {
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
    }

    private val debugCallback = object : GestureResultCallback() {
        override fun onCompleted(gestureDescription: GestureDescription?) {
            Log.d(TAG, "Gesture COMPLETED")
        }
        override fun onCancelled(gestureDescription: GestureDescription?) {
            Log.d(TAG, "Gesture CANCELLED")
            toast("Jarvish: gesture CANCELLED by system")
        }
    }

    fun performScroll(scrollDown: Boolean) {
        val displayMetrics = resources.displayMetrics
        val centerX = displayMetrics.widthPixels / 2f
        val startY = if (scrollDown) displayMetrics.heightPixels * 0.75f else displayMetrics.heightPixels * 0.25f
        val endY = if (scrollDown) displayMetrics.heightPixels * 0.25f else displayMetrics.heightPixels * 0.75f

        val path = Path().apply {
            moveTo(centerX, startY)
            lineTo(centerX, endY)
        }

        val gestureBuilder = GestureDescription.Builder()
        gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 0, 300))
        val dispatched = dispatchGesture(gestureBuilder.build(), debugCallback, null)
        Log.d(TAG, "performScroll dispatch result: $dispatched")
        if (!dispatched) toast("Jarvish: scroll dispatch FAILED")
    }

    fun performTap(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        val gestureBuilder = GestureDescription.Builder()
        gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 0, 100))
        val dispatched = dispatchGesture(gestureBuilder.build(), debugCallback, null)
        Log.d(TAG, "performTap dispatch result: $dispatched")
        if (!dispatched) toast("Jarvish: tap dispatch FAILED")
    }

    fun performSwipe(rightToLeft: Boolean) {
        val displayMetrics = resources.displayMetrics
        val centerY = displayMetrics.heightPixels / 2f
        val startX = if (rightToLeft) displayMetrics.widthPixels * 0.8f else displayMetrics.widthPixels * 0.2f
        val endX = if (rightToLeft) displayMetrics.widthPixels * 0.2f else displayMetrics.widthPixels * 0.8f

        val path = Path().apply {
            moveTo(startX, centerY)
            lineTo(endX, centerY)
        }

        val gestureBuilder = GestureD
