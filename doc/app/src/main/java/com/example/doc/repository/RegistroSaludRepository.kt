package com.example.doc.repository

import com.example.doc.data.daos.RegistroSaludDao
import com.example.doc.data.entities.RegistroSaludEntity

class RegistroSaludRepository(private val dao: RegistroSaludDao) {

    suspend fun guardarRegistro(registro: RegistroSaludEntity) {
        dao.insert(registro)
    }

    suspend fun obtenerRegistros(usuarioId: Int): List<RegistroSaludEntity> {
        return dao.getRegistrosPorUsuario(usuarioId)
    }

    suspend fun obtenerRegistroPorFecha(usuarioId: Int, fecha: String): RegistroSaludEntity? {
        return dao.getRegistroPorFecha(usuarioId, fecha)
    }

    suspend fun eliminarRegistrosDeUsuario(usuarioId: Int) {
        dao.deleteRegistrosUsuario(usuarioId)
    }
}