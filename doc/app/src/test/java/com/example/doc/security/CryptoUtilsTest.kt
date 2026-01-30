package com.example.doc.security

import org.junit.Test
import org.junit.Assert.*

/**
 * Tests para CryptoUtils
 */
class CryptoUtilsTest {

    @Test
    fun `generar clave funciona`() {
        val key = CryptoUtils.generateKey()
        assertNotNull(key)
        assertEquals("AES", key.algorithm)
    }

    @Test
    fun `cifrar y descifrar texto`() {
        val key = CryptoUtils.generateKey()
        val original = "Datos sensibles del paciente"

        val encrypted = CryptoUtils.encrypt(original, key)
        val decrypted = CryptoUtils.decrypt(encrypted, key)

        assertEquals(original, decrypted)
    }

    @Test
    fun `cifrado produce diferentes resultados con mismo texto`() {
        val key = CryptoUtils.generateKey()
        val original = "Test"

        val encrypted1 = CryptoUtils.encrypt(original, key)
        val encrypted2 = CryptoUtils.encrypt(original, key)

        // Deben ser diferentes porque el IV es aleatorio
        assertNotEquals(encrypted1, encrypted2)

        // Pero ambos deben descifrar al original
        assertEquals(original, CryptoUtils.decrypt(encrypted1, key))
        assertEquals(original, CryptoUtils.decrypt(encrypted2, key))
    }

    @Test
    fun `cifrar y descifrar double`() {
        val key = CryptoUtils.generateKey()
        val original = 120.5

        val encrypted = CryptoUtils.encryptDouble(original, key)
        val decrypted = CryptoUtils.decryptToDouble(encrypted, key)

        assertEquals(original, decrypted, 0.001)
    }

    @Test
    fun `cifrar y descifrar int`() {
        val key = CryptoUtils.generateKey()
        val original = 42

        val encrypted = CryptoUtils.encryptInt(original, key)
        val decrypted = CryptoUtils.decryptToInt(encrypted, key)

        assertEquals(original, decrypted)
    }

    @Test
    fun `convertir clave a string y viceversa`() {
        val originalKey = CryptoUtils.generateKey()
        val keyString = CryptoUtils.keyToString(originalKey)
        val restoredKey = CryptoUtils.stringToKey(keyString)

        // Verificar que la clave restaurada funciona
        val testData = "Test"
        val encrypted = CryptoUtils.encrypt(testData, originalKey)
        val decrypted = CryptoUtils.decrypt(encrypted, restoredKey)

        assertEquals(testData, decrypted)
    }

    @Test(expected = SecurityException::class)
    fun `descifrar con clave incorrecta falla`() {
        val key1 = CryptoUtils.generateKey()
        val key2 = CryptoUtils.generateKey()
        val original = "Test"

        val encrypted = CryptoUtils.encrypt(original, key1)
        CryptoUtils.decrypt(encrypted, key2) // Debe lanzar excepción
    }

    @Test(expected = SecurityException::class)
    fun `descifrar datos alterados falla`() {
        val key = CryptoUtils.generateKey()
        val original = "Test"

        val encrypted = CryptoUtils.encrypt(original, key)
        val tampered = encrypted + "xyz" // Alterar datos

        CryptoUtils.decrypt(tampered, key) // Debe lanzar excepción
    }

    @Test
    fun `canDecrypt valida correctamente`() {
        val key = CryptoUtils.generateKey()
        val original = "Test"

        val encrypted = CryptoUtils.encrypt(original, key)

        assertTrue(CryptoUtils.canDecrypt(encrypted, key))

        val wrongKey = CryptoUtils.generateKey()
        assertFalse(CryptoUtils.canDecrypt(encrypted, wrongKey))
    }

    @Test
    fun `cifrar textos vacios funciona`() {
        val key = CryptoUtils.generateKey()
        val original = ""

        val encrypted = CryptoUtils.encrypt(original, key)
        val decrypted = CryptoUtils.decrypt(encrypted, key)

        assertEquals(original, decrypted)
    }

    @Test
    fun `cifrar textos largos funciona`() {
        val key = CryptoUtils.generateKey()
        val original = "A".repeat(10000) // 10KB de texto

        val encrypted = CryptoUtils.encrypt(original, key)
        val decrypted = CryptoUtils.decrypt(encrypted, key)

        assertEquals(original, decrypted)
    }

    @Test
    fun `cifrar caracteres especiales funciona`() {
        val key = CryptoUtils.generateKey()
        val original = "Español: ñáéíóú¿?¡! Símbolos: @#$%^&*()"

        val encrypted = CryptoUtils.encrypt(original, key)
        val decrypted = CryptoUtils.decrypt(encrypted, key)

        assertEquals(original, decrypted)
    }

    @Test
    fun `cifrar emojis funciona`() {
        val key = CryptoUtils.generateKey()
        val original = "Test 😀 🔐 ❤️"

        val encrypted = CryptoUtils.encrypt(original, key)
        val decrypted = CryptoUtils.decrypt(encrypted, key)

        assertEquals(original, decrypted)
    }

    @Test
    fun `cifrado produce base64 valido`() {
        val key = CryptoUtils.generateKey()
        val original = "Test"

        val encrypted = CryptoUtils.encrypt(original, key)

        // Verificar que es Base64 válido (no debe tener espacios ni caracteres raros)
        assertFalse(encrypted.contains(" "))
        assertFalse(encrypted.contains("\n"))
        assertTrue(encrypted.matches(Regex("^[A-Za-z0-9+/]+=*$")))
    }
}

