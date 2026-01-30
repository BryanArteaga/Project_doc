package com.example.doc.utils

object ValidationUtils {

    fun validarCedula(cedula: String): ValidationResult {
        if (cedula.isBlank()) {
            return ValidationResult(false, "La cédula no puede estar vacía")
        }
        if (!cedula.matches(Regex("^\\d{10}$"))) {
            return ValidationResult(false, "La cédula debe tener 10 dígitos")
        }
        return ValidationResult(true, "Cédula válida")
    }

    fun validarNombre(nombre: String): ValidationResult {
        if (nombre.isBlank()) {
            return ValidationResult(false, "El nombre no puede estar vacío")
        }
        if (nombre.length < 2) {
            return ValidationResult(false, "El nombre debe tener al menos 2 caracteres")
        }
        if (!nombre.matches(Regex("^[a-zA-ZáéíóúÁÉÍÓÚñÑ ]+$"))) {
            return ValidationResult(false, "El nombre solo puede contener letras")
        }
        return ValidationResult(true, "Nombre válido")
    }

    fun validarEdad(edad: Int): ValidationResult {
        if (edad < 1 || edad > 120) {
            return ValidationResult(false, "La edad debe estar entre 1 y 120 años")
        }
        return ValidationResult(true, "Edad válida")
    }

    fun validarPeso(peso: Double): ValidationResult {
        if (peso < 20.0 || peso > 300.0) {
            return ValidationResult(false, "El peso debe estar entre 20 y 300 kg")
        }
        return ValidationResult(true, "Peso válido")
    }

    fun validarAltura(altura: Double): ValidationResult {
        if (altura < 0.5 || altura > 2.5) {
            return ValidationResult(false, "La altura debe estar entre 0.5 y 2.5 metros")
        }
        return ValidationResult(true, "Altura válida")
    }

    fun validarPresionArterial(presion: String): ValidationResult {
        if (presion.isBlank()) {
            return ValidationResult(false, "La presión arterial no puede estar vacía")
        }
        val partes = presion.split("/")
        if (partes.size != 2) {
            return ValidationResult(false, "Formato inválido. Use: 120/80")
        }
        val sistolica = partes[0].toIntOrNull()
        val diastolica = partes[1].toIntOrNull()
        if (sistolica == null || diastolica == null) {
            return ValidationResult(false, "Los valores deben ser números")
        }
        if (sistolica < 60 || sistolica > 250) {
            return ValidationResult(false, "Presión sistólica debe estar entre 60 y 250")
        }
        if (diastolica < 40 || diastolica > 150) {
            return ValidationResult(false, "Presión diastólica debe estar entre 40 y 150")
        }
        return ValidationResult(true, "Presión arterial válida")
    }

    fun validarGlucosa(glucosa: Int): ValidationResult {
        if (glucosa < 40 || glucosa > 600) {
            return ValidationResult(false, "La glucosa debe estar entre 40 y 600 mg/dL")
        }
        return ValidationResult(true, "Glucosa válida")
    }

    fun validarColesterol(colesterol: Int): ValidationResult {
        if (colesterol < 100 || colesterol > 500) {
            return ValidationResult(false, "El colesterol debe estar entre 100 y 500 mg/dL")
        }
        return ValidationResult(true, "Colesterol válido")
    }

    fun validarCalorias(calorias: Int): ValidationResult {
        if (calorias < 0 || calorias > 10000) {
            return ValidationResult(false, "Las calorías deben estar entre 0 y 10000")
        }
        return ValidationResult(true, "Calorías válidas")
    }
}

data class ValidationResult(
    val isValid: Boolean,
    val message: String
)

