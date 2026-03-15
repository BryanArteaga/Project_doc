package com.example.doc.model

data class RegistroSalud(
    val id: Int = 0,
    val usuarioId: Int,
    val fecha: String,
    val hora: String? = null,
    // Campos de presión arterial
    val presionArterial: String? = null,  // Legacy: "sistolica/diastolica"
    val presionSistolica: Int? = null,
    val presionDiastolica: Int? = null,
    // Campos de salud
    val glucosa: Int? = null,
    val imc: Double? = null,
    val colesterol: Int? = null,
    val temperatura: Double? = null,
    val peso: Double? = null,
    val pulso: Int? = null,  // ✅ Pulso (opcional, solo para Obesidad)
    // Actividad y estado
    val calorias: Int? = null,
    val ejercicio: String? = null,  // Ahora es string (nombre del ejercicio)
    val ejercicioMinutos: Int = 0,  // Minutos de ejercicio (para evaluación desde pantalla)
    val estadoAnimo: String? = null,
    val notas: String? = null,
    // Datos de sincronización
    val registradoEn: Long? = null
)