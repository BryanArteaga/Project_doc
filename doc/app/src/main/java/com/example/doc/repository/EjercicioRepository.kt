package com.example.doc.repository

import com.example.doc.data.daos.EjercicioDao
import com.example.doc.data.entities.EjercicioEntity

class EjercicioRepository(private val dao: EjercicioDao) {

    suspend fun obtenerEjerciciosPorEnfermedad(enfermedad: String): List<EjercicioEntity> {
        return dao.getEjerciciosPorEnfermedad(enfermedad)
    }

    suspend fun eliminarTodos() {
        dao.deleteAll()
    }
}