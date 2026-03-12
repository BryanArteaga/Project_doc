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

    @Query("SELECT * FROM ejercicios WHERE enfermedad = :enfermedad")
    suspend fun getEjerciciosPorEnfermedad(enfermedad: String): List<EjercicioEntity>

    @Query("""
        SELECT * FROM ejercicios
        WHERE id IN (
            SELECT MIN(id)
            FROM ejercicios
            WHERE enfermedad = :enfermedad
            GROUP BY nombre, enfermedad
        )
        ORDER BY id
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
        ORDER BY id
    """)
    fun getEjerciciosPorEnfermedadUnicosFlow(enfermedad: String): kotlinx.coroutines.flow.Flow<List<EjercicioEntity>>

    @Query("DELETE FROM ejercicios")
    suspend fun deleteAll()
    @Query("SELECT * FROM ejercicios")
    suspend fun getAll(): List<EjercicioEntity>
    @Query("SELECT * FROM ejercicios WHERE id = :id")
    suspend fun getEjercicioById(id: Int): EjercicioEntity?
}
