package com.example.doc.worker

import android.content.Context
import android.content.SharedPreferences
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.doc.repository.SyncRepository
import com.example.doc.utils.NetworkMonitor
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

/**
 * SyncWorker - Sincroniza datos con el backend automáticamente
 * 
 * Se ejecuta periódicamente (cada 6-12 horas) cuando hay conexión
 * También se ejecuta cuando el usuario abre/cierra la app
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val syncRepository: SyncRepository,
    private val prefs: SharedPreferences
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            Timber.d("🔄 SyncWorker iniciado")

            // 1. Verificar si hay internet
            if (!NetworkMonitor.isConnected.value) {
                Timber.w("⚠️ Sin conexión a internet, reintentando más tarde")
                return Result.retry()
            }

            // 2. Obtener ID del usuario actual
            val userId = prefs.getInt("user_id", -1)
            if (userId == -1) {
                Timber.w("⚠️ Usuario no autenticado, cancelando sync")
                return Result.failure()
            }

            // 3. Ejecutar sincronización completa
            val syncResult = syncRepository.fullSync(userId)

            if (syncResult.isSuccess) {
                Timber.d("✅ SyncWorker completado exitosamente")
                
                // 4. Guardar timestamp de última sincronización exitosa
                prefs.edit().putLong("last_successful_sync", System.currentTimeMillis()).apply()
                
                Result.success()
            } else {
                Timber.e("❌ Error en SyncWorker: ${syncResult.exceptionOrNull()?.message}")
                Result.retry()
            }

        } catch (e: Exception) {
            Timber.e(e, "❌ Excepción en SyncWorker")
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "sync_work"
        const val SYNC_INTERVAL_HOURS = 6L // Sincronizar cada 6 horas
    }
}
