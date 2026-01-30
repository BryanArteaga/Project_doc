package com.example.doc.security

import timber.log.Timber

/**
 * Validadores de entrada para datos sensibles
 */
object InputValidator {

    /**
     * Valida una cédula ecuatoriana
     */
    /**
     * Valida cédula ecuatoriana según algoritmo oficial del Registro Civil
     * Algoritmo "Módulo 10":
     * 1. Multiplica índices pares por 2, impares por 1
     * 2. Si resultado >= 10, resta 9
     * 3. Suma todos los resultados
     * 4. Toma último dígito de la suma
     * 5. Verificador = 10 - último dígito (si es 10, verificador = 0)
     *
     * Ejemplo: 0903686962
     * - Coeficientes: [2,1,2,1,2,1,2,1,2]
     * - Multiplicación: [0,9,0,3,12,8,12,9,12]
     * - Ajuste (>=10): [0,9,0,3,3,8,3,9,3]
     * - Suma: 38
     * - Último dígito: 8
     * - Verificador: 10-8 = 2 ✅
     */
    fun isValidCedula(cedula: String): Boolean {
        // Validación 1: Debe tener exactamente 10 dígitos
        if (cedula.length != 10) {
            Timber.w("❌ Cédula rechazada: debe tener 10 dígitos (actual: ${cedula.length})")
            return false
        }

        // Validación 2: Debe contener solo números
        if (!cedula.all { it.isDigit() }) {
            Timber.w("❌ Cédula rechazada: debe tener solamente números")
            return false
        }

        // Validación 3: Código de provincia (primeros 2 dígitos)
        val codigoProvincia = cedula.substring(0, 2).toIntOrNull() ?: return false
        val provinciasValidas = mapOf(
            1 to "Azuay", 2 to "Bolívar", 3 to "Cañar", 4 to "Carchi",
            5 to "Cotopaxi", 6 to "Chimborazo", 7 to "El Oro", 8 to "Esmeraldas",
            9 to "Guayas", 10 to "Imbabura", 11 to "Loja", 12 to "Los Ríos",
            13 to "Manabí", 14 to "Morona Santiago", 15 to "Napo", 16 to "Pastaza",
            17 to "Pichincha", 18 to "Tungurahua", 19 to "Zamora Chinchipe", 20 to "Galápagos",
            21 to "Sucumbíos", 22 to "Orellana", 23 to "Santo Domingo de los Tsáchilas",
            24 to "Santa Elena", 30 to "Ecuatorianos residentes en el exterior"
        )

        if (codigoProvincia !in provinciasValidas.keys) {
            Timber.w("❌ Cédula rechazada: no pertenece a ninguna provincia ($codigoProvincia)")
            return false
        }

        // Validación 4: Tercer dígito debe ser menor a 6
        val tercerDigito = cedula[2].digitToInt()
        if (tercerDigito >= 6) {
            Timber.w("❌ Cédula rechazada: tercer dígito no válido ($tercerDigito)")
            return false
        }

        // Validación 5: Algoritmo Módulo 10
        // Coeficientes: índices pares multiplican por 2, impares por 1
        val coeficientes = intArrayOf(2, 1, 2, 1, 2, 1, 2, 1, 2)
        var suma = 0

        for (i in 0..8) {
            var resultado = cedula[i].digitToInt() * coeficientes[i]

            // Si resultado >= 10, restar 9
            if (resultado >= 10) {
                resultado -= 9
            }

            suma += resultado
        }

        // Tomar último dígito de la suma
        val ultimoDigito = suma % 10

        // Calcular dígito verificador: 10 - últimoDigito
        // Si el resultado es 10, el verificador es 0
        val digitoVerificador = if (ultimoDigito == 0) 0 else (10 - ultimoDigito)

        val digitoIngresado = cedula[9].digitToInt()

        if (digitoVerificador != digitoIngresado) {
            Timber.w("❌ Cédula rechazada: dígito verificador no válido (esperado: $digitoVerificador, ingresado: $digitoIngresado)")
            return false
        }

        val provincia = provinciasValidas[codigoProvincia]
        Timber.d("✅ La cédula pertenece a la provincia de $provincia")
        return true
    }

    /**
     * Valida un nombre (solo letras y espacios)
     */
    fun isValidName(name: String): Boolean {
        if (name.isBlank()) return false
        if (name.length < 2 || name.length > 100) return false
        return name.all { it.isLetter() || it.isWhitespace() }
    }

    /**
     * Valida edad (entre 0 y 120 años)
     */
    fun isValidAge(age: Int): Boolean {
        return age in 0..120
    }

    /**
     * Valida peso (entre 1 y 500 kg)
     */
    fun isValidPeso(peso: Double): Boolean {
        return peso in 1.0..500.0
    }

    /**
     * Valida altura (entre 0.3 y 3.0 metros)
     */
    fun isValidAltura(altura: Double): Boolean {
        return altura in 0.3..3.0
    }

    /**
     * Valida valor de glucosa (mg/dL)
     * Rango típico: 40-600 mg/dL
     */
    fun isValidGlucosa(glucosa: Double): Boolean {
        return glucosa in 40.0..600.0
    }

    /**
     * Valida valor de colesterol (mg/dL)
     * Rango típico: 100-400 mg/dL
     */
    fun isValidColesterol(colesterol: Double): Boolean {
        return colesterol in 100.0..400.0
    }

    /**
     * Valida presión arterial (formato: "120/80")
     */
    fun isValidPresionArterial(presion: String): Boolean {
        val parts = presion.split("/")
        if (parts.size != 2) return false

        val sistolica = parts[0].toIntOrNull() ?: return false
        val diastolica = parts[1].toIntOrNull() ?: return false

        // Rango típico: sistólica 70-250, diastólica 40-150
        return sistolica in 70..250 && diastolica in 40..150
    }

    /**
     * Valida frecuencia cardíaca (latidos por minuto)
     */
    fun isValidFrecuenciaCardiaca(frecuencia: Int): Boolean {
        return frecuencia in 30..220
    }

    /**
     * Valida saturación de oxígeno (%)
     */
    fun isValidSaturacionOxigeno(saturacion: Double): Boolean {
        return saturacion in 50.0..100.0
    }

    /**
     * Valida temperatura corporal (°C)
     */
    fun isValidTemperatura(temperatura: Double): Boolean {
        return temperatura in 35.0..42.0
    }

    /**
     * Sanitiza texto para evitar inyecciones
     */
    fun sanitizeText(text: String): String {
        return text.trim()
            .replace(Regex("[<>\"']"), "")
            .take(500) // Limitar longitud
    }

    /**
     * Valida que un string no contenga caracteres peligrosos
     */
    fun isSafeString(text: String): Boolean {
        val dangerousChars = listOf("<", ">", "\"", "'", ";", "--", "/*", "*/")
        return dangerousChars.none { text.contains(it) }
    }

    /**
     * Clase para resultados de validación
     */
    data class ValidationResult(
        val isValid: Boolean,
        val errorMessage: String? = null
    ) {
        companion object {
            fun success() = ValidationResult(true)
            fun error(message: String) = ValidationResult(false, message)
        }
    }

    /**
     * Valida todos los campos del registro de usuario
     */
    fun validateUserRegistration(
        cedula: String,
        nombre: String,
        edad: Int,
        peso: Double,
        altura: Double
    ): ValidationResult {
        if (!isValidCedula(cedula)) {
            return ValidationResult.error("Cédula inválida")
        }
        if (!isValidName(nombre)) {
            return ValidationResult.error("Nombre inválido (solo letras y espacios)")
        }
        if (!isValidAge(edad)) {
            return ValidationResult.error("Edad inválida (debe estar entre 0 y 120 años)")
        }
        if (!isValidPeso(peso)) {
            return ValidationResult.error("Peso inválido (debe estar entre 1 y 500 kg)")
        }
        if (!isValidAltura(altura)) {
            return ValidationResult.error("Altura inválida (debe estar entre 0.3 y 3.0 metros)")
        }
        return ValidationResult.success()
    }

    /**
     * Valida datos de salud
     */
    fun validateHealthData(
        glucosa: Double?,
        colesterol: Double?,
        presion: String?
    ): ValidationResult {
        glucosa?.let {
            if (!isValidGlucosa(it)) {
                return ValidationResult.error("Valor de glucosa inválido (40-600 mg/dL)")
            }
        }

        colesterol?.let {
            if (!isValidColesterol(it)) {
                return ValidationResult.error("Valor de colesterol inválido (100-400 mg/dL)")
            }
        }

        presion?.let {
            if (it.isNotBlank() && !isValidPresionArterial(it)) {
                return ValidationResult.error("Presión arterial inválida (formato: 120/80)")
            }
        }

        return ValidationResult.success()
    }
}

