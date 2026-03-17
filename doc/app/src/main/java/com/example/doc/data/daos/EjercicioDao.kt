package com.example.doc.data.daos

import androidx.room.*
import com.example.doc.data.entities.EjercicioEntity

@Dao
interface EjercicioDao {
    @Insert
    suspend fun insert(ejercicio: EjercicioEntity)

    @Update
    suspend fun update(ejercicio: EjercicioEntity)

    @Insert
    suspend fun insertAll(ejercicios: List<EjercicioEntity>)

    @Upsert
    suspend fun upsertAll(ejercicios: List<EjercicioEntity>)

    @Query("SELECT * FROM ejercicios WHERE enfermedad = :enfermedad LIMIT :limit")
    suspend fun getEjerciciosPorEnfermedad(
        enfermedad: String,
        limit: Int = 50  // FIX: Limita queries a 50 ejercicios maximo
    ): List<EjercicioEntity>

    @Query("""
        SELECT * FROM ejercicios
        WHERE id IN (
            SELECT MIN(id)
            FROM ejercicios
            WHERE enfermedad = :enfermedad
            GROUP BY nombre, enfermedad
        )
        ORDER BY createdAt DESC
    """)
    suspend fun getEjerciciosPorEnfermedadUnicos(enfermedad: String): List<EjercicioEntity>

    @Query("SELECT * FROM ejercicios WHERE enfermedad = :enfermedad")
    fun getEjerciciosPorEnfermedadFlow(enfermedad: String): kotlinx.coroutines.flow.Flow<List<EjercicioEntity>>

    @Query("""
        SELECT * FROM ejercicios
        WHERE id IN (
            SELECT MIN(id)
            FROM ejercicios
            WHERE enfermedad = :enfermedad
            GROUP BY nombre, enfermedad
        )
        ORDER BY createdAt DESC
    """)
    fun getEjerciciosPorEnfermedadUnicosFlow(enfermedad: String): kotlinx.coroutines.flow.Flow<List<EjercicioEntity>>

    @Query("SELECT * FROM ejercicios WHERE id = :id")
    suspend fun getEjercicioById(id: Int): EjercicioEntity?

    @Query("SELECT COUNT(*) FROM ejercicios")
    suspend fun getCount(): Int  // FIX: Solo cuenta, no carga tabla completa

    @Query("SELECT * FROM ejercicios LIMIT :limit")
    suspend fun getAll(limit: Int = 100): List<EjercicioEntity>  // FIX: Con LIMIT para evitar OOM

    @Query("DELETE FROM ejercicios")
    suspend fun deleteAll()
}
