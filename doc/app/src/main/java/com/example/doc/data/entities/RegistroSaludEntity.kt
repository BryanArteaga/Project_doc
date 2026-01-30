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
    val presionArterial: String? = null,
    val glucosa: Int? = null,
    val imc: Double? = null,
    val colesterol: Int? = null,
    val calorias: Int,
    val ejercicio: Int,
    val estadoAnimo: String
)