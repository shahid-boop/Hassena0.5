package com.example.services.voice

import android.content.Context
import android.media.MediaPlayer
import android.os.Build
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/** Serial online speech playback. Cancellation generation prevents stale audio after barge-in. */
class ElevenLabsVoicePlayer(context: Context) {
    private val appContext = context.applicationContext
    private val client = OkHttpClient.Builder().callTimeout(45, TimeUnit.SECONDS).build()
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val generation = AtomicInteger(0)
    @Volatile private var activeCall: okhttp3.Call? = null
    @Volatile private var mediaPlayer: MediaPlayer? = null
    @Volatile private var playbackLatch: CountDownLatch? = null
    @Volatile private var released = false

    fun speak(text: String, apiKey: String, voiceId: String, onComplete: (Boolean) -> Unit) {
        val ticket = generation.get()
        executor.execute {
            if (released || ticket != generation.get()) return@execute
            var success = false
            var file: File? = null
            try {
                val json = JSONObject().put("text", text)
                    .put("model_id", "eleven_multilingual_v2")
                    .put("voice_settings", JSONObject().put("stability", 0.38).put("similarity_boost", 0.82).put("style", 0.42).put("use_speaker_boost", true))
                    .toString()
                val request = Request.Builder()
                    .url("https://api.elevenlabs.io/v1/text-to-speech/${voiceId.trim()}/stream?output_format=mp3_44100_128")
                    .header("xi-api-key", apiKey)
                    .header("Accept", "audio/mpeg")
                    .post(json.toRequestBody("application/json; charset=utf-8".toMediaType()))
                    .build()
                val call = client.newCall(request); activeCall = call
                call.execute().use { response ->
                    if (!response.isSuccessful) throw IOException("ElevenLabs HTTP ${response.code}")
                    val bytes = response.body?.bytes() ?: throw IOException("Empty voice audio")
                    if (bytes.isEmpty()) throw IOException("Empty voice audio")
                    if (ticket != generation.get() || released) return@execute
                    file = File.createTempFile("jarvis_voice_", ".mp3", appContext.cacheDir).apply { writeBytes(bytes) }
                }
                if (ticket != generation.get() || released) return@execute
                val latch = CountDownLatch(1); playbackLatch = latch
                val player = MediaPlayer(); mediaPlayer = player
                player.setOnCompletionListener { latch.countDown() }
                player.setOnErrorListener { _, _, _ -> latch.countDown(); true }
                player.setDataSource(file!!.absolutePath)
                player.prepare()
                if (ticket != generation.get() || released) return@execute
                player.start()
                while (ticket == generation.get() && !released && !latch.await(150, TimeUnit.MILLISECONDS)) { }
                success = ticket == generation.get() && !released
            } catch (_: Exception) {
                success = false
            } finally {
                try { mediaPlayer?.release() } catch (_: Exception) { }
                mediaPlayer = null; playbackLatch = null; activeCall = null
                try { file?.delete() } catch (_: Exception) { }
                if (!released && ticket == generation.get()) onComplete(success)
            }
        }
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
