package com.example.doc.security

import timber.log.Timber
import javax.crypto.SecretKey
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Servicio para cifrar/descifrar datos de salud sensibles
 */
@Singleton
class HealthDataEncryption @Inject constructor(
    private val keyManager: KeyManager
) {

    private val encryptionKey: SecretKey by lazy {
        keyManager.getOrCreateEncryptionKey()
    }

    /**
     * Cifra un valor de glucosa
     */
    fun encryptGlucosa(glucosa: Double): String {
        return try {
            CryptoUtils.encryptDouble(glucosa, encryptionKey)
        } catch (e: Exception) {
            Timber.e(e, "Error al cifrar glucosa")
            throw SecurityException("No se pudo cifrar el valor de glucosa", e)
        }
    }

    /**
     * Descifra un valor de glucosa
     */
    fun decryptGlucosa(encryptedGlucosa: String): Double {
        return try {
            CryptoUtils.decryptToDouble(encryptedGlucosa, encryptionKey)
        } catch (e: Exception) {
            Timber.e(e, "Error al descifrar glucosa")
            throw SecurityException("No se pudo descifrar el valor de glucosa", e)
        }
    }

    /**
     * Cifra un valor de colesterol
     */
    fun encryptColesterol(colesterol: Double): String {
        return try {
            CryptoUtils.encryptDouble(colesterol, encryptionKey)
        } catch (e: Exception) {
            Timber.e(e, "Error al cifrar colesterol")
            throw SecurityException("No se pudo cifrar el valor de colesterol", e)
        }
    }

    /**
     * Descifra un valor de colesterol
     */
    fun decryptColesterol(encryptedColesterol: String): Double {
        return try {
            CryptoUtils.decryptToDouble(encryptedColesterol, encryptionKey)
        } catch (e: Exception) {
            Timber.e(e, "Error al descifrar colesterol")
            throw SecurityException("No se pudo descifrar el valor de colesterol", e)
        }
    }

    /**
     * Cifra un valor de presión arterial
     */
    fun encryptPresion(presion: String): String {
        return try {
            CryptoUtils.encrypt(presion, encryptionKey)
        } catch (e: Exception) {
            Timber.e(e, "Error al cifrar presión")
            throw SecurityException("No se pudo cifrar la presión arterial", e)
        }
    }

    /**
     * Descifra un valor de presión arterial
     */
    fun decryptPresion(encryptedPresion: String): String {
        return try {
            CryptoUtils.decrypt(encryptedPresion, encryptionKey)
        } catch (e: Exception) {
            Timber.e(e, "Error al descifrar presión")
            throw SecurityException("No se pudo descifrar la presión arterial", e)
        }
    }

    /**
     * Cifra un valor de peso
     */
    fun encryptPeso(peso: Double): String {
        return try {
            CryptoUtils.encryptDouble(peso, encryptionKey)
        } catch (e: Exception) {
            Timber.e(e, "Error al cifrar peso")
            throw SecurityException("No se pudo cifrar el peso", e)
        }
    }

    /**
     * Descifra un valor de peso
     */
    fun decryptPeso(encryptedPeso: String): Double {
        return try {
            CryptoUtils.decryptToDouble(encryptedPeso, encryptionKey)
        } catch (e: Exception) {
            Timber.e(e, "Error al descifrar peso")
            throw SecurityException("No se pudo descifrar el peso", e)
        }
    }

    /**
     * Cifra datos sensibles del usuario
     */
    fun encryptUserData(data: String): String {
        return try {
            CryptoUtils.encrypt(data, encryptionKey)
        } catch (e: Exception) {
            Timber.e(e, "Error al cifrar datos de usuario")
            throw SecurityException("No se pudo cifrar los datos del usuario", e)
        }
    }

    /**
     * Descifra datos sensibles del usuario
     */
    fun decryptUserData(encryptedData: String): String {
        return try {
            CryptoUtils.decrypt(encryptedData, encryptionKey)
        } catch (e: Exception) {
            Timber.e(e, "Error al descifrar datos de usuario")
            throw SecurityException("No se pudo descifrar los datos del usuario", e)
        }
    }

    /**
     * Verifica si los datos pueden ser descifrados
     */
    fun canDecrypt(encryptedData: String): Boolean {
        return CryptoUtils.canDecrypt(encryptedData, encryptionKey)
    }

    /**
     * Cifra de forma segura con manejo de errores
     */
    fun safeEncrypt(value: String): String? {
        return try {
            CryptoUtils.encrypt(value, encryptionKey)
        } catch (e: Exception) {
            Timber.e(e, "Error en safeEncrypt")
            null
        }
    }

    /**
     * Descifra de forma segura con manejo de errores
     */
    fun safeDecrypt(encryptedValue: String): String? {
        return try {
            CryptoUtils.decrypt(encryptedValue, encryptionKey)
        } catch (e: Exception) {
            Timber.e(e, "Error en safeDecrypt")
            null
        }
    }
}

