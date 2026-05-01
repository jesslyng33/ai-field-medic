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
private const val MIN_USEFUL_RESPONSE_CHARS = 10

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

A PATIENT MEDICAL RECORD has been attached above this prompt (between
the "=== PATIENT MEDICAL RECORD ===" markers). Use it silently on every
turn to ground your reasoning. Never read it aloud, never quote the JSON,
never say "according to your record" — speak as a medic who already
knows this person. Cross-reference it specifically:
  - Allergies → never suggest a medication, food, or material the user
    is allergic to. Mentally check the allergy list before recommending
    any drug, antiseptic, latex glove, adhesive, etc.
  - Conditions & medications → factor them into severity. Blood
    thinners mean bleeding is more serious. Asthma raises the bar on
    shortness of breath. Diabetes changes how you treat wounds and
    handle low energy. Heart conditions change CPR + chest pain
    triage. Beta blockers change pulse interpretation.
  - First aid kit → only recommend supplies the user ACTUALLY HAS in
    their kit. If the right tool isn't there, tell them how to
    improvise with what they do have or with household items.
  - Trip location → tailor your differential. Remote wilderness vs.
    urban, high altitude, cold/hot environment, distance to a
    hospital — these shift what's likely and how aggressive the
    self-care plan has to be.
  - Solo traveler flag → if true, never say "have someone help you"
    or "ask someone to". Every instruction must be self-doable with
    one or two hands.
  - Pregnancy, organ donor, DNR, advance directive → respect when
    they're relevant to the situation.

When you commit to a diagnosis (DIAGNOSIS mode) or fire off the first
action (EMERGENCY mode), weave the record-grounded reasoning into the
sentence naturally — for example "Given your asthma, this shortness
of breath needs your inhaler now" or "Wrap the gauze from your kit
twice around the cut and press hard". Make it feel like the advice
is for THIS person specifically.

REGIONAL HAZARDS (apply silently when the trip location matches):
  - Great Basin National Park, Nevada — Rubber Rabbitbrush blooms in
    late summer and releases heavy pollen; common cause of severe
    seasonal allergy symptoms (sneezing, itchy eyes, asthma flare,
    rhinitis) there.

You can SEE through their camera. The camera is ONLY useful when it
shows a body part or a medical concern. Otherwise IGNORE it.
  - DO NOT NARRATE the camera view. Never say things like "I see a
    laptop" or "I see a wall" or "I see a square bracket". Random
    objects in frame are not relevant — pretend the camera is off
    until something medical appears.
  - When you do see a relevant body part or symptom (cut, swelling,
    burn, rash, bleeding, etc.), describe it specifically and
    medically — but only the parts that matter to the diagnosis or
    next action.
  - Never invent visible details. Never describe colors, shapes, or
    objects unless they are part of an actual medical observation.
  - If the camera shows nothing useful, just ask your question or
    give your guidance based on what the user has told you. You can
    optionally ask them to point the camera at the problem area, but
    do not narrate what is or isn't there.

CRITICAL ANTI-NARRATION RULE:
You will see a patient context block above this prompt and possibly
an image. NEVER read, recite, list, or describe these inputs to the
user. Do not say field names, JSON keys, brackets, punctuation,
labels, or names of objects you see. The user already knows their
own profile — they don't need it read back. Speak as a medic who
silently knows everything in the context. Only mention specific
profile facts (allergy, condition, kit item, location) when they
directly inform the next sentence of advice.

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
Ask ONE focused question on a NEW angle you haven't covered yet —
onset, pain quality (sharp/dull/throbbing), severity 1–10, what
triggered it, what makes it better or worse, prior history, numbness
or tingling, dizziness, etc. Vary your phrasing. One question only.

Do NOT narrate the camera view. Do NOT describe random objects you
see. Do NOT list profile fields. Just ask the question.
""".trimIndent()

private val GUIDE_DIRECTIVE = """
[MODE: GUIDE]
Give the user the next concrete thing to do — or, if they want a
diagnosis, your best assessment plus the immediate next action. Use
what they've told you and what's medically visible. One step, or one
diagnosis plus one action. No question this turn.

Do NOT narrate the camera view. Do NOT describe random objects you
see. Do NOT list profile fields. Speak directly to the user.
""".trimIndent()

private fun buildSystemPrompt(): String {
    val ctx = AssessmentData.userContext
    if (ctx == null) {
        Log.w(
            TAG,
            "[Prompt] No userContext available — sending BASE_SYSTEM_PROMPT only. " +
                "Did onboarding/trip setup complete and call loadContext()?",
        )
        return BASE_SYSTEM_PROMPT
    }
    // Use the prose narrative, NOT the JSON. Smaller models recite JSON field names
    // and bracket characters back to the user when they appear in context.
    val block = ctx.toNarrativeBlock()
    Log.i(
        TAG,
        "[Prompt] Injecting medical record (prose): name=\"${ctx.name}\" " +
            "trip=\"${ctx.tripLocation}\" solo=${ctx.soloTraveler} " +
            "kit=${ctx.firstAidKit.size} allergies=${ctx.allergies.size} " +
            "conditions=${ctx.conditions.size} meds=${ctx.medications.size} " +
            "block_chars=${block.length}",
    )
    return buildString {
        appendLine(block)
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

    private val _debugLog = MutableStateFlow<List<String>>(emptyList())
    val debugLog: StateFlow<List<String>> = _debugLog

    private fun debug(line: String) {
        Log.i(TAG, "[Debug] $line")
        val ts = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date())
        _debugLog.value = (_debugLog.value + "$ts  $line").takeLast(40)
    }

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

                val firstRaw = try {
                    sendTurn(contents)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    debug("sendTurn threw: ${e.javaClass.simpleName}: ${e.message}")
                    throw e
                }
                debug("Turn ${turnCount + 1} mode=$mode raw_len=${firstRaw.length} raw=\"${firstRaw.take(60)}\"")
                var cleaned = cleanAssistantResponse(firstRaw)
                val tooShort = cleaned != null && cleaned.length < MIN_USEFUL_RESPONSE_CHARS

                if (cleaned == null || tooShort) {
                    val why = if (cleaned == null) "junk" else "too short (\"$cleaned\")"
                    debug("First reply $why — retrying once")
                    val retryRaw = try {
                        sendTurn(
                            listOf(
                                Content.Text(
                                    "Your previous reply was empty or too short. " +
                                        "Please give a complete, useful response to the user now in one or two sentences.",
                                )
                            )
                        )
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        debug("Retry sendTurn threw: ${e.javaClass.simpleName}: ${e.message}")
                        throw e
                    }
                    val retryCleaned = cleanAssistantResponse(retryRaw)
                    debug("Retry raw_len=${retryRaw.length} raw=\"${retryRaw.take(60)}\" cleaned_len=${retryCleaned?.length ?: 0}")
                    cleaned = retryCleaned ?: cleaned
                }

                if (cleaned == null) {
                    debug("Still junk after retry — dropping turn")
                    continue
                }
                _currentPrompt.value = cleaned
                ttsManager.speak(cleaned)
                logMessage(TriageRole.ASSISTANT, cleaned)
                turnCount += 1
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Turn processing error", e)
                debug("Turn processing error: ${e.javaClass.simpleName}: ${e.message}")
            } finally {
                _isProcessing.value = false
            }
        }
    }

    /**
     * Validates a raw model response. Strips a leading [MODE: ASK] / [MODE: GUIDE]
     * directive echo if present, then returns null if what's left is empty or
     * pure punctuation/brackets (the failure mode where the model emits just
     * "[" or "]" and stops). Otherwise returns the trimmed text to speak.
     */
    private fun cleanAssistantResponse(raw: String): String? {
        val withoutTag = raw.replaceFirst(
            Regex("^\\s*\\[\\s*MODE\\s*:\\s*(ASK|GUIDE)\\s*\\]\\s*", RegexOption.IGNORE_CASE),
            "",
        )
        val trimmed = withoutTag.trim()
        if (trimmed.isEmpty()) return null
        // Reject responses that contain no letters/digits at all (only brackets,
        // quotes, commas, periods, etc.). This is the "[", "]", "[]", "{}" case.
        if (!trimmed.any { it.isLetterOrDigit() }) return null
        return trimmed
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
