package com.example.doc.repository

import com.example.doc.data.daos.ConsejoDao
import com.example.doc.data.entities.ConsejoEntity

class ConsejoRepository(private val consejoDao: ConsejoDao) {
    suspend fun obtenerConsejosPorEnfermedad(enfermedad: String): List<ConsejoEntity> =
        consejoDao.getConsejosPorEnfermedad(enfermedad)

    // FIX #6: Obtener consejo aleatorio directamente de BD sin cargar lista completa
    suspend fun obtenerConsejoAleatorio(enfermedad: String): ConsejoEntity? =
        consejoDao.getConsejoAleatorio(enfermedad)
}