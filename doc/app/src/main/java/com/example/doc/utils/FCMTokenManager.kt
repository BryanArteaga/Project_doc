package com.example.doc.utils

import android.content.Context
import com.example.doc.network.ApiService
import com.example.doc.network.FCMTokenRequest
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper para registrar el token FCM en el servidor backend
 */
@Singleton
class FCMTokenManager @Inject constructor(
    private val apiService: ApiService
) {

    /**
     * Obtener y registrar token FCM en el servidor
     */
    fun registerFCMToken(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Obtener el token de Firebase
                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        Timber.w("Error al obtener FCM token: ${task.exception}")
                        return@addOnCompleteListener
                    }

                    // Obtener el token
                    val token = task.result
                    Timber.d("Token FCM obtenido: $token")

                    // Guardar localmente
                    val prefs = context.getSharedPreferences("doc_prefs", Context.MODE_PRIVATE)
                    prefs.edit().putString("fcm_token", token).apply()

                    // Enviar al servidor
                    CoroutineScope(Dispatchers.IO).launch {
                        sendTokenToServer(token)
                    }
                }
            } catch (e: Exception) {
                Timber.e("Error al registrar token FCM: ${e.message}")
            }
        }
    }

    /**
     * Enviar token al servidor backend
     */
    private suspend fun sendTokenToServer(token: String) {
        try {
            val response = apiService.registerFCMToken(FCMTokenRequest(token))
            if (response.isSuccessful) {
                Timber.d("✅ Token FCM registrado en el servidor")
            } else {
                Timber.w("Error al registrar token en servidor: ${response.code()}")
            }
        } catch (e: Exception) {
            Timber.e("Excepción al enviar token: ${e.message}")
        }
    }
}
