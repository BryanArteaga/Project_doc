package com.example.doc.data.daos

import androidx.paging.PagingSource
import androidx.room.*
import com.example.doc.data.entities.RegistroSaludEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RegistroSaludDao {
    @Insert
    suspend fun insert(registro: RegistroSaludEntity)

    @Update
    suspend fun update(registro: RegistroSaludEntity)

    @Query("SELECT * FROM registros_salud WHERE usuarioId = :usuarioId ORDER BY fecha DESC")
    suspend fun getRegistrosPorUsuario(usuarioId: Int): List<RegistroSaludEntity>

    // Paginación con Paging3 - Para listas grandes de registros
    @Query("SELECT * FROM registros_salud WHERE usuarioId = :usuarioId ORDER BY fecha DESC")
    fun getRegistrosPaginados(usuarioId: Int): PagingSource<Int, RegistroSaludEntity>

    // Flow para observar cambios en tiempo real
    @Query("SELECT * FROM registros_salud WHERE usuarioId = :usuarioId ORDER BY fecha DESC LIMIT :limit")
    fun getRegistrosRecientesFlow(usuarioId: Int, limit: Int = 10): Flow<List<RegistroSaludEntity>>

    @Query("SELECT * FROM registros_salud WHERE usuarioId = :usuarioId AND fecha = :fecha LIMIT 1")
    suspend fun getRegistroPorFecha(usuarioId: Int, fecha: String): RegistroSaludEntity?

    @Query("DELETE FROM registros_salud WHERE usuarioId = :usuarioId")
    suspend fun deleteRegistrosUsuario(usuarioId: Int)

    // Obtener conteo de registros
    @Query("SELECT COUNT(*) FROM registros_salud WHERE usuarioId = :usuarioId")
    suspend fun getCountRegistros(usuarioId: Int): Int

    // Obtener registros no sincronizados
    @Query("SELECT * FROM registros_salud WHERE usuarioId = :usuarioId AND isSynced = 0 ORDER BY fecha DESC")
    suspend fun getUnsyncedRegistros(usuarioId: Int): List<RegistroSaludEntity>

    // Contar registros no sincronizados
    @Query("SELECT COUNT(*) FROM registros_salud WHERE usuarioId = :usuarioId AND isSynced = 0")
    suspend fun getUnsyncedCount(usuarioId: Int): Int

    // Marcar registro como sincronizado
    @Query("UPDATE registros_salud SET isSynced = 1, syncId = :syncId, lastSyncTimestamp = :timestamp WHERE id = :id")
    suspend fun markAsSynced(id: Int, syncId: String, timestamp: Long)
}