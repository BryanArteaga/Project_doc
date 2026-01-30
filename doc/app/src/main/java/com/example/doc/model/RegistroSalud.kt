package com.example.doc.model

data class RegistroSalud(
    val id: Int = 0,
    val usuarioId: Int,
    val fecha: String,
    val hora: String? = null,
    val presionArterial: String? = null,
    val glucosa: Int? = null,
    val imc: Double? = null,
    val colesterol: Int? = null,
    val calorias: Int,
    val ejercicio: Int,
    val estadoAnimo: String
)