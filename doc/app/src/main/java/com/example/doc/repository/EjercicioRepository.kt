package com.example.doc.repository

import com.example.doc.data.daos.EjercicioDao
import com.example.doc.data.daos.AsignacionEjercicioDao
import com.example.doc.data.entities.EjercicioEntity
import kotlinx.coroutines.flow.Flow

class EjercicioRepository(
    private val dao: EjercicioDao,
    private val asignacionDao: AsignacionEjercicioDao
) {

    suspend fun obtenerEjerciciosPorEnfermedad(enfermedad: String): List<EjercicioEntity> {
        return dao.getEjerciciosPorEnfermedadUnicos(enfermedad)
    }

    fun obtenerEjerciciosPorEnfermedadFlow(enfermedad: String): Flow<List<EjercicioEntity>> {
        return dao.getEjerciciosPorEnfermedadUnicosFlow(enfermedad)
    }

    /**
     * ✅ Obtiene ejercicios generales + asignaciones directas del usuario
     * Combinadas y ordenadas por fecha DESC (más recientes primero)
     */
    suspend fun obtenerEjerciciosConAsignaciones(
        enfermedad: String,
        usuarioId: Int
    ): List<EjercicioEntity> {
        val ahora = System.currentTimeMillis()
        
        // 1️⃣ Obtener ejercicios generales de la enfermedad
        val ejerciciosGenerales = dao.getEjerciciosPorEnfermedadUnicos(enfermedad)
        
        // 2️⃣ Obtener asignaciones directas (por paciente) activas
        val asignacionesDirectas = asignacionDao.getAsignacionesActivasDirectas(usuarioId, ahora)
        val ejerciciosAsignados: List<EjercicioEntity> = asignacionesDirectas.mapNotNull { asignacion ->
            dao.getEjercicioById(asignacion.ejercicioId)?.copy(
                createdAt = asignacion.fechaAsignacion  // Usar fecha de asignación como timestamp
            )
        }
        
        // 3️⃣ Combinar y eliminar duplicados, ordenar por createdAt DESC
        val combinados = (ejerciciosGenerales + ejerciciosAsignados)
            .distinctBy { it.id }  // Eliminar duplicads
            .sortedByDescending { it.createdAt }  // Más recientes primero
        
        return combinados
    }

    suspend fun eliminarTodos() {
        dao.deleteAll()
    }
}