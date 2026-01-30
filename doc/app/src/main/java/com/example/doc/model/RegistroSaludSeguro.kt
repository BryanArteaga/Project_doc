package com.example.doc.model

/**
 * Modelo de dominio para Registro de Salud con datos cifrados
 */
data class RegistroSaludSeguro(
    val id: Int = 0,
    val usuarioId: Int,
    val fecha: String,
    val hora: String? = null,
    val presionArterial: String? = null,
    val glucosa: Double? = null,         // Será cifrado
    val imc: Double? = null,
    val colesterol: Double? = null,      // Será cifrado
    val calorias: Int,
    val ejercicio: Int,
    val estadoAnimo: String
) {
    companion object {
        /**
         * Crea un registro seguro desde valores sin cifrar
         */
        fun fromPlainData(
            id: Int = 0,
            usuarioId: Int,
            fecha: String,
            hora: String? = null,
            presionArterial: String? = null,
            glucosa: Double? = null,
            imc: Double? = null,
            colesterol: Double? = null,
            calorias: Int,
            ejercicio: Int,
            estadoAnimo: String
        ) = RegistroSaludSeguro(
            id = id,
            usuarioId = usuarioId,
            fecha = fecha,
            hora = hora,
            presionArterial = presionArterial,
            glucosa = glucosa,
            imc = imc,
            colesterol = colesterol,
            calorias = calorias,
            ejercicio = ejercicio,
            estadoAnimo = estadoAnimo
        )
    }
}

