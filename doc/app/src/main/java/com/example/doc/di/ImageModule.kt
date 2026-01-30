package com.example.doc.di

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.util.DebugLogger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ImageModule {

    @Provides
    @Singleton
    fun provideImageLoader(
        @ApplicationContext context: Context
    ): ImageLoader {
        return ImageLoader.Builder(context)
            // Configuración de caché en memoria
            .memoryCache {
                MemoryCache.Builder(context)
                    // Usar máximo 20% de la RAM disponible para imágenes
                    .maxSizePercent(0.20)
                    // Permitir weak references para liberación rápida
                    .weakReferencesEnabled(true)
                    .build()
            }
            // Configuración de caché en disco
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    // Límite de 50 MB en disco
                    .maxSizeBytes(50 * 1024 * 1024)
                    .build()
            }
            // Políticas de caché
            .respectCacheHeaders(false)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            // Crossfade para transiciones suaves
            .crossfade(true)
            .crossfade(200)
            // Logger para debug (solo en development)
            .apply {
                if (com.example.doc.BuildConfig.DEBUG) {
                    logger(DebugLogger())
                }
            }
            .build()
    }
}

