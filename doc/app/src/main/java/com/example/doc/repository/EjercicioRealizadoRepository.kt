package com.example.doc.repository

import com.example.doc.data.daos.EjercicioRealizadoDao
import com.example.doc.data.entities.EjercicioRealizadoEntity

class EjercicioRealizadoRepository(private val dao: EjercicioRealizadoDao) {
    suspend fun marcarRealizado(realizado: EjercicioRealizadoEntity) = dao.insert(realizado)
    suspend fun realizadosHoy(usuarioId: Int, fecha: String) = dao.getEjerciciosRealizadosHoy(usuarioId, fecha)
    suspend fun realizado(usuarioId: Int, ejercicioId: Int, fecha: String) = dao.getRealizado(usuarioId, ejercicioId, fecha)
    suspend fun desmarcarRealizado(usuarioId: Int, ejercicioId: Int, fecha: String) {
        dao.deleteByUsuarioEjercicioFecha(usuarioId, ejercicioId, fecha)
    }
}