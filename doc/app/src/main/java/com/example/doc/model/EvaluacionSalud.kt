package com.example.doc.model

data class EvaluacionSalud(
    val mensaje: String,
    val estado: Estado,
    val detalles: List<String>
) {
    enum class Estado { NORMAL, LEVE, ADVERTENCIA, EMERGENCIA }
}