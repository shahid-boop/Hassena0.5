package com.example.services.voice

import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Phoneme-Level Lip-Sync Engine.
 * Synchronizes realistic mouth shapes (mouthOpen, mouthSmile, phoneme codes)
 * with actual TTS speech output in real-time.
 */
data class VisemeData(
    val id: Int,
    val phoneme: String,
    val mouthOpen: Float,
    val mouthSmile: Float,
    val durationMs: Long
)

class PhonemeLipSyncEngine {

    companion object {
        // Standard Viseme Table (0 to 11)
        val VISEME_SIL = VisemeData(0, "sil", 0.0f, 0.1f, 120L)
        val VISEME_P_B_M = VisemeData(1, "p_b_m", 0.02f, 0.12f, 90L)      // Bilabial (lips pressed)
        val VISEME_F_V = VisemeData(2, "f_v", 0.18f, 0.28f, 95L)          // Labiodental (teeth on lower lip)
        val VISEME_TH = VisemeData(3, "th", 0.24f, 0.22f, 90L)            // Dental
        val VISEME_T_D_N = VisemeData(4, "t_d_n", 0.30f, 0.38f, 85L)      // Alveolar
        val VISEME_S_Z = VisemeData(5, "s_z", 0.18f, 0.48f, 100L)         // Sibilant (tight smile)
        val VISEME_CH_J_SH = VisemeData(6, "ch_j_sh", 0.34f, 0.12f, 100L) // Post-alveolar
        val VISEME_K_G = VisemeData(7, "k_g", 0.38f, 0.22f, 85L)          // Velar
        val VISEME_AA_AH = VisemeData(8, "aa_ah", 0.88f, 0.18f, 130L)     // Open vowels (wide jaw)
        val VISEME_EE_IY = VisemeData(9, "ee_iy", 0.28f, 0.85f, 125L)     // Front unrounded vowel (wide smile)
        val VISEME_OO_UW = VisemeData(10, "oo_uw", 0.50f, 0.05f, 130L)    // Back rounded vowel (pucker/round)
        val VISEME_EH_AE = VisemeData(11, "eh_ae", 0.64f, 0.46f, 120L)    // Mid front vowel
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    private val _currentViseme = MutableStateFlow(VISEME_SIL)
    val currentViseme: StateFlow<VisemeData> = _currentViseme.asStateFlow()

    private var activeTimeline = mutableListOf<VisemeData>()
    private var timelineIndex = 0
    private var isPlaying = false
    private var currentUtteranceText: String = ""

    private val tickerRunnable = object : Runnable {
        override fun run() {
            if (!isPlaying || timelineIndex >= activeTimeline.size) {
                _currentViseme.value = VISEME_SIL
                isPlaying = false
                return
            }
            val frame = activeTimeline[timelineIndex]
            _currentViseme.value = frame
            timelineIndex++
            mainHandler.postDelayed(this, frame.durationMs.coerceAtLeast(40L))
        }
    }

    /**
     * Called when a new utterance is queued or begins speaking.
     * Generates a phoneme-aligned timeline based on text and timing.
     */
    fun startUtterance(text: String, estimatedDurationMs: Long = 0L) {
        stop()
        currentUtteranceText = text
        val frames = parseTextToVisemes(text)
        if (frames.isEmpty()) {
            _currentViseme.value = VISEME_SIL
            return
        }

        // Adjust durations if total estimated duration is known
        if (estimatedDurationMs > 300L) {
            val totalRawMs = frames.sumOf { it.durationMs }.coerceAtLeast(100L)
            val scale = estimatedDurationMs.toDouble() / totalRawMs
            val scaledFrames = frames.map { f ->
                val newDur = (f.durationMs * scale).toLong().coerceIn(45L, 250L)
                f.copy(durationMs = newDur)
            }
            activeTimeline = scaledFrames.toMutableList()
        } else {
            activeTimeline = frames.toMutableList()
        }

        timelineIndex = 0
        isPlaying = true
        mainHandler.post(tickerRunnable)
    }

    /**
     * High-precision Android TTS word/range callback.
     * When TextToSpeech calls onRangeStart, syncs the viseme directly with the spoken word span.
     */
    fun onRangeStart(utteranceId: String?, start: Int, end: Int) {
        if (currentUtteranceText.isEmpty() || start < 0 || end > currentUtteranceText.length || start >= end) {
            return
        }
        val spokenWord = currentUtteranceText.substring(start, end).trim()
        if (spokenWord.isEmpty()) return

        val wordVisemes = parseWordToVisemes(spokenWord)
        if (wordVisemes.isNotEmpty()) {
            mainHandler.removeCallbacks(tickerRunnable)
            activeTimeline = wordVisemes.toMutableList()
            timelineIndex = 0
            isPlaying = true
            mainHandler.post(tickerRunnable)
        }
    }

    /**
     * Stops lip-sync immediately and returns mouth to neutral resting position.
     */
    fun stop() {
        isPlaying = false
        mainHandler.removeCallbacks(tickerRunnable)
        timelineIndex = 0
        activeTimeline.clear()
        _currentViseme.value = VISEME_SIL
    }

    /**
     * Parses full sentence into a sequence of phoneme viseme frames with pauses.
     */
    private fun parseTextToVisemes(text: String): List<VisemeData> {
        val result = mutableListOf<VisemeData>()
        val words = text.split("\\s+".toRegex())

        for ((index, word) in words.withIndex()) {
            val clean = word.filter { it.isLetterOrDigit() }.lowercase(Locale.ROOT)
            if (clean.isNotEmpty()) {
                result.addAll(parseWordToVisemes(clean))
            }
            // Add natural inter-word micro pause
            result.add(VISEME_SIL.copy(durationMs = 45L))

            // Add longer punctuation pauses
            if (word.endsWith(".") || word.endsWith("!") || word.endsWith("?")) {
                result.add(VISEME_SIL.copy(durationMs = 180L))
            } else if (word.endsWith(",") || word.endsWith(";") || word.endsWith(":")) {
                result.add(VISEME_SIL.copy(durationMs = 110L))
            }
        }

        if (result.isEmpty()) {
            result.add(VISEME_SIL)
        }
        return result
    }

    /**
     * Maps an individual word into its constituent phonetic visemes.
     */
    private fun parseWordToVisemes(word: String): List<VisemeData> {
        val list = mutableListOf<VisemeData>()
        var i = 0
        val len = word.length

        while (i < len) {
            // Check 2-letter digraphs first
            if (i + 1 < len) {
                val pair = word.substring(i, i + 2)
                val digraphViseme = when (pair) {
                    "th" -> VISEME_TH
                    "sh", "ch" -> VISEME_CH_J_SH
                    "ph" -> VISEME_F_V
                    "ee", "ea" -> VISEME_EE_IY
                    "oo" -> VISEME_OO_UW
                    "ai", "ay" -> VISEME_EH_AE
                    "ou", "ow" -> VISEME_OO_UW
                    "aa" -> VISEME_AA_AH
                    "ck", "ng" -> VISEME_K_G
                    else -> null
                }
                if (digraphViseme != null) {
                    list.add(digraphViseme)
                    i += 2
                    continue
                }
            }

            // Single letter phoneme mapping
            val char = word[i]
            val singleViseme = when (char) {
                'a' -> VISEME_AA_AH
                'e' -> VISEME_EH_AE
                'i', 'y' -> VISEME_EE_IY
                'o' -> VISEME_OO_UW
                'u', 'w' -> VISEME_OO_UW
                'm', 'b', 'p' -> VISEME_P_B_M
                'f', 'v' -> VISEME_F_V
                's', 'z', 'c' -> VISEME_S_Z
                't', 'd', 'n', 'l', 'r' -> VISEME_T_D_N
                'j' -> VISEME_CH_J_SH
                'k', 'g', 'q', 'x' -> VISEME_K_G
                'h' -> VISEME_EH_AE.copy(mouthOpen = 0.4f, mouthSmile = 0.2f, durationMs = 60L)
                else -> VISEME_T_D_N
            }
            list.add(singleViseme)
            i++
        }
        return list
    }
}
