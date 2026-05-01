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

// Engine context budget. Must match EngineConfig.maxNumTokens below.
// Gemma 4 E2B's model-side context window is 128K, but the real ceiling
// is GPU KV-cache memory on this device — bumping above 2048 made
// Engine.initialize() crash on the Galaxy at startup, so we hold here.
// The conversation-reset path below absorbs the smaller budget by
// rebuilding the session before it overflows.
private const val MAX_NUM_TOKENS = 2048
// When estimated KV-cache usage crosses this, rebuild the Conversation
// before the next turn rather than risk a silent overflow hang. Held at
// ~73% of the budget to leave room for one more full turn worth of
// directive + user input + response before the actual ceiling.
private const val CONTEXT_RESET_THRESHOLD = 1500
// Rough multimodal image cost in tokens. Gemma 4's image tokenizer has
// configurable budgets of 70 / 140 / 280 / 560 / 1120; the LiteRT Android
// SDK doesn't expose the dial, so we budget against the documented mid
// (280) which matches the model's default for general-purpose vision.
private const val IMAGE_TOKEN_ESTIMATE = 280

// Image-dedup tuning. We avg-hash each captured frame down to a 64-bit
// fingerprint; if the Hamming distance from the last frame we sent to
// Gemma is below the threshold, we skip re-attaching the image and let
// that turn run text-only. Camera shake on a static scene tends to land
// under 4 bits of difference; real medical changes (bleeding spread,
// swelling progression) tend to land above 8.
private const val IMAGE_HASH_DEDUP_THRESHOLD = 6
private const val IMAGE_HASH_DIM = 8 // 8×8 = 64-bit aHash

private val BASE_SYSTEM_PROMPT = """
Field medic helping one user via phone+camera. Second person only ("you","your").
MODES — EMERGENCY: act immediately, step-by-step. DIAGNOSIS: 2-3 questions then commit to best diagnosis+action.
Severe signs (heavy bleeding, chest pain, can't breathe, seizure) → guide immediately regardless of mode.
Patient record is above. Use silently, never recite it. Check allergies before any recommendation. Factor conditions/meds into severity. Only suggest kit items they have. Tailor to location. Solo=self-doable instructions only.
Camera: only note medically relevant observations. Ignore non-medical objects. Never narrate the view.
Never recite JSON, field names, or profile data. 1-2 sentences per turn. Vary focus. Never repeat.
""".trimIndent()

private val ASK_DIRECTIVE = "[MODE: ASK] Ask ONE new diagnostic question. No narration."

private val GUIDE_DIRECTIVE = "[MODE: GUIDE] Give next concrete action or diagnosis+action. No questions."

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

    private val _sessionReady = MutableStateFlow(false)
    val sessionReady: StateFlow<Boolean> = _sessionReady

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
    private val sessionStartMs = System.currentTimeMillis()
    private var engine: Engine? = null
    private var conversation: Conversation? = null
    private var modeRouter: ModeRouter? = null
    private var turnCount: Int = 0
    private var consecutiveAskCount: Int = 0
    private val maxConsecutiveAsks: Int = 3

    // Rough running estimate of the main conversation's KV-cache usage.
    // Used to decide when to rebuild the Conversation before LiteRT silently
    // overflows and stops calling onDone.
    @Volatile private var estimatedTokens: Int = 0
    val ttsManager = TtsManager(application)

    @Volatile private var latestFrameBitmap: Bitmap? = null
    @Volatile private var latestVlmDesc: String = ""
    // 64-bit aHash of the last frame we sent to Gemma. Used by
    // onFrameCaptured to skip frames that are visually ~identical so we
    // don't pay the image-token tax on every turn while the camera sits
    // on the same wound.
    @Volatile private var lastSentImageHash: Long? = null

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

                // Prime conversation with system context + patient record, then use hardcoded opening.
                // Use sendTurnRaw to bypass resetConversationIfNeeded — this IS the
                // initial priming, so there's nothing to reset yet.
                val systemPrompt = buildSystemPrompt()
                Log.i(TAG, "▶ startLoop: system prompt length=${systemPrompt.length}")
                val primingContents = listOf(Content.Text(systemPrompt))
                Log.i(TAG, "▶ startLoop: sending priming turn...")
                sendTurnRaw(primingContents)
                Log.i(TAG, "▶ startLoop: priming DONE")
                estimatedTokens = estimateTokens(primingContents)
                Log.i(TAG, "▶ startLoop: estimatedTokens=$estimatedTokens, speaking opening line")
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
                        try {
                            speechRecognizer?.cancel()
                        } catch (e: Exception) {
                            Log.e(TAG, "speechRecognizer.cancel() failed", e)
                        }
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
        Log.i(TAG, "▶ initEngine: creating EngineConfig")
        val config = EngineConfig(
            modelPath = MODEL_PATH,
            backend = Backend.GPU(),
            visionBackend = Backend.GPU(),
            audioBackend = Backend.CPU(),
            maxNumTokens = MAX_NUM_TOKENS,
            cacheDir = app.getExternalFilesDir(null)?.absolutePath,
        )
        Log.i(TAG, "▶ initEngine: Engine()")
        val eng = Engine(config)
        Log.i(TAG, "▶ initEngine: eng.initialize()")
        eng.initialize()
        Log.i(TAG, "▶ initEngine: createConversation()")
        engine = eng
        conversation = buildConversation(eng)
        estimatedTokens = 0
        modeRouter = ModeRouter(eng)
        Log.i(TAG, "▶ initEngine: DONE")
    }

    private fun buildConversation(eng: Engine): Conversation =
        eng.createConversation(
            ConversationConfig(
                samplerConfig = SamplerConfig(topK = 40, topP = 0.95, temperature = 0.7),
            )
        )

    // --- Speech recognition ---

    private fun setupSpeechRecognizer() {
        val app = getApplication<Application>()
        if (!SpeechRecognizer.isRecognitionAvailable(app)) {
            Log.w(TAG, "Speech recognition not available on this device")
            return
        }
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
        if (_isListening.value) return // already listening — avoid double-start crash
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
        }
        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "startListening failed", e)
            _isListening.value = false
        }
    }

    private fun startListeningDelayed() {
        Handler(Looper.getMainLooper()).postDelayed({
            if (ttsManager.isSpeaking.value) return@postDelayed
            startListening()
        }, 300)
    }

    // --- Turn processing ---

    private suspend fun processTurns() {
        Log.i(TAG, "▶ processTurns: started, waiting for turns...")
        for (turn in turnQueue) {
            _isProcessing.value = true
            try {
                val userText = when (turn) {
                    is TriageTurn.TouchTurn -> turn.input
                }
                Log.i(TAG, "▶ processTurns: got turn, userText=\"${userText.take(50)}\"")

                // Snapshot the frame
                val frame = latestFrameBitmap
                val frameBytes = frame?.let { bitmapToPng(it) }
                Log.i(TAG, "▶ processTurns: frame=${if (frame != null) "${frame.width}x${frame.height}" else "null"} frameBytes=${frameBytes?.size ?: 0}")
                if (frame != null) {
                    lastSentImageHash = perceptualHash(frame)
                    latestFrameBitmap = null
                    logMessage(TriageRole.SYSTEM, "Image attached to turn")
                }

                // Decide mode
                Log.i(TAG, "▶ processTurns: deciding mode (turnCount=$turnCount)")
                val mode = when {
                    turnCount == 0 -> {
                        Log.i(TAG, "▶ processTurns: turn 0, forcing ASK (skipping router)")
                        TriageMode.ASK
                    }
                    consecutiveAskCount >= maxConsecutiveAsks -> {
                        Log.i(TAG, "▶ processTurns: consecutive ASK cap, forcing GUIDE")
                        TriageMode.GUIDE
                    }
                    else -> {
                        Log.i(TAG, "▶ processTurns: calling ModeRouter...")
                        val router = modeRouter
                        val routed = router?.route(
                            historySnippet = formatHistoryForRouter(),
                            userText = userText,
                            imageBytes = frameBytes,
                            previousMode = _currentMode.value,
                            turnNumber = turnCount + 1,
                            userIntent = _userIntent.value,
                        ) ?: TriageMode.ASK
                        Log.i(TAG, "▶ processTurns: router returned $routed")
                        routed
                    }
                }
                _currentMode.value = mode
                consecutiveAskCount = if (mode == TriageMode.ASK) consecutiveAskCount + 1 else 0
                val directive = if (mode == TriageMode.ASK) ASK_DIRECTIVE else GUIDE_DIRECTIVE
                Log.i(TAG, "▶ processTurns: mode=$mode, building contents")

                val contents = mutableListOf<Content>()
                val textTokens = estimateTokens(listOf(Content.Text(directive), Content.Text(userText)))
                val roomForImage = (estimatedTokens + textTokens + IMAGE_TOKEN_ESTIMATE) < MAX_NUM_TOKENS
                Log.i(TAG, "▶ processTurns: estTokens=$estimatedTokens textTokens=$textTokens roomForImage=$roomForImage")
                if (frameBytes != null && roomForImage) {
                    contents.add(Content.ImageBytes(frameBytes))
                    Log.i(TAG, "▶ processTurns: image attached (${frameBytes.size} bytes)")
                } else if (frameBytes != null) {
                    Log.w(TAG, "▶ processTurns: skipping image — budget too tight")
                }
                contents.add(Content.Text(directive))
                contents.add(Content.Text(userText))
                logMessage(TriageRole.USER, userText)
                Log.i(TAG, "▶ processTurns: about to sendTurn with ${contents.size} content items")

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

    private fun estimateTokens(contents: List<Content>): Int {
        var total = 0
        for (c in contents) {
            when (c) {
                is Content.Text -> total += (c.text.length / 3) + 8
                is Content.ImageBytes -> total += IMAGE_TOKEN_ESTIMATE
                else -> total += 8
            }
        }
        return total
    }

    // If we're about to blow past the context budget, close the current
    // Conversation and start a fresh one re-primed with the system prompt
    // and a short recap of the last few turns. This is the actual fix for
    // "model hangs after a few turns" — Gemma's KV cache cannot grow forever
    // and LiteRT does not always surface overflow as an exception.
    private suspend fun resetConversationIfNeeded(nextTurnEstimate: Int) {
        if (estimatedTokens + nextTurnEstimate < CONTEXT_RESET_THRESHOLD) return
        val eng = engine ?: return
        Log.w(
            TAG,
            "[CtxReset] tokens≈$estimatedTokens + $nextTurnEstimate ≥ " +
                "$CONTEXT_RESET_THRESHOLD — rebuilding conversation",
        )
        debug("Resetting conversation (tokens≈$estimatedTokens)")
        try { conversation?.close() } catch (_: Exception) {}
        conversation = buildConversation(eng)
        estimatedTokens = 0
        // Fresh KV cache has no visual context either — clear the dedup hash
        // so the next captured frame goes through and re-grounds the model.
        lastSentImageHash = null

        val systemPrompt = buildSystemPrompt()
        val recap = formatRecapForResume()
        val priming = mutableListOf<Content>(Content.Text(systemPrompt))
        if (recap.isNotBlank()) {
            priming.add(
                Content.Text("Recent conversation so far (most recent last):\n$recap"),
            )
        }
        val primingTokens = estimateTokens(priming)
        try {
            sendTurnRaw(priming)
            estimatedTokens = primingTokens
            debug("Re-primed after reset (tokens≈$primingTokens)")
        } catch (e: Exception) {
            Log.e(TAG, "Re-priming after reset failed", e)
        }
    }

    private fun formatRecapForResume(maxTurns: Int = 4): String {
        val log = _conversationLog.value
            .filter { it.role != TriageRole.SYSTEM }
            .takeLast(maxTurns * 2)
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
        val inputEst = estimateTokens(contents)
        val types = contents.map { it::class.simpleName }.joinToString(",")
        Log.i(TAG, "▶ sendTurn: types=[$types] inputEst=$inputEst estTokens=$estimatedTokens")
        resetConversationIfNeeded(inputEst)
        Log.i(TAG, "▶ sendTurn: calling sendTurnRaw...")
        val result = sendTurnRaw(contents)
        estimatedTokens += inputEst + (result.length / 3) + 8
        Log.i(TAG, "▶ sendTurn: DONE result_len=${result.length} newEstTokens=$estimatedTokens")
        return result
    }

    // Bypasses the context-reset check so it can be used to re-prime the
    // freshly built conversation without recursing.
    // No timeout — native sendMessageAsync must complete before we release
    // the engine. Cancelling while native code is in-flight causes SIGSEGV.
    private suspend fun sendTurnRaw(contents: List<Content>): String {
        val conv = conversation ?: throw IllegalStateException("No conversation")
        Log.i(TAG, "▶ sendTurnRaw: calling sendMessageAsync on thread=${Thread.currentThread().name}")
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

            // Image-token dedup: if this frame is visually almost identical to
            // the last one we actually sent to Gemma, don't queue it. The
            // previous image is still in the KV cache and the model doesn't
            // need a redundant copy. Only computed when there's a frame to
            // compare against; the first frame after a reset always passes.
            val hash = perceptualHash(scaled)
            val previous = lastSentImageHash
            val skip = previous != null &&
                hammingDistance(hash, previous) <= IMAGE_HASH_DEDUP_THRESHOLD
            if (skip) {
                Log.i(
                    TAG,
                    "Frame captured (${scaled.width}x${scaled.height}) — skipped " +
                        "(visually unchanged, hamming<=$IMAGE_HASH_DEDUP_THRESHOLD)",
                )
                _vlmDescription.value = ""
                latestVlmDesc = ""
                return@launch
            }

            latestFrameBitmap = scaled
            _vlmDescription.value = ""
            latestVlmDesc = ""
            Log.i(TAG, "Frame captured (${scaled.width}x${scaled.height}) — queued for Gemma")
        }
    }

    // --- End session ---

    fun endSession() {
        viewModelScope.launch(Dispatchers.Default) {
            // Stop listening and speaking
            Handler(Looper.getMainLooper()).post {
                speechRecognizer?.cancel()
                _isListening.value = false
            }
            ttsManager.stop()
            turnJob?.cancel()

            // Generate AI summary from conversation log
            val summary = generateSummary()

            // Build report
            val report = SessionReport(
                aiSummary = summary,
                conversationLog = _conversationLog.value,
                sessionStartMs = sessionStartMs,
                sessionEndMs = System.currentTimeMillis(),
                patientName = AssessmentData.userContext?.name ?: "Unknown",
                location = AssessmentData.tripLocation,
                soloTraveler = AssessmentData.soloTraveler,
                firstAidKit = AssessmentData.firstAidKit,
            )
            AssessmentData.sessionReport = report
            _sessionReady.value = true
        }
    }

    /**
     * 64-bit average-hash of a bitmap. Downsamples to IMAGE_HASH_DIM × IMAGE_HASH_DIM
     * grayscale, computes mean, returns a bit per pixel (1 if above mean). Cheap
     * (~few ms) and stable to lighting/scale changes — but sensitive enough that
     * a real medical change in frame (bleeding spread, swelling) shifts enough
     * pixels to clear the dedup threshold.
     */
    private fun perceptualHash(bitmap: Bitmap): Long {
        val side = IMAGE_HASH_DIM
        val small = Bitmap.createScaledBitmap(bitmap, side, side, true)
        val pixels = IntArray(side * side)
        small.getPixels(pixels, 0, side, 0, 0, side, side)
        val grays = IntArray(side * side)
        var sum = 0
        for (i in pixels.indices) {
            val p = pixels[i]
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            // Rec. 601 luma — fine for our dedup needs and integer-fast.
            val gray = (r * 299 + g * 587 + b * 114) / 1000
            grays[i] = gray
            sum += gray
        }
        if (small !== bitmap) small.recycle()
        val mean = sum / grays.size
        var hash = 0L
        for (i in grays.indices) {
            if (grays[i] >= mean) hash = hash or (1L shl i)
        }
        return hash
    }

    private fun hammingDistance(a: Long, b: Long): Int =
        java.lang.Long.bitCount(a xor b)

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
