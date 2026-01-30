package com.example.doc.data.daos

import androidx.room.*
import com.example.doc.data.entities.EjercicioRealizadoEntity

@Dao
interface EjercicioRealizadoDao {
    @Insert
    suspend fun insert(realizado: EjercicioRealizadoEntity)

    @Query("SELECT * FROM ejercicios_realizados WHERE usuarioId = :usuarioId AND fecha = :fecha")
    suspend fun getEjerciciosRealizadosHoy(usuarioId: Int, fecha: String): List<EjercicioRealizadoEntity>

    @Query("SELECT * FROM ejercicios_realizados WHERE usuarioId = :usuarioId AND ejercicioId = :ejercicioId AND fecha = :fecha LIMIT 1")
    suspend fun getRealizado(usuarioId: Int, ejercicioId: Int, fecha: String): EjercicioRealizadoEntity?

    @Query("DELETE FROM ejercicios_realizados WHERE usuarioId = :usuarioId AND ejercicioId = :ejercicioId AND fecha = :fecha")
    suspend fun deleteByUsuarioEjercicioFecha(usuarioId: Int, ejercicioId: Int, fecha: String)
}