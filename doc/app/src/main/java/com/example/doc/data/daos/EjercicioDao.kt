package com.example.doc.data.daos

import androidx.room.*
import com.example.doc.data.entities.EjercicioEntity

@Dao
interface EjercicioDao {
    @Insert
    suspend fun insert(ejercicio: EjercicioEntity)

    @Query("SELECT * FROM ejercicios WHERE enfermedad = :enfermedad")
    suspend fun getEjerciciosPorEnfermedad(enfermedad: String): List<EjercicioEntity>

    @Query("DELETE FROM ejercicios")
    suspend fun deleteAll()
    @Query("SELECT * FROM ejercicios")
    suspend fun getAll(): List<EjercicioEntity>
    @Query("SELECT * FROM ejercicios WHERE id = :id")
    suspend fun getEjercicioById(id: Int): EjercicioEntity?
}
