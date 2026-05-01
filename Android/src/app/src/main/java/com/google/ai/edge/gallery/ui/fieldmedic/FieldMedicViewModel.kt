package com.google.ai.edge.gallery.ui.fieldmedic

import android.app.Application
import android.graphics.Bitmap
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.AndroidViewModel
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CancellationException
import kotlin.concurrent.thread

private const val TAG = "FieldMedicVM"
private const val MODEL_PATH = "/data/local/tmp/gemma-4-E2B-it.litertlm"
private const val SAMPLE_RATE = 16000
private const val MAX_RECORDING_SEC = 15

enum class InferenceState { IDLE, LOADING, INFERRING, DONE, ERROR }

class FieldMedicViewModel(application: Application) : AndroidViewModel(application) {

    // --- Audio recording state ---
    private var audioRecord: AudioRecord? = null
    @Volatile var isRecording = false
        private set

    private val _audioRecorded = MutableStateFlow(false)
    val audioRecorded: StateFlow<Boolean> = _audioRecorded

    // --- Photo state ---
    private val _photoBitmap = MutableStateFlow<Bitmap?>(null)
    val photoBitmap: StateFlow<Bitmap?> = _photoBitmap

    // --- LLM state ---
    private var engine: Engine? = null
    private var conversation: Conversation? = null

    private val _inferenceState = MutableStateFlow(InferenceState.IDLE)
    val inferenceState: StateFlow<InferenceState> = _inferenceState

    private val _llmResponse = MutableStateFlow("")
    val llmResponse: StateFlow<String> = _llmResponse

    // ---- Audio Recording ----

    fun startRecording() {
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
            Log.e(TAG, "Mic permission not granted", e)
            return
        }

        val recorder = audioRecord ?: return
        isRecording = true
        _audioRecorded.value = false
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
                if (SystemClock.elapsedRealtime() - startMs >= MAX_RECORDING_SEC * 1000L) {
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

            AssessmentData.audioWavBytes = wavBytes
            _audioRecorded.value = true
        }
    }

    fun stopRecording() {
        isRecording = false
    }

    // ---- Photo ----

    fun setPhoto(bitmap: Bitmap) {
        val scaled = scaleBitmap(bitmap, 1024)
        _photoBitmap.value = scaled
        AssessmentData.photoBitmap = scaled
    }

    // ---- Notes ----

    fun setNotes(notes: String) {
        AssessmentData.notes = notes
    }

    // ---- LLM Inference ----

    fun runInference() {
        if (_inferenceState.value == InferenceState.LOADING || _inferenceState.value == InferenceState.INFERRING) return

        _inferenceState.value = InferenceState.LOADING
        _llmResponse.value = ""

        val hasAudio = AssessmentData.audioWavBytes != null
        val hasImage = AssessmentData.photoBitmap != null
        val hasText = AssessmentData.notes.isNotBlank()

        thread {
            try {
                // Load engine with correct backends
                val app = getApplication<Application>()
                val engineConfig = EngineConfig(
                    modelPath = MODEL_PATH,
                    backend = Backend.GPU(),
                    visionBackend = if (hasImage) Backend.GPU() else null,
                    audioBackend = if (hasAudio) Backend.CPU() else null,
                    maxNumTokens = 2048,
                    cacheDir = app.getExternalFilesDir(null)?.absolutePath,
                )

                // Close old engine if exists
                try { conversation?.close() } catch (_: Exception) {}
                try { engine?.close() } catch (_: Exception) {}

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
                engine = eng
                conversation = conv

                Log.i(TAG, "Engine loaded (image=$hasImage, audio=$hasAudio)")

                // Build contents
                val contentParts = mutableListOf<Content>()

                if (hasAudio) {
                    contentParts.add(Content.AudioBytes(AssessmentData.audioWavBytes!!))
                }
                if (hasImage) {
                    val pngBytes = bitmapToPng(AssessmentData.photoBitmap!!)
                    contentParts.add(Content.ImageBytes(pngBytes))
                }

                // Build the prompt
                val prompt = buildPrompt(hasAudio, hasImage, hasText)

                contentParts.add(Content.Text(prompt))

                val contents = Contents.of(contentParts)

                _inferenceState.value = InferenceState.INFERRING
                val responseBuilder = StringBuilder()

                conv.sendMessageAsync(
                    contents,
                    object : MessageCallback {
                        override fun onMessage(message: Message) {
                            responseBuilder.append(message.toString())
                            _llmResponse.value = responseBuilder.toString()
                        }

                        override fun onDone() {
                            Log.i(TAG, "Inference done: ${responseBuilder.length} chars")
                            _inferenceState.value = InferenceState.DONE
                        }

                        override fun onError(throwable: Throwable) {
                            if (throwable is CancellationException) {
                                Log.i(TAG, "Inference cancelled")
                            } else {
                                Log.e(TAG, "Inference error", throwable)
                            }
                            _llmResponse.value = responseBuilder.toString().ifEmpty {
                                "Error: ${throwable.message}"
                            }
                            _inferenceState.value = if (responseBuilder.isNotEmpty()) InferenceState.DONE else InferenceState.ERROR
                        }
                    },
                    emptyMap(),
                )
            } catch (e: Exception) {
                Log.e(TAG, "Engine/inference failed", e)
                _llmResponse.value = "Error loading model: ${e.message}"
                _inferenceState.value = InferenceState.ERROR
            }
        }
    }

    fun resetSession() {
        _inferenceState.value = InferenceState.IDLE
        _llmResponse.value = ""
        _audioRecorded.value = false
        _photoBitmap.value = null
        AssessmentData.clear()
    }

    override fun onCleared() {
        super.onCleared()
        try { conversation?.close() } catch (_: Exception) {}
        try { engine?.close() } catch (_: Exception) {}
    }

    // ---- Helpers ----

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

    private fun buildWav(pcmData: ByteArray, sampleRate: Int): ByteArray {
        val channels = 1
        val bitsPerSample = 16
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8
        val dataSize = pcmData.size
        val fileSize = 36 + dataSize

        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put('R'.code.toByte()); put('I'.code.toByte())
            put('F'.code.toByte()); put('F'.code.toByte())
            putInt(fileSize)
            put('W'.code.toByte()); put('A'.code.toByte())
            put('V'.code.toByte()); put('E'.code.toByte())
            put('f'.code.toByte()); put('m'.code.toByte())
            put('t'.code.toByte()); put(' '.code.toByte())
            putInt(16)
            putShort(1)
            putShort(channels.toShort())
            putInt(sampleRate)
            putInt(byteRate)
            putShort(blockAlign.toShort())
            putShort(bitsPerSample.toShort())
            put('d'.code.toByte()); put('a'.code.toByte())
            put('t'.code.toByte()); put('a'.code.toByte())
            putInt(dataSize)
        }

        return header.array() + pcmData
    }
}
