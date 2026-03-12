package com.example.doc.data.daos

import androidx.room.*
import com.example.doc.data.entities.AsignacionEjercicioEntity

@Dao
interface AsignacionEjercicioDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(asignacion: AsignacionEjercicioEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(asignaciones: List<AsignacionEjercicioEntity>)

    @Update
    suspend fun update(asignacion: AsignacionEjercicioEntity)

    @Delete
    suspend fun delete(asignacion: AsignacionEjercicioEntity)

    @Query("SELECT * FROM asignaciones_ejercicios WHERE id = :id")
    suspend fun getById(id: Int): AsignacionEjercicioEntity?

    @Query("SELECT * FROM asignaciones_ejercicios WHERE ejercicioId = :ejercicioId")
    suspend fun getByEjercicioId(ejercicioId: Int): List<AsignacionEjercicioEntity>

    // ✅ Obtener asignaciones activas para el usuario
    // - Directas (usuarioId = ? AND activo = true)
    // - Generales (usuarioId IS NULL AND enfermedad = ? AND activo = true)
    // - No expiradas (fechaExpiracion IS NULL OR fechaExpiracion > ahora)
    @Query("""
        SELECT * FROM asignaciones_ejercicios
        WHERE activo = 1
        AND usuarioId = :usuarioId
        AND (fechaExpiracion IS NULL OR fechaExpiracion > :ahora)
        ORDER BY fechaAsignacion DESC
    """)
    suspend fun getAsignacionesActivasDirectas(usuarioId: Int, ahora: Long): List<AsignacionEjercicioEntity>

    @Query("""
        SELECT * FROM asignaciones_ejercicios
        WHERE activo = 1
        AND usuarioId IS NULL
        AND enfermedad = :enfermedad
        AND (fechaExpiracion IS NULL OR fechaExpiracion > :ahora)
        ORDER BY fechaAsignacion DESC
    """)
    suspend fun getAsignacionesActivasGenerales(enfermedad: String, ahora: Long): List<AsignacionEjercicioEntity>

    // ✅ Obtener todas las asignaciones del usuario (para sincronizar)
    @Query("SELECT * FROM asignaciones_ejercicios WHERE usuarioId = :usuarioId ORDER BY fechaAsignacion DESC")
    suspend fun getAsignacionesForSyncDirectas(usuarioId: Int): List<AsignacionEjercicioEntity>

    @Query("SELECT * FROM asignaciones_ejercicios WHERE usuarioId IS NULL AND enfermedad = :enfermedad ORDER BY fechaAsignacion DESC")
    suspend fun getAsignacionesForSyncGenerales(enfermedad: String): List<AsignacionEjercicioEntity>

    // ✅ Obtener asignaciones no sincronizadas
    @Query("SELECT * FROM asignaciones_ejercicios WHERE isSynced = 0 ORDER BY fechaAsignacion DESC")
    suspend fun getUnsyncedAsignaciones(): List<AsignacionEjercicioEntity>

    // ✅ Marcar como sincronizada
    @Query("UPDATE asignaciones_ejercicios SET isSynced = 1, syncId = :syncId, lastSyncTimestamp = :timestamp WHERE id = :id")
    suspend fun markAsSynced(id: Int, syncId: String, timestamp: Long)

    // ✅ Obtener todas las asignaciones
    @Query("SELECT * FROM asignaciones_ejercicios ORDER BY fechaAsignacion DESC")
    suspend fun getAll(): List<AsignacionEjercicioEntity>

    // ✅ Contar asignaciones activas
    @Query("""
        SELECT COUNT(*) FROM asignaciones_ejercicios
        WHERE activo = 1
        AND usuarioId = :usuarioId
        AND (fechaExpiracion IS NULL OR fechaExpiracion > :ahora)
    """)
    suspend fun countAsignacionesActivasDirectas(usuarioId: Int, ahora: Long): Int

    @Query("""
        SELECT COUNT(*) FROM asignaciones_ejercicios
        WHERE activo = 1
        AND usuarioId IS NULL
        AND enfermedad = :enfermedad
        AND (fechaExpiracion IS NULL OR fechaExpiracion > :ahora)
    """)
    suspend fun countAsignacionesActivasGenerales(enfermedad: String, ahora: Long): Int

    // ✅ Eliminar asignaciones expiradas
    @Query("DELETE FROM asignaciones_ejercicios WHERE fechaExpiracion IS NOT NULL AND fechaExpiracion <= :ahora")
    suspend fun deleteExpired(ahora: Long)

    // ✅ Sincronizar: eliminar asignaciones del usuario
    @Query("DELETE FROM asignaciones_ejercicios WHERE usuarioId = :usuarioId")
    suspend fun deleteForSyncDirectas(usuarioId: Int)

    @Query("DELETE FROM asignaciones_ejercicios WHERE usuarioId IS NULL AND enfermedad = :enfermedad")
    suspend fun deleteForSyncGenerales(enfermedad: String)
}
