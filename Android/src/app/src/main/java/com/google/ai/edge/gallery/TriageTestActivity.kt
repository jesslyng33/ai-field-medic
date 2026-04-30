/*
 * Field Medic — Gemma 4 E2B multimodal test harness.
 * Day 1: proves text, image, and audio input modalities work on-device.
 */

package com.google.ai.edge.gallery

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import android.app.Activity
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
import java.io.File
import java.util.concurrent.CancellationException
import kotlin.concurrent.thread

private const val TAG = "FieldMedic"

private const val MODEL_PATH =
    "/storage/emulated/0/Android/data/com.google.aiedge.gallery/files/gemma-4-E2B-it.litertlm"

private const val TEST_IMAGE_PATH =
    "/storage/emulated/0/Android/data/com.google.aiedge.gallery/files/test_wound.jpg"
private const val TEST_AUDIO_PATH =
    "/storage/emulated/0/Android/data/com.google.aiedge.gallery/files/test_audio.wav"

class TriageTestActivity : Activity() {

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_triage_test)

        tvStatus = findViewById(R.id.tvStatus)
        tvOutput = findViewById(R.id.tvOutput)
        scrollView = findViewById(R.id.scrollView)
        btnText = findViewById(R.id.btnTextOnly)
        btnImage = findViewById(R.id.btnImage)
        btnAudio = findViewById(R.id.btnAudio)

        if (modelLoaded) {
            tvStatus.text = "Model loaded (warm)"
        }

        btnText.setOnClickListener { runTextTest() }
        btnImage.setOnClickListener { runImageTest() }
        btnAudio.setOnClickListener { runAudioTest() }
    }

    // ---- Model loading (once) ------------------------------------------------

    private fun ensureModelLoaded(
        enableImage: Boolean = false,
        enableAudio: Boolean = false,
        onReady: () -> Unit,
    ) {
        if (modelLoaded && engine != null && conversation != null) {
            // Already warm — but if we need a modality we didn't enable before,
            // recreate the conversation (engine stays).
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

    /**
     * Tear down and rebuild Engine+Conversation with the requested modality flags.
     * This is needed because visionBackend / audioBackend are set at Engine creation time.
     */
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
                // Close old resources.
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

    // ---- Milestone 2: Text + Image -------------------------------------------

    private fun runImageTest() {
        val imageFile = File(TEST_IMAGE_PATH)
        if (!imageFile.exists()) {
            appendOutput("ERROR: test image not found at $TEST_IMAGE_PATH")
            appendOutput("Push a JPG with: adb push test_wound.jpg /sdcard/Download/test_wound.jpg")
            return
        }

        // Need vision backend — reload engine.
        reloadEngine(enableImage = true, enableAudio = false) { doImageInference() }
    }

    // ---- Milestone 3: Text + Audio -------------------------------------------

    private fun runAudioTest() {
        val audioFile = File(TEST_AUDIO_PATH)
        if (!audioFile.exists()) {
            appendOutput("ERROR: test audio not found at $TEST_AUDIO_PATH")
            appendOutput("Push a 16kHz mono WAV with: adb push test_audio.wav /sdcard/Download/test_audio.wav")
            return
        }

        // Need audio backend — reload engine.
        reloadEngine(enableImage = false, enableAudio = true) { doAudioInference() }
    }

    private fun doImageInference() {
        val prompt = "Describe what you see in this image. Focus on any injuries or wounds."
        val bitmap = BitmapFactory.decodeFile(TEST_IMAGE_PATH)
        if (bitmap == null) {
            appendOutput("ERROR: could not decode image at $TEST_IMAGE_PATH")
            return
        }
        val scaled = scaleBitmap(bitmap, 1024)
        val pngBytes = bitmapToPng(scaled)
        appendOutput("Image loaded: ${scaled.width}x${scaled.height}, ${pngBytes.size} bytes")
        val contents = Contents.of(listOf(
            Content.ImageBytes(pngBytes),
            Content.Text(prompt),
        ))
        runInference("IMAGE", contents, prompt)
    }

    private fun doAudioInference() {
        val prompt = "Listen to this audio and describe what the person is saying."
        val audioBytes = File(TEST_AUDIO_PATH).readBytes()
        appendOutput("Audio loaded: ${audioBytes.size} bytes")
        val contents = Contents.of(listOf(
            Content.AudioBytes(audioBytes),
            Content.Text(prompt),
        ))
        runInference("AUDIO", contents, prompt)
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
}
