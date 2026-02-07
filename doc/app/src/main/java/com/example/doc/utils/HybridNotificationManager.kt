package com.example.doc.utils

import android.content.Context
import com.google.firebase.messaging.FirebaseMessaging
import com.example.doc.receiver.NotificationReceiver
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import timber.log.Timber

/**
 * Gestor híbrido de notificaciones
 * 
 * COMPORTAMIENTO:
 * - SIN INTERNET → Notificaciones locales (AlarmManager)
 * - CON INTERNET → Notificaciones push (Firebase FCM)
 * 
 * TIPOS DE NOTIFICACIONES:
 * 1. Recordatorio de salud (diario/semanal/mensual)
 * 2. Recordatorio de ejercicio (10:00 AM y 6:00 PM)
 * 3. Activación de cuenta (SOLO push, cuando doctor activa cuenta)
 */
class HybridNotificationManager(
    private val context: Context,
    private val scope: CoroutineScope
) {

    private val firebaseMessaging = FirebaseMessaging.getInstance()
    private val prefs = context.getSharedPreferences("doc_prefs", Context.MODE_PRIVATE)

    /**
     * Inicializar manager y monitorear cambios de internet
     */
    fun init() {
        Timber.d("🔔 Inicializando HybridNotificationManager")
        
        scope.launch {
            NetworkMonitor.isConnected.collectLatest { isConnected ->
                Timber.d("📡 Estado de internet cambió: ${if (isConnected) "CONECTADO" else "DESCONECTADO"}")
                
                if (isConnected) {
                    onInternetAvailable()
                } else {
                    onInternetLost()
                }
            }
        }
    }

    /**
     * Cuando HAY INTERNET: Activar Firebase, desactivar AlarmManager
     */
    private fun onInternetAvailable() {
        Timber.d("✅ Internet disponible - Activando Firebase, desactivando AlarmManager")

        // Desactivar AlarmManager
        NotificationReceiver.cancelAllAlarms(context)
        Timber.d("⏰ AlarmManager desactivado")

        // Suscribirse a topics de Firebase
        subscribeToTopics()

        Timber.d("🔔 Sistema híbrido: MODO PUSH activo")
    }

    /**
     * Cuando NO HAY INTERNET: Desactivar Firebase, activar AlarmManager
     */
    private fun onInternetLost() {
        Timber.d("❌ Internet perdido - Activando AlarmManager, desactivando Firebase")

        // Desuscribirse de Firebase topics
        unsubscribeFromTopics()
        Timber.d("📡 Firebase desuscrito")

        // Activar AlarmManager
        // 1. Notificaciones de ejercicio (10:00 AM y 6:00 PM)
        NotificationReceiver.scheduleExerciseReminders(context)
        Timber.d("⏰ Alarmas de ejercicio programadas: 10:00 AM y 6:00 PM")

        // 2. Notificaciones de salud (según configuración del usuario)
        val userId = prefs.getInt("user_id", -1)
        if (userId != -1) {
            val notifType = prefs.getString("notif_type_$userId", "daily_fixed") ?: "daily_fixed"
            val notifDetails = prefs.getString("notif_details_$userId", "{\"hour\":20,\"minute\":0}")
                ?: "{\"hour\":20,\"minute\":0}"

            NotificationReceiver.scheduleHealthReminder(context, notifType, notifDetails)
            Timber.d("⏰ Alarmas de salud programadas: type=$notifType")
        } else {
            Timber.w("⚠️ No se encontró user_id, no se pueden programar alarmas de salud")
        }

        Timber.d("🔔 Sistema híbrido: MODO LOCAL activo")
    }

    /**
     * Suscribirse a topics de Firebase
     */
    private fun subscribeToTopics() {
        Timber.d("📡 Suscribiendo a topics de Firebase...")

        // Topic global para recordatorios de ejercicio
        firebaseMessaging.subscribeToTopic("exercise_reminders")
            .addOnSuccessListener {
                Timber.d("✅ Suscrito a: exercise_reminders")
            }
            .addOnFailureListener { e ->
                Timber.e(e, "❌ Error al suscribirse a exercise_reminders")
            }

        // Topic global para recordatorios de salud
        firebaseMessaging.subscribeToTopic("health_reminders")
            .addOnSuccessListener {
                Timber.d("✅ Suscrito a: health_reminders")
            }
            .addOnFailureListener { e ->
                Timber.e(e, "❌ Error al suscribirse a health_reminders")
            }

        // Topic para notificaciones personales (activación de cuenta, mensajes del doctor)
        val userId = prefs.getInt("user_id", -1)
        if (userId != -1) {
            firebaseMessaging.subscribeToTopic("user_$userId")
                .addOnSuccessListener {
                    Timber.d("✅ Suscrito a: user_$userId (notificaciones personales)")
                }
                .addOnFailureListener { e ->
                    Timber.e(e, "❌ Error al suscribirse a user_$userId")
                }
        } else {
            Timber.w("⚠️ No se encontró user_id, no se puede suscribir a notificaciones personales")
        }
    }

    /**
     * Desuscribirse de topics de Firebase
     */
    private fun unsubscribeFromTopics() {
        Timber.d("📡 Desuscribiendo de topics de Firebase...")

        firebaseMessaging.unsubscribeFromTopic("exercise_reminders")
            .addOnSuccessListener { Timber.d("✅ Desuscrito de: exercise_reminders") }
            .addOnFailureListener { e -> Timber.e(e, "❌ Error al desuscribirse de exercise_reminders") }

        firebaseMessaging.unsubscribeFromTopic("health_reminders")
            .addOnSuccessListener { Timber.d("✅ Desuscrito de: health_reminders") }
            .addOnFailureListener { e -> Timber.e(e, "❌ Error al desuscribirse de health_reminders") }

        val userId = prefs.getInt("user_id", -1)
        if (userId != -1) {
            firebaseMessaging.unsubscribeFromTopic("user_$userId")
                .addOnSuccessListener { Timber.d("✅ Desuscrito de: user_$userId") }
                .addOnFailureListener { e -> Timber.e(e, "❌ Error al desuscribirse de user_$userId") }
        }

        Timber.d("📡 Desuscrito de todos los topics")
    }

    /**
     * Forzar actualización del estado de conexión (útil cuando el usuario cambia configuración)
     */
    fun forceUpdate() {
        NetworkMonitor.updateConnectivity(context)
        Timber.d("🔄 Actualización forzada del estado de conexión")
    }
}


