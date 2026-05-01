package com.google.ai.edge.gallery.ui.fieldmedic

import android.app.Application
import android.graphics.Bitmap
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.edge.gallery.customtasks.fieldmedic.EfficientDetFrameGate
import com.google.ai.edge.gallery.customtasks.fieldmedic.FrameGate
import com.google.ai.edge.gallery.customtasks.fieldmedic.StubWoundDescriber
import com.google.ai.edge.gallery.customtasks.fieldmedic.WoundDescriber
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val TAG = "TriageLoopVM"
private const val MODEL_PATH = "/data/local/tmp/gemma-4-E2B-it.litertlm"
private const val SAMPLE_RATE = 16000
private const val AUDIO_CHUNK_SEC = 7
private const val FRAME_INTERVAL_MS = 5000L
private const val EFFICIENTDET_PATH = "/data/local/tmp/efficientdet_lite0_detection.tflite"

private val SYSTEM_PROMPT = """
You are an emergency field medic dispatcher guiding a person through treating an injury in a remote location with no medical help available.

Rules:
- Ask ONE question at a time
- Keep responses under 2 sentences
- Use simple, direct language
- If you receive audio, listen to what the person said and respond accordingly
- If you receive an image, assess the visible injury
- If the user presses YES or NO, incorporate that answer and continue
- Start by asking what happened
""".trimIndent()

enum class LoopState { INITIALIZING, RUNNING, ERROR }

sealed class TriageTurn {
    data class AudioTurn(val wavBytes: ByteArray) : TriageTurn()
    data class TouchTurn(val input: String) : TriageTurn()
}

class TriageLoopViewModel(application: Application) : AndroidViewModel(application) {

    // --- UI state ---
    private val _currentPrompt = MutableStateFlow("Initializing...")
    val currentPrompt: StateFlow<String> = _currentPrompt

    private val _vlmDescription = MutableStateFlow("")
    val vlmDescription: StateFlow<String> = _vlmDescription

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing

    // Frame gate status: true = passed (person detected), false = dropped, null = no frame yet
    private val _frameGateStatus = MutableStateFlow<Boolean?>(null)
    val frameGateStatus: StateFlow<Boolean?> = _frameGateStatus

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted

    private val _isFlashOn = MutableStateFlow(false)
    val isFlashOn: StateFlow<Boolean> = _isFlashOn

    private val _loopState = MutableStateFlow(LoopState.INITIALIZING)
    val loopState: StateFlow<LoopState> = _loopState

    // --- Internal ---
    private var engine: Engine? = null
    private var conversation: Conversation? = null
    val ttsManager = TtsManager(application)

    // Latest camera context (updated passively, attached to next user turn)
    @Volatile private var latestFrameBitmap: Bitmap? = null
    @Volatile private var latestVlmDesc: String = ""
    private val frameGate: FrameGate = try {
        EfficientDetFrameGate(EFFICIENTDET_PATH)
    } catch (e: Exception) {
        Log.e(TAG, "EfficientDet failed to load, using stub gate", e)
        com.google.ai.edge.gallery.customtasks.fieldmedic.StubFrameGate()
    }
    private val woundDescriber: WoundDescriber = StubWoundDescriber()
    private val turnQueue = Channel<TriageTurn>(capacity = 8)

    private var audioJob: Job? = null
    private var turnJob: Job? = null
    private var audioRecord: AudioRecord? = null
    @Volatile private var isRecording = false

    private var started = false

    fun startLoop() {
        if (started) return
        started = true

        // Initialize engine on background thread
        viewModelScope.launch(Dispatchers.Default) {
            try {
                initEngine()
                _loopState.value = LoopState.RUNNING

                // Send initial prompt to get Gemma's first question
                val firstResponse = sendTurn(listOf(Content.Text(SYSTEM_PROMPT)))
                _currentPrompt.value = firstResponse
                ttsManager.speak(firstResponse)
            } catch (e: Exception) {
                Log.e(TAG, "Engine init failed", e)
                _currentPrompt.value = "Error: ${e.message}"
                _loopState.value = LoopState.ERROR
                return@launch
            }

            // Start turn processor
            turnJob = launch(Dispatchers.Default) { processTurns() }

            // Start audio recording
            audioJob = launch(Dispatchers.IO) { recordContinuously() }
        }
    }

    private fun initEngine() {
        val app = getApplication<Application>()
        val config = EngineConfig(
            modelPath = MODEL_PATH,
            backend = Backend.GPU(),
            visionBackend = Backend.GPU(),
            audioBackend = Backend.CPU(),
            maxNumTokens = 2048,
            cacheDir = app.getExternalFilesDir(null)?.absolutePath,
        )

        val eng = Engine(config)
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
        Log.i(TAG, "Engine initialized with all backends")
    }

    private suspend fun processTurns() {
        for (turn in turnQueue) {
            _isProcessing.value = true
            try {
                val contents = mutableListOf<Content>()

                // Attach latest camera context if available
                val frame = latestFrameBitmap
                val desc = latestVlmDesc
                if (frame != null && desc.isNotBlank()) {
                    contents.add(Content.ImageBytes(bitmapToPng(frame)))
                    contents.add(Content.Text("[Visual context: $desc]"))
                    latestFrameBitmap = null // consume it
                }

                // Add the user's actual input
                when (turn) {
                    is TriageTurn.AudioTurn -> {
                        contents.add(Content.AudioBytes(turn.wavBytes))
                        contents.add(Content.Text("The user just spoke. Listen to their audio and respond."))
                    }
                    is TriageTurn.TouchTurn -> {
                        contents.add(Content.Text(turn.input))
                    }
                }

                val response = sendTurn(contents)
                _currentPrompt.value = response
                ttsManager.speak(response)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Turn processing error", e)
            } finally {
                _isProcessing.value = false
            }
        }
    }

    private suspend fun sendTurn(contents: List<Content>): String {
        val conv = conversation ?: throw IllegalStateException("No conversation")
        return suspendCancellableCoroutine { cont ->
            val sb = StringBuilder()
            conv.sendMessageAsync(
                Contents.of(contents),
                object : MessageCallback {
                    override fun onMessage(message: Message) {
                        sb.append(message.toString())
                    }
                    override fun onDone() {
                        Log.i(TAG, "Turn done: ${sb.length} chars")
                        cont.resume(sb.toString())
                    }
                    override fun onError(throwable: Throwable) {
                        if (throwable is CancellationException) {
                            cont.cancel(throwable)
                        } else {
                            Log.e(TAG, "Inference error", throwable)
                            cont.resumeWithException(throwable)
                        }
                    }
                },
                emptyMap(),
            )
        }
    }

    // --- Audio recording ---

    private suspend fun recordContinuously() {
        while (viewModelScope.isActive) {
            // Pause while TTS is speaking to avoid feedback
            if (ttsManager.isSpeaking.value) {
                delay(200)
                continue
            }

            val wavBytes = recordChunk()
            if (wavBytes != null && wavBytes.size > 44) { // more than just header
                turnQueue.send(TriageTurn.AudioTurn(wavBytes))
            }
        }
    }

    private fun recordChunk(): ByteArray? {
        val minBuf = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )

        val recorder = try {
            AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBuf,
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "Mic permission denied", e)
            return null
        }

        isRecording = true
        recorder.startRecording()
        audioRecord = recorder

        val stream = ByteArrayOutputStream()
        val buffer = ByteArray(minBuf)
        val startMs = SystemClock.elapsedRealtime()

        while (isRecording && SystemClock.elapsedRealtime() - startMs < AUDIO_CHUNK_SEC * 1000L) {
            // Stop early if TTS starts speaking
            if (ttsManager.isSpeaking.value) break

            val bytesRead = recorder.read(buffer, 0, buffer.size)
            if (bytesRead > 0) {
                stream.write(buffer, 0, bytesRead)
            }
        }

        recorder.stop()
        recorder.release()
        audioRecord = null
        isRecording = false

        val pcm = stream.toByteArray()
        if (pcm.isEmpty()) return null

        return buildWav(pcm, SAMPLE_RATE)
    }

    // --- Frame capture ---

    fun onFrameCaptured(bitmap: Bitmap) {
        viewModelScope.launch(Dispatchers.Default) {
            val scaled = scaleBitmap(bitmap, 512)
            val passed = frameGate.shouldProcess(scaled)
            _frameGateStatus.value = passed
            if (passed) {
                val desc = woundDescriber.describe(scaled)
                _vlmDescription.value = desc
                // Store as passive context — will be attached to next user-triggered turn
                latestFrameBitmap = scaled
                latestVlmDesc = desc
            }
        }
    }

    // --- Touch ---

    fun onYesPressed() {
        viewModelScope.launch { turnQueue.send(TriageTurn.TouchTurn("User pressed YES")) }
    }

    fun onNoPressed() {
        viewModelScope.launch { turnQueue.send(TriageTurn.TouchTurn("User pressed NO")) }
    }

    // --- Toggles ---

    fun toggleMute() {
        ttsManager.isMuted = !ttsManager.isMuted
        _isMuted.value = ttsManager.isMuted
        if (ttsManager.isMuted) ttsManager.stop()
    }

    fun toggleFlash() {
        _isFlashOn.value = !_isFlashOn.value
    }

    // --- Helpers ---

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

    // --- Cleanup ---

    override fun onCleared() {
        super.onCleared()
        isRecording = false
        audioJob?.cancel()
        turnJob?.cancel()
        try { audioRecord?.stop() } catch (_: Exception) {}
        try { audioRecord?.release() } catch (_: Exception) {}
        ttsManager.shutdown()
        frameGate.close()
        try { conversation?.close() } catch (_: Exception) {}
        try { engine?.close() } catch (_: Exception) {}
    }
}
