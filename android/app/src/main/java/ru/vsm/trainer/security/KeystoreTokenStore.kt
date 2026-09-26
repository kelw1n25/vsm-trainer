package ru.vsm.trainer.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.serialization.json.Json
import ru.vsm.trainer.data.remote.dto.SessionTokens

/**
 * Сессия в SharedPreferences, зашифрованная AES-256-GCM. Ключ создаётся в Android Keystore и никогда
 * не покидает его: скопированный файл настроек без устройства бесполезен.
 */
class KeystoreTokenStore(context: Context, private val json: Json) : TokenStore {
    private val prefs = context.getSharedPreferences("session", Context.MODE_PRIVATE)

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (keyStore.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    @Synchronized
    override fun load(): SessionTokens? {
        val stored = prefs.getString(FIELD, null) ?: return null
        return runCatching {
            val bytes = Base64.decode(stored, Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes, 0, IV_SIZE))
            json.decodeFromString<SessionTokens>(String(cipher.doFinal(bytes, IV_SIZE, bytes.size - IV_SIZE)))
        }.getOrElse {
            // Ключ потерян (сброс блокировки экрана, восстановление на другом устройстве) — сессии нет
            clear()
            null
        }
    }

    @Synchronized
    override fun save(tokens: SessionTokens) {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key()) }
        val encrypted = cipher.iv + cipher.doFinal(json.encodeToString(SessionTokens.serializer(), tokens).toByteArray())
        prefs.edit().putString(FIELD, Base64.encodeToString(encrypted, Base64.NO_WRAP)).apply()
    }

    @Synchronized
    override fun clear() {
        prefs.edit().remove(FIELD).apply()
    }

    private companion object {
        const val KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "vsm_session_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_SIZE = 12
        const val FIELD = "tokens"
    }
}
