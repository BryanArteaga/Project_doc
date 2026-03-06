package com.example.doc.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "registros_salud",
    foreignKeys = [
        ForeignKey(
            entity = UsuarioEntity::class,
            parentColumns = ["id"],
            childColumns = ["usuarioId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["usuarioId"])]
)
data class RegistroSaludEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val usuarioId: Int,
    val fecha: String,
    val hora: String? = null,
    val presionSistolica: Int? = null,
    val presionDiastolica: Int? = null,
    val glucosa: Int? = null,
    val colesterol: Int? = null,
    val temperatura: Double? = null,
    val peso: Double? = null,
    val calorias: Int? = null,
    val ejercicio: String? = null,
    val estadoAnimo: String? = null,
    val notas: String? = null,
    val registradoEn: Long,
    val syncId: String? = null,
    val isSynced: Boolean = false,
    val lastSyncTimestamp: Long? = null
)