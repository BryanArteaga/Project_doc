package com.example.doc.data.daos

import androidx.room.*
import com.example.doc.data.entities.UsuarioEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UsuarioDao {
    @Insert
    suspend fun insert(usuario: UsuarioEntity)

    @Update
    suspend fun update(usuario: UsuarioEntity)

    @Delete
    suspend fun delete(usuario: UsuarioEntity)

    @Query("SELECT * FROM usuarios LIMIT 1")
    fun getUsuario(): Flow<UsuarioEntity?>

    @Query("SELECT * FROM usuarios LIMIT 1")
    suspend fun getUsuarioSuspend(): UsuarioEntity?

    @Query("DELETE FROM usuarios")
    suspend fun deleteAll()
}