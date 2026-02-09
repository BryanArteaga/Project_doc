package com.example.doc.model

data class Usuario(
    val id: Int = 0,
    val cedula: String?,
    val nombre: String,
    val apellido: String? = null,  // Nuevo: apellido del usuario
    val edad: Int,
    val peso: Double,
    val altura: Double, // En metros
    val sexo: String? = null, // Nuevo: "Hombre", "Mujer", o null
    val enfermedad: String,
    val colorEnfermedad: String,
    val notifEnabled: Boolean = true, // Nuevo
    val notifType: String = "daily_fixed", // Nuevo
    val notifDetails: String = "{\"hour\":20}", // Nuevo, JSON para detalles
    val glucosaInicial: Int? = null,
    val colesterolInicial: Int? = null
)