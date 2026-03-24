package com.example.doc.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import timber.log.Timber
import javax.crypto.SecretKey

/**
 * Gestor de claves de cifrado usando EncryptedSharedPreferences
 */
class KeyManager(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "doccontrol_secure_prefs"
        private const val KEY_ENCRYPTION_KEY = "encryption_key"
    }

    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val encryptedPrefs by lazy {
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * Obtiene o crea la clave de cifrado de datos
     */
    fun getOrCreateEncryptionKey(): SecretKey {
        val keyString = encryptedPrefs.getString(KEY_ENCRYPTION_KEY, null)

        return if (keyString != null) {
            // Clave existente
            try {
                CryptoUtils.stringToKey(keyString)
            } catch (e: Exception) {
                Timber.e(e, "Error al cargar clave existente, generando nueva")
                generateAndSaveNewKey()
            }
        } else {
            // Primera vez, generar nueva clave
            generateAndSaveNewKey()
        }
    }

    /**
     * Genera y guarda una nueva clave de cifrado
     */
    private fun generateAndSaveNewKey(): SecretKey {
        val newKey = CryptoUtils.generateKey()
        val keyString = CryptoUtils.keyToString(newKey)

        encryptedPrefs.edit()
            .putString(KEY_ENCRYPTION_KEY, keyString)
            .apply()

        Timber.d("Nueva clave de cifrado generada y guardada")
        return newKey
    }

    /**
     * Elimina la clave de cifrado (usar con precaución)
     */
    fun deleteEncryptionKey() {
        encryptedPrefs.edit()
            .remove(KEY_ENCRYPTION_KEY)
            .apply()
        Timber.w("Clave de cifrado eliminada")
    }

    /**
     * Verifica si existe una clave de cifrado
     */
    fun hasEncryptionKey(): Boolean {
        return encryptedPrefs.contains(KEY_ENCRYPTION_KEY)
    }

    /**
     * Guarda un valor string cifrado
     */
    fun saveEncryptedString(key: String, value: String) {
        encryptedPrefs.edit()
            .putString(key, value)
            .apply()
    }

    /**
     * Obtiene un valor string cifrado
     */
    fun getEncryptedString(key: String, defaultValue: String? = null): String? {
        return encryptedPrefs.getString(key, defaultValue)
    }

    /**
     * Guarda un valor int cifrado
     */
    fun saveEncryptedInt(key: String, value: Int) {
        encryptedPrefs.edit()
            .putInt(key, value)
            .apply()
    }

    /**
     * Obtiene un valor int cifrado
     */
    fun getEncryptedInt(key: String, defaultValue: Int = -1): Int {
        return encryptedPrefs.getInt(key, defaultValue)
    }

    /**
     * Guarda un valor boolean cifrado
     */
    fun saveEncryptedBoolean(key: String, value: Boolean) {
        encryptedPrefs.edit()
            .putBoolean(key, value)
            .apply()
    }

    /**
     * Obtiene un valor boolean cifrado
     */
    fun getEncryptedBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return encryptedPrefs.getBoolean(key, defaultValue)
    }

    /**
     * Limpia todos los datos cifrados
     */
    fun clearAll() {
        encryptedPrefs.edit().clear().apply()
        Timber.w("Todos los datos cifrados han sido eliminados")
    }
}

