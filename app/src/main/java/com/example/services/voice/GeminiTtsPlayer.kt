package com.example.services.voice

import android.content.Context
import android.media.MediaPlayer
import android.util.Base64
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/** Gemini speech playback. Audio PCM is wrapped in a WAV header for MediaPlayer. */
class GeminiTtsPlayer(context: Context) {
    private val appContext = context.applicationContext
    private val client = OkHttpClient.Builder()
        .callTimeout(60, TimeUnit.SECONDS)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val generation = AtomicInteger(0)

    @Volatile private var activeCall: okhttp3.Call? = null
    @Volatile private var mediaPlayer: MediaPlayer? = null
    @Volatile private var playbackLatch: CountDownLatch? = null
    @Volatile private var released = false

    fun speak(text: String, apiKey: String, onComplete: (Boolean) -> Unit) {
        val ticket = generation.get()
        executor.execute {
            if (released || ticket != generation.get()) return@execute
            var success = false
            var file: File? = null
            try {
                val body = JSONObject()
                    .put("contents", JSONArray().put(
                        JSONObject().put("parts", JSONArray().put(
                            JSONObject().put(
                                "text",
                                "Speak the following text in natural Urdu as spoken in Pakistan. Do not translate it:\n$text"
                            )
                        ))
                    ))
                    .put("generationConfig", JSONObject()
                        .put("responseModalities", JSONArray().put("AUDIO"))
                        .put("speechConfig", JSONObject()
                            .put("voiceConfig", JSONObject()
                                .put("prebuiltVoiceConfig",
                                    JSONObject().put("voiceName", "Achernar")
                                )
                            )
                        )
                    )
                    .toString()

                val encodedKey = java.net.URLEncoder.encode(apiKey.trim(), "UTF-8")
                val request = Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash-tts:generateContent?key=$encodedKey")
                    .post(body.toRequestBody("application/json; charset=utf-8".toMediaType()))
                    .build()

                val call = client.newCall(request)
                activeCall = call
                val pcm = call.execute().use { response ->
                    if (!response.isSuccessful) throw IOException("Gemini TTS HTTP ${response.code}")
                    val json = JSONObject(response.body?.string() ?: throw IOException("Empty Gemini response"))
                    val parts = json.getJSONArray("candidates")
                        .getJSONObject(0).getJSONObject("content").getJSONArray("parts")
                    var audio: ByteArray? = null
                    for (i in 0 until parts.length()) {
                        val part = parts.getJSONObject(i)
                        if (part.has("inlineData")) {
                            val inline = part.getJSONObject("inlineData")
                            if (inline.optString("mimeType").startsWith("audio/")) {
                                audio = Base64.decode(inline.getString("data"), Base64.DEFAULT)
                                break
                            }
                        }
                    }
                    audio?.takeIf { it.isNotEmpty() } ?: throw IOException("Gemini audio data missing")
                }

                if (ticket != generation.get() || released) return@execute
                // Gemini TTS PCM output is 24 kHz, signed 16-bit, mono.
                val wav = wavHeader(pcm.size, 24000) + pcm
                file = File.createTempFile("jarvis_gemini_", ".wav", appContext.cacheDir)
                    .apply { writeBytes(wav) }

                val latch = CountDownLatch(1)
                playbackLatch = latch
                val mp = MediaPlayer()
                mediaPlayer = mp
                mp.setOnCompletionListener { latch.countDown() }
                mp.setOnErrorListener { _, _, _ -> latch.countDown(); true }
                mp.setDataSource(file.absolutePath)
                mp.prepare()
                if (ticket != generation.get() || released) return@execute
                mp.start()
                while (ticket == generation.get() && !released &&
                    !latch.await(150, TimeUnit.MILLISECONDS)) { }
                success = ticket == generation.get() && !released
            } catch (_: Exception) {
                success = false
            } finally {
                try { mediaPlayer?.release() } catch (_: Exception) { }
                mediaPlayer = null
                playbackLatch = null
                activeCall = null
                try { file?.delete() } catch (_: Exception) { }
                if (!released && ticket == generation.get()) onComplete(success)
            }
        }
    }

    private fun wavHeader(dataSize: Int, sampleRate: Int): ByteArray {
        val header = ByteArray(44)
        fun putAscii(offset: Int, value: String) {
            value.toByteArray(Charsets.US_ASCII).copyInto(header, offset)
        }
        fun put16(offset: Int, value: Int) {
            header[offset] = (value and 0xff).toByte()
            header[offset + 1] = ((value shr 8) and 0xff).toByte()
        }
        fun put32(offset: Int, value: Int) {
            for (i in 0..3) header[offset + i] = ((value shr (8 * i)) and 0xff).toByte()
        }
        putAscii(0, "RIFF"); put32(4, dataSize + 36); putAscii(8, "WAVE")
        putAscii(12, "fmt "); put32(16, 16); put16(20, 1); put16(22, 1)
        put32(24, sampleRate); put32(28, sampleRate * 2); put16(32, 2)
        put16(34, 16); putAscii(36, "data"); put32(40, dataSize)
        return header
    }

    fun stop() {
        generation.incrementAndGet()
        try { activeCall?.cancel() } catch (_: Exception) { }
        try { mediaPlayer?.stop() } catch (_: Exception) { }
        try { mediaPlayer?.release() } catch (_: Exception) { }
        mediaPlayer = null
        playbackLatch?.countDown()
        playbackLatch = null
    }

    fun release() {
        released = true
        stop()
        executor.shutdownNow()
        client.dispatcher.cancelAll()
        client.connectionPool.evictAll()
    }
}
