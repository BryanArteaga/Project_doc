package com.example.doc.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.example.doc.MainActivity
import com.example.doc.R
import com.example.doc.receiver.NotificationReceiver
import com.example.doc.worker.NotificationWorker
import timber.log.Timber
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Utilidad para gestionar notificaciones y WorkManager
 */
object NotificationHelper {

    private const val CHANNEL_ID = "medicini_reminders"
    private const val CHANNEL_NAME = "Recordatorios de Salud"
    private const val WORK_NAME = "medicini_notification_work"

    /**
     * Crear canal de notificaciones (Android 8.0+)
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH // Cambiado para mostrar heads-up
            ).apply {
                description = "Recordatorios para registrar datos de salud y hacer ejercicio"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500) // Vibración más notable
                enableLights(true)
                lightColor = android.graphics.Color.BLUE
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setBypassDnd(false) // No molestar
                // Habilitar sonido (muy importante para heads-up)
                setSound(
                    android.provider.Settings.System.DEFAULT_NOTIFICATION_URI,
                    android.media.AudioAttributes.Builder()
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION_EVENT)
                        .build()
                )
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)

            Timber.d("Canal de notificaciones creado con IMPORTANCE_HIGH y sonido habilitado")
        }
    }

    /**
     * Programar notificaciones según configuración del usuario
     * Usa AlarmManager para máxima precisión
     * @param context Contexto de la aplicación
     * @param notifType Tipo de notificación: "daily_fixed", "weekly_fixed", "monthly_fixed"
     * @param notifDetails JSON con detalles: {"hour":20,"minute":0,"dayOfWeek":2,"dayOfMonth":15}
     */
    fun scheduleNotifications(context: Context, notifType: String, notifDetails: String) {
        Timber.d("Programando notificaciones: type=$notifType, details=$notifDetails")

        try {
            // Usar AlarmManager para notificaciones exactas
            com.example.doc.receiver.NotificationReceiver.scheduleHealthReminder(context, notifType, notifDetails)
            
            Timber.d("Notificaciones programadas exitosamente con AlarmManager")
        } catch (e: Exception) {
            Timber.e(e, "Error al programar notificaciones")
        }
    }

    /**
     * Programar notificación inmediata (para testing)
     * Envía notificación en 5 segundos
     */
    fun scheduleImmediateNotification(context: Context) {
        Timber.d("Programando notificación de prueba inmediata (5 segundos)")

        try {
            val workManager = WorkManager.getInstance(context)

            // Cancelar trabajos anteriores
            workManager.cancelUniqueWork(WORK_NAME)

            // Crear work request inmediato (5 segundos)
            val notificationWork = OneTimeWorkRequestBuilder<NotificationWorker>()
                .setInitialDelay(5, TimeUnit.SECONDS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(false)
                        .build()
                )
                .build()

            // Encolar trabajo
            workManager.enqueueUniqueWork(
                "${WORK_NAME}_immediate",
                ExistingWorkPolicy.REPLACE,
                notificationWork
            )

            Timber.d("Notificación de prueba programada para dentro de 5 segundos")
        } catch (e: Exception) {
            Timber.e(e, "Error al programar notificación inmediata")
        }
    }

    /**
     * Calcular delay inicial hasta la próxima notificación
     */
    private fun calculateInitialDelay(notifType: String, notifDetails: String): Long {
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis

        // Parsear hora de notificación (formato: {"hour":20})
        val hour = try {
            val json = org.json.JSONObject(notifDetails)
            val parsedHour = json.getInt("hour")
            Timber.d("Hora parseada desde notifDetails: $parsedHour (JSON: $notifDetails)")
            parsedHour
        } catch (e: Exception) {
            Timber.e(e, "Error al parsear notifDetails, usando default 20")
            20 // Default: 8 PM
        }

        Timber.d("Configurando notificación para las $hour:00")

        // Configurar calendario para hoy a la hora especificada
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        // Si ya pasó la hora hoy, programar para mañana
        if (calendar.timeInMillis <= now) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            Timber.d("Hora ya pasó hoy, programando para mañana")
        }

        val delayMs = calendar.timeInMillis - now
        val delayHours = delayMs / (1000 * 60 * 60)
        Timber.d("Notificación programada en $delayHours horas ($delayMs ms)")

        return delayMs
    }

    /**
     * Cancelar todas las notificaciones programadas
     */
    fun cancelNotifications(context: Context) {
        try {
            val workManager = WorkManager.getInstance(context)
            workManager.cancelUniqueWork(WORK_NAME)
            Timber.d("Notificaciones canceladas")
        } catch (e: Exception) {
            Timber.e(e, "Error al cancelar notificaciones")
        }
    }

    /**
     * Enviar notificación de prueba inmediata
     */
    fun sendTestNotification(context: Context) {
        Timber.d("Enviando notificación de prueba")

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Crear canal si no existe
        createNotificationChannel(context)

        // Intent para abrir la app
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Crear notificación
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Prueba de Notificación")
            .setContentText("Si ves esto, las notificaciones están funcionando correctamente")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        notificationManager.notify(9999, notification)
        Timber.d("Notificación de prueba enviada")
    }

    /**
     * Verificar estado de Work Manager
     */
    fun checkWorkStatus(context: Context) {
        try {
            val workManager = WorkManager.getInstance(context)
            val workInfos = workManager.getWorkInfosForUniqueWork(WORK_NAME).get()

            if (workInfos.isEmpty()) {
                Timber.w("No hay trabajos programados")
            } else {
                workInfos.forEach { workInfo ->
                    Timber.d("Estado del trabajo: ${workInfo.state}")
                    Timber.d("Próxima ejecución en: ${workInfo.nextScheduleTimeMillis}")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error al verificar estado de WorkManager")
        }
    }

    /**
     * Programar notificaciones de ejercicios diarias
     * Se enviarán 2 notificaciones por día: mañana y tarde
     */
    fun scheduleExerciseNotifications(context: Context) {
        Timber.d("Programando notificaciones de ejercicios")

        try {
            // Notificación de la mañana (10:00 AM)
            com.example.doc.receiver.NotificationReceiver.scheduleExerciseReminder(context, 10, 0, "morning")
            
            // Notificación de la tarde (18:00 / 6:00 PM)
            com.example.doc.receiver.NotificationReceiver.scheduleExerciseReminder(context, 18, 0, "afternoon")

            Timber.d("Notificaciones de ejercicio programadas con AlarmManager: 10:00 AM y 6:00 PM")
        } catch (e: Exception) {
            Timber.e(e, "Error al programar notificaciones de ejercicio")
        }
    }

    /**
     * Cancelar notificaciones de ejercicios
     */
    fun cancelExerciseNotifications(context: Context) {
        try {
            com.example.doc.receiver.NotificationReceiver.cancelAllReminders(context)
            Timber.d("Notificaciones de ejercicio canceladas")
        } catch (e: Exception) {
            Timber.e(e, "Error al cancelar notificaciones de ejercicio")
        }
    }

    /**
     * Calcular delay para una hora específica del día
     */
    private fun calculateDelayForTime(hour: Int, minute: Int): Long {
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis

        // Configurar hora objetivo
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        var targetTime = calendar.timeInMillis

        // Si la hora ya pasó hoy, programar para mañana
        if (targetTime <= now) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            targetTime = calendar.timeInMillis
            Timber.d("Hora ya pasó hoy ($hour:$minute), programando para mañana")
        }

        val delay = targetTime - now
        val hours = TimeUnit.MILLISECONDS.toHours(delay)
        Timber.d("Notificación programada para las $hour:$minute en $hours horas ($delay ms)")

        return delay
    }
}

