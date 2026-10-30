package com.example.services.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.models.AIProviderType
import com.example.services.ai.ApiKeyStore
import java.util.Locale
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

class VoiceAssistantService(
    private val context: Context
) {

    private val mainHandler = Handler(Looper.getMainLooper())

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    val voiceApiKeyStore = VoiceApiKeyStore(context)
    val lipSyncEngine = PhonemeLipSyncEngine()
    private val elevenLabsPlayer = ElevenLabsVoicePlayer(context)
    private val geminiTtsPlayer = GeminiTtsPlayer(context)
    private val aiApiKeyStore = ApiKeyStore(context)

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _isTtsReady = MutableStateFlow(false)
    val isTtsReady: StateFlow<Boolean> = _isTtsReady.asStateFlow()

    private val _partialTranscript = MutableStateFlow("")
    val partialTranscript: StateFlow<String> = _partialTranscript.asStateFlow()

    private val _audioLevel = MutableStateFlow(0f)
    val audioLevel: StateFlow<Float> = _audioLevel.asStateFlow()

    private val _voiceError = MutableStateFlow<String?>(null)
    val voiceError: StateFlow<String?> = _voiceError.asStateFlow()

    private val _isWakeWordListening = MutableStateFlow(false)
    val isWakeWordListening: StateFlow<Boolean> = _isWakeWordListening.asStateFlow()

    private val activeUtteranceCount = AtomicInteger(0)
    private val sentenceBuffer = StringBuilder()

    private var speechPulseRunning = false
    private val speechPulseRunnable = object : Runnable {
        private var pulseTime = 0.0
        override fun run() {
            if (_isSpeaking.value) {
                pulseTime += 0.1
                val base = 0.40f + 0.35f * kotlin.math.sin(pulseTime * 7.5).toFloat()
                val jitter = ((kotlin.random.Random.nextFloat() - 0.5f) * 0.2f)
                _audioLevel.value = (base + jitter).coerceIn(0.15f, 0.95f)
                mainHandler.postDelayed(this, 50)
            } else {
                speechPulseRunning = false
                _audioLevel.value = 0f
            }
        }
    }

    private fun startSpeechPulse() {
        mainHandler.post {
            if (!speechPulseRunning) {
                speechPulseRunning = true
                mainHandler.post(speechPulseRunnable)
            }
        }
    }

    private fun stopSpeechPulse() {
        mainHandler.post {
            speechPulseRunning = false
            mainHandler.removeCallbacks(speechPulseRunnable)
            _audioLevel.value = 0f
        }
    }

    var onFinalTranscript: ((String) -> Unit)? = null
    var onWakeWordDetected: ((String) -> Unit)? = null
    var onTtsCompletedAllChunks: (() -> Unit)? = null
    var onBargeInTriggered: (() -> Unit)? = null

    init {
        initializeTts()
    }

    private fun initializeTts() {
        try {
            textToSpeech = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    textToSpeech?.language = Locale.forLanguageTag("ur-PK")
                    textToSpeech?.setSpeechRate(1.06f)
                    textToSpeech?.setPitch(1.0f)
                    _isTtsReady.value = true
                } else {
                    _isTtsReady.value = false
                }
            }.apply {
                setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                        startSpeechPulse()
                    }

                    override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                        lipSyncEngine.onRangeStart(utteranceId, start, end)
                    }

                    override fun onDone(utteranceId: String?) {
                        val remaining = activeUtteranceCount.decrementAndGet().coerceAtLeast(0)
                        if (remaining == 0) {
                            _isSpeaking.value = false
                            stopSpeechPulse()
                            lipSyncEngine.stop()
                            mainHandler.post {
                                onTtsCompletedAllChunks?.invoke()
                            }
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        val remaining = activeUtteranceCount.decrementAndGet().coerceAtLeast(0)
                        if (remaining == 0) {
                            _isSpeaking.value = false
                            stopSpeechPulse()
                            lipSyncEngine.stop()
                        }
                    }
                })
            }
        } catch (_: Exception) {
            _isTtsReady.value = false
        }
    }

    fun hasMicrophonePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Starts real-time Android Speech-to-Text with partial results enabled.
     * If Hasseena is currently speaking, triggers immediate Barge-In interruption first!
     */
    fun startListening(bargeInAllowed: Boolean = true) {
        if (!hasMicrophonePermission()) {
            _voiceError.value = "Microphone permission is required for voice commands."
            return
        }

        if (_isSpeaking.value && bargeInAllowed) {
            stopSpeakingImmediately()
            onBargeInTriggered?.invoke()
        }

        _voiceError.value = null
        _partialTranscript.value = ""

        mainHandler.post {
            try {
                if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                    _voiceError.value = "Speech recognition service is not installed or enabled on this device."
                    _isListening.value = false
                    return@post
                }

                if (speechRecognizer == null) {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                        setRecognitionListener(createRecognitionListener())
                    }
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                    )
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                }

                _isListening.value = true
                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                _isListening.value = false
                _voiceError.value = "Could not start microphone: ${e.localizedMessage ?: "SpeechRecognizer error"}"
            }
        }
    }

    fun startContinuousWakeWordListening() {
        if (!hasMicrophonePermission()) {
            _voiceError.value = "Microphone permission is required for wake-word detection."
            return
        }
        _isWakeWordListening.value = true
        startListening(bargeInAllowed = false)
    }

    fun stopContinuousWakeWordListening() {
        _isWakeWordListening.value = false
        stopListening()
    }

    fun stopListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
            } catch (_: Exception) {
            }
            _isListening.value = false
            _audioLevel.value = 0f
        }
    }

    /**
     * Immediate Barge-In / Interruption: stops TTS and clears sentence queue immediately.
     */
    fun stopSpeakingImmediately() {
        synchronized(sentenceBuffer) {
            sentenceBuffer.clear()
        }
        activeUtteranceCount.set(0)
        elevenLabsPlayer.stop()
        geminiTtsPlayer.stop()
        _isSpeaking.value = false
        stopSpeechPulse()
        lipSyncEngine.stop()
        try {
            textToSpeech?.stop()
        } catch (_: Exception) {
        }
    }

    /**
     * Speaks a complete short utterance immediately (e.g. "Opening YouTube.").
     */
    /** Plays a short ElevenLabs preview for the Voice Settings test button. */
    fun testElevenLabsVoice(voiceId: String, onResult: (Boolean, String) -> Unit) {
        val apiKey = voiceApiKeyStore.getApiKey()
        val cleanVoiceId = voiceId.trim()
        if (apiKey.isNullOrBlank()) {
            onResult(false, "Save your ElevenLabs API key first.")
            return
        }
        if (cleanVoiceId.isBlank()) {
            onResult(false, "Enter and save this voice pack's Voice ID first.")
            return
        }
        elevenLabsPlayer.stop()
        geminiTtsPlayer.stop()
        elevenLabsPlayer.speak(
            "Hello. I am JARVIS. Your selected voice is ready.",
            apiKey,
            cleanVoiceId
        ) { success ->
            mainHandler.post {
                onResult(success, if (success) "Voice test played successfully." else "Voice test failed. Check the Voice ID, API key, network, and ElevenLabs quota.")
            }
        }
    }

    fun speakImmediate(text: String) {
        val cleaned = cleanMarkdownForSpeech(text)
        if (cleaned.isBlank()) return
        stopSpeakingImmediately()
        enqueueCleanedSentence(cleaned)
    }

    /**
     * Streaming TTS Pipeline: feeds incoming AI token deltas into a sentence buffer
     * and starts speaking as soon as the first sentence boundary is reached.
     */
    fun resetStreamingTtsBuffer() {
        synchronized(sentenceBuffer) {
            sentenceBuffer.clear()
        }
    }

    fun feedStreamingDeltaForTts(delta: String, enabled: Boolean) {
        if (!enabled || delta.isEmpty()) return
        val sentencesToSpeak = mutableListOf<String>()

        synchronized(sentenceBuffer) {
            sentenceBuffer.append(delta)
            var current = sentenceBuffer.toString()

            while (true) {
                val boundaryIdx = findFirstSentenceBoundary(current)
                if (boundaryIdx != -1 && boundaryIdx >= 16) {
                    val sentence = current.substring(0, boundaryIdx + 1).trim()
                    if (sentence.isNotEmpty()) {
                        sentencesToSpeak.add(sentence)
                    }
                    current = current.substring(boundaryIdx + 1)
                } else {
                    break
                }
            }
            sentenceBuffer.clear()
            sentenceBuffer.append(current)
        }

        sentencesToSpeak.forEach { rawSentence ->
            val cleaned = cleanMarkdownForSpeech(rawSentence)
            if (cleaned.isNotBlank()) {
                enqueueCleanedSentence(cleaned)
            }
        }
    }

    fun flushRemainingTtsBuffer(enabled: Boolean) {
        if (!enabled) {
            synchronized(sentenceBuffer) { sentenceBuffer.clear() }
            return
        }
        val tail = synchronized(sentenceBuffer) {
            val str = sentenceBuffer.toString().trim()
            sentenceBuffer.clear()
            str
        }
        val cleaned = cleanMarkdownForSpeech(tail)
        if (cleaned.isNotBlank()) {
            enqueueCleanedSentence(cleaned)
        }
    }

    private fun enqueueCleanedSentence(sentence: String) {
        val apiKey = voiceApiKeyStore.getApiKey()
        val voiceId = voiceApiKeyStore.getVoiceId(voiceApiKeyStore.getSelectedVoicePack())
        if (!apiKey.isNullOrBlank() && voiceId.isNotBlank()) {
            activeUtteranceCount.incrementAndGet()
            _isSpeaking.value = true
            startSpeechPulse()
            lipSyncEngine.startUtterance(sentence)
            elevenLabsPlayer.speak(sentence, apiKey, voiceId) { onlineSuccess ->
                if (onlineSuccess) {
                    val remaining = activeUtteranceCount.decrementAndGet().coerceAtLeast(0)
                    if (remaining == 0) {
                        _isSpeaking.value = false
                        stopSpeechPulse()
                        lipSyncEngine.stop()
                        mainHandler.post { onTtsCompletedAllChunks?.invoke() }
                    }
                } else {
                    // Online voice failed or quota/network unavailable: use Android TTS as fallback.
                    mainHandler.post {
                        if (_isTtsReady.value) {
                            val utteranceId = UUID.randomUUID().toString()
                            try {
                                lipSyncEngine.startUtterance(sentence)
                                val result = textToSpeech?.speak(sentence, TextToSpeech.QUEUE_ADD, null, utteranceId)
                                if (result == TextToSpeech.ERROR) {
                                    val remaining = activeUtteranceCount.decrementAndGet().coerceAtLeast(0)
                                    if (remaining == 0) { _isSpeaking.value = false; stopSpeechPulse(); lipSyncEngine.stop() }
                                }
                            } catch (_: Exception) {
                                val remaining = activeUtteranceCount.decrementAndGet().coerceAtLeast(0)
                                if (remaining == 0) { _isSpeaking.value = false; stopSpeechPulse(); lipSyncEngine.stop() }
                            }
                        } else {
                            val remaining = activeUtteranceCount.decrementAndGet().coerceAtLeast(0)
                            if (remaining == 0) { _isSpeaking.value = false; stopSpeechPulse(); lipSyncEngine.stop() }
                        }
                    }
                }
            }
            return
        }
        val geminiKey = aiApiKeyStore.get(AIProviderType.GEMINI).trim()
        if (geminiKey.isNotBlank() && !geminiKey.startsWith("MY_")) {
            activeUtteranceCount.incrementAndGet()
            _isSpeaking.value = true
            startSpeechPulse()
            lipSyncEngine.startUtterance(sentence)
            geminiTtsPlayer.speak(sentence, geminiKey) { onlineSuccess ->
                mainHandler.post {
                    if (onlineSuccess) {
                        val remaining = activeUtteranceCount.decrementAndGet().coerceAtLeast(0)
                        if (remaining == 0) {
                            _isSpeaking.value = false
                            stopSpeechPulse()
                            lipSyncEngine.stop()
                            mainHandler.post { onTtsCompletedAllChunks?.invoke() }
                        }
                    } else if (_isTtsReady.value) {
                        val utteranceId = UUID.randomUUID().toString()
                        try {
                            lipSyncEngine.startUtterance(sentence)
                            val result = textToSpeech?.speak(
                                sentence, TextToSpeech.QUEUE_ADD, null, utteranceId
                            )
                            if (result == TextToSpeech.ERROR) {
                                val remaining = activeUtteranceCount.decrementAndGet().coerceAtLeast(0)
                                if (remaining == 0) {
                                    _isSpeaking.value = false
                                    stopSpeechPulse()
                                    lipSyncEngine.stop()
                                }
                            }
                        } catch (_: Exception) {
                            val remaining = activeUtteranceCount.decrementAndGet().coerceAtLeast(0)
                            if (remaining == 0) {
                                _isSpeaking.value = false
                                stopSpeechPulse()
                                lipSyncEngine.stop()
                            }
                        }
                    } else {
                        val remaining = activeUtteranceCount.decrementAndGet().coerceAtLeast(0)
                        if (remaining == 0) {
                            _isSpeaking.value = false
                            stopSpeechPulse()
                            lipSyncEngine.stop()
                        }
                    }
                }
            }
            return
        }
        if (!_isTtsReady.value) return
        try {
            val utteranceId = UUID.randomUUID().toString()
            activeUtteranceCount.incrementAndGet()
            _isSpeaking.value = true
            startSpeechPulse()
            lipSyncEngine.startUtterance(sentence)
            textToSpeech?.speak(sentence, TextToSpeech.QUEUE_ADD, null, utteranceId)
        } catch (_: Exception) {
            activeUtteranceCount.decrementAndGet()
            lipSyncEngine.stop()
        }
    }

    private fun findFirstSentenceBoundary(text: String): Int {
        for (i in text.indices) {
            val ch = text[i]
            if (ch == '\n' && i >= 16) return i
            if ((ch == '.' || ch == '!' || ch == '?') && i >= 16) {
                val nextChar = text.getOrNull(i + 1)
                if (nextChar == null || nextChar.isWhitespace()) {
                    return i
                }
            }
        }
        return -1
    }

    private fun cleanMarkdownForSpeech(raw: String): String {
        return raw
            .replace(Regex("\\[TOOL:[^\\]]*\\]"), "")
            .replace(Regex("\\*\\*([^*]+)\\*\\*"), "$1")
            .replace(Regex("\\*([^*]+)\\*"), "$1")
            .replace(Regex("`([^`]+)`"), "$1")
            .replace(Regex("#+\\s*"), "")
            .replace(Regex("\\[(\\d+)\\]"), "")
            .replace("•", "")
            .trim()
    }

    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _isListening.value = true
                _voiceError.value = null
            }

            override fun onBeginningOfSpeech() {
                _isListening.value = true
            }

            override fun onRmsChanged(rmsdB: Float) {
                val normalized = ((rmsdB + 2f) / 12f).coerceIn(0.05f, 1.0f)
                _audioLevel.value = normalized
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                _isListening.value = false
                _audioLevel.value = 0f
            }

            override fun onError(error: Int) {
                _isListening.value = false
                _audioLevel.value = 0f
                val partial = _partialTranscript.value.trim()
                if (partial.isNotEmpty() && (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT)) {
                    _partialTranscript.value = ""
                    if (_isWakeWordListening.value && (partial.contains("jarvis", ignoreCase = true) || partial.contains("hasseena", ignoreCase = true))) {
                        onWakeWordDetected?.invoke(partial)
                        return
                    }
                    onFinalTranscript?.invoke(partial)
                    return
                }

                // If in continuous wake-word mode, restart listening after a brief delay
                if (_isWakeWordListening.value && !_isSpeaking.value) {
                    mainHandler.postDelayed({
                        if (_isWakeWordListening.value && !_isSpeaking.value) {
                            startListening(bargeInAllowed = false)
                        }
                    }, 450)
                    return
                }

                val message = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Microphone audio recording error."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission denied."
                    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network error during speech recognition."
                    SpeechRecognizer.ERROR_NO_MATCH -> "Didn't catch that. Tap the orb to speak again."
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected."
                    else -> null
                }
                if (message != null) {
                    _voiceError.value = message
                }
            }

            override fun onResults(results: Bundle?) {
                _isListening.value = false
                _audioLevel.value = 0f
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val best = matches?.firstOrNull()?.trim().orEmpty()
                _partialTranscript.value = ""
                if (best.isNotEmpty()) {
                    if (_isWakeWordListening.value && (best.contains("jarvis", ignoreCase = true) || best.contains("hasseena", ignoreCase = true))) {
                        onWakeWordDetected?.invoke(best)
                        return
                    }
                    onFinalTranscript?.invoke(best)
                } else if (_isWakeWordListening.value && !_isSpeaking.value) {
                    mainHandler.postDelayed({
                        if (_isWakeWordListening.value && !_isSpeaking.value) {
                            startListening(bargeInAllowed = false)
                        }
                    }, 450)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val partial = matches?.firstOrNull()?.trim().orEmpty()
                if (partial.isNotEmpty()) {
                    _partialTranscript.value = partial
                    if (_isWakeWordListening.value && (partial.contains("jarvis", ignoreCase = true) || partial.contains("hasseena", ignoreCase = true))) {
                        _partialTranscript.value = ""
                        onWakeWordDetected?.invoke(partial)
                    }
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    fun clearVoiceError() {
        _voiceError.value = null
    }

    fun release() {
        stopSpeechPulse()
        elevenLabsPlayer.release()
        geminiTtsPlayer.release()
        mainHandler.post {
            try {
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (_: Exception) {
            }
            try {
                textToSpeech?.stop()
                textToSpeech?.shutdown()
                textToSpeech = null
            } catch (_: Exception) {
            }
        }
    }
}
