package com.example.doc.utils

/**
 * Utilidades para formatear datos de salud para mostrar en la UI
 */
object HealthFormatUtils {

    /**
     * Convierte minutos a un formato legible
     * 0 → "0 min"
     * 45 → "45 min"
     * 90 → "1h 30min"
     * 150 → "2h 30min"
     */
    fun formatearMinutos(minutos: Int?): String {
        val totalMinutos = minutos ?: 0
        
        return when {
            totalMinutos <= 0 -> "0 min"
            totalMinutos < 60 -> "$totalMinutos min"
            else -> {
                val horas = totalMinutos / 60
                val mins = totalMinutos % 60
                if (mins > 0) {
                    "${horas}h ${mins}min"
                } else {
                    "${horas}h"
                }
            }
        }
    }

    /**
     * Alternativa corta para mostrar solo "45 min" o "1h 30" (sin "min" al final)
     */
    fun formatearMinutosCorto(minutos: Int?): String {
        val totalMinutos = minutos ?: 0
        
        return when {
            totalMinutos <= 0 -> "0"
            totalMinutos < 60 -> "$totalMinutos"
            else -> {
                val horas = totalMinutos / 60
                val mins = totalMinutos % 60
                if (mins > 0) {
                    "${horas}h ${mins}min"
                } else {
                    "${horas}h"
                }
            }
        }
    }
}
