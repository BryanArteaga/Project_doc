package com.example.doc.data.daos

import androidx.room.*
import com.example.doc.data.entities.ConsejoEntity

@Dao
interface ConsejoDao {
    @Insert
    suspend fun insert(consejo: ConsejoEntity)

    @Query("SELECT * FROM consejos WHERE enfermedad = :enfermedad")
    suspend fun getConsejosPorEnfermedad(enfermedad: String): List<ConsejoEntity>
}