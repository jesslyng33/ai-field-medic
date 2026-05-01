package com.google.ai.edge.gallery.customtasks.fieldmedic

import android.util.Log
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.MessageCallback
import com.google.ai.edge.litertlm.SamplerConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val TAG = "ModeRouter"

enum class TriageMode { ASK, GUIDE }

enum class TriageIntent { DIAGNOSIS, EMERGENCY }

private val ROUTER_PROMPT_TEMPLATE = """
You are a router for a field-medic assistant. Decide whether the
assistant's NEXT response should ASK a question or GUIDE the user with
advice or a concrete action.

This is turn number %d of the conversation.

The user has selected the "%s" mode on their interface RIGHT NOW. This
is the strongest signal of what they want from this turn:
  - DIAGNOSIS  → they want to be diagnosed. Lean ASK so you can gather
                 information across different diagnostic angles (onset,
                 pain quality, severity, triggers, history). After 2–3
                 questions, if a REASONABLE diagnosis is possible from
                 the answers and image so far, switch to GUIDE — the
                 assistant should commit to a likely call and recommend
                 next steps rather than keep asking. Don't get stuck in
                 ASK forever.
  - EMERGENCY  → they want fast help. Lean GUIDE every turn. ASK only
                 if the next action is impossible to choose without one
                 critical missing fact.

Hard caps: never ASK more than 3 turns in a row. By turn 4–5 you should
mostly be in GUIDE regardless of intent.

Override the bias only on strong signals:
  - Visible severe injury in the image → GUIDE immediately even in
    DIAGNOSIS mode.
  - User reply was uselessly vague (e.g. "I don't know") → one more ASK
    is fine even in EMERGENCY mode.

Recent conversation (most recent last):
%s

Latest user input:
"%s"

Output exactly one word, no punctuation, no explanation: ASK or GUIDE.
""".trimIndent()

/**
 * Routes each turn of the live triage loop to either ASK or GUIDE mode using
 * a separate ephemeral [Conversation] on the shared Gemma [Engine]. The main
 * triage conversation never sees router prompts or outputs.
 *
 * Routing is intentionally text-only: attaching the camera frame here would
 * burn ~256+ image tokens per turn and force the vision backend to run
 * twice (router + main turn), which contributed to the post-few-turns hang.
 * The main conversation still gets the image.
 *
 * The [engineMutex] is shared with [TriageLoopViewModel] so that no two
 * sendMessageAsync calls run concurrently on the same Engine.
 */
class ModeRouter(
    private val engine: Engine,
) {

    suspend fun route(
        historySnippet: String,
        userText: String,
        imageBytes: ByteArray?,
        previousMode: TriageMode?,
        turnNumber: Int,
        userIntent: TriageIntent,
    ): TriageMode {
        // imageBytes is accepted for API compatibility but intentionally unused.
        @Suppress("UNUSED_VARIABLE") val ignoredImage = imageBytes
        val started = System.currentTimeMillis()
        Log.i(TAG, "▶ Router: createConversation + sendMessage...")
        return try {
            val conv = engine.createConversation(
                ConversationConfig(
                    samplerConfig = SamplerConfig(topK = 1, topP = 1.0, temperature = 0.0),
                )
            )
            try {
                val contents = mutableListOf<Content>()
                contents.add(
                    Content.Text(
                        ROUTER_PROMPT_TEMPLATE.format(
                            turnNumber,
                            userIntent.name,
                            historySnippet.ifBlank { "(none yet)" },
                            userText,
                        )
                    )
                )
                val raw = sendMessage(conv, contents)
                val parsed = parseMode(raw)
                val mode = parsed ?: previousMode ?: TriageMode.ASK
                val took = System.currentTimeMillis() - started
                Log.i(
                    TAG,
                    "[Router] mode=$mode parsed=${parsed != null} latency=${took}ms raw=\"${raw.take(60)}\"",
                )
                mode
            } finally {
                try { conv.close() } catch (_: Exception) {}
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val took = System.currentTimeMillis() - started
            val fallback = previousMode ?: TriageMode.ASK
            Log.e(TAG, "[Router] FAILED in ${took}ms; falling back to $fallback", e)
            fallback
        }
    }

    private suspend fun sendMessage(conv: Conversation, contents: List<Content>): String =
        suspendCancellableCoroutine { cont ->
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

    private fun parseMode(raw: String): TriageMode? {
        val cleaned = raw
            .replace(Regex("<start_of_turn>(?:user|model)?"), "")
            .replace("<end_of_turn>", "")
            .replace(Regex("<\\|.*?\\|>"), "")
            .uppercase()
        val askIdx = cleaned.indexOf("ASK")
        val guideIdx = cleaned.indexOf("GUIDE")
        return when {
            askIdx == -1 && guideIdx == -1 -> null
            askIdx == -1 -> TriageMode.GUIDE
            guideIdx == -1 -> TriageMode.ASK
            askIdx < guideIdx -> TriageMode.ASK
            else -> TriageMode.GUIDE
        }
    }
}
