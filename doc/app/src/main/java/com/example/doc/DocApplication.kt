package com.example.doc

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import coil.Coil
import coil.ImageLoader
import com.example.doc.utils.HybridNotificationManager
import com.example.doc.utils.NetworkMonitor
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

        // Inicializar NetworkMonitor
        NetworkMonitor.init(this)

        // Inicializar HybridNotificationManager
        val hybridManager = HybridNotificationManager(
            this,
            ProcessLifecycleOwner.get().lifecycleScope
        )
        hybridManager.init()

        Timber.d("DocApplication iniciada con sistema hibrido de notificaciones")
    }

    /**
     * Arbol de logging para produccion que no imprime logs sensibles
     */
    private class ReleaseTree : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            if (priority == android.util.Log.ERROR || priority == android.util.Log.WARN) {
                // Aqui se pueden enviar logs a servicios de analytics
                // Ejemplo: FirebaseCrashlytics.getInstance().log(message)
                // Por ahora, solo filtramos para no saturar logs en produccion
            }
        }
    }
}

