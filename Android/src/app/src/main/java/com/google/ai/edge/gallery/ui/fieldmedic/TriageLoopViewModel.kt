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
import com.google.ai.edge.gallery.customtasks.fieldmedic.FastVlmWoundDescriber
import com.google.ai.edge.gallery.customtasks.fieldmedic.ModeRouter
import com.google.ai.edge.gallery.customtasks.fieldmedic.StubWoundDescriber
import com.google.ai.edge.gallery.customtasks.fieldmedic.TriageIntent
import com.google.ai.edge.gallery.customtasks.fieldmedic.TriageMode
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

private val BASE_SYSTEM_PROMPT = """
You are a confident, knowledgeable field medic helping a single user through
a medical situation. They are alone with a phone, camera pointed at their
own injury (or at what's worrying them), and they will speak to you directly.
Speak to THEM in second person ("you", "your hand"). There is no third
person — only you and the user.

You opened the conversation with "What's your emergency?". The user has a
toggle on screen between two modes — DIAGNOSIS and EMERGENCY — which they
can switch at any time. Your guidance for that turn will be tagged with
the selected mode in mind:
  - EMERGENCY: pivot to ACTION fast. Spend at most one turn confirming
    the situation, then start guiding them through what to do, step by
    step, until they're stable.
  - DIAGNOSIS: ask 2–3 focused diagnostic questions across DIFFERENT
    angles (onset, location, pain quality, what makes it worse, prior
    history). After a few questions, if you can make a REASONABLE
    diagnosis from what you've heard and seen, commit to it and tell
    them what to do — even if you're not 100% certain, give your best
    likely call ("this sounds most like a tension headache; try…").
    Do not keep asking questions once you have enough to make a
    reasonable assessment. Don't drag it out.
Even when in EMERGENCY mode, if what you see or hear is clearly severe —
heavy bleeding, chest pain, unconscious-feeling, can't breathe, seizure,
severe burn — skip ahead and start guiding immediately.

You can SEE through their camera. Use the image honestly:
  - If the image clearly shows the problem area, name what you actually
    see — "I can see the cut on your left forearm, about an inch long,
    still bleeding a little." Be specific about real visual details.
  - If the camera shows nothing medically relevant (just background, a
    wall, the floor, too dark, too blurry, no body part visible), SAY
    SO PLAINLY and ask the user to point the camera at the problem.
    Do NOT invent injuries, redness, swelling, rashes, or anything
    else that isn't actually visible.
  - Only speculate about something you can genuinely see. If you can't
    see it, ask about it instead of guessing.
  - Tie what you say back to what's in the image whenever you can.

Each turn begins with a directive — [MODE: ASK] or [MODE: GUIDE]:
  - [MODE: ASK]   — ask one focused question.
  - [MODE: GUIDE] — give the next concrete thing to do, or a diagnosis.

Always:
  - Short, warm, direct. One or two sentences.
  - VARY your phrasing and what you focus on. If your last turn talked
    about location, this one might talk about timing, severity, or what
    you see. Do not start every reply the same way.
  - NEVER repeat yourself. Do not re-ask a question you've already asked,
    even rephrased. Do not restate observations you've already made. Do
    not give the same instruction twice. Read the conversation so far
    and skip anything already covered.
  - Don't mention the directive. Just answer in that mode.
""".trimIndent()

private val ASK_DIRECTIVE = """
[MODE: ASK]
If the image clearly shows the problem area, you MAY lead with a brief
one-clause observation of what's actually visible. If the image shows
nothing medical (just background, blank wall, blurry, no body part),
DO NOT pretend to see anything — instead ask them to point the camera
at the problem, or skip image-talk entirely and ask a different
question.

Then ask ONE focused question on a NEW angle you haven't covered yet —
onset, pain quality (sharp/dull/throbbing), severity 1–10, what
triggered it, what makes it better or worse, prior history, numbness
or tingling, dizziness, etc. Vary your phrasing. One question only.
""".trimIndent()

private val GUIDE_DIRECTIVE = """
[MODE: GUIDE]
Based on the image and what you've heard, give the user the next thing to
do — or, if they want a diagnosis, give your best assessment and what
they should do about it. Be specific to what's actually visible or what
they've actually told you.

If the image isn't showing anything useful (background, blurry, no
body part visible), base your guidance on what they SAID instead, and
optionally ask them to point the camera at the problem so you can help
better next turn. Don't invent visual details.

One concrete step, or one diagnosis plus one next action. No question
this turn.
""".trimIndent()

private fun buildSystemPrompt(): String {
    val ctx = AssessmentData.userContext ?: return BASE_SYSTEM_PROMPT
    return buildString {
        appendLine(ctx.toContextBlock())
        appendLine()
        append(BASE_SYSTEM_PROMPT)
    }
}

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

    private val _currentMode = MutableStateFlow(TriageMode.ASK)
    val currentMode: StateFlow<TriageMode> = _currentMode

    private val _userIntent = MutableStateFlow(TriageIntent.DIAGNOSIS)
    val userIntent: StateFlow<TriageIntent> = _userIntent

    fun setUserIntent(intent: TriageIntent) {
        if (_userIntent.value != intent) {
            _userIntent.value = intent
            Log.i(TAG, "User intent switched to $intent")
        }
    }

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
    private var modeRouter: ModeRouter? = null
    private var turnCount: Int = 0
    private var consecutiveAskCount: Int = 0
    private val maxConsecutiveAsks: Int = 3
    val ttsManager = TtsManager(application)

    @Volatile private var latestFrameBitmap: Bitmap? = null
    @Volatile private var latestVlmDesc: String = ""

    // Gate stubbed — all frames pass through. Real impls (MlKitFaceFrameGate,
    // EfficientDetFrameGate) are still in FrameGate.kt for later use.
    private val frameGate: FrameGate =
        com.google.ai.edge.gallery.customtasks.fieldmedic.StubFrameGate()
    // Describer stubbed — Gemma is multimodal and sees images directly, no need
    // for a separate VLM description step. FastVlmWoundDescriber is still in
    // WoundDescriber.kt for later use.
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

                // Prime conversation with system context + patient record, then use hardcoded opening
                sendTurn(listOf(Content.Text(buildSystemPrompt())))
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
        modeRouter = ModeRouter(eng)
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
                val userText = when (turn) {
                    is TriageTurn.TouchTurn -> turn.input
                }

                // Snapshot the frame once — used for both the router call and the main turn
                val frame = latestFrameBitmap
                val frameBytes = frame?.let { bitmapToPng(it) }
                if (frame != null) {
                    Log.i(TAG, "→→ Sending to Gemma | image attached | frame_size=${frame.width}x${frame.height}")
                    latestFrameBitmap = null
                    logMessage(TriageRole.SYSTEM, "Image attached to turn")
                } else {
                    Log.i(TAG, "→→ Sending to Gemma | no image attached")
                }

                // Decide mode: turn 1 is forced ASK; if we've ASKed too many in a
                // row, force GUIDE to stop pestering the user; otherwise route.
                val mode = when {
                    turnCount == 0 -> TriageMode.ASK
                    consecutiveAskCount >= maxConsecutiveAsks -> {
                        Log.i(TAG, "Hit consecutive ASK cap ($consecutiveAskCount) — forcing GUIDE")
                        TriageMode.GUIDE
                    }
                    else -> {
                        val router = modeRouter
                        router?.route(
                            historySnippet = formatHistoryForRouter(),
                            userText = userText,
                            imageBytes = frameBytes,
                            previousMode = _currentMode.value,
                            turnNumber = turnCount + 1,
                            userIntent = _userIntent.value,
                        ) ?: TriageMode.ASK
                    }
                }
                _currentMode.value = mode
                consecutiveAskCount = if (mode == TriageMode.ASK) consecutiveAskCount + 1 else 0
                val directive = if (mode == TriageMode.ASK) ASK_DIRECTIVE else GUIDE_DIRECTIVE
                Log.i(TAG, "Turn ${turnCount + 1} mode=$mode")

                val contents = mutableListOf<Content>()
                if (frameBytes != null) contents.add(Content.ImageBytes(frameBytes))
                contents.add(Content.Text(directive))
                contents.add(Content.Text(userText))
                logMessage(TriageRole.USER, userText)

                val response = sendTurn(contents)
                _currentPrompt.value = response
                ttsManager.speak(response)
                logMessage(TriageRole.ASSISTANT, response)
                turnCount += 1
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Turn processing error", e)
            } finally {
                _isProcessing.value = false
            }
        }
    }

    private fun formatHistoryForRouter(maxTurns: Int = 6): String {
        val log = _conversationLog.value
            .filter { it.role != TriageRole.SYSTEM }
            .takeLast(maxTurns)
        if (log.isEmpty()) return ""
        return log.joinToString("\n") { msg ->
            when (msg.role) {
                TriageRole.USER -> "PATIENT: ${msg.content}"
                TriageRole.ASSISTANT -> "MEDIC: ${msg.content}"
                TriageRole.SYSTEM -> ""
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
            // Stub gate always passes — store the frame for the next Gemma turn.
            // No describer call: Gemma sees the image directly.
            latestFrameBitmap = scaled
            _vlmDescription.value = ""
            latestVlmDesc = ""
            Log.i(TAG, "Frame captured (${scaled.width}x${scaled.height}) — queued for Gemma")
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
        try { woundDescriber.close() } catch (_: Exception) {}
        try { conversation?.close() } catch (_: Exception) {}
        try { engine?.close() } catch (_: Exception) {}
    }
}
