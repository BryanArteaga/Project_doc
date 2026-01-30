package com.example.doc.data.entities

import androidx.annotation.DrawableRes
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ejercicios")
data class EjercicioEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val enfermedad: String, // A qué enfermedad está asociado
    val nombre: String,
    val descripcion: String,
    val duracion: Int, // Duración de cada serie en segundos
    val intensidad: String, // Baja, Media, Alta
    @DrawableRes val imagenResId: Int, // Imagen del ejercicio
    val series: Int = 3, // Número de series (por defecto 3)
    val descansoEntreSeries: Int = 30 // Descanso en segundos entre series (por defecto 30s)
)