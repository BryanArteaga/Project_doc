package com.example.doc.model

import androidx.annotation.DrawableRes

data class Ejercicio(
    val id: Int = 0,
    val enfermedad: String,
    val nombre: String,
    val descripcion: String,
    val duracion: Int, // Duración de cada serie en segundos
    val intensidad: String,
    @DrawableRes val imagenResId: Int,
    val series: Int = 3, // Número de series
    val descansoEntreSeries: Int = 30, // Descanso entre series en segundos
    val urlImagen: String? = null, // URL de imagen desde backend
    val urlVideo: String? = null // URL de video desde backend
)