package com.example.services.ai

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.example.models.AIProviderType
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class ApiKeyStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(
        "secure_api_keys",
        Context.MODE_PRIVATE
    )

    private val alias = "hasseena_api_key_encryption_v1"
    private val lock = Any()

    private fun secretKey(): SecretKey = synchronized(lock) {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply {
            load(null)
        }

        (keyStore.getKey(alias, null) as? SecretKey) ?: run {
            val generator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                "AndroidKeyStore"
            )
            generator.init(
                KeyGenParameterSpec.Builder(
                    alias,
                    KeyProperties.PURPOSE_ENCRYPT or
                        KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(
                        KeyProperties.ENCRYPTION_PADDING_NONE
                    )
                    .setRandomizedEncryptionRequired(true)
                    .build()
            )
            generator.generateKey()
        }
    }

    fun save(provider: AIProviderType, apiKey: String) {
        val cleanKey = apiKey.trim()
        require(cleanKey.isNotEmpty()) { "API key cannot be empty" }

        synchronized(lock) {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey())
            val encrypted = cipher.doFinal(cleanKey.toByteArray(Charsets.UTF_8))

            val iv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
            val data = Base64.encodeToString(encrypted, Base64.NO_WRAP)

            check(
                prefs.edit()
                    .putString(provider.name, "$iv:$data")
                    .commit()
            ) { "Could not save API key" }
        }
    }

    fun get(provider: AIProviderType): String {
        return runCatching {
            synchronized(lock) {
                val stored = prefs.getString(provider.name, null)
                    ?: return@synchronized ""

                val parts = stored.split(":", limit = 2)
                require(parts.size == 2) { "Invalid encrypted value" }

                val iv = Base64.decode(parts[0], Base64.NO_WRAP)
                val data = Base64.decode(parts[1], Base64.NO_WRAP)

                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(
                    Cipher.DECRYPT_MODE,
                    secretKey(),
                    GCMParameterSpec(128, iv)
                )
                String(cipher.doFinal(data), Charsets.UTF_8)
            }
        }.getOrDefault("")
    }

    fun remove(provider: AIProviderType) {
        synchronized(lock) {
            check(prefs.edit().remove(provider.name).commit()) {
                "Could not remove API key"
            }
        }
    }

    fun hasKey(provider: AIProviderType): Boolean =
        get(provider).let { it.isNotBlank() && !it.startsWith("MY_") }
}
