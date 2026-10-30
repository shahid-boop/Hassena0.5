package com.example.services.voice

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Stores the ElevenLabs API key encrypted with an Android Keystore AES/GCM key. */
class VoiceApiKeyStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("hasseena_voice_secure", Context.MODE_PRIVATE)
    private val alias = "hasseena_elevenlabs_key_v1"

    @Synchronized fun saveApiKey(apiKey: String) {
        val clean = apiKey.trim()
        require(clean.isNotEmpty()) { "API key cannot be empty" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(clean.toByteArray(Charsets.UTF_8))
        prefs.edit().putString("elevenlabs_key_iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .putString("elevenlabs_key_data", Base64.encodeToString(encrypted, Base64.NO_WRAP)).apply()
    }

    @Synchronized fun getApiKey(): String? {
        return try {
            val ivText = prefs.getString("elevenlabs_key_iv", null) ?: return null
            val dataText = prefs.getString("elevenlabs_key_data", null) ?: return null
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, Base64.decode(ivText, Base64.NO_WRAP)))
            String(cipher.doFinal(Base64.decode(dataText, Base64.NO_WRAP)), Charsets.UTF_8).takeIf { it.isNotBlank() }
        } catch (_: Exception) { null }
    }

    @Synchronized fun removeApiKey() {
        prefs.edit().remove("elevenlabs_key_iv").remove("elevenlabs_key_data").apply()
    }

    /** Save a Voice ID for a named voice pack without overwriting the other packs. */
    fun saveVoiceId(voiceId: String) = saveVoiceId(getSelectedVoicePack(), voiceId)

    @Synchronized fun saveVoiceId(packId: String, voiceId: String) {
        val safeId = packId.trim().takeIf { it in VOICE_PACK_IDS } ?: DEFAULT_VOICE_PACK
        val editor = prefs.edit().putString("elevenlabs_voice_id_$safeId", voiceId.trim())
        // Keep the legacy field for compatibility, but only mirror the Custom pack.
        if (safeId == DEFAULT_VOICE_PACK) editor.putString("elevenlabs_voice_id", voiceId.trim())
        editor.apply()
    }

    fun getVoiceId(): String = getVoiceId(getSelectedVoicePack())

    fun getVoiceId(packId: String): String {
        val safeId = packId.trim().takeIf { it in VOICE_PACK_IDS } ?: DEFAULT_VOICE_PACK
        val packIdValue = prefs.getString("elevenlabs_voice_id_$safeId", null)?.trim()
        if (!packIdValue.isNullOrBlank()) return packIdValue
        // Existing installs only had one Voice ID; keep it usable as the Custom pack.
        return if (safeId == DEFAULT_VOICE_PACK) prefs.getString("elevenlabs_voice_id", "")?.trim().orEmpty() else ""
    }

    fun saveSelectedVoicePack(packId: String) {
        val safeId = packId.trim().takeIf { it in VOICE_PACK_IDS } ?: DEFAULT_VOICE_PACK
        prefs.edit().putString("elevenlabs_selected_voice_pack", safeId).apply()
    }

    fun getSelectedVoicePack(): String = prefs.getString("elevenlabs_selected_voice_pack", DEFAULT_VOICE_PACK)
        ?.takeIf { it in VOICE_PACK_IDS } ?: DEFAULT_VOICE_PACK

    fun hasApiKey(): Boolean = !getApiKey().isNullOrBlank()

    companion object {
        const val DEFAULT_VOICE_PACK = "hasseena_custom"
        val VOICE_PACK_IDS = setOf("charmion", "hasseena_custom", "tiffany", "aurora", "nyx")
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(alias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true).build())
        return generator.generateKey()
    }
}
