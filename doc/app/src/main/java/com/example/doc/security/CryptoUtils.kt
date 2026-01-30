package com.example.doc.security

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Utilidad para cifrado y descifrado de datos sensibles usando AES-GCM
 */
object CryptoUtils {
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val KEY_SIZE = 256
    private const val IV_SIZE = 12
    private const val TAG_SIZE = 128

    /**
     * Genera una clave AES de 256 bits
     */
    fun generateKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance("AES")
        keyGenerator.init(KEY_SIZE)
        return keyGenerator.generateKey()
    }

    /**
     * Convierte una clave a string Base64
     */
    fun keyToString(key: SecretKey): String {
        return Base64.encodeToString(key.encoded, Base64.NO_WRAP)
    }

    /**
     * Convierte un string Base64 a clave
     */
    fun stringToKey(keyString: String): SecretKey {
        val keyBytes = Base64.decode(keyString, Base64.NO_WRAP)
        return SecretKeySpec(keyBytes, "AES")
    }

    /**
     * Cifra un texto usando AES-GCM
     * @param plainText Texto a cifrar
     * @param key Clave secreta
     * @return Texto cifrado en Base64 (incluye IV)
     */
    fun encrypt(plainText: String, key: SecretKey): String {
        try {
            // Generar IV aleatorio
            val iv = ByteArray(IV_SIZE)
            SecureRandom().nextBytes(iv)

            // Configurar cifrado
            val cipher = Cipher.getInstance(ALGORITHM)
            val gcmSpec = GCMParameterSpec(TAG_SIZE, iv)
            cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec)

            // Cifrar
            val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

            // Combinar IV + cipherText
            val combined = iv + cipherText

            // Retornar como Base64
            return Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            throw SecurityException("Error al cifrar: ${e.message}", e)
        }
    }

    /**
     * Descifra un texto cifrado con AES-GCM
     * @param cipherText Texto cifrado en Base64
     * @param key Clave secreta
     * @return Texto descifrado
     */
    fun decrypt(cipherText: String, key: SecretKey): String {
        try {
            // Decodificar Base64
            val combined = Base64.decode(cipherText, Base64.NO_WRAP)

            // Separar IV y cipherText
            val iv = combined.copyOfRange(0, IV_SIZE)
            val actualCipherText = combined.copyOfRange(IV_SIZE, combined.size)

            // Configurar descifrado
            val cipher = Cipher.getInstance(ALGORITHM)
            val gcmSpec = GCMParameterSpec(TAG_SIZE, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec)

            // Descifrar
            val plainText = cipher.doFinal(actualCipherText)

            return String(plainText, Charsets.UTF_8)
        } catch (e: Exception) {
            throw SecurityException("Error al descifrar: ${e.message}", e)
        }
    }

    /**
     * Cifra un número Double
     */
    fun encryptDouble(value: Double, key: SecretKey): String {
        return encrypt(value.toString(), key)
    }

    /**
     * Descifra a Double
     */
    fun decryptToDouble(cipherText: String, key: SecretKey): Double {
        return decrypt(cipherText, key).toDouble()
    }

    /**
     * Cifra un número Int
     */
    fun encryptInt(value: Int, key: SecretKey): String {
        return encrypt(value.toString(), key)
    }

    /**
     * Descifra a Int
     */
    fun decryptToInt(cipherText: String, key: SecretKey): Int {
        return decrypt(cipherText, key).toInt()
    }

    /**
     * Valida si un texto puede ser descifrado
     */
    fun canDecrypt(cipherText: String, key: SecretKey): Boolean {
        return try {
            decrypt(cipherText, key)
            true
        } catch (e: Exception) {
            false
        }
    }
}

