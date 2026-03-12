package com.example.doc.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "asignaciones_ejercicios",
    foreignKeys = [
        ForeignKey(
            entity = EjercicioEntity::class,
            parentColumns = ["id"],
            childColumns = ["ejercicioId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UsuarioEntity::class,
            parentColumns = ["id"],
            childColumns = ["usuarioId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["ejercicioId"]),
        Index(value = ["usuarioId"]),
        Index(value = ["activo"]),
        Index(value = ["enfermedad"])
    ]
)
data class AsignacionEjercicioEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ejercicioId: Int,
    val doctorId: Int,                          // Información del doctor (solo almacenada, no relación)
    val usuarioId: Int? = null,                  // null = asignación general; Int = asignación individual
    val enfermedad: String? = null,              // Filtro: asignar solo a pacientes con esta enfermedad
    val descripcion: String? = null,
    val activo: Boolean = true,
    val fechaAsignacion: Long,                   // Timestamp de cuándo fue asignado
    val fechaExpiracion: Long? = null,           // null = nunca expira; Long = fecha de expiración
    val syncId: String? = null,                  // ID de sincronización con backend
    val isSynced: Boolean = false,
    val lastSyncTimestamp: Long? = null
)
