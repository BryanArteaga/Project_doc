package com.example.doc.repository

import android.content.SharedPreferences
import com.example.doc.data.daos.EjercicioRealizadoDao
import com.example.doc.data.daos.RegistroSaludDao
import com.example.doc.network.ApiService
import com.example.doc.network.SyncPushRequest
import com.example.doc.network.PushRegistroSaludRequest
import com.example.doc.network.PushEjercicioRealizadoRequest
import com.example.doc.network.FCMTokenRequest
import timber.log.Timber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * DTOs para Response
 */
data class SyncPushResponse(
    val procesados: Int,
    val syncId: String,
    val timestamp: String
)

data class SyncPullResponse(
    val ejercicios: List<EjercicioDTO>,
    val notificaciones: List<NotificacionDTO>,
    val estadoCuenta: EstadoCuentaDTO
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
    private val sharedPreferences: SharedPreferences
) {

    /**
     * PUSH: Enviar datos locales al backend
     * - Datos de salud registrados
     * - Ejercicios completados
     */
    suspend fun pushDataToBackend(usuarioId: Int): Result<SyncPushResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.d("📤 Iniciando PUSH de datos al backend para usuario: $usuarioId")

                // 1. Obtener registros de salud sin sincronizar
                val registrosSinSync = registroSaludDao.getUnsyncedRegistros(usuarioId)
                Timber.d("📋 Registros de salud sin sincronizar: ${registrosSinSync.size}")

                // 2. Obtener ejercicios realizados sin sincronizar
                val ejerciciosSinSync = ejercicioRealizadoDao.getUnsyncedEjercicios(usuarioId)
                Timber.d("📋 Ejercicios realizados sin sincronizar: ${ejerciciosSinSync.size}")

                // 3. Construir request (incluir usuarioId en el body)
                val pushRequest = SyncPushRequest(
                    usuarioId = usuarioId,  // Sin JWT, se envía en el body
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
                    Timber.d("✅ PUSH exitoso: ${body.procesados} registros sincronizados, syncId: ${body.syncId}")

                    // 5. Marcar como sincronizados localmente
                    val timestamp = System.currentTimeMillis()
                    registrosSinSync.forEach { rs ->
                        registroSaludDao.markAsSynced(rs.id, body.syncId, timestamp)
                    }

                    ejerciciosSinSync.forEach { er ->
                        ejercicioRealizadoDao.markAsSynced(er.id, body.syncId, timestamp)
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
}
