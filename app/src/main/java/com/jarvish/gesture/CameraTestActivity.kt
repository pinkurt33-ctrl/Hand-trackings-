package com.jarvish.gesture

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.camera.core.Preview
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import com.google.mediapipe.framework.image.BitmapImageBuilder
import java.util.concurrent.Executors

@androidx.camera.core.ExperimentalGetImage
class CameraTestActivity : AppCompatActivity() {

    private lateinit var handLandmarker: HandLandmarker
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private lateinit var overlayView: OverlayView
    private lateinit var statusText: TextView
    private var frameCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_test)

        overlayView = findViewById(R.id.overlayView)
        statusText = findViewById(R.id.testStatusText)
        val previewView = findViewById<PreviewView>(R.id.previewView)

        setupHandLandmarker()
        startCamera(previewView)
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
                .setMinHandDetectionConfidence(0.3f)
                .setResultListener(::onHandResult)
                .setErrorListener { e ->
                    runOnUiThread { statusText.text = "Landmarker error: ${e.message}" }
                }
                .build()

            handLandmarker = HandLandmarker.createFromOptions(this, options)
        } catch (e: Exception) {
            statusText.text = "Model load FAILED: ${e.message}"
        }
    }

    private fun startCamera(previewView: PreviewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

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
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)
                runOnUiThread { statusText.text = "Camera chalu - haath dikha" }
            } catch (e: Exception) {
                runOnUiThread { statusText.text = "Camera bind FAILED: ${e.message}" }
            }
        }, cameraExecutor)
    }

    private fun processFrame(imageProxy: ImageProxy) {
        try {
            frameCount++
            val rotation = imageProxy.imageInfo.rotationDegrees
            val rawBitmap = imageProxy.toBitmap()
            val bitmap = if (rotation == 0) {
                rawBitmap
            } else {
                val matrix = android.graphics.Matrix().apply { postRotate(rotation.toFloat()) }
                android.graphics.Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
            }
            val mpImage = BitmapImageBuilder(bitmap).build()
            handLandmarker.detectAsync(mpImage, System.currentTimeMillis())
        } catch (e: Exception) {
            runOnUiThread { statusText.text = "Frame error: ${e.message}" }
        } finally {
            imageProxy.close()
        }
    }

    private fun onHandResult(result: HandLandmarkerResult, input: com.google.mediapipe.framework.image.MPImage) {
        runOnUiThread {
            if (result.landmarks().isEmpty()) {
                overlayView.updateLandmarks(emptyList())
                statusText.text = "Frames: $frameCount | Haath NAHI dikh raha"
            } else {
                overlayView.updateLandmarks(result.landmarks()[0])
                statusText.text = "Frames: $frameCount | HAATH DIKH RAHA HAI ✅"
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        if (::handLandmarker.isInitialized) handLandmarker.close()
    }
}
