package com.google.ai.edge.gallery.customtasks.fieldmedic

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.util.Log
import org.tensorflow.lite.InterpreterApi
import org.tensorflow.lite.support.common.FileUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val TAG = "FrameGate"

/** Decides whether a camera frame is worth processing (EfficientDet gate). */
interface FrameGate {
    fun shouldProcess(bitmap: Bitmap): Boolean
    fun close()
}

/** Stub — always passes. Swap with EfficientDet implementation later. */
class StubFrameGate : FrameGate {
    override fun shouldProcess(bitmap: Bitmap): Boolean = true
    override fun close() {}
}

/**
 * EfficientDet Lite0 frame gate.
 * Runs object detection and passes frames that contain a person or relevant object
 * above the confidence threshold.
 *
 * EfficientDet Lite0 input:  320x320 RGB uint8
 * EfficientDet Lite0 output:
 *   0: detection boxes     [1, 25, 4] float32
 *   1: detection classes   [1, 25]    float32
 *   2: detection scores    [1, 25]    float32
 *   3: number of detections [1]       float32
 */
class EfficientDetFrameGate(modelPath: String) : FrameGate {

    private val interpreter: InterpreterApi

    // COCO class IDs we care about (0-indexed):
    // 0 = person
    private val relevantClasses = setOf(0)
    private val confidenceThreshold = 0.3f
    private val inputSize = 320

    init {
        val options = InterpreterApi.Options()
            .setRuntime(InterpreterApi.Options.TfLiteRuntime.FROM_SYSTEM_ONLY)
        interpreter = InterpreterApi.create(java.io.File(modelPath), options)
        Log.i(TAG, "EfficientDet loaded from $modelPath")
    }

    override fun shouldProcess(bitmap: Bitmap): Boolean {
        return try {
            val input = preprocessBitmap(bitmap)
            val detections = runDetection(input)
            detections.any { det ->
                det.classId in relevantClasses && det.score >= confidenceThreshold
            }
        } catch (e: Exception) {
            Log.e(TAG, "Detection failed, passing frame", e)
            true // fail open — if detection crashes, still process the frame
        }
    }

    override fun close() {
        try { interpreter.close() } catch (_: Exception) {}
    }

    private fun preprocessBitmap(bitmap: Bitmap): ByteBuffer {
        // Scale to 320x320
        val scaled = Bitmap.createBitmap(inputSize, inputSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(scaled)
        val scaleX = inputSize.toFloat() / bitmap.width
        val scaleY = inputSize.toFloat() / bitmap.height
        val matrix = Matrix().apply { setScale(scaleX, scaleY) }
        canvas.drawBitmap(bitmap, matrix, null)

        // Convert to RGB uint8 ByteBuffer
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

    private fun runDetection(input: ByteBuffer): List<Detection> {
        val maxDetections = 25

        // Output buffers
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
        val detections = mutableListOf<Detection>()
        for (i in 0 until count) {
            detections.add(
                Detection(
                    classId = classes[0][i].toInt(),
                    score = scores[0][i],
                )
            )
        }
        return detections
    }

    private data class Detection(val classId: Int, val score: Float)
}
