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
                        sendTokenToServer(token, context)
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
    private suspend fun sendTokenToServer(token: String, context: Context) {
        try {
            // Obtener userId de SharedPreferences (si existe sesión)
            val prefs = context.getSharedPreferences("doc_prefs", Context.MODE_PRIVATE)
            val userId = prefs.getInt("user_id", -1).let { if (it == -1) null else it }
            
            // Si no hay usuario, el token ya fue registrado al crear el usuario (tokenDispositivo)
            if (userId == null) {
                Timber.d("⚠️ Sin userId - Token ya registrado al crear usuario")
                return
            }
            
            // Si hay usuario, enviar para actualizar dispositivo
            val response = apiService.registerFCMToken(FCMTokenRequest(token, userId))
            if (response.isSuccessful) {
                Timber.d("✅ Token FCM actualizado en el servidor")
            } else {
                Timber.w("Error al actualizar token en servidor: ${response.code()}")
            }
        } catch (e: Exception) {
            Timber.e("Excepción al enviar token: ${e.message}")
        }
    }
}
