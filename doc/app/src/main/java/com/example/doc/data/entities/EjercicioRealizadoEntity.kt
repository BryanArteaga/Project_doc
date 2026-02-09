package com.example.doc.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ejercicios_realizados")
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