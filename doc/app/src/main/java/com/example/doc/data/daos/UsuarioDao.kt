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

    @Update
    suspend fun updateUsuario(usuarioActualizado: UsuarioEntity)

    // 🔍 Buscar usuario por cédula (para verificar si ya existe)
    @Query("SELECT * FROM usuarios WHERE cedula = :cedula LIMIT 1")
    suspend fun getByCedula(cedula: String?): UsuarioEntity?

    // 🔍 Verificar si un usuario existe por cédula
    @Query("SELECT EXISTS(SELECT 1 FROM usuarios WHERE cedula = :cedula)")
    suspend fun existsByCedula(cedula: String?): Boolean
}