package com.example.doc

import android.app.Application
import coil.Coil
import coil.ImageLoader
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class DocApplication : Application() {

    @Inject
    lateinit var imageLoader: ImageLoader

    override fun onCreate() {
        super.onCreate()

        // Inicializar Timber
        // Detectar si está en modo debug verificando el applicationInfo
        if ((applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(ReleaseTree())
        }

        // Configurar Coil con el ImageLoader optimizado
        Coil.setImageLoader(imageLoader)

        Timber.d("DocApplication iniciada con optimizaciones de imágenes")
    }

    /**
     * Árbol de logging para producción que no imprime logs sensibles
     */
    private class ReleaseTree : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            if (priority == android.util.Log.ERROR || priority == android.util.Log.WARN) {
                // Aquí se pueden enviar logs a servicios de analytics
                // Ejemplo: FirebaseCrashlytics.getInstance().log(message)
                // Por ahora, solo filtramos para no saturar logs en producción
            }
        }
    }
}

