package com.example.doc.repository

import com.example.doc.data.daos.EjercicioDao
import com.example.doc.data.entities.EjercicioEntity
import kotlinx.coroutines.flow.Flow

class EjercicioRepository(private val dao: EjercicioDao) {

    suspend fun obtenerEjerciciosPorEnfermedad(enfermedad: String): List<EjercicioEntity> {
        return dao.getEjerciciosPorEnfermedadUnicos(enfermedad)
    }

    fun obtenerEjerciciosPorEnfermedadFlow(enfermedad: String): Flow<List<EjercicioEntity>> {
        return dao.getEjerciciosPorEnfermedadUnicosFlow(enfermedad)
    }

    suspend fun eliminarTodos() {
        dao.deleteAll()
    }
}