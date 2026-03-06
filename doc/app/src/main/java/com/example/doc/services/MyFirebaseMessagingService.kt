package com.example.doc.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import com.example.doc.MainActivity
import com.example.doc.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import timber.log.Timber

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        // Canales de notificación
        private const val CHANNEL_REMINDERS_ID = "fcm_reminders"
        private const val CHANNEL_REMINDERS_NAME = "Recordatorios"
        
        private const val CHANNEL_ACCOUNT_ID = "fcm_account"
        private const val CHANNEL_ACCOUNT_NAME = "Estado de Cuenta"
        
        private const val CHANNEL_MESSAGES_ID = "fcm_messages"
        private const val CHANNEL_MESSAGES_NAME = "Mensajes del Doctor"
    }

    /**
     * Se llama cuando se recibe un mensaje PUSH de Firebase
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Timber.d("PUSH recibido de Firebase")

        // Mostrar datos del mensaje
        Timber.d("From: ${remoteMessage.from}")
        Timber.d("Notification Title: ${remoteMessage.notification?.title}")
        Timber.d("Notification Body: ${remoteMessage.notification?.body}")
        Timber.d("Data: ${remoteMessage.data}")

        // Determinar tipo de notificación
        val notificationType = remoteMessage.data["type"] ?: "generic"
        
        when (notificationType) {
            "health_reminder" -> handleHealthReminder(remoteMessage)
            "exercise_reminder" -> handleExerciseReminder(remoteMessage)
            "account_activated" -> handleAccountActivated(remoteMessage)
            "message_from_doctor" -> handleMessageFromDoctor(remoteMessage)
            else -> handleGenericNotification(remoteMessage)
        }
    }

    /**
     * Se llama cuando se registra un nuevo token FCM
     */
    override fun onNewToken(token: String) {
        Timber.d("Nuevo FCM Token: $token")

        // Guardar el token en SharedPreferences
        val prefs = getSharedPreferences("doc_prefs", Context.MODE_PRIVATE)
        prefs.edit { putString("fcm_token", token) }

        // TODO: Enviar este token al servidor backend para notificaciones personalizadas
        sendTokenToServer(token)
    }

    /**
     * Manejar notificación de recordatorio de salud
     */
    private fun handleHealthReminder(message: RemoteMessage) {
        val title = message.notification?.title ?: "Recordatorio de Salud"
        val body = message.notification?.body ?: "Recuerda registrar tus datos de salud"

        Timber.d("Notificación de Salud: $title - $body")

        showNotification(
            title = title,
            body = body,
            channelId = CHANNEL_REMINDERS_ID,
            channelName = CHANNEL_REMINDERS_NAME,
            notificationType = "health",
            importance = NotificationManager.IMPORTANCE_HIGH
        )
    }

    /**
     * Manejar notificación de recordatorio de ejercicio
     */
    private fun handleExerciseReminder(message: RemoteMessage) {
        val title = message.notification?.title ?: "Recordatorio de Ejercicio"
        val body = message.notification?.body ?: "Es hora de realizar tu ejercicio"

        Timber.d("Notificación de Ejercicio: $title - $body")

        showNotification(
            title = title,
            body = body,
            channelId = CHANNEL_REMINDERS_ID,
            channelName = CHANNEL_REMINDERS_NAME,
            notificationType = "exercise",
            importance = NotificationManager.IMPORTANCE_HIGH
        )
    }

    /**
     * Manejar notificación de cuenta activada (SOLO PUSH)
     */
    private fun handleAccountActivated(message: RemoteMessage) {
        val title = message.notification?.title ?: "¡Cuenta Activada!"
        val body = message.notification?.body ?: "Tu médico ha activado tu cuenta. Ya puedes usar todas las funciones."

        Timber.d("Notificación de Activación: $title - $body")

        showNotification(
            title = title,
            body = body,
            channelId = CHANNEL_ACCOUNT_ID,
            channelName = CHANNEL_ACCOUNT_NAME,
            notificationType = "account_activated",
            importance = NotificationManager.IMPORTANCE_HIGH
        )
    }

    /**
     * Manejar mensaje del doctor (futuro)
     */
    private fun handleMessageFromDoctor(message: RemoteMessage) {
        val title = message.notification?.title ?: "Mensaje del Doctor"
        val body = message.notification?.body ?: "Tienes un nuevo mensaje"

        Timber.d("Notificación de Mensaje: $title - $body")

        showNotification(
            title = title,
            body = body,
            channelId = CHANNEL_MESSAGES_ID,
            channelName = CHANNEL_MESSAGES_NAME,
            notificationType = "message",
            importance = NotificationManager.IMPORTANCE_HIGH
        )
    }

    /**
     * Manejar notificación genérica
     */
    private fun handleGenericNotification(message: RemoteMessage) {
        val title = message.notification?.title ?: "Notificación"
        val body = message.notification?.body ?: ""

        Timber.d("Notificación Genérica: $title - $body")

        showNotification(
            title = title,
            body = body,
            channelId = CHANNEL_REMINDERS_ID,
            channelName = CHANNEL_REMINDERS_NAME,
            notificationType = "generic",
            importance = NotificationManager.IMPORTANCE_DEFAULT
        )
    }

    /**
     * Mostrar notificación en pantalla
     */
    private fun showNotification(
        title: String,
        body: String,
        channelId: String,
        channelName: String,
        notificationType: String = "generic",
        importance: Int = NotificationManager.IMPORTANCE_DEFAULT
    ) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Crear canal (Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                importance
            ).apply {
                description = "Notificaciones de $channelName"
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Intent para abrir la app
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP

            // Navegar según tipo de notificación
            when (notificationType) {
                "exercise" -> putExtra("navigate_to", "exercises")
                "health" -> putExtra("navigate_to", "health_log")
                "account_activated" -> putExtra("navigate_to", "main")
                "message" -> putExtra("navigate_to", "messages") // Futuro
                else -> {}
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            notificationType.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Construir notificación
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(if (importance == NotificationManager.IMPORTANCE_HIGH) 
                NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)

        // Mostrar notificación
        val notificationId = System.currentTimeMillis().toInt()
        
        try {
            notificationManager.notify(notificationId, notificationBuilder.build())
            Timber.d("✅ Notificación mostrada: ID=$notificationId, Type=$notificationType, Title=$title")
        } catch (e: Exception) {
            Timber.e(e, "❌ Error mostrando notificación: $title")
        }
    }

    /**
     * Enviar token FCM al servidor backend
     */
    private fun sendTokenToServer(token: String) {
        Timber.d("Enviando token al servidor: $token")
        
        val prefs = getSharedPreferences("doc_prefs", Context.MODE_PRIVATE)
        val userId = prefs.getInt("user_id", -1)
        
        // ✅ PERMITIR ENVÍO INCLUSO SIN USER_ID
        // El token se registra como "temporal" y se asocia al usuario cuando se crea la cuenta
        // O se envía con userId si ya existe sesión
        
        Timber.d("📤 Enviando token: userId=${if(userId == -1) "sin usuario" else userId}")
        
        // Enviar al backend usando Retrofit - MainActivity/UsuarioViewModel lo hace
        // a través de FCMTokenManager
    }
}


