package com.example.doc

import android.app.Application
import coil.Coil
import coil.ImageLoader
import com.example.doc.utils.HybridNotificationManager
import com.example.doc.utils.NetworkMonitor
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class DocApplication : Application() {

    @Inject
    lateinit var imageLoader: ImageLoader

    // CoroutineScope para la aplicación
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

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
            appScope
        )
        hybridManager.init()

        Timber.d("DocApplication iniciada con sistema hibrido de notificaciones")
    }

    private class ReleaseTree : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            if (priority == android.util.Log.ERROR || priority == android.util.Log.WARN) {
            }
        }
    }
}

