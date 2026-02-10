package com.example.doc.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ejercicios_realizados",
    indices = [
        Index(value = ["usuarioId", "ejercicioId", "fecha"], unique = true)
    ]
)
data class EjercicioRealizadoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val usuarioId: Int,
    val ejercicioId: Int,
    val fecha: String, // yyyy-MM-dd
    val syncId: String? = null,
    val isSynced: Boolean = false,
    val lastSyncTimestamp: Long? = null
)