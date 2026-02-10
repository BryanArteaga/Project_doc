package com.example.doc.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "usuarios")
data class UsuarioEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val cedula: String?,
    val nombre: String,
    val apellido: String? = null,  // Nuevo: apellido del usuario
    val edad: Int,
    val peso: Double,
    val altura: Double, // En metros
    val sexo: String? = null,
    val enfermedad: String,  // Hipertensión, Diabetes, Obesidad, Colesterol alto
    val colorEnfermedad: String, // Código hexadecimal del color asociado
    val notifEnabled: Boolean = true, // Nuevo
    val notifType: String = "daily_fixed", // Nuevo
    val notifDetails: String = "{\"hour\":20}", // Nuevo, JSON para detalles
    val glucosaInicial: Int? = null, // Añadido
    val colesterolInicial: Int? = null, // Añadido
    val estadoCuenta: String = "INACTIVE" // INACTIVE, ACTIVE, SUSPENDED
)