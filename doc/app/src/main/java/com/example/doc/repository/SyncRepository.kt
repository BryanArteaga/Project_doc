package com.example.doc.repository

import android.content.SharedPreferences
import com.example.doc.data.daos.EjercicioRealizadoDao
import com.example.doc.data.daos.RegistroSaludDao
import com.example.doc.data.daos.UsuarioDao
import com.example.doc.data.entities.UsuarioEntity
import com.example.doc.network.ApiService
import com.example.doc.network.SyncPushRequest
import com.example.doc.network.PushRegistroSaludRequest
import com.example.doc.network.PushEjercicioRealizadoRequest
import com.example.doc.network.FCMTokenRequest
import com.example.doc.network.CreateUsuarioRequest
import timber.log.Timber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * DTOs para Response
 */
data class SyncPushResponse(
    val success: Boolean,
    val message: String,
    val registrosSincronizados: Int,
    val ejerciciosSincronizados: Int,
    val timestamp: String,
    val errores: Map<String, List<String>>? = null
)

data class SyncPullResponse(
    val usuario: UsuarioPullDTO,
    val ejercicios: List<EjercicioDTO>,
    val notificaciones: List<NotificacionDTO>,
    val estadoCuenta: EstadoCuentaDTO
)

data class UsuarioPullDTO(
    val id: Int,
    val nombre: String,
    val apellido: String?,
    val email: String?,
    val telefono: String?,
    val estadoCuenta: String? = null  // Agregar estadoCuenta aquí también
)

data class EjercicioDTO(
    val id: Int,
    val nombre: String,
    val descripcion: String?,
    val duracion: Int?,
    val intensidad: String?
)

data class NotificacionDTO(
    val id: Int,
    val tipo: String,
    val titulo: String,
    val mensaje: String,
    val leido: Boolean
)

data class EstadoCuentaDTO(
    val activo: Boolean,
    val ultimaSincronizacion: String,
    val rol: String
)

data class SyncStatus(
    val sincronizado: Boolean,
    val ultimaSincronizacion: String?,
    val datosLocales: Map<String, Int>,
    val datosRemote: Map<String, Int>
)

/**
 * Repositorio de Sincronización
 * Maneja la sincronización bidireccional (PUSH/PULL) con el backend
 */
class SyncRepository(
    private val apiService: ApiService,
    private val registroSaludDao: RegistroSaludDao,
    private val ejercicioRealizadoDao: EjercicioRealizadoDao,
    private val usuarioDao: UsuarioDao,
    private val sharedPreferences: SharedPreferences
) {

    /**
     * Detectar si hay cambios nuevos para sincronizar (ejercicios o registros)
     */
    suspend fun hayDatosParaSincronizar(usuarioId: Int): Boolean {
        return withContext(Dispatchers.IO) {
            val registrosSinSync = registroSaludDao.getUnsyncedRegistros(usuarioId)
            val ejerciciosSinSync = ejercicioRealizadoDao.getUnsyncedEjercicios(usuarioId)

            val hayCambios = registrosSinSync.isNotEmpty() || ejerciciosSinSync.isNotEmpty()
            Timber.d("🔍 Detectar cambios: ${registrosSinSync.size} registros, ${ejerciciosSinSync.size} ejercicios (¿Hay cambios? $hayCambios)")

            hayCambios
        }
    }

    /**
     * PUSH: Enviar datos locales al backend
     * - Datos de salud registrados
     * - Ejercicios completados
     */
    suspend fun pushDataToBackend(usuarioId: Int): Result<SyncPushResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.d("📤 Iniciando PUSH de datos al backend para usuario: $usuarioId")

                // 0️⃣ NUEVA LÓGICA: Verificar si el usuario existe en el backend
                // Si se registró offline, crear en el backend primero
                val usuarioEnRoom = usuarioDao.getUsuarioSuspend()
                if (usuarioEnRoom != null) {
                    Timber.d("📋 Usuario en Room antes de crear en backend: ID=${usuarioEnRoom.id}, nombre=${usuarioEnRoom.nombre}")
                    crearUsuarioEnBackendSiNoExiste(usuarioEnRoom)
                    // Pequeño delay para asegurar que la actualización se propagó
                    kotlinx.coroutines.delay(100)
                }

                // 🔄 Refrescar usuarioId del usuario actual en caso que haya cambiado
                val usuarioActualizado = usuarioDao.getUsuarioSuspend()
                val usuarioIdActual = usuarioActualizado?.id ?: usuarioId
                Timber.d("📋 Usuario en Room después de crear en backend: ID=${usuarioActualizado?.id}, nombre=${usuarioActualizado?.nombre}")
                if (usuarioIdActual != usuarioId) {
                    Timber.d("🔄 usuarioId actualizado: $usuarioId → $usuarioIdActual (cambió por offline registration)")
                }

                // 1. Obtener registros de salud sin sincronizar
                val registrosSinSync = registroSaludDao.getUnsyncedRegistros(usuarioIdActual)
                Timber.d("📋 Registros de salud sin sincronizar: ${registrosSinSync.size}")

                // 2. Obtener ejercicios realizados sin sincronizar
                val ejerciciosSinSync = ejercicioRealizadoDao.getUnsyncedEjercicios(usuarioIdActual)
                Timber.d("📋 Ejercicios realizados sin sincronizar: ${ejerciciosSinSync.size}")
                if (ejerciciosSinSync.isEmpty()) {
                    Timber.w("⚠️ No hay ejercicios sin sincronizar (comprobando si existen ejercicios en BD...)")
                    // DEBUG: contar total de ejercicios
                    val totalEjercicios = ejercicioRealizadoDao.getEjerciciosRealizadosHoy(usuarioIdActual, java.time.LocalDate.now().toString())
                    Timber.d("DEBUG: Total de ejercicios hoy: ${totalEjercicios.size}")
                    totalEjercicios.forEach { ej ->
                        Timber.d("  - ID: ${ej.id}, ejercicioId: ${ej.ejercicioId}, isSynced: ${ej.isSynced}, syncId: ${ej.syncId}")
                    }
                }

                // 3. Construir request (incluir usuarioId ACTUALIZADO en el body)
                val pushRequest = SyncPushRequest(
                    usuarioId = usuarioIdActual,  // Sin JWT, se envía en el body (ID actualizado)
                    registrosSalud = registrosSinSync.map { rs ->
                        PushRegistroSaludRequest(
                            presionSistolica = rs.presionArterial?.split("/")?.getOrNull(0)?.toIntOrNull(),
                            presionDiastolica = rs.presionArterial?.split("/")?.getOrNull(1)?.toIntOrNull(),
                            glucosa = rs.glucosa?.toFloat(),
                            colesterol = rs.colesterol?.toFloat(),
                            calorias = rs.calorias,
                            ejercicio = rs.ejercicio,
                            estadoAnimo = rs.estadoAnimo,
                            fecha = rs.fecha,
                            hora = rs.hora
                        )
                    },
                    ejerciciosRealizados = ejerciciosSinSync.map { er ->
                        PushEjercicioRealizadoRequest(
                            ejercicioId = er.ejercicioId,
                            asignacionId = null,
                            fecha = er.fecha,
                            completadoEn = er.fecha
                        )
                    },
                    ultimaSincronizacion = sharedPreferences.getString("last_sync", null)
                )

                // 4. Enviar al backend
                val response = apiService.pushSync(pushRequest)

                if (response.isSuccessful) {
                    val body = response.body()!!
                    Timber.d("✅ PUSH exitoso: ${body.registrosSincronizados} registros, ${body.ejerciciosSincronizados} ejercicios")

                    // 5. Marcar como sincronizados localmente
                    val syncId = body.timestamp  // Usar timestamp como ID de sincronización
                    val timestamp = System.currentTimeMillis()
                    registrosSinSync.forEach { rs ->
                        registroSaludDao.markAsSynced(rs.id, syncId, timestamp)
                    }

                    ejerciciosSinSync.forEach { er ->
                        ejercicioRealizadoDao.markAsSynced(er.id, syncId, timestamp)
                    }

                    // 6. Actualizar timestamp de última sincronización
                    sharedPreferences.edit()
                        .putString("last_sync", body.timestamp)
                        .putLong("last_successful_sync", timestamp)
                        .apply()

                    Result.success(body)
                } else {
                    Timber.e("❌ Error en PUSH: ${response.code()} - ${response.message()}")
                    Result.failure(Exception("Error en PUSH: ${response.message()}"))
                }
            } catch (e: Exception) {
                Timber.e(e, "❌ Excepción en PUSH")
                Result.failure(e)
            }
        }
    }

    /**
     * PULL: Obtener datos del backend
     * - Ejercicios asignados
     * - Notificaciones del doctor
     * - Estado de la cuenta
     */
    suspend fun pullDataFromBackend(usuarioId: Int): Result<SyncPullResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.d("📥 Iniciando PULL de datos del backend para usuario: $usuarioId")

                val response = apiService.pullSync(usuarioId)

                if (response.isSuccessful) {
                    val body = response.body()!!
                    Timber.d("✅ PULL exitoso: ${body.ejercicios.size} ejercicios, ${body.notificaciones.size} notificaciones")

                    // Verificar estado de cuenta
                    if (!body.estadoCuenta.activo) {
                        Timber.w("⚠️ Cuenta inactiva: ${body.estadoCuenta}")
                        sharedPreferences.edit().putBoolean("account_inactive", true).apply()
                    } else {
                        sharedPreferences.edit().putBoolean("account_inactive", false).apply()
                    }

                    // Actualizar timestamp
                    sharedPreferences.edit()
                        .putString("last_sync", body.estadoCuenta.ultimaSincronizacion)
                        .apply()

                    Result.success(body)
                } else {
                    Timber.e("❌ Error en PULL: ${response.code()} - ${response.message()}")
                    Result.failure(Exception("Error en PULL: ${response.message()}"))
                }
            } catch (e: Exception) {
                Timber.e(e, "❌ Excepción en PULL")
                Result.failure(e)
            }
        }
    }

    /**
     * Sincronización completa: PUSH primero, luego PULL
     */
    suspend fun fullSync(usuarioId: Int): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.d("🔄 Iniciando sincronización completa para usuario: $usuarioId")

                // 1. PUSH primero
                val pushResult = pushDataToBackend(usuarioId)
                if (pushResult.isFailure) {
                    Timber.e("❌ PUSH falló, abortando sync completo")
                    return@withContext pushResult.mapCatching { "" }
                }

                // 2. PULL después
                val pullResult = pullDataFromBackend(usuarioId)
                if (pullResult.isFailure) {
                    Timber.e("❌ PULL falló, pero PUSH fue exitoso")
                    return@withContext pullResult.mapCatching { "" }
                }

                // 3. Actualizar usuario en Room con los datos del backend
                val pullData = pullResult.getOrNull()
                if (pullData != null) {
                    // Pasar tanto el usuario como el estadoCuenta
                    actualizarUsuarioEnRoom(pullData.usuario, pullData.estadoCuenta)
                    Timber.d("✅ Usuario actualizado en Room con datos del backend")
                }

                Timber.d("✅ Sincronización completa exitosa")
                Result.success("Sync completed successfully")
            } catch (e: Exception) {
                Timber.e(e, "❌ Error durante sincronización completa")
                Result.failure(e)
            }
        }
    }

    /**
     * Obtener estado actual de sincronización
     */
    suspend fun getSyncStatus(usuarioId: Int): Result<SyncStatus> {
        return withContext(Dispatchers.IO) {
            try {
                val unsyncedRegistros = registroSaludDao.getUnsyncedCount(usuarioId)
                val unsyncedEjercicios = ejercicioRealizadoDao.getUnsyncedCount(usuarioId)
                val lastSync = sharedPreferences.getString("last_sync", null)

                val status = SyncStatus(
                    sincronizado = unsyncedRegistros == 0 && unsyncedEjercicios == 0,
                    ultimaSincronizacion = lastSync,
                    datosLocales = mapOf(
                        "registrosSalud" to unsyncedRegistros,
                        "ejerciciosRealizados" to unsyncedEjercicios
                    ),
                    datosRemote = emptyMap()
                )

                Result.success(status)
            } catch (e: Exception) {
                Timber.e(e, "❌ Error obteniendo estado de sync")
                Result.failure(e)
            }
        }
    }

    /**
     * Registrar token FCM en el backend
     */
    suspend fun registerFCMToken(token: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.d("🔔 Registrando token FCM en backend: $token")

                val response = apiService.registerFCMToken(
                    FCMTokenRequest(token)
                )

                if (response.isSuccessful) {
                    sharedPreferences.edit().putString("fcm_token", token).apply()
                    Timber.d("✅ Token FCM registrado exitosamente")
                    Result.success(Unit)
                } else {
                    Timber.e("❌ Error registrando token: ${response.message()}")
                    Result.failure(Exception("Error registrando token FCM"))
                }
            } catch (e: Exception) {
                Timber.e(e, "❌ Excepción registrando token")
                Result.failure(e)
            }
        }
    }

    /**
     * Obtener datos pendientes de sincronización
     */
    suspend fun getPendingSyncData(usuarioId: Int): Result<Map<String, Int>> {
        return withContext(Dispatchers.IO) {
            try {
                val registros = registroSaludDao.getUnsyncedCount(usuarioId)
                val ejercicios = ejercicioRealizadoDao.getUnsyncedCount(usuarioId)

                Result.success(
                    mapOf(
                        "registrosSalud" to registros,
                        "ejerciciosRealizados" to ejercicios,
                        "total" to (registros + ejercicios)
                    )
                )
            } catch (e: Exception) {
                Timber.e(e, "❌ Error obteniendo datos pendientes")
                Result.failure(e)
            }
        }
    }

    /**
     * Limpiar estado de sincronización (e.g., en logout)
     */
    suspend fun clearSyncState() {
        withContext(Dispatchers.IO) {
            try {
                sharedPreferences.edit()
                    .remove("last_sync")
                    .remove("last_successful_sync")
                    .remove("account_inactive")
                    .remove("fcm_token")
                    .apply()

                Timber.d("✅ Estado de sync limpiado")
            } catch (e: Exception) {
                Timber.e(e, "❌ Error limpiando estado de sync")
            }
        }
    }

    /**
     * Actualizar usuario en Room con datos del backend (PULL)
     * Esto asegura que el apellido y otros datos estén siempre sincronizados
     */
    private suspend fun actualizarUsuarioEnRoom(usuarioPull: UsuarioPullDTO, estadoCuenta: EstadoCuentaDTO) {
        withContext(Dispatchers.IO) {
            try {
                // Obtener el usuario actual de Room para conservar campos que no vienen del backend
                val usuarioActual = usuarioDao.getUsuarioSuspend()

                if (usuarioActual != null) {
                    // Determinar el estadoCuenta basado en el DTO
                    val estadoCuentaStr = when {
                        estadoCuenta.activo -> "ACTIVE"
                        else -> "PENDING"
                    }
                    
                    // Actualizar solo los campos que vienen del backend
                    val usuarioActualizado = usuarioActual.copy(
                        nombre = usuarioPull.nombre,
                        apellido = usuarioPull.apellido ?: usuarioActual.apellido,
                        estadoCuenta = estadoCuentaStr
                    )

                    usuarioDao.update(usuarioActualizado)
                    Timber.d("✅ Usuario actualizado: ${usuarioPull.nombre} ${usuarioPull.apellido} (Estado: $estadoCuentaStr)")
                } else {
                    Timber.w("⚠️ No hay usuario en Room para actualizar")
                }
            } catch (e: Exception) {
                Timber.e(e, "❌ Error actualizando usuario en Room")
            }
        }
    }

    /**
     * Crear usuario en el backend si no existe (para registros offline)
     * Esto permite que usuarios registrados sin internet se sincronicen cuando hay conexión
     */
    private suspend fun crearUsuarioEnBackendSiNoExiste(usuarioEntity: UsuarioEntity) {
        withContext(Dispatchers.IO) {
            try {
                Timber.d("🔍 Verificando si usuario ${usuarioEntity.nombre} existe en backend...")

                // Verificar si ya intentamos crear este usuario en el backend
                val usuarioCreado = sharedPreferences.getBoolean("usuario_creado_en_backend_${usuarioEntity.nombre}", false)
                val usuarioBackendId = sharedPreferences.getInt("usuario_backend_id_${usuarioEntity.nombre}", -1)

                if (usuarioCreado && usuarioBackendId != -1) {
                    // Ya se intentó crear, solo actualizar el usuarioId local si es diferente
                    Timber.d("✅ Usuario ya fue creado en backend con ID: $usuarioBackendId, actualizando localmente...")
                    if (usuarioBackendId != usuarioEntity.id) {
                        val oldUsuarioId = usuarioEntity.id
                        
                        // Room no permite actualizar PRIMARY KEY con update(), hay que eliminar e insertar
                        usuarioDao.delete(usuarioEntity)
                        val usuarioActualizado = usuarioEntity.copy(id = usuarioBackendId)
                        usuarioDao.insert(usuarioActualizado)
                        Timber.d("📝 Usuario actualizado en Room: $oldUsuarioId → $usuarioBackendId")

                        // Actualizar registros y ejercicios con el nuevo ID
                        val registrosAntiguos = registroSaludDao.getAllUnsyncedRegistros()
                        registrosAntiguos.forEach { registro ->
                            registroSaludDao.update(registro.copy(usuarioId = usuarioBackendId))
                        }
                        Timber.d("📝 ${registrosAntiguos.size} registros actualizados")

                        val ejerciciosAntiguos = ejercicioRealizadoDao.getAllUnsyncedEjercicios()
                        ejerciciosAntiguos.forEach { ejercicio ->
                            ejercicioRealizadoDao.update(ejercicio.copy(usuarioId = usuarioBackendId))
                        }
                        Timber.d("📝 ${ejerciciosAntiguos.size} ejercicios actualizados")

                        Timber.d("✅ Usuario y datos actualizados con ID de backend: $usuarioBackendId")
                    }
                    return@withContext
                }

                // Intentar hacer PULL para verificar si existe en el backend
                val pullResponse = apiService.pullSync(usuarioEntity.id)

                if (pullResponse.code() == 404) {
                    // Usuario no existe en backend, crearlo
                    Timber.w("⚠️ Usuario no encontrado en backend, creando...")

                    // Obtener token FCM
                    val fcmToken = sharedPreferences.getString("fcm_token", null)
                    
                    // Calcular fecha de nacimiento aproximada si tenemos edad
                    val fechaNacimiento = usuarioEntity.edad?.let { edad ->
                        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                        val birthYear = currentYear - edad
                        "$birthYear-01-01"  // Usar 1 de enero como fecha aproximada
                    }

                    val createRequest = CreateUsuarioRequest(
                        nombre = usuarioEntity.nombre,
                        apellido = usuarioEntity.apellido ?: "",
                        cedula = usuarioEntity.cedula,
                        fechaNacimiento = fechaNacimiento,
                        edad = usuarioEntity.edad,
                        peso = usuarioEntity.peso,
                        altura = usuarioEntity.altura,
                        sexo = usuarioEntity.sexo,
                        enfermedad = usuarioEntity.enfermedad,
                        colorEnfermedad = usuarioEntity.colorEnfermedad,
                        tokenDispositivo = fcmToken,
                        estadoCuenta = "ACTIVE"
                    )

                    val createResponse = apiService.createUsuario(createRequest)

                    if (createResponse.isSuccessful && createResponse.body() != null) {
                        val backendUsuarioId = createResponse.body()!!.usuario.id
                        Timber.d("✅ Usuario creado en backend con ID: $backendUsuarioId (antes era local: ${usuarioEntity.id})")

                        // Si el ID cambió, actualizar en Room (usuario, registros y ejercicios)
                        if (backendUsuarioId != usuarioEntity.id) {
                            val oldUsuarioId = usuarioEntity.id

                            // 1. Actualizar usuario (Room no permite update de PRIMARY KEY, hay que delete + insert)
                            usuarioDao.delete(usuarioEntity)
                            val usuarioActualizado = usuarioEntity.copy(id = backendUsuarioId)
                            usuarioDao.insert(usuarioActualizado)
                            Timber.d("📝 Usuario actualizado en Room: $oldUsuarioId → $backendUsuarioId")

                            // Verificar que se actualizó correctamente
                            val usuarioVerificacion = usuarioDao.getUsuarioSuspend()
                            Timber.d("✅ Verificación: usuario en Room ahora es ID=${usuarioVerificacion?.id}")

                            // 2. Actualizar TODOS los registros de salud sin sincronizar (sin filtrar por usuarioId)
                            // Esto es importante porque pueden tener usuarioId=0 o el ID antiguo
                            val registrosAntiguos = registroSaludDao.getAllUnsyncedRegistros()
                            Timber.d("📝 Actualizando ${registrosAntiguos.size} registros de salud...")
                            registrosAntiguos.forEach { registro ->
                                val registroActualizado = registro.copy(usuarioId = backendUsuarioId)
                                registroSaludDao.update(registroActualizado)
                                Timber.d("   - Registro ID=${registro.id}: usuarioId ${registro.usuarioId} → $backendUsuarioId")
                            }
                            if (registrosAntiguos.isNotEmpty()) {
                                Timber.d("✅ ${registrosAntiguos.size} registros de salud actualizados")
                            }

                            // 3. Actualizar TODOS los ejercicios realizados sin sincronizar (sin filtrar por usuarioId)
                            // Esto es importante porque pueden tener usuarioId=0 o el ID antiguo
                            val ejerciciosAntiguos = ejercicioRealizadoDao.getAllUnsyncedEjercicios()
                            Timber.d("📝 Actualizando ${ejerciciosAntiguos.size} ejercicios...")
                            ejerciciosAntiguos.forEach { ejercicio ->
                                val ejercicioActualizado = ejercicio.copy(usuarioId = backendUsuarioId)
                                ejercicioRealizadoDao.update(ejercicioActualizado)
                                Timber.d("   - Ejercicio ID=${ejercicio.id}: usuarioId ${ejercicio.usuarioId} → $backendUsuarioId")
                            }
                            if (ejerciciosAntiguos.isNotEmpty()) {
                                Timber.d("✅ ${ejerciciosAntiguos.size} ejercicios actualizados")
                            }

                            // Guardar en SharedPreferences que el usuario fue creado exitosamente en backend
                            sharedPreferences.edit()
                                .putBoolean("usuario_creado_en_backend_${usuarioEntity.nombre}", true)
                                .putInt("usuario_backend_id_${usuarioEntity.nombre}", backendUsuarioId)
                                .apply()
                            Timber.d("💾 Flag guardado: usuario_creado_en_backend_${usuarioEntity.nombre}=true, ID=$backendUsuarioId")
                        }
                    } else {
                        Timber.e("❌ Error creando usuario en backend: ${createResponse.code()}")
                    }
                } else if (pullResponse.isSuccessful) {
                    // Usuario ya existe en backend
                    Timber.d("✅ Usuario ya existe en backend con ID: ${usuarioEntity.id}")
                } else {
                    Timber.w("⚠️ Error verificando usuario en backend: ${pullResponse.code()}")
                }
            } catch (e: Exception) {
                Timber.e(e, "❌ Error verificando/creando usuario en backend")
                // No lanzar excepción, permitir que continúe el PUSH
            }
        }
    }
}
