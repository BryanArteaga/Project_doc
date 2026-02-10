package com.example.doc.data.daos

import androidx.room.*
import com.example.doc.data.entities.EjercicioRealizadoEntity

@Dao
interface EjercicioRealizadoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(realizado: EjercicioRealizadoEntity)

    @Update
    suspend fun update(realizado: EjercicioRealizadoEntity)

    @Query("SELECT * FROM ejercicios_realizados WHERE usuarioId = :usuarioId AND fecha = :fecha")
    suspend fun getEjerciciosRealizadosHoy(usuarioId: Int, fecha: String): List<EjercicioRealizadoEntity>

    @Query("SELECT * FROM ejercicios_realizados WHERE usuarioId = :usuarioId AND ejercicioId = :ejercicioId AND fecha = :fecha LIMIT 1")
    suspend fun getRealizado(usuarioId: Int, ejercicioId: Int, fecha: String): EjercicioRealizadoEntity?

    @Query("DELETE FROM ejercicios_realizados WHERE usuarioId = :usuarioId AND ejercicioId = :ejercicioId AND fecha = :fecha")
    suspend fun deleteByUsuarioEjercicioFecha(usuarioId: Int, ejercicioId: Int, fecha: String)

    // Obtener ejercicios no sincronizados
    @Query("SELECT * FROM ejercicios_realizados WHERE usuarioId = :usuarioId AND isSynced = 0 ORDER BY fecha DESC")
    suspend fun getUnsyncedEjercicios(usuarioId: Int): List<EjercicioRealizadoEntity>

    // Obtener TODOS los ejercicios no sincronizados (sin filtrar por usuarioId)
    // Útil cuando el usuarioId ha cambiado por offline registration
    @Query("SELECT * FROM ejercicios_realizados WHERE isSynced = 0 ORDER BY fecha DESC")
    suspend fun getAllUnsyncedEjercicios(): List<EjercicioRealizadoEntity>

    // Contar ejercicios no sincronizados
    @Query("SELECT COUNT(*) FROM ejercicios_realizados WHERE usuarioId = :usuarioId AND isSynced = 0")
    suspend fun getUnsyncedCount(usuarioId: Int): Int

    // Marcar ejercicio como sincronizado
    @Query("UPDATE ejercicios_realizados SET isSynced = 1, syncId = :syncId, lastSyncTimestamp = :timestamp WHERE id = :id")
    suspend fun markAsSynced(id: Int, syncId: String, timestamp: Long)
}