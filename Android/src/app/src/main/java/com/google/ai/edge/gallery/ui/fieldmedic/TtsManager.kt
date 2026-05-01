package com.google.ai.edge.gallery.ui.fieldmedic

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale
import java.util.UUID

private const val TAG = "TtsManager"

class TtsManager(context: Context) {

    private var tts: TextToSpeech? = null
    private var ready = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking

    var isMuted = false

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val engine = tts ?: return@TextToSpeech
                engine.language = Locale.US
                engine.setSpeechRate(0.9f)
                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }
                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                    }
                    override fun onError(utteranceId: String?, errorCode: Int) {
                        Log.e(TAG, "TTS error: $errorCode")
                        _isSpeaking.value = false
                    }
                })
                ready = true
                Log.i(TAG, "TTS initialized")
            } else {
                Log.e(TAG, "TTS init failed: $status")
            }
        }
    }

    fun speak(text: String) {
        if (!ready || isMuted) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString())
    }

    fun stop() {
        tts?.stop()
        _isSpeaking.value = false
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ready = false
        _isSpeaking.value = false
    }
}
