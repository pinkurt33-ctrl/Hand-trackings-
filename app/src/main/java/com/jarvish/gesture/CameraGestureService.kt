package com.jarvish.gesture

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.KeyEvent
import java.io.ByteArrayOutputStream
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.ImageProxyKt
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import com.google.mediapipe.framework.image.BitmapImageBuilder
import java.util.concurrent.Executors

@androidx.camera.core.ExperimentalGetImage
class CameraGestureService : LifecycleService() {

    private val TAG = "CameraGestureService"
    private val CHANNEL_ID = "jarvish_gesture_channel"
    private val NOTIF_ID = 101

    private lateinit var handLandmarker: HandLandmarker
    private val classifier = GestureClassifier()
    private val cameraExecutor = Executors.newSingleThreadExecutor()

    private var lastActionTime = 0L
    private val ACTION_COOLDOWN_MS = 800
    private var handEverDetected = false
    private var frameErrorShown = false

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIF_ID, buildNotification())
        setupHandLandmarker()
        startCamera()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    private fun buildNotification(): android.app.Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText(getString(R.string.notif_text))
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setOngoing(true)
            .build()
    }

    private fun setupHandLandmarker() {
        try {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath("hand_landmarker.task")
                .build()

            val options = HandLandmarker.HandLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setNumHands(1)
                .setResultListener(::onHandResult)
                .setErrorListener { e -> Log.e(TAG, "HandLandmarker error: ${e.message}") }
                .build()

            handLandmarker = HandLandmarker.createFromOptions(this, options)
            showToast("Jarvish Gesture: model load ho gaya")
        } catch (e: Exception) {
            Log.e(TAG, "Model load FAILED: ${e.message}", e)
            showToast("Jarvish ERROR: model load nahi hua - ${e.message}")
        }
    }

    private fun showToast(message: String) {
        android.os.Handler(mainLooper).post {
            android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_LONG).show()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()

            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                processFrame(imageProxy)
            }

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis)
            } catch (e: Exception) {
                Log.e(TAG, "Camera bind failed: ${e.message}")
            }
        }, cameraExecutor)
    }

    private fun processFrame(imageProxy: ImageProxy) {
        try {
            val rotation = imageProxy.imageInfo.rotationDegrees
            val rawBitmap = ImageProxyKt.toBitmap(imageProxy)
            val bitmap = if (rotation == 0) {
                rawBitmap
            } else {
                val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
                Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
            }
            val mpImage = BitmapImageBuilder(bitmap).build()
            handLandmarker.detectAsync(mpImage, System.currentTimeMillis())
        } catch (e: Exception) {
            Log.e(TAG, "Frame processing failed: ${e.message}", e)
            if (!frameErrorShown) {
                frameErrorShown = true
                showToast("Jarvish ERROR: frame process fail - ${e.message}")
            }
        } finally {
            imageProxy.close()
        }
    }

    private fun onHandResult(result: HandLandmarkerResult, input: com.google.mediapipe.framework.image.MPImage) {
        if (result.landmarks().isEmpty()) return
        val landmarks = result.landmarks()[0]

        if (!handEverDetected) {
            handEverDetected = true
            showToast("Haath dikh gaya! Ab gesture try kar")
        }

        val gesture = classifier.classify(landmarks)

        val now = System.currentTimeMillis()
        if (gesture != Gesture.NONE && now - lastActionTime > ACTION_COOLDOWN_MS) {
            lastActionTime = now
            handleGesture(gesture)
        }
    }

    private fun handleGesture(gesture: Gesture) {
        Log.d(TAG, "Gesture detected: $gesture")
        vibrateFeedback()
        val a11yService = GestureAccessibilityService.instance

        when (gesture) {
            Gesture.OPEN_PALM -> sendMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            Gesture.SWIPE_UP -> a11yService?.performScroll(scrollDown = false)
            Gesture.SWIPE_DOWN -> a11yService?.performScroll(scrollDown = true)
            Gesture.SWIPE_LEFT -> a11yService?.performSwipe(rightToLeft = true)
            Gesture.SWIPE_RIGHT -> a11yService?.performSwipe(rightToLeft = false)
            Gesture.POINT -> {
                val metrics = resources.displayMetrics
                a11yService?.performTap(metrics.widthPixels / 2f, metrics.heightPixels / 2f)
            }
            Gesture.FIST, Gesture.NONE -> { }
        }

        if (a11yService == null) {
            Log.w(TAG, "Accessibility service not enabled - scroll/tap actions won't work. Enable it in Settings > Accessibility.")
        }
    }

    private fun sendMediaKey(keyCode: Int) {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        val eventDown = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
        val eventUp = KeyEvent(KeyEvent.ACTION_UP, keyCode)
        audioManager.dispatchMediaKeyEvent(eventDown)
        audioManager.dispatchMediaKeyEvent(eventUp)
    }

    private fun vibrateFeedback() {
        val vibrator = getSystemService(VIBRATOR_SERVICE) as? Vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        handLandmarker.close()
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }
}
