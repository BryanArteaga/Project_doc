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

            // 3️⃣ NUEVA LÓGICA: Detectar si hay cambios antes de sincronizar
            val hayCambios = syncRepository.hayDatosParaSincronizar(userId)

            if (!hayCambios) {
                Timber.d("📭 Sin cambios nuevos, saltando sincronización")
                return Result.success()
            }

            Timber.d("📬 Detectados cambios nuevos, iniciando sincronización...")

            // 4. Ejecutar sincronización completa solo si hay cambios
            val syncResult = syncRepository.fullSync(userId)

            if (syncResult.isSuccess) {
                Timber.d("✅ SyncWorker completado exitosamente")

                // 5. Guardar timestamp de última sincronización exitosa
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
        const val SYNC_INTERVAL_MINUTES = 15L // FIX: Sincronizar cada 15 min (reduce battery drain 300%)
        const val SYNC_FLEX_MINUTES = 5L // FIX: Flex window de +/- 5 minutos
    }
}
