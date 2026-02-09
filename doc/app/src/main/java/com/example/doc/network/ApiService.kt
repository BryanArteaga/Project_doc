package com.example.doc.network

import com.example.doc.repository.SyncPushResponse
import com.example.doc.repository.SyncPullResponse
import com.example.doc.repository.SyncStatus
import retrofit2.Response
import retrofit2.http.*

/**
 * DTOs para PUSH/PULL
 */
data class SyncPushRequest(
    val usuarioId: Int,  // ID del usuario (sin JWT, se envía en el body)
    val registrosSalud: List<PushRegistroSaludRequest>,
    val ejerciciosRealizados: List<PushEjercicioRealizadoRequest>,
    val ultimaSincronizacion: String?
)

data class PushRegistroSaludRequest(
    val presionSistolica: Int?,
    val presionDiastolica: Int?,
    val glucosa: Float?,
    val colesterol: Float?,
    val calorias: Int?,
    val ejercicio: Int?,
    val estadoAnimo: String?,
    val fecha: String?,
    val hora: String?
)

data class PushEjercicioRealizadoRequest(
    val ejercicioId: Int,
    val asignacionId: Int?,
    val fecha: String?,
    val completadoEn: String
)

data class FCMTokenRequest(
    val token: String
)

/**
 * DTOs para creación de usuario
 */
data class CreateUsuarioRequest(
    val nombre: String,
    val apellido: String,
    val estadoCuenta: String = "ACTIVE"
)

data class CreateUsuarioResponse(
    val message: String,
    val usuario: UsuarioBackendData
)

data class UsuarioBackendData(
    val id: Int,
    val nombre: String,
    val apellido: String,
    val estadoCuenta: String
)

/**
 * API Service - Retrofit interface para comunicación con el backend
 */
interface ApiService {

    // ============================================
    // � Usuarios
    // ============================================

    /**
     * POST /api/usuarios/create
     * Crear nuevo usuario en el backend
     * Se usa cuando el paciente se registra en la app
     */
    @POST("usuarios/create")
    suspend fun createUsuario(
        @Body request: CreateUsuarioRequest
    ): Response<CreateUsuarioResponse>

    // ============================================
    // �📤 PUSH: Enviar datos a Supabase
    // ============================================
    
    /**
     * POST /api/sync/push
     * Enviar datos de salud y ejercicios al backend
     */
    @POST("sync/push")
    suspend fun pushSync(
        @Body request: SyncPushRequest
    ): Response<SyncPushResponse>

    // ============================================
    // 📥 PULL: Obtener datos de Supabase
    // ============================================

    /**
     * GET /api/sync/pull?usuarioId={id}
     * Obtener ejercicios asignados y notificaciones
     * 
     * NOTA: Sin autenticación JWT. El usuarioId viene en query params.
     */
    @GET("sync/pull")
    suspend fun pullSync(
        @Query("usuarioId") usuarioId: Int
    ): Response<SyncPullResponse>

    // ============================================
    // 🔔 Notificaciones y Tokens
    // ============================================

    /**
     * POST /api/sync/register-fcm-token
     * Registrar token de Firebase Cloud Messaging
     */
    @POST("sync/register-fcm-token")
    suspend fun registerFCMToken(
        @Body request: FCMTokenRequest
    ): Response<Unit>

    /**
     * GET /api/sync/status
     * Obtener estado actual de sincronización
     */
    @GET("sync/status")
    suspend fun getSyncStatus(): Response<SyncStatus>
}
