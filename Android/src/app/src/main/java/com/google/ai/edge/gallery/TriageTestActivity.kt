/*
 * Field Medic — Gemma 4 E2B multimodal test harness.
 * Proves text, live camera, and live mic input all work on-device.
 */

package com.google.ai.edge.gallery

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.MessageCallback
import com.google.ai.edge.litertlm.SamplerConfig
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.concurrent.thread

private const val TAG = "FieldMedic"

private const val MODEL_PATH =
    "/data/local/tmp/gemma-4-E2B-it.litertlm"

private const val SAMPLE_RATE = 16000
private const val MAX_RECORDING_SEC = 15

class TriageTestActivity : ComponentActivity() {

    // Singleton engine + conversation, survives across button presses.
    companion object {
        private var engine: Engine? = null
        private var conversation: Conversation? = null
        private var modelLoaded = false
    }

    private lateinit var tvStatus: TextView
    private lateinit var tvOutput: TextView
    private lateinit var scrollView: ScrollView
    private lateinit var btnText: Button
    private lateinit var btnImage: Button
    private lateinit var btnAudio: Button
    private lateinit var cameraContainer: FrameLayout
    private lateinit var previewView: PreviewView
    private lateinit var btnCapture: Button
    private lateinit var tvRecording: TextView

    // CameraX
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService

    // Audio recording
    private var audioRecord: AudioRecord? = null
    @Volatile private var isRecording = false

    // Permission launchers
    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startCameraPreview()
            } else {
                appendOutput("ERROR: Camera permission denied")
            }
        }

    private val audioPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startAudioRecording()
            } else {
                appendOutput("ERROR: Microphone permission denied")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_triage_test)

        tvStatus = findViewById(R.id.tvStatus)
        tvOutput = findViewById(R.id.tvOutput)
        scrollView = findViewById(R.id.scrollView)
        btnText = findViewById(R.id.btnTextOnly)
        btnImage = findViewById(R.id.btnImage)
        btnAudio = findViewById(R.id.btnAudio)
        cameraContainer = findViewById(R.id.cameraContainer)
        previewView = findViewById(R.id.previewView)
        btnCapture = findViewById(R.id.btnCapture)
        tvRecording = findViewById(R.id.tvRecording)

        cameraExecutor = Executors.newSingleThreadExecutor()

        if (modelLoaded) {
            tvStatus.text = "Model loaded (warm)"
        }

        btnText.setOnClickListener { runTextTest() }
        btnImage.setOnClickListener { onImageButtonPressed() }
        btnAudio.setOnClickListener { onAudioButtonPressed() }
        btnCapture.setOnClickListener { capturePhoto() }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    // ---- Model loading (once) ------------------------------------------------

    private fun ensureModelLoaded(
        enableImage: Boolean = false,
        enableAudio: Boolean = false,
        onReady: () -> Unit,
    ) {
        if (modelLoaded && engine != null && conversation != null) {
            onReady()
            return
        }

        setAllButtonsEnabled(false)
        appendOutput("Loading model from:\n  $MODEL_PATH")
        tvStatus.text = "Loading model..."

        thread {
            try {
                val t0 = SystemClock.elapsedRealtime()

                val engineConfig = EngineConfig(
                    modelPath = MODEL_PATH,
                    backend = Backend.GPU(),
                    visionBackend = if (enableImage) Backend.GPU() else null,
                    audioBackend = if (enableAudio) Backend.CPU() else null,
                    maxNumTokens = 1024,
                    cacheDir = getExternalFilesDir(null)?.absolutePath,
                )
                val eng = Engine(engineConfig)
                eng.initialize()

                val conv = eng.createConversation(
                    ConversationConfig(
                        samplerConfig = SamplerConfig(
                            topK = 40,
                            topP = 0.95,
                            temperature = 0.7,
                        ),
                    )
                )

                val loadMs = SystemClock.elapsedRealtime() - t0
                engine = eng
                conversation = conv
                modelLoaded = true

                Log.i(TAG, "Model loaded in ${loadMs}ms")
                runOnUiThread {
                    tvStatus.text = "Model loaded (${loadMs}ms)"
                    appendOutput("Model loaded in ${loadMs}ms")
                    setAllButtonsEnabled(true)
                    onReady()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Model load failed", e)
                runOnUiThread {
                    tvStatus.text = "LOAD FAILED"
                    appendOutput("ERROR loading model: ${e.message}")
                    setAllButtonsEnabled(true)
                }
            }
        }
    }

    private fun reloadEngine(
        enableImage: Boolean,
        enableAudio: Boolean,
        onReady: () -> Unit,
    ) {
        appendOutput("Reloading engine (image=$enableImage, audio=$enableAudio)...")
        tvStatus.text = "Reloading engine..."
        setAllButtonsEnabled(false)

        thread {
            try {
                try { conversation?.close() } catch (_: Exception) {}
                try { engine?.close() } catch (_: Exception) {}
                conversation = null
                engine = null
                modelLoaded = false

                val t0 = SystemClock.elapsedRealtime()

                val engineConfig = EngineConfig(
                    modelPath = MODEL_PATH,
                    backend = Backend.GPU(),
                    visionBackend = if (enableImage) Backend.GPU() else null,
                    audioBackend = if (enableAudio) Backend.CPU() else null,
                    maxNumTokens = 1024,
                    cacheDir = getExternalFilesDir(null)?.absolutePath,
                )
                val eng = Engine(engineConfig)
                eng.initialize()

                val conv = eng.createConversation(
                    ConversationConfig(
                        samplerConfig = SamplerConfig(
                            topK = 40,
                            topP = 0.95,
                            temperature = 0.7,
                        ),
                    )
                )

                val loadMs = SystemClock.elapsedRealtime() - t0
                engine = eng
                conversation = conv
                modelLoaded = true

                Log.i(TAG, "Engine reloaded in ${loadMs}ms")
                runOnUiThread {
                    tvStatus.text = "Engine reloaded (${loadMs}ms)"
                    appendOutput("Engine reloaded in ${loadMs}ms")
                    setAllButtonsEnabled(true)
                    onReady()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Engine reload failed", e)
                runOnUiThread {
                    tvStatus.text = "RELOAD FAILED"
                    appendOutput("ERROR reloading engine: ${e.message}")
                    setAllButtonsEnabled(true)
                }
            }
        }
    }

    // ---- Milestone 1: Text-only ----------------------------------------------

    private fun runTextTest() {
        val prompt = "You are a field medic assistant. A hiker says: " +
            "'I cut my hand on a rock, it's bleeding a lot.' " +
            "Describe the injury in one short sentence."

        ensureModelLoaded(enableImage = false, enableAudio = false) {
            runInference("TEXT", Contents.of(listOf(Content.Text(prompt))), prompt)
        }
    }

    // ---- Milestone 2: Live camera capture ------------------------------------

    private fun onImageButtonPressed() {
        if (cameraContainer.visibility == View.VISIBLE) {
            // Already showing preview — hide it
            cameraContainer.visibility = View.GONE
            return
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCameraPreview()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCameraPreview() {
        cameraContainer.visibility = View.VISIBLE
        appendOutput("Starting camera preview...")

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture
            )

            appendOutput("Camera ready — tap Capture")
        }, ContextCompat.getMainExecutor(this))
    }

    private fun capturePhoto() {
        val capture = imageCapture ?: run {
            appendOutput("ERROR: ImageCapture not ready")
            return
        }

        btnCapture.isEnabled = false
        appendOutput("Capturing photo...")

        capture.takePicture(cameraExecutor, object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(imageProxy: ImageProxy) {
                val bitmap = imageProxyToBitmap(imageProxy)
                imageProxy.close()

                if (bitmap == null) {
                    runOnUiThread {
                        appendOutput("ERROR: failed to convert captured image")
                        btnCapture.isEnabled = true
                    }
                    return
                }

                val scaled = scaleBitmap(bitmap, 1024)
                val pngBytes = bitmapToPng(scaled)

                runOnUiThread {
                    cameraContainer.visibility = View.GONE
                    // Unbind camera to free resources
                    ProcessCameraProvider.getInstance(this@TriageTestActivity).get().unbindAll()

                    appendOutput("Photo captured: ${scaled.width}x${scaled.height}, ${pngBytes.size} bytes")

                    reloadEngine(enableImage = true, enableAudio = false) {
                        val prompt = "Describe what you see in this image. Focus on any injuries or wounds."
                        val contents = Contents.of(listOf(
                            Content.ImageBytes(pngBytes),
                            Content.Text(prompt),
                        ))
                        runInference("IMAGE", contents, prompt)
                    }
                }
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e(TAG, "Photo capture failed", exception)
                runOnUiThread {
                    appendOutput("ERROR capturing photo: ${exception.message}")
                    btnCapture.isEnabled = true
                }
            }
        })
    }

    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
        val buffer = imageProxy.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: return null

        // Apply rotation from image metadata
        val rotation = imageProxy.imageInfo.rotationDegrees
        if (rotation == 0) return bitmap

        val matrix = Matrix()
        matrix.postRotate(rotation.toFloat())
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    // ---- Milestone 3: Live mic recording -------------------------------------

    private fun onAudioButtonPressed() {
        if (isRecording) {
            stopAudioRecording()
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED
            ) {
                startAudioRecording()
            } else {
                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    private fun startAudioRecording() {
        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBufferSize,
            )
        } catch (e: SecurityException) {
            appendOutput("ERROR: Microphone permission not granted")
            return
        }

        val recorder = audioRecord ?: return
        isRecording = true
        btnAudio.text = "Stop"
        tvRecording.visibility = View.VISIBLE
        setButtonsEnabled(btnText, false)
        setButtonsEnabled(btnImage, false)
        appendOutput("Recording audio (tap Stop when done, max ${MAX_RECORDING_SEC}s)...")

        recorder.startRecording()

        thread {
            val audioStream = ByteArrayOutputStream()
            val buffer = ByteArray(minBufferSize)
            val startMs = SystemClock.elapsedRealtime()

            while (isRecording) {
                val bytesRead = recorder.read(buffer, 0, buffer.size)
                if (bytesRead > 0) {
                    audioStream.write(buffer, 0, bytesRead)
                }
                val elapsed = SystemClock.elapsedRealtime() - startMs
                if (elapsed >= MAX_RECORDING_SEC * 1000L) {
                    runOnUiThread { appendOutput("Max recording duration reached.") }
                    break
                }
            }

            recorder.stop()
            recorder.release()
            audioRecord = null
            isRecording = false

            val pcmBytes = audioStream.toByteArray()
            val wavBytes = buildWav(pcmBytes, SAMPLE_RATE)
            val durationSec = pcmBytes.size.toFloat() / (SAMPLE_RATE * 2)

            Log.i(TAG, "Audio recorded: ${durationSec}s, ${wavBytes.size} bytes")

            runOnUiThread {
                btnAudio.text = "Audio"
                tvRecording.visibility = View.GONE
                setButtonsEnabled(btnText, true)
                setButtonsEnabled(btnImage, true)
                appendOutput("Audio recorded: %.1fs, ${wavBytes.size} bytes".format(durationSec))

                reloadEngine(enableImage = false, enableAudio = true) {
                    val prompt = "Listen to this audio and describe what the person is saying."
                    val contents = Contents.of(listOf(
                        Content.AudioBytes(wavBytes),
                        Content.Text(prompt),
                    ))
                    runInference("AUDIO", contents, prompt)
                }
            }
        }
    }

    private fun stopAudioRecording() {
        isRecording = false
    }

    // ---- Inference runner (shared) -------------------------------------------

    private fun runInference(label: String, contents: Contents, promptSummary: String) {
        setAllButtonsEnabled(false)
        appendOutput("\n--- $label TEST ---")
        appendOutput("Prompt: $promptSummary")
        appendOutput("Generating...")
        tvStatus.text = "Inferring ($label)..."

        val conv = conversation
        if (conv == null) {
            appendOutput("ERROR: conversation is null")
            setAllButtonsEnabled(true)
            return
        }

        val t0 = SystemClock.elapsedRealtime()
        val responseBuilder = StringBuilder()

        conv.sendMessageAsync(
            contents,
            object : MessageCallback {
                override fun onMessage(message: Message) {
                    val token = message.toString()
                    responseBuilder.append(token)
                }

                override fun onDone() {
                    val latencyMs = SystemClock.elapsedRealtime() - t0
                    val response = responseBuilder.toString()
                    Log.i(TAG, "[$label] Latency: ${latencyMs}ms")
                    Log.i(TAG, "[$label] Response: $response")
                    runOnUiThread {
                        appendOutput("Response (${latencyMs}ms):\n$response")
                        tvStatus.text = "$label done (${latencyMs}ms)"
                        setAllButtonsEnabled(true)
                    }
                }

                override fun onError(throwable: Throwable) {
                    if (throwable is CancellationException) {
                        Log.i(TAG, "[$label] Cancelled")
                        runOnUiThread {
                            appendOutput("Cancelled.")
                            setAllButtonsEnabled(true)
                        }
                    } else {
                        val latencyMs = SystemClock.elapsedRealtime() - t0
                        Log.e(TAG, "[$label] Error after ${latencyMs}ms", throwable)
                        runOnUiThread {
                            appendOutput("ERROR (${latencyMs}ms): ${throwable.message}")
                            tvStatus.text = "$label FAILED"
                            setAllButtonsEnabled(true)
                        }
                    }
                }
            },
            emptyMap(),
        )
    }

    // ---- Helpers --------------------------------------------------------------

    private fun appendOutput(text: String) {
        Log.d(TAG, text)
        tvOutput.append("$text\n")
        scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
    }

    private fun setAllButtonsEnabled(enabled: Boolean) {
        btnText.isEnabled = enabled
        btnImage.isEnabled = enabled
        btnAudio.isEnabled = enabled
        btnCapture.isEnabled = enabled
    }

    private fun setButtonsEnabled(button: Button, enabled: Boolean) {
        button.isEnabled = enabled
    }

    private fun scaleBitmap(bitmap: Bitmap, maxEdge: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= maxEdge && h <= maxEdge) return bitmap
        val scale = maxEdge.toFloat() / maxOf(w, h)
        return Bitmap.createScaledBitmap(bitmap, (w * scale).toInt(), (h * scale).toInt(), true)
    }

    private fun bitmapToPng(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }

    /** Build a WAV file from raw PCM bytes (16-bit mono). */
    private fun buildWav(pcmData: ByteArray, sampleRate: Int): ByteArray {
        val channels = 1
        val bitsPerSample = 16
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8
        val dataSize = pcmData.size
        val fileSize = 36 + dataSize

        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            // RIFF header
            put('R'.code.toByte()); put('I'.code.toByte())
            put('F'.code.toByte()); put('F'.code.toByte())
            putInt(fileSize)
            put('W'.code.toByte()); put('A'.code.toByte())
            put('V'.code.toByte()); put('E'.code.toByte())
            // fmt chunk
            put('f'.code.toByte()); put('m'.code.toByte())
            put('t'.code.toByte()); put(' '.code.toByte())
            putInt(16)                          // chunk size
            putShort(1)                         // PCM format
            putShort(channels.toShort())
            putInt(sampleRate)
            putInt(byteRate)
            putShort(blockAlign.toShort())
            putShort(bitsPerSample.toShort())
            // data chunk
            put('d'.code.toByte()); put('a'.code.toByte())
            put('t'.code.toByte()); put('a'.code.toByte())
            putInt(dataSize)
        }

        return header.array() + pcmData
    }
}
