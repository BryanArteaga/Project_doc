package com.example.doc.di

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.doc.worker.SyncWorker
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
@Suppress("unused")
object WorkerModule {

    /**
     * Configura y programa el sync worker para ejecutarse periódicamente
     */
    @Provides
    @Singleton
    fun provideSyncWorkerManager(
        @ApplicationContext context: Context
    ): WorkManager {
        val workManager = WorkManager.getInstance(context)

        // Constraints: solo cuando hay conexión a internet
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Request periódico: cada 15 minutos (mínimo permitido por Android)
        // Si hay cambios, sincroniza inmediatamente. Si no hay, salta.
        val syncWorkRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            15, TimeUnit.MINUTES
        ).setConstraints(constraints)
            .build()

        // Enqueue el trabajo (evitar duplicados)
        workManager.enqueueUniquePeriodicWork(
            SyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            syncWorkRequest
        )

        return workManager
    }
}
