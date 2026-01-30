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
import com.example.doc.repository.RegistroSaludRepository
import com.example.doc.repository.UsuarioRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

class NotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val CHANNEL_ID = "medicini_reminders"
        private const val CHANNEL_NAME = "Recordatorios de Salud"
        private const val NOTIFICATION_ID = 1001
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Timber.d("NotificationWorker iniciado")

            // Obtener usuario desde prefs
            val prefs = applicationContext.getSharedPreferences("medicini_prefs", Context.MODE_PRIVATE)
            val userId = prefs.getInt("user_id", -1)

            if (userId == -1) {
                Timber.w("No hay usuario registrado, no se enviará notificación")
                return@withContext Result.success()
            }

            // Obtener base de datos
            val db = AppDatabase.getDatabase(
                applicationContext,
                kotlinx.coroutines.CoroutineScope(Dispatchers.IO)
            )

            val userRepo = UsuarioRepository(db.usuarioDao())
            val registroRepo = RegistroSaludRepository(db.registroSaludDao())

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

            // Verificar si ya registró según el tipo de notificación
            val notifType = user.notifType
            val hasRegistered = checkIfRegistered(userId, notifType, registroRepo)

            if (hasRegistered) {
                Timber.d("Usuario ya registró datos, no se enviará notificación")
                return@withContext Result.success()
            }

            // Enviar notificación
            sendNotification(user.nombre, notifType)
            Timber.d("Notificación enviada exitosamente")

            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Error en NotificationWorker")
            Result.failure()
        }
    }

    private suspend fun checkIfRegistered(
        userId: Int,
        notifType: String,
        repo: RegistroSaludRepository
    ): Boolean {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        return when (notifType) {
            "daily_fixed" -> {
                val today = dateFormat.format(Date())
                repo.obtenerRegistroPorFecha(userId, today) != null
            }
            "weekly_fixed" -> {
                // Verificar si hay registro esta semana (lunes a domingo)
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                val startWeek = dateFormat.format(calendar.time)
                calendar.add(Calendar.DAY_OF_WEEK, 6)
                val endWeek = dateFormat.format(calendar.time)
                repo.obtenerRegistros(userId).any { it.fecha in startWeek..endWeek }
            }
            "monthly_fixed" -> {
                // Verificar si hay registro este mes
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                val startMonth = dateFormat.format(calendar.time)
                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                val endMonth = dateFormat.format(calendar.time)
                repo.obtenerRegistros(userId).any { it.fecha in startMonth..endMonth }
            }
            else -> false
        }
    }

    private fun sendNotification(userName: String, notifType: String) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE)
            as NotificationManager

        // Crear canal de notificación (Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH // IMPORTANCE_HIGH permite heads-up
            ).apply {
                description = "Recordatorios para registrar datos de salud"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500)
                enableLights(true)
                lightColor = android.graphics.Color.BLUE
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                // No sobrescribir el DND (No molestar)
                setBypassDnd(false)
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
            Timber.d("📢 Canal de notificaciones de salud actualizado con IMPORTANCE_HIGH")
        }

        // Intent para abrir la app al tocar la notificación (con acción específica)
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("openHealthLog", true) // Señalar que debe abrir el registro de salud
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            NOTIFICATION_ID, // Usar ID único por notificación
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Full Screen Intent para Android 14+ (crítico para notificaciones importantes)
        val fullScreenIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("openHealthLog", true)
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            applicationContext,
            NOTIFICATION_ID + 1000,
            fullScreenIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Mensaje según tipo de notificación
        val message = when (notifType) {
            "daily_fixed" -> "Hola $userName, recuerda registrar tus datos de salud hoy"
            "weekly_fixed" -> "Hola $userName, es hora de tu registro semanal de salud"
            "monthly_fixed" -> "Hola $userName, registra tus datos mensuales de salud"
            else -> "Hola $userName, recuerda registrar tus datos de salud"
        }

        // Crear notificación con configuración para heads-up
        val notificationBuilder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Recordatorio de Salud")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message)) // Estilo expandido
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // HIGH es suficiente para heads-up
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setVibrate(longArrayOf(0, 500, 250, 500)) // Vibración más notable
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
        Timber.d("✅ Notificación heads-up mostrada: $message")
    }
}