package com.example.doc.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.doc.MainActivity
import com.example.doc.R
import com.example.doc.data.AppDatabase
import com.example.doc.repository.UsuarioRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

/**
 * Worker para enviar notificaciones de recordatorio de ejercicios
 */
class ExerciseNotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val CHANNEL_ID = "doccontrol_exercise_reminders"
        private const val CHANNEL_NAME = "Recordatorios de Ejercicio"
        private const val NOTIFICATION_ID = 1002
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Timber.d("ExerciseNotificationWorker iniciado")

            // Obtener usuario desde prefs
            val prefs = applicationContext.getSharedPreferences("medicini_prefs", Context.MODE_PRIVATE)
            val userId = prefs.getInt("user_id", -1)

            if (userId == -1) {
                Timber.w("No hay usuario registrado, no se enviará notificación de ejercicio")
                return@withContext Result.success()
            }

            // Obtener base de datos
            val db = AppDatabase.getDatabase(
                applicationContext,
                kotlinx.coroutines.CoroutineScope(Dispatchers.IO)
            )

            val userRepo = UsuarioRepository(db.usuarioDao())
            val ejercicioRealizadoDao = db.ejercicioRealizadoDao()

            // Obtener usuario
            val user = userRepo.obtenerUsuarioFlow().firstOrNull()
            if (user == null) {
                Timber.w("Usuario no encontrado en BD")
                return@withContext Result.success()
            }

            // Verificar si las notificaciones están habilitadas
            if (!user.notifEnabled) {
                Timber.d("Notificaciones deshabilitadas para el usuario")
                return@withContext Result.success()
            }

            // Verificar si ya hizo ejercicio hoy
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val today = sdf.format(Date())
            val ejerciciosRealizadosHoy = ejercicioRealizadoDao.getEjerciciosRealizadosHoy(userId, today)

            if (ejerciciosRealizadosHoy.isNotEmpty()) {
                Timber.d("Usuario ya realizó ${ejerciciosRealizadosHoy.size} ejercicio(s) hoy, no se enviará notificación")
                return@withContext Result.success()
            }

            // Enviar notificación
            sendExerciseNotification(user.nombre)
            Timber.d("Notificación de ejercicio enviada exitosamente")

            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Error en ExerciseNotificationWorker")
            Result.failure()
        }
    }

    private fun sendExerciseNotification(userName: String) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE)
            as NotificationManager

        // Crear canal de notificación (Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH // HIGH permite heads-up
            ).apply {
                description = "Recordatorios para realizar ejercicios"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500)
                enableLights(true)
                lightColor = android.graphics.Color.GREEN
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setBypassDnd(false) // No sobrescribir DND
                // Configurar sonido con atributos de audio
                setSound(
                    android.provider.Settings.System.DEFAULT_NOTIFICATION_URI,
                    android.media.AudioAttributes.Builder()
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION_EVENT)
                        .build()
                )
            }
            notificationManager.createNotificationChannel(channel)
            Timber.d("📢 Canal de notificaciones de ejercicio actualizado con IMPORTANCE_HIGH")
        }

        // Intent para abrir la app en la pantalla de ejercicios
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "exercises") // Para navegar a ejercicios
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            NOTIFICATION_ID, // Usar ID único
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Full Screen Intent para Android 14+ (crítico para notificaciones importantes)
        val fullScreenIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "exercises")
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            applicationContext,
            NOTIFICATION_ID + 1000,
            fullScreenIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val messages = listOf(
            "Hola $userName, es hora de moverte un poco",
            "Hola $userName, tus ejercicios te están esperando",
            "Hola $userName, dedica unos minutos a tu salud",
            "Hola $userName, tu cuerpo te lo agradecerá",
            "Hola $userName, mantén tu rutina de ejercicios"
        )

        val randomMessage = messages.random()

        // Crear notificación con configuración para heads-up
        val notificationBuilder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Recordatorio de Ejercicio")
            .setContentText(randomMessage)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(randomMessage)
            )
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // HIGH es suficiente para heads-up
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setVibrate(longArrayOf(0, 500, 250, 500))
            .setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI)
            .setOnlyAlertOnce(false) // Permitir que alerte cada vez
            .setWhen(System.currentTimeMillis()) // Timestamp actual
            .setShowWhen(true)
            .setLocalOnly(false) // Permitir en wearables

        // Agregar Full Screen Intent para Android 10+ (crítico para Android 14+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            notificationBuilder.setFullScreenIntent(fullScreenPendingIntent, true)
            Timber.d("📱 Full Screen Intent agregado para Android 10+")
        }

        val notification = notificationBuilder.build()

        notificationManager.notify(NOTIFICATION_ID, notification)
        Timber.d("✅ Notificación de ejercicio heads-up mostrada: $randomMessage")
    }
}

