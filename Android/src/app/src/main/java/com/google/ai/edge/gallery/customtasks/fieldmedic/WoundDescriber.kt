package com.google.ai.edge.gallery.customtasks.fieldmedic

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
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
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.ByteArrayOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val TAG = "WoundDescriber"
private const val FASTVLM_MODEL_PATH = "/data/local/tmp/FastVLM-0.5B.litertlm"
private const val DESCRIBE_PROMPT =
    "You are a medical visual assistant. In one short sentence (under 20 words), " +
        "describe any visible injury, wound, bleeding, burn, or trauma. Be specific about " +
        "body part and severity. If no injury is visible, respond with: 'No visible injury.'"

/** Produces a text description of a wound from a camera frame (FastVLM). */
interface WoundDescriber {
    suspend fun describe(bitmap: Bitmap): String
    fun close() {}
}

/** Stub — returns placeholder. Used as fallback if FastVLM fails to load. */
class StubWoundDescriber : WoundDescriber {
    override suspend fun describe(bitmap: Bitmap): String = "Wound visible in frame"
}

/**
 * FastVLM 0.5B LiteRT-LM wound describer.
 *
 * Loads the FastVLM model lazily on first describe() call, then reuses the engine across calls.
 * Each call creates a fresh Conversation so descriptions don't accumulate context.
 *
 * Calls are serialized via mutex to avoid concurrent inference (single GPU/engine).
 */
class FastVlmWoundDescriber(private val context: Context) : WoundDescriber {

    private var engine: Engine? = null
    private val mutex = Mutex()

    private fun ensureEngine(): Engine {
        engine?.let { return it }
        Log.i(TAG, "Initializing FastVLM engine from $FASTVLM_MODEL_PATH")
        val config = EngineConfig(
            modelPath = FASTVLM_MODEL_PATH,
            backend = Backend.GPU(),
            visionBackend = Backend.GPU(),
            // Image alone takes ~300 vision tokens; plus prompt + room for output.
            maxNumTokens = 1024,
            cacheDir = context.getExternalFilesDir(null)?.absolutePath,
        )
        val eng = Engine(config)
        eng.initialize()
        engine = eng
        Log.i(TAG, "FastVLM engine initialized")
        return eng
    }

    override suspend fun describe(bitmap: Bitmap): String = mutex.withLock {
        Log.i(TAG, "FastVLM describe() called | bitmap=${bitmap.width}x${bitmap.height}")

        val eng = try {
            ensureEngine()
        } catch (e: Exception) {
            Log.e(TAG, "FastVLM ENGINE INIT FAILED: ${e.javaClass.simpleName}: ${e.message}", e)
            return@withLock "Visual analysis unavailable (init: ${e.message})"
        }

        val conv = try {
            eng.createConversation(
                ConversationConfig(
                    samplerConfig = SamplerConfig(topK = 64, topP = 0.95, temperature = 1.0),
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "FastVLM CONVERSATION CREATE FAILED: ${e.javaClass.simpleName}: ${e.message}", e)
            return@withLock "Visual analysis unavailable (conv: ${e.message})"
        }

        try {
            val pngBytes = bitmapToPng(bitmap)
            Log.i(TAG, "FastVLM sending image (${pngBytes.size} bytes) + prompt to model...")
            val contents = listOf(
                Content.ImageBytes(pngBytes),
                Content.Text(DESCRIBE_PROMPT),
            )
            val rawResponse = sendMessage(conv, contents).trim()
            val response = cleanResponse(rawResponse)
            Log.i(TAG, "FastVLM RAW: \"$rawResponse\"")
            Log.i(TAG, "FastVLM CLEAN: \"$response\"")
            response.ifBlank { "No visible injury." }
        } catch (e: Exception) {
            Log.e(TAG, "FastVLM INFERENCE FAILED: ${e.javaClass.simpleName}: ${e.message}", e)
            "Visual analysis failed (${e.javaClass.simpleName}: ${e.message})"
        } finally {
            try { conv.close() } catch (_: Exception) {}
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

    // Strips Gemma chat-template control tokens that sometimes leak into FastVLM output.
    private fun cleanResponse(raw: String): String {
        return raw
            .replace(Regex("<start_of_turn>(?:user|model)?"), "")
            .replace("<end_of_turn>", "")
            .replace(Regex("<\\|.*?\\|>"), "")
            .lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .firstOrNull()
            ?.take(200)
            ?: ""
    }

    private fun bitmapToPng(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }

    override fun close() {
        try { engine?.close() } catch (_: Exception) {}
        engine = null
    }
}
