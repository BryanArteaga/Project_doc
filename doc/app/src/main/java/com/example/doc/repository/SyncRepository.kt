package com.example.doc.repository

import android.content.SharedPreferences
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.doc.data.daos.EjercicioRealizadoDao
import com.example.doc.data.daos.RegistroSaludDao
import com.example.doc.data.daos.UsuarioDao
import com.example.doc.data.daos.EjercicioDao
import com.example.doc.data.entities.UsuarioEntity
import com.example.doc.network.ApiService
import com.example.doc.network.SyncPushRequest
import com.example.doc.network.PushRegistroSaludRequest
import com.example.doc.network.PushEjercicioRealizadoRequest
import com.example.doc.network.FCMTokenRequest
import com.example.doc.network.CreateUsuarioRequest
import com.example.doc.network.AsignacionEjercicioDTO
import com.example.doc.data.daos.AsignacionEjercicioDao
import timber.log.Timber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

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
    val estadoCuenta: EstadoCuentaDTO,
    val registrosSalud: List<RegistroSaludDTO> = emptyList(),  // ✅ NUEVO: Registros históricos del backend
    val asignaciones: List<AsignacionEjercicioDTO> = emptyList()  // ✅ NUEVO: Asignaciones de ejercicios
)

data class RegistroSaludDTO(
    val id: Int,
    val fecha: String,
    val hora: String? = null,
    val presionSistolica: Int? = null,
    val presionDiastolica: Int? = null,
    val glucosa: Int? = null,
    val colesterol: Int? = null,
    val temperatura: Double? = null,
    val peso: Double? = null,
    val calorias: Int? = null,
    val ejercicioMinutos: Int? = null,  // ✅ Cambiar de ejercicio (String) a ejercicioMinutos (Int)
    val estadoAnimo: String? = null,
    val notas: String? = null,
    val registradoEn: Long
)

data class UsuarioPullDTO(
    val id: Int,
    val nombre: String,
    val apellido: String?,
    val email: String?,
    val telefono: String?,
    val estadoCuenta: String? = null,
    // 🔧 Datos personales que ahora vienen del backend
    val edad: Int? = null,
    val peso: Double? = null,
    val altura: Double? = null,
    val sexo: String? = null,
    val enfermedad: String? = null,
    val colorEnfermedad: String? = null,
    // ✅ NUEVO: Configuración de notificaciones (no mostrar pop-up si existe config)
    val notifEnabled: Boolean = true,
    val notifType: String = "daily_fixed",
    val notifDetails: String = "{\"hour\":20}"
)

data class EjercicioDTO(
    val id: Int,
    val nombre: String,
    val descripcion: String?,
    val duracion: Int?,
    val intensidad: String?,
    val enfermedad: String?,  // ✅ Campo agregado para sincronización
    val series: Int?,
    val descansoEntreSeries: Int?,
    val urlImagen: String? = null,
    val urlVideo: String? = null
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
    private val asignacionEjercicioDao: AsignacionEjercicioDao,
    private val usuarioDao: UsuarioDao,
    private val ejercicioDao: EjercicioDao,
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
    @RequiresApi(Build.VERSION_CODES.O)
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
                    val todayString = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time)
                    val totalEjercicios = ejercicioRealizadoDao.getEjerciciosRealizadosHoy(usuarioIdActual, todayString)
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
                            presionSistolica = rs.presionSistolica?.toFloat(),
                            presionDiastolica = rs.presionDiastolica?.toFloat(),
                            glucosa = rs.glucosa?.toFloat(),
                            colesterol = rs.colesterol?.toFloat(),
                            temperatura = rs.temperatura?.toFloat(),
                            peso = rs.peso?.toFloat(),
                            calorias = rs.calorias,
                            ejercicioMinutos = rs.ejercicioMinutos,  // ✅ Cambiar a ejercicioMinutos (Int)
                            estadoAnimo = rs.estadoAnimo,
                            notas = rs.notas,
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
                    Timber.d("✅ PULL exitoso: ${body.ejercicios.size} ejercicios, ${body.notificaciones.size} notificaciones, ${body.registrosSalud.size} registros históricos")

                    // ✅ NUEVO: Guardar registros de salud históricos en Room
                    if (body.registrosSalud.isNotEmpty()) {
                        try {
                            body.registrosSalud.forEach { registroDto ->
                                val entity = com.example.doc.data.entities.RegistroSaludEntity(
                                    id = registroDto.id,
                                    usuarioId = usuarioId,
                                    fecha = registroDto.fecha,
                                    hora = registroDto.hora,
                                    presionSistolica = registroDto.presionSistolica,
                                    presionDiastolica = registroDto.presionDiastolica,
                                    glucosa = registroDto.glucosa,
                                    colesterol = registroDto.colesterol,
                                    temperatura = registroDto.temperatura,
                                    peso = registroDto.peso,
                                    calorias = registroDto.calorias,
                                    ejercicioMinutos = registroDto.ejercicioMinutos ?: 0,  // ✅ Ya viene como Int
                                    estadoAnimo = registroDto.estadoAnimo,
                                    notas = registroDto.notas,
                                    registradoEn = registroDto.registradoEn,
                                    isSynced = true  // Ya están en el backend
                                )
                                // ✅ USAR upsertRegistroSalud para evitar duplicados por fecha+hora
                                registroSaludDao.upsertRegistroSalud(entity)
                            }
                            Timber.d("✅ ${body.registrosSalud.size} registros históricos guardados en Room (sin duplicados)")
                        } catch (e: Exception) {
                            Timber.e(e, "❌ Error guardando registros históricos en Room")
                        }
                    }

                    // ✅ NUEVO: Guardar asignaciones de ejercicios
                    if (body.asignaciones.isNotEmpty()) {
                        try {
                            // Obtener enfermedad del usuario para filtro
                            val usuarioLocal = usuarioDao.getUsuarioSuspend()
                            val enfermedad = usuarioLocal?.enfermedad
                            
                            // Borrar asignaciones anteriores para actualizar
                            asignacionEjercicioDao.deleteForSyncDirectas(usuarioId)
                            if (enfermedad != null) {
                                asignacionEjercicioDao.deleteForSyncGenerales(enfermedad)
                            }
                            
                            // Guardar nuevas asignaciones
                            val ahora = System.currentTimeMillis()
                            val asignacionesEntities = body.asignaciones.map { dto ->
                                com.example.doc.data.entities.AsignacionEjercicioEntity(
                                    id = dto.id,
                                    ejercicioId = dto.ejercicioId,
                                    doctorId = dto.doctorId,
                                    usuarioId = dto.usuarioId,
                                    enfermedad = dto.enfermedad,
                                    descripcion = dto.descripcion,
                                    activo = dto.activo,
                                    fechaAsignacion = dto.fechaAsignacion.toLongOrNull() ?: ahora,
                                    fechaExpiracion = dto.fechaExpiracion?.toLongOrNull(),
                                    isSynced = true  // Ya están en el backend
                                )
                            }
                            asignacionEjercicioDao.insertAll(asignacionesEntities)
                            Timber.d("✅ ${body.asignaciones.size} asignaciones de ejercicios guardadas en Room")
                        } catch (e: Exception) {
                            Timber.e(e, "❌ Error guardando asignaciones en Room")
                        }
                    }

                    // ✅ NUEVO: Guardar ejercicios en la base de datos local
                    if (body.ejercicios.isNotEmpty()) {
                        try {
                            // 🔧 Obtener ejercicios existentes para preservar imagenResId
                            val idsToSync = body.ejercicios.map { it.id }
                            val ejerciciosExistentes = try {
                                ejercicioDao.getAll()
                                    .filter { it.id in idsToSync }
                                    .associateBy { it.id }
                            } catch (e: Exception) {
                                emptyMap()
                            }

                            val ejerciciosEntities = body.ejercicios.map { dto ->
                                val existente = ejerciciosExistentes[dto.id]
                                
                                // 🎨 Para ejercicios sin URL del backend, usar drawable local correcto
                                val imagenResIdFinal = when {
                                    // Si tiene URL del backend, no necesita drawable local
                                    !dto.urlImagen.isNullOrEmpty() -> 0
                                    // Preservar existente si ya está en DB
                                    existente != null -> existente.imagenResId
                                    // Fallback: intentar inferir del nombre+enfermedad (temporal hasta corrección)
                                    else -> getImagenResIdPorDefecto(dto.nombre, dto.enfermedad ?: "")
                                }
                                
                                // 🔍 DEBUG: Log detallado para cada ejercicio
                                val urlLength = dto.urlImagen?.length ?: 0
                                val urlPreview = dto.urlImagen?.take(100) ?: "null"
                                Timber.d("🏃 EJERCICIO #${dto.id}: '${dto.nombre}' | enfermedad='${dto.enfermedad}' | urlImagen_length=$urlLength | urlImagen_start=$urlPreview | imagenResId_local=${existente?.imagenResId ?: "NUEVO ($imagenResIdFinal)"}")
                                
                                com.example.doc.data.entities.EjercicioEntity(
                                    id = dto.id,
                                    nombre = dto.nombre,
                                    descripcion = dto.descripcion ?: "",
                                    duracion = dto.duracion ?: 0,
                                    intensidad = dto.intensidad ?: "Media",
                                    enfermedad = dto.enfermedad ?: "General",
                                    imagenResId = imagenResIdFinal,
                                    series = dto.series ?: existente?.series ?: 3,  // Backend o local o default
                                    descansoEntreSeries = dto.descansoEntreSeries ?: existente?.descansoEntreSeries ?: 30,
                                    urlImagen = dto.urlImagen ?: existente?.urlImagen,  // Preservar si backend no envía
                                    urlVideo = dto.urlVideo ?: existente?.urlVideo
                                )
                            }
                            ejercicioDao.upsertAll(ejerciciosEntities)
                            Timber.d("✅ ${body.ejercicios.size} ejercicios guardados en Room")
                        } catch (e: Exception) {
                            Timber.e(e, "❌ Error guardando ejercicios en Room")
                        }
                    }

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
     * Sincronización: PUSH siempre + PULL solo en primer arranque/errores críticos
     * @param usuarioId ID del usuario
     * @param forcePull Si true, ejecutar PULL aunque ya se haya hecho (para admin manual)
     */
    suspend fun fullSync(usuarioId: Int, forcePull: Boolean = false): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.d("🔄 Iniciando sincronización para usuario: $usuarioId (forcePull: $forcePull)")

                // 1. PUSH siempre
                val pushResult = pushDataToBackend(usuarioId)
                if (pushResult.isFailure) {
                    Timber.e("❌ PUSH falló, abortando sync")
                    return@withContext pushResult.mapCatching { "" }
                }
                Timber.d("✅ PUSH completado exitosamente")

                // 2. Determinar si hacer PULL
                val firstPullDone = sharedPreferences.getBoolean("first_pull_done_$usuarioId", false)
                val shouldPull = !firstPullDone || forcePull

                if (shouldPull) {
                    Timber.d("📥 Ejecutando PULL${if (forcePull) " (FORZADO POR ADMIN)" else " (PRIMER ARRANQUE)"}")
                    
                    val pullResult = pullDataFromBackend(usuarioId)
                    if (pullResult.isFailure) {
                        Timber.e("❌ PULL falló, pero PUSH fue exitoso")
                        // No abortar si PULL falla - el PUSH es lo importante
                    } else {
                        // 3. Actualizar usuario en Room con los datos del backend
                        val pullData = pullResult.getOrNull()
                        if (pullData != null) {
                            actualizarUsuarioEnRoom(pullData.usuario, pullData.estadoCuenta)
                            Timber.d("✅ Usuario actualizado con datos del backend")
                        }
                        
                        // Marcar que ya hizo el primer PULL (solo si no fue forzado)
                        if (!forcePull) {
                            sharedPreferences.edit()
                                .putBoolean("first_pull_done_$usuarioId", true)
                                .apply()
                            Timber.d("✅ Primer PULL completado. Futuras syncs solo harán PUSH")
                        }
                    }
                } else {
                    Timber.d("📤 PULL omitido - Ya completado en arranque anterior. Solo PUSH ejecutado")
                }

                Timber.d("✅ Sincronización completada")
                Result.success("Sync completed successfully")
            } catch (e: Exception) {
                Timber.e(e, "❌ Error durante sincronización")
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
                    
                    // 🔧 Actualizar TODOS los campos que vienen del backend (incluyendo datos personales)
                    val usuarioActualizado = usuarioActual.copy(
                        id = usuarioPull.id,  // Actualizar ID si cambió
                        nombre = usuarioPull.nombre,
                        apellido = usuarioPull.apellido ?: usuarioActual.apellido,
                        edad = usuarioPull.edad ?: usuarioActual.edad,  // Usar backend si viene, sino mantener local
                        peso = usuarioPull.peso ?: usuarioActual.peso,
                        altura = usuarioPull.altura ?: usuarioActual.altura,
                        sexo = usuarioPull.sexo ?: usuarioActual.sexo,
                        enfermedad = usuarioPull.enfermedad ?: usuarioActual.enfermedad,  // IMPORTANTE: sincronizar enfermedad
                        colorEnfermedad = usuarioPull.colorEnfermedad ?: usuarioActual.colorEnfermedad,
                        // ✅ NUEVO: Actualizar configuración de notificaciones desde backend
                        notifEnabled = usuarioPull.notifEnabled,
                        notifType = usuarioPull.notifType,
                        notifDetails = usuarioPull.notifDetails,
                        estadoCuenta = estadoCuentaStr
                    )

                    // Borrar usuario con ID viejo si cambió
                    if (usuarioActual.id != usuarioPull.id) {
                        Timber.d("📝 Actualizando ID de usuario en Room: ${usuarioActual.id} → ${usuarioPull.id}")
                        usuarioDao.delete(usuarioActual)
                        usuarioDao.insert(usuarioActualizado)
                    } else {
                        usuarioDao.update(usuarioActualizado)
                    }
                    
                    Timber.d("✅ Usuario actualizado: ${usuarioPull.nombre} ${usuarioPull.apellido}")
                    Timber.d("   📋 Edad: ${usuarioActualizado.edad}, Peso: ${usuarioActualizado.peso}kg, Altura: ${usuarioActualizado.altura}m")
                    Timber.d("   🏥 Enfermedad: ${usuarioActualizado.enfermedad}, Color: ${usuarioActualizado.colorEnfermedad}")
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

    /**
     * Actualizar notificaciones en el backend
     * Se llama cuando el usuario cambia configuración de notificaciones desde Perfil
     */
    suspend fun actualizarNotificacionesEnBackend(
        usuarioId: Int,
        notifEnabled: Boolean,
        notifType: String,
        notifDetails: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            val requestBody = com.example.doc.network.UpdateProfileRequest(
                usuarioId = usuarioId,
                notifEnabled = notifEnabled,
                notifType = notifType,
                notifDetails = notifDetails
            )

            val response = apiService.updateProfile(requestBody)

            if (response.isSuccessful && response.body() != null) {
                Timber.d("✅ Notificaciones actualizadas en backend para usuario $usuarioId")
                response.body()?.toString()
            } else {
                Timber.e("❌ Error actualizando notificaciones: ${response.code()}")
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ Error al actualizar notificaciones en backend")
            null
        }
    }

    /**
     * 🔍 BÚSQUEDA Y SINCRONIZACIÓN OFFLINE-FIRST
     * Cuando app se reinstala o se abre con internet:
     * 1. Busca usuario por cédula en backend
     * 2. Si existe: actualiza Room y auto-sincroniza
     * 3. Si no existe: permite que continúe el registro normal
     */
    suspend fun searchAndSyncByCedula(cedula: String): Result<Int?> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.d("🔍 Buscando usuario por cédula en backend: $cedula")

                val response = apiService.searchUserByCedula(cedula)

                if (!response.isSuccessful) {
                    Timber.w("⚠️ Usuario no encontrado por cédula en backend")
                    return@withContext Result.success(null)  // Usuario no existe, permitir nuevo registro
                }

                val body = response.body()
                if (body?.found == true && body.usuario != null) {
                    Timber.d("✅ Usuario encontrado en backend con ID: ${body.usuario.id}")

                    // Actualizar Room con datos del backend
                    val usuarioActual = usuarioDao.getUsuarioSuspend()
                    if (usuarioActual != null) {
                        val usuarioActualizado = usuarioActual.copy(
                            id = body.usuario.id  // Sincronizar ID con backend
                        )
                        usuarioDao.updateUsuario(usuarioActualizado)
                        Timber.d("💾 Room actualizado con usuario de backend ID=${body.usuario.id}")

                        // Ejecutar sync automático
                        Timber.d("🔄 Ejecutando sincronización automática para usuario recuperado...")
                        val syncResult = fullSync(body.usuario.id)

                        if (syncResult.isSuccess) {
                            Timber.d("✅ Sincronización automática exitosa para usuario recuperado")
                        } else {
                            Timber.w("⚠️ Sincronización automática falló: ${syncResult.exceptionOrNull()?.message}")
                        }
                    }

                    return@withContext Result.success(body.usuario.id)
                } else {
                    Timber.w("⚠️ Usuario no encontrado o respuesta vacía")
                    return@withContext Result.success(null)
                }
            } catch (e: Exception) {
                Timber.e(e, "❌ Error buscando usuario por cédula")
                Result.failure(e)
            }
        }
    }
    
    /**
     * Obtiene el imagenResId por defecto basado en nombre y enfermedad del ejercicio
     * Usa el mismo mapeo que AppDatabaseCallback.corregirImagenesLocales()
     */
    private fun getImagenResIdPorDefecto(nombre: String, enfermedad: String): Int {
        val mapeo: Map<Pair<String, String>, Int> = mapOf(
            // Hipertensión
            Pair("Caminata rápida", "Hipertensión") to com.example.doc.R.drawable.ejercicio_caminar,
            Pair("Sentadillas", "Hipertensión") to com.example.doc.R.drawable.ejercicio_sentadilla,
            Pair("Subir escaleras", "Hipertensión") to com.example.doc.R.drawable.ejercicio_escalera,
            Pair("Marcha en el lugar", "Hipertensión") to com.example.doc.R.drawable.ejercicio_marcha,
            Pair("Estiramientos suaves", "Hipertensión") to com.example.doc.R.drawable.ejercicio_estiramiento,
            Pair("Elevaciones de piernas", "Hipertensión") to com.example.doc.R.drawable.ejercicio_piernas,
            Pair("Flexiones de pared", "Hipertensión") to com.example.doc.R.drawable.ejercicio_flexiones,
            Pair("Respiración diafragmática", "Hipertensión") to com.example.doc.R.drawable.ejercicio_respiracion,
            Pair("Puentes de glúteo", "Hipertensión") to com.example.doc.R.drawable.ejercicio_gluteos,
            Pair("Plancha con apoyo de rodilla", "Hipertensión") to com.example.doc.R.drawable.ejercicio_plancha,
            Pair("Estocadas estáticas", "Hipertensión") to com.example.doc.R.drawable.ejercicio_estocadas,
            Pair("Rotación de brazos", "Hipertensión") to com.example.doc.R.drawable.ejercicio_brazos,
            Pair("Elevación de talones", "Hipertensión") to com.example.doc.R.drawable.ejercicio_talones,
            Pair("Caminata lenta", "Hipertensión") to com.example.doc.R.drawable.ejercicio_caminar,
            Pair("Estiramiento de espalda", "Hipertensión") to com.example.doc.R.drawable.ejercicio_espalda,
            Pair("Rotaciones de cuello", "Hipertensión") to com.example.doc.R.drawable.ejercicio_cuello,
            Pair("Elevaciones laterales", "Hipertensión") to com.example.doc.R.drawable.ejercicio_brazoslat,
            Pair("Puntas de pies", "Hipertensión") to com.example.doc.R.drawable.ejercicio_puntas,
            Pair("Estiramiento de hombros", "Hipertensión") to com.example.doc.R.drawable.ejercicio_hombros,
            Pair("Movilidad de muñecas", "Hipertensión") to com.example.doc.R.drawable.ejercicio_munecas
            // Nota: Solo incluimos algunos ejemplos. AppDatabaseCallback.corregirImagenesLocales tiene el mapeo completo
        )
        return mapeo[Pair(nombre, enfermedad)] ?: 0
    }
}
