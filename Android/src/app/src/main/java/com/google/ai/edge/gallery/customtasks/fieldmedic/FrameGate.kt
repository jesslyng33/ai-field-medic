package com.google.ai.edge.gallery.customtasks.fieldmedic

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import org.tensorflow.lite.InterpreterApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.coroutines.resume

private const val TAG = "FrameGate"

/** Normalized bounding box (0..1) plus label/score, relative to the analyzed bitmap. */
data class DetectionBox(
    val label: String,
    val score: Float,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

data class FrameGateResult(
    val passed: Boolean,
    val detections: List<DetectionBox>,
)

/** Decides whether a camera frame is worth processing and returns what was seen. */
interface FrameGate {
    suspend fun analyze(bitmap: Bitmap): FrameGateResult
    fun close()
}

/** Stub — always passes, no detections. */
class StubFrameGate : FrameGate {
    override suspend fun analyze(bitmap: Bitmap): FrameGateResult =
        FrameGateResult(passed = true, detections = emptyList())
    override fun close() {}
}

/**
 * ML Kit face-detection gate. Frames pass only if a human face is detected.
 * Returns bounding boxes (normalized to bitmap dims) for overlay/debug display.
 */
class MlKitFaceFrameGate : FrameGate {

    private val detector: FaceDetector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .setMinFaceSize(0.1f)
            .build()
    )

    override suspend fun analyze(bitmap: Bitmap): FrameGateResult =
        suspendCancellableCoroutine { cont ->
            val image = InputImage.fromBitmap(bitmap, 0)
            detector.process(image)
                .addOnSuccessListener { faces ->
                    val w = bitmap.width.toFloat().coerceAtLeast(1f)
                    val h = bitmap.height.toFloat().coerceAtLeast(1f)
                    val boxes = faces.map { face ->
                        val r = face.boundingBox
                        DetectionBox(
                            label = "face",
                            score = face.trackingId?.let { 1f } ?: 1f,
                            left = (r.left / w).coerceIn(0f, 1f),
                            top = (r.top / h).coerceIn(0f, 1f),
                            right = (r.right / w).coerceIn(0f, 1f),
                            bottom = (r.bottom / h).coerceIn(0f, 1f),
                        )
                    }
                    cont.resume(FrameGateResult(passed = boxes.isNotEmpty(), detections = boxes))
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Face detection failed", e)
                    cont.resume(FrameGateResult(passed = false, detections = emptyList()))
                }
        }

    override fun close() {
        try { detector.close() } catch (_: Exception) {}
    }
}

/**
 * EfficientDet Lite0 frame gate. Kept for reference / fallback testing.
 *
 * EfficientDet Lite0 input:  320x320 RGB uint8
 * Output: boxes [1,25,4], classes [1,25], scores [1,25], num [1].
 */
class EfficientDetFrameGate(modelPath: String) : FrameGate {

    private val interpreter: InterpreterApi
    private val relevantClasses = setOf(0) // COCO: 0 = person
    private val confidenceThreshold = 0.3f
    private val inputSize = 320
    private val cocoLabels = mapOf(0 to "person")

    init {
        val options = InterpreterApi.Options()
            .setRuntime(InterpreterApi.Options.TfLiteRuntime.FROM_SYSTEM_ONLY)
        interpreter = InterpreterApi.create(java.io.File(modelPath), options)
        Log.i(TAG, "EfficientDet loaded from $modelPath")
    }

    override suspend fun analyze(bitmap: Bitmap): FrameGateResult {
        return try {
            val input = preprocessBitmap(bitmap)
            val raw = runDetection(input)
            val boxes = raw
                .filter { it.classId in relevantClasses && it.score >= confidenceThreshold }
                .map {
                    DetectionBox(
                        label = cocoLabels[it.classId] ?: "?",
                        score = it.score,
                        left = it.left, top = it.top, right = it.right, bottom = it.bottom,
                    )
                }
            FrameGateResult(passed = boxes.isNotEmpty(), detections = boxes)
        } catch (e: Exception) {
            Log.e(TAG, "Detection failed, passing frame", e)
            FrameGateResult(passed = true, detections = emptyList())
        }
    }

    override fun close() {
        try { interpreter.close() } catch (_: Exception) {}
    }

    private fun preprocessBitmap(bitmap: Bitmap): ByteBuffer {
        val scaled = Bitmap.createBitmap(inputSize, inputSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(scaled)
        val scaleX = inputSize.toFloat() / bitmap.width
        val scaleY = inputSize.toFloat() / bitmap.height
        val matrix = Matrix().apply { setScale(scaleX, scaleY) }
        canvas.drawBitmap(bitmap, matrix, null)

        val buffer = ByteBuffer.allocateDirect(1 * inputSize * inputSize * 3)
        buffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(inputSize * inputSize)
        scaled.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)

        for (pixel in pixels) {
            buffer.put((Color.red(pixel) and 0xFF).toByte())
            buffer.put((Color.green(pixel) and 0xFF).toByte())
            buffer.put((Color.blue(pixel) and 0xFF).toByte())
        }

        buffer.rewind()
        scaled.recycle()
        return buffer
    }

    private fun runDetection(input: ByteBuffer): List<RawDet> {
        val maxDetections = 25
        val boxes = Array(1) { Array(maxDetections) { FloatArray(4) } }
        val classes = Array(1) { FloatArray(maxDetections) }
        val scores = Array(1) { FloatArray(maxDetections) }
        val numDetections = FloatArray(1)

        val outputs = mapOf<Int, Any>(
            0 to boxes,
            1 to classes,
            2 to scores,
            3 to numDetections,
        )
        interpreter.runForMultipleInputsOutputs(arrayOf(input), outputs)

        val count = numDetections[0].toInt().coerceAtMost(maxDetections)
        val out = mutableListOf<RawDet>()
        for (i in 0 until count) {
            val b = boxes[0][i] // [ymin, xmin, ymax, xmax] normalized
            out.add(
                RawDet(
                    classId = classes[0][i].toInt(),
                    score = scores[0][i],
                    left = b[1], top = b[0], right = b[3], bottom = b[2],
                )
            )
        }
        return out
    }

    private data class RawDet(
        val classId: Int,
        val score: Float,
        val left: Float, val top: Float, val right: Float, val bottom: Float,
    )
}
