package com.example.doc.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * Monitor de conectividad de red
 * 
 * Detecta automáticamente cuando el dispositivo se conecta/desconecta de internet
 * y actualiza el estado para que el HybridNotificationManager pueda reaccionar.
 */
object NetworkMonitor {
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    /**
     * Inicializar el monitor de red con callback automático
     */
    fun init(context: Context) {
        connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
            as ConnectivityManager

        // Obtener estado inicial
        updateConnectivity(context)

        // Registrar callback para cambios de red (Android N+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    Timber.d("📡 Red disponible")
                    _isConnected.value = true
                }

                override fun onLost(network: Network) {
                    Timber.d("📡 Red perdida")
                    _isConnected.value = false
                }

                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities
                ) {
                    val hasInternet = networkCapabilities.hasCapability(
                        NetworkCapabilities.NET_CAPABILITY_INTERNET
                    ) && networkCapabilities.hasCapability(
                        NetworkCapabilities.NET_CAPABILITY_VALIDATED
                    )
                    
                    if (hasInternet != _isConnected.value) {
                        _isConnected.value = hasInternet
                        Timber.d("📡 Capacidades de red cambiaron: internet=${hasInternet}")
                    }
                }
            }

            val networkRequest = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                .build()

            connectivityManager?.registerNetworkCallback(networkRequest, networkCallback!!)
            Timber.d("✅ NetworkMonitor inicializado con callback automático")
        } else {
            Timber.d("✅ NetworkMonitor inicializado (sin callback, API < 24)")
        }
    }

    /**
     * Actualizar estado de conectividad manualmente
     */
    fun updateConnectivity(context: Context) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
            as ConnectivityManager

        val hasInternet = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            @Suppress("DEPRECATION")
            networkInfo?.isConnected == true
        }

        if (hasInternet != _isConnected.value) {
            _isConnected.value = hasInternet
            Timber.d("📡 Estado de internet actualizado: $hasInternet")
        }
    }

    /**
     * Obtener estado actual de forma síncrona
     */
    fun isCurrentlyConnected(): Boolean = _isConnected.value
}


