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
    indices = [
        Index(value = ["usuarioId"]),
        Index(value = ["fecha", "hora", "usuarioId"], unique = true)  // ✅ Previene duplicados la misma hora
    ]
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
    val imc: Double? = null,  // ✅ IMC (calculado para Obesidad)
    val pulso: Int? = null,  // ✅ Pulso (opcional, solo para Obesidad)
    val calorias: Int? = null,
    val ejercicio: String? = null,
    val ejercicioMinutos: Int = 0,  // ✅ Minutos de ejercicio (calculado desde app)
    val estadoAnimo: String? = null,
    val notas: String? = null,
    val registradoEn: Long,
    val syncId: String? = null,
    val isSynced: Boolean = false,
    val lastSyncTimestamp: Long? = null
)