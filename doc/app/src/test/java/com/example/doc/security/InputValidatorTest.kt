package com.example.doc.security

import org.junit.Test
import org.junit.Assert.*

/**
 * Tests para InputValidator
 */
class InputValidatorTest {

    @Test
    fun `cedula valida ecuatoriana`() {
        // Cédulas válidas reales
        assertTrue(InputValidator.isValidCedula("0123456789"))
        assertTrue(InputValidator.isValidCedula("1712345678"))
    }

    @Test
    fun `cedula invalida longitud incorrecta`() {
        assertFalse(InputValidator.isValidCedula("123456789")) // 9 dígitos
        assertFalse(InputValidator.isValidCedula("12345678901")) // 11 dígitos
    }

    @Test
    fun `cedula invalida provincia fuera de rango`() {
        assertFalse(InputValidator.isValidCedula("2512345678")) // Provincia 25
        assertFalse(InputValidator.isValidCedula("0012345678")) // Provincia 00
    }

    @Test
    fun `nombre valido`() {
        assertTrue(InputValidator.isValidName("Juan Pérez"))
        assertTrue(InputValidator.isValidName("María José"))
        assertTrue(InputValidator.isValidName("Ana"))
    }

    @Test
    fun `nombre invalido con numeros`() {
        assertFalse(InputValidator.isValidName("Juan123"))
        assertFalse(InputValidator.isValidName("María@"))
    }

    @Test
    fun `nombre invalido muy corto o largo`() {
        assertFalse(InputValidator.isValidName("A"))
        assertFalse(InputValidator.isValidName("A".repeat(101)))
    }

    @Test
    fun `edad valida`() {
        assertTrue(InputValidator.isValidAge(0))
        assertTrue(InputValidator.isValidAge(30))
        assertTrue(InputValidator.isValidAge(120))
    }

    @Test
    fun `edad invalida fuera de rango`() {
        assertFalse(InputValidator.isValidAge(-1))
        assertFalse(InputValidator.isValidAge(121))
    }

    @Test
    fun `peso valido`() {
        assertTrue(InputValidator.isValidPeso(50.0))
        assertTrue(InputValidator.isValidPeso(70.5))
        assertTrue(InputValidator.isValidPeso(100.0))
    }

    @Test
    fun `peso invalido fuera de rango`() {
        assertFalse(InputValidator.isValidPeso(0.5))
        assertFalse(InputValidator.isValidPeso(501.0))
    }

    @Test
    fun `altura valida`() {
        assertTrue(InputValidator.isValidAltura(1.50))
        assertTrue(InputValidator.isValidAltura(1.75))
        assertTrue(InputValidator.isValidAltura(2.00))
    }

    @Test
    fun `altura invalida fuera de rango`() {
        assertFalse(InputValidator.isValidAltura(0.2))
        assertFalse(InputValidator.isValidAltura(3.1))
    }

    @Test
    fun `glucosa valida`() {
        assertTrue(InputValidator.isValidGlucosa(70.0))
        assertTrue(InputValidator.isValidGlucosa(120.0))
        assertTrue(InputValidator.isValidGlucosa(200.0))
    }

    @Test
    fun `glucosa invalida fuera de rango`() {
        assertFalse(InputValidator.isValidGlucosa(39.0))
        assertFalse(InputValidator.isValidGlucosa(601.0))
    }

    @Test
    fun `colesterol valido`() {
        assertTrue(InputValidator.isValidColesterol(150.0))
        assertTrue(InputValidator.isValidColesterol(200.0))
        assertTrue(InputValidator.isValidColesterol(250.0))
    }

    @Test
    fun `colesterol invalido fuera de rango`() {
        assertFalse(InputValidator.isValidColesterol(99.0))
        assertFalse(InputValidator.isValidColesterol(401.0))
    }

    @Test
    fun `presion arterial valida`() {
        assertTrue(InputValidator.isValidPresionArterial("120/80"))
        assertTrue(InputValidator.isValidPresionArterial("140/90"))
        assertTrue(InputValidator.isValidPresionArterial("110/70"))
    }

    @Test
    fun `presion arterial invalida formato incorrecto`() {
        assertFalse(InputValidator.isValidPresionArterial("120"))
        assertFalse(InputValidator.isValidPresionArterial("120-80"))
        assertFalse(InputValidator.isValidPresionArterial("abc/def"))
    }

    @Test
    fun `presion arterial invalida fuera de rango`() {
        assertFalse(InputValidator.isValidPresionArterial("60/40")) // Muy baja
        assertFalse(InputValidator.isValidPresionArterial("300/200")) // Muy alta
    }

    @Test
    fun `validacion completa usuario valido`() {
        val result = InputValidator.validateUserRegistration(
            cedula = "0123456789",
            nombre = "Juan Pérez",
            edad = 30,
            peso = 70.0,
            altura = 1.75
        )
        assertTrue(result.isValid)
        assertNull(result.errorMessage)
    }

    @Test
    fun `validacion completa usuario invalido cedula`() {
        val result = InputValidator.validateUserRegistration(
            cedula = "1234567890",
            nombre = "Juan Pérez",
            edad = 30,
            peso = 70.0,
            altura = 1.75
        )
        assertFalse(result.isValid)
        assertNotNull(result.errorMessage)
    }

    @Test
    fun `validacion completa usuario invalido nombre`() {
        val result = InputValidator.validateUserRegistration(
            cedula = "0123456789",
            nombre = "Juan123",
            edad = 30,
            peso = 70.0,
            altura = 1.75
        )
        assertFalse(result.isValid)
        assertTrue(result.errorMessage?.contains("Nombre") == true)
    }

    @Test
    fun `validacion datos salud validos`() {
        val result = InputValidator.validateHealthData(
            glucosa = 120.0,
            colesterol = 180.0,
            presion = "120/80"
        )
        assertTrue(result.isValid)
    }

    @Test
    fun `validacion datos salud glucosa invalida`() {
        val result = InputValidator.validateHealthData(
            glucosa = 700.0,
            colesterol = 180.0,
            presion = "120/80"
        )
        assertFalse(result.isValid)
        assertTrue(result.errorMessage?.contains("glucosa") == true)
    }

    @Test
    fun `sanitize text elimina caracteres peligrosos`() {
        val input = "<script>alert('xss')</script>"
        val sanitized = InputValidator.sanitizeText(input)
        assertFalse(sanitized.contains("<"))
        assertFalse(sanitized.contains(">"))
    }

    @Test
    fun `safe string detecta caracteres peligrosos`() {
        assertTrue(InputValidator.isSafeString("Juan Pérez"))
        assertFalse(InputValidator.isSafeString("<script>"))
        assertFalse(InputValidator.isSafeString("'; DROP TABLE--"))
    }
}

