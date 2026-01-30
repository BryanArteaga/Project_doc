package com.example.doc.repository

import com.example.doc.data.daos.UsuarioDao
import com.example.doc.data.entities.UsuarioEntity
import kotlinx.coroutines.flow.Flow

class UsuarioRepository(private val usuarioDao: UsuarioDao) {

    suspend fun guardarUsuario(usuario: UsuarioEntity) {
        usuarioDao.insert(usuario)
    }

    fun obtenerUsuarioFlow(): Flow<UsuarioEntity?> {
        return usuarioDao.getUsuario()
    }

    suspend fun obtenerUsuario(): UsuarioEntity? {
        return usuarioDao.getUsuarioSuspend()
    }

    suspend fun actualizarUsuario(usuario: UsuarioEntity) {
        usuarioDao.update(usuario)
    }

    suspend fun eliminarUsuario(usuario: UsuarioEntity) {
        usuarioDao.delete(usuario)
    }

    suspend fun eliminarTodos() {
        usuarioDao.deleteAll()
    }
}