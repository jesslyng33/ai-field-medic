package com.google.ai.edge.gallery.ui.fieldmedic

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.edge.gallery.customtasks.fieldmedic.DetectionBox
import com.google.ai.edge.gallery.customtasks.fieldmedic.FrameGate
import com.google.ai.edge.gallery.customtasks.fieldmedic.MlKitFaceFrameGate
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.ByteArrayOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val TAG = "TriageLoopVM"
private const val MODEL_PATH = "/data/local/tmp/gemma-4-E2B-it.litertlm"
private const val FRAME_INTERVAL_MS = 5000L
private const val OPENING_LINE = "What's your emergency?"

private val SYSTEM_PROMPT = """
You are an emergency field medic dispatcher guiding a person through treating an injury in a remote location with no medical help available.

CRITICAL RULES:
- Every question MUST be answerable with yes or no.
- NEVER append "YES or NO", "yes or no", "Yes or No", or any variation to the end of a sentence. This is forbidden. End the question with a question mark only.
- NEVER ask open-ended questions. NEVER ask "what" or "how" or "describe".
- Examples of GOOD questions:
  * "Is there bleeding?"
  * "Is the person conscious?"
  * "Is the wound on the arm?"
- Examples of BAD questions (NEVER ask these):
  * "What happened?"
  * "Where is the injury?"
  * "Can you describe the wound?"
- Ask ONE question at a time.
- Keep each response to ONE short sentence.
- Use simple, direct language.
- If you receive an image, assess the visible injury and ask the next binary question.
- If the user presses YES or NO, incorporate it and ask the next binary question.
- Start by asking a binary question to narrow down the situation, e.g. "Is the person conscious?"
""".trimIndent()

enum class LoopState { INITIALIZING, RUNNING, ERROR }

enum class CameraFacing { BACK, FRONT }

enum class LoopActivity { LISTENING, SPEAKING, PROCESSING, IDLE }

enum class TriageRole { USER, ASSISTANT, SYSTEM }

data class TriageMessage(
    val role: TriageRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
)

sealed class TriageTurn {
    data class TouchTurn(val input: String) : TriageTurn()
}

class TriageLoopViewModel(application: Application) : AndroidViewModel(application) {

    // --- UI state ---
    private val _currentPrompt = MutableStateFlow("Initializing...")
    val currentPrompt: StateFlow<String> = _currentPrompt

    private val _userTranscript = MutableStateFlow("")
    val userTranscript: StateFlow<String> = _userTranscript

    private val _vlmDescription = MutableStateFlow("")
    val vlmDescription: StateFlow<String> = _vlmDescription

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing

    private val _frameGateStatus = MutableStateFlow<Boolean?>(null)
    val frameGateStatus: StateFlow<Boolean?> = _frameGateStatus

    private val _latestDetections = MutableStateFlow<List<DetectionBox>>(emptyList())
    val latestDetections: StateFlow<List<DetectionBox>> = _latestDetections

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted

    private val _isFlashOn = MutableStateFlow(false)
    val isFlashOn: StateFlow<Boolean> = _isFlashOn

    private val _cameraFacing = MutableStateFlow(CameraFacing.BACK)
    val cameraFacing: StateFlow<CameraFacing> = _cameraFacing

    private val _loopState = MutableStateFlow(LoopState.INITIALIZING)
    val loopState: StateFlow<LoopState> = _loopState

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening

    // Full conversation log — used for the post-crisis summary
    private val _conversationLog = MutableStateFlow<List<TriageMessage>>(emptyList())
    val conversationLog: StateFlow<List<TriageMessage>> = _conversationLog

    private fun logMessage(role: TriageRole, content: String) {
        if (content.isBlank()) return
        _conversationLog.value = _conversationLog.value + TriageMessage(role, content)
    }

    // --- Internal ---
    private var engine: Engine? = null
    private var conversation: Conversation? = null
    val ttsManager = TtsManager(application)

    @Volatile private var latestFrameBitmap: Bitmap? = null
    @Volatile private var latestVlmDesc: String = ""

    private val frameGate: FrameGate = try {
        MlKitFaceFrameGate()
    } catch (e: Exception) {
        Log.e(TAG, "Face detector failed to load, using stub gate", e)
        com.google.ai.edge.gallery.customtasks.fieldmedic.StubFrameGate()
    }
    private val woundDescriber: WoundDescriber = StubWoundDescriber()
    private val turnQueue = Channel<TriageTurn>(capacity = 8)

    private var turnJob: Job? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var started = false

    fun startLoop() {
        if (started) return
        started = true

        viewModelScope.launch(Dispatchers.Default) {
            try {
                initEngine()
                _loopState.value = LoopState.RUNNING

                // Prime conversation with system context, then use hardcoded opening
                sendTurn(listOf(Content.Text(SYSTEM_PROMPT)))
                _currentPrompt.value = OPENING_LINE
                ttsManager.speak(OPENING_LINE)
                logMessage(TriageRole.ASSISTANT, OPENING_LINE)
            } catch (e: Exception) {
                Log.e(TAG, "Engine init failed", e)
                _currentPrompt.value = "Error: ${e.message}"
                _loopState.value = LoopState.ERROR
                return@launch
            }

            turnJob = launch(Dispatchers.Default) { processTurns() }
        }

        // Cancel recognition when TTS starts speaking (prevent feedback)
        viewModelScope.launch {
            ttsManager.isSpeaking.collect { speaking ->
                if (speaking) {
                    Handler(Looper.getMainLooper()).post {
                        speechRecognizer?.cancel()
                        _isListening.value = false
                    }
                } else {
                    // TTS just finished — restart listening after a brief settle
                    if (_loopState.value == LoopState.RUNNING) {
                        delay(400)
                        Handler(Looper.getMainLooper()).post { startListening() }
                    }
                }
            }
        }

        // Set up speech recognition on main thread
        Handler(Looper.getMainLooper()).post { setupSpeechRecognizer() }
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
                samplerConfig = SamplerConfig(topK = 40, topP = 0.95, temperature = 0.7),
            )
        )
        engine = eng
        conversation = conv
        Log.i(TAG, "Engine initialized")
    }

    // --- Speech recognition ---

    private fun setupSpeechRecognizer() {
        val app = getApplication<Application>()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(app)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { _isListening.value = true }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { _isListening.value = false }

            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull() ?: return
                _userTranscript.value = partial
            }

            override fun onResults(results: Bundle?) {
                _isListening.value = false
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull() ?: ""
                if (text.isNotBlank()) {
                    _userTranscript.value = text
                    viewModelScope.launch {
                        turnQueue.send(TriageTurn.TouchTurn("User said: \"$text\""))
                        delay(2000)
                        _userTranscript.value = ""
                    }
                }
                startListeningDelayed()
            }

            override fun onError(error: Int) {
                _isListening.value = false
                Log.d(TAG, "SpeechRecognizer error code: $error")
                startListeningDelayed()
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        // Don't start listening immediately — wait for engine to be ready
        viewModelScope.launch {
            _loopState.collect { state ->
                if (state == LoopState.RUNNING && !ttsManager.isSpeaking.value) {
                    Handler(Looper.getMainLooper()).post { startListening() }
                }
            }
        }
    }

    private fun startListening() {
        if (ttsManager.isSpeaking.value || _loopState.value != LoopState.RUNNING) return
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
        }
        speechRecognizer?.startListening(intent)
    }

    private fun startListeningDelayed() {
        Handler(Looper.getMainLooper()).postDelayed({
            if (ttsManager.isSpeaking.value) return@postDelayed
            startListening()
        }, 300)
    }

    // --- Turn processing ---

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
                    latestFrameBitmap = null
                    logMessage(TriageRole.SYSTEM, "Visual: $desc")
                }

                when (turn) {
                    is TriageTurn.TouchTurn -> {
                        contents.add(Content.Text(turn.input))
                        logMessage(TriageRole.USER, turn.input)
                    }
                }

                val response = sendTurn(contents)
                _currentPrompt.value = response
                ttsManager.speak(response)
                logMessage(TriageRole.ASSISTANT, response)
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
                    override fun onMessage(message: Message) { sb.append(message.toString()) }
                    override fun onDone() { cont.resume(sb.toString()) }
                    override fun onError(throwable: Throwable) {
                        if (throwable is CancellationException) cont.cancel(throwable)
                        else cont.resumeWithException(throwable)
                    }
                },
                emptyMap(),
            )
        }
    }

    // --- Frame capture ---

    fun onFrameCaptured(bitmap: Bitmap) {
        viewModelScope.launch(Dispatchers.Default) {
            val scaled = scaleBitmap(bitmap, 512)
            val result = frameGate.analyze(scaled)
            _frameGateStatus.value = result.passed
            _latestDetections.value = result.detections
            if (result.passed) {
                val desc = woundDescriber.describe(scaled)
                _vlmDescription.value = desc
                latestFrameBitmap = scaled
                latestVlmDesc = desc
            }
        }
    }

    // --- Summary ---

    /**
     * Asks Gemma to summarize the full conversation log into a short incident report.
     * Safe to call after the triage session ends. Returns the summary text.
     */
    suspend fun generateSummary(): String {
        val log = _conversationLog.value
        if (log.isEmpty()) return "No conversation recorded."

        val transcript = log.joinToString("\n") { msg ->
            when (msg.role) {
                TriageRole.USER -> "PATIENT: ${msg.content}"
                TriageRole.ASSISTANT -> "MEDIC: ${msg.content}"
                TriageRole.SYSTEM -> "[${msg.content}]"
            }
        }

        val prompt = """
You are now writing a post-incident medical report. Forget the dispatcher role.

Summarize this emergency triage conversation as a brief medical incident report.
Include: type of injury, severity, body location, key symptoms, actions taken, and final status.
Use plain prose, under 120 words. Do NOT ask any questions.

Transcript:
$transcript

Report:
        """.trimIndent()

        return try {
            sendTurn(listOf(Content.Text(prompt)))
        } catch (e: Exception) {
            Log.e(TAG, "Summary generation failed", e)
            "Summary unavailable: ${e.message}"
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

    fun toggleFlash() { _isFlashOn.value = !_isFlashOn.value }

    fun toggleCameraFacing() {
        _cameraFacing.value =
            if (_cameraFacing.value == CameraFacing.BACK) CameraFacing.FRONT else CameraFacing.BACK
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

    // --- Cleanup ---

    override fun onCleared() {
        super.onCleared()
        turnJob?.cancel()
        Handler(Looper.getMainLooper()).post {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        }
        ttsManager.shutdown()
        frameGate.close()
        try { conversation?.close() } catch (_: Exception) {}
        try { engine?.close() } catch (_: Exception) {}
    }
}
