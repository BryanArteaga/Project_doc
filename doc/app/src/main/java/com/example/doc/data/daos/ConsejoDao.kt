package com.example.doc.data.daos

import androidx.room.*
import com.example.doc.data.entities.ConsejoEntity

@Dao
interface ConsejoDao {
    @Insert
    suspend fun insert(consejo: ConsejoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(consejos: List<ConsejoEntity>)

    @Query("SELECT * FROM consejos WHERE enfermedad = :enfermedad ORDER BY creadoEn DESC LIMIT :limit")
    suspend fun getConsejosPorEnfermedad(
        enfermedad: String,
        limit: Int = 20  // FIX: Limita a 20 consejos maximo
    ): List<ConsejoEntity>

    @Query("SELECT * FROM consejos WHERE enfermedad = :enfermedad ORDER BY RANDOM() LIMIT 1")
    suspend fun getConsejoAleatorio(enfermedad: String): ConsejoEntity?  // FIX: Query directa sin randomOrNull()
}