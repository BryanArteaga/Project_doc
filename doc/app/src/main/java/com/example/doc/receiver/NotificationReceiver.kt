package com.example.doc.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.doc.worker.NotificationWorker
import com.example.doc.worker.ExerciseNotificationWorker
import timber.log.Timber
import java.util.Calendar

/**
 * BroadcastReceiver para manejar alarmas de notificaciones exactas
 */
class NotificationReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_HEALTH_REMINDER = "com.example.doc.HEALTH_REMINDER"
        const val ACTION_EXERCISE_REMINDER = "com.example.doc.EXERCISE_REMINDER"
        const val ACTION_BOOT_COMPLETED = Intent.ACTION_BOOT_COMPLETED
        
        private const val HEALTH_REQUEST_CODE = 1001
        private const val EXERCISE_MORNING_REQUEST_CODE = 1002
        private const val EXERCISE_AFTERNOON_REQUEST_CODE = 1003

        // WakeLock tag para identificar
        private const val WAKELOCK_TAG = "DOC:NotificationWakeLock"

        /**
         * Programar la PRÓXIMA alarma de salud (después de que se dispare una)
         * Esta función calcula correctamente cuándo debe ser la siguiente ocurrencia
         */
        private fun scheduleNextHealthReminder(context: Context, notifType: String, notifDetails: String) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                action = ACTION_HEALTH_REMINDER
                putExtra("notif_type", notifType)
                putExtra("notif_details", notifDetails)
            }
            
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                HEALTH_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Parsear detalles
            val json = try {
                org.json.JSONObject(notifDetails)
            } catch (e: Exception) {
                Timber.e(e, "Error al parsear notifDetails")
                org.json.JSONObject("{\"hour\":20,\"minute\":0}")
            }

            val hour = json.optInt("hour", 20)
            val minute = json.optInt("minute", 0)

            val calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            // Calcular la PRÓXIMA ocurrencia según el tipo
            when (notifType) {
                "daily_fixed" -> {
                    // Próximo día a la misma hora
                    calendar.add(Calendar.DAY_OF_MONTH, 1)
                    Timber.d("🔄 Próxima alarma DIARIA: mañana a las $hour:$minute")
                }

                "weekly_fixed" -> {
                    val dayOfWeek = json.optInt("dayOfWeek", Calendar.SUNDAY)
                    
                    // Avanzar a la próxima semana, mismo día
                    calendar.set(Calendar.DAY_OF_WEEK, dayOfWeek)
                    calendar.add(Calendar.WEEK_OF_YEAR, 1)
                    
                    val dayName = getDayName(dayOfWeek)
                    Timber.d("🔄 Próxima alarma SEMANAL: próximo $dayName a las $hour:$minute")
                }

                "monthly_fixed" -> {
                    val dayOfMonth = json.optInt("dayOfMonth", 1)
                    
                    // Próximo mes, mismo día
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    calendar.add(Calendar.MONTH, 1)
                    
                    Timber.d("🔄 Próxima alarma MENSUAL: próximo mes día $dayOfMonth a las $hour:$minute")
                }

                else -> {
                    Timber.w("⚠️ Tipo de notificación desconocido: $notifType, usando diario")
                    calendar.add(Calendar.DAY_OF_MONTH, 1)
                }
            }

            try {
                // Para Android 15+, usar setAlarmClock para máxima prioridad
                // Esto garantiza que la alarma se ejecute incluso en Doze Mode agresivo
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                    // Android 15+ requiere setAlarmClock para garantizar precisión
                    val showIntent = Intent(context, com.example.doc.MainActivity::class.java)
                    val showPendingIntent = PendingIntent.getActivity(
                        context,
                        0,
                        showIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    val alarmClockInfo = AlarmManager.AlarmClockInfo(
                        calendar.timeInMillis,
                        showPendingIntent
                    )
                    alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
                    Timber.d("📱 Android 15+: Usando setAlarmClock para máxima prioridad")
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }

                val daysUntil = (calendar.timeInMillis - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)
                val hoursUntil = (calendar.timeInMillis - System.currentTimeMillis()) / (1000 * 60 * 60)
                Timber.d("✅ Próxima alarma programada en $daysUntil días ($hoursUntil horas)")
            } catch (e: Exception) {
                Timber.e(e, "❌ Error al programar próxima alarma")
            }
        }

        /**
         * Obtener nombre del día de la semana
         */
        private fun getDayName(dayOfWeek: Int): String {
            return when (dayOfWeek) {
                Calendar.MONDAY -> "Lunes"
                Calendar.TUESDAY -> "Martes"
                Calendar.WEDNESDAY -> "Miércoles"
                Calendar.THURSDAY -> "Jueves"
                Calendar.FRIDAY -> "Viernes"
                Calendar.SATURDAY -> "Sábado"
                Calendar.SUNDAY -> "Domingo"
                else -> "Desconocido"
            }
        }

        /**
         * Programar alarma exacta para recordatorio de salud según frecuencia
         * @param context Contexto de la aplicación
         * @param notifType Tipo de notificación: "daily_fixed", "weekly_fixed", "monthly_fixed"
         * @param notifDetails JSON con detalles: {"hour":20,"minute":0,"dayOfWeek":2,"dayOfMonth":15}
         */
        fun scheduleHealthReminder(context: Context, notifType: String, notifDetails: String) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            
            // Guardar tipo y detalles en SharedPreferences para reprogramación
            val prefs = context.getSharedPreferences("doc_prefs", Context.MODE_PRIVATE)
            val userId = prefs.getInt("user_id", -1)
            if (userId != -1) {
                prefs.edit().apply {
                    putString("notif_type_$userId", notifType)
                    putString("notif_details_$userId", notifDetails)
                    apply()
                }
            }
            
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                action = ACTION_HEALTH_REMINDER
                putExtra("notif_type", notifType)
                putExtra("notif_details", notifDetails)
            }
            
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                HEALTH_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Parsear detalles
            val json = try {
                org.json.JSONObject(notifDetails)
            } catch (e: Exception) {
                Timber.e(e, "Error al parsear notifDetails, usando valores por defecto")
                org.json.JSONObject("{\"hour\":20,\"minute\":0}")
            }

            val hour = json.optInt("hour", 20)
            val minute = json.optInt("minute", 0)

            val calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            // Configurar según tipo de notificación
            when (notifType) {
                "daily_fixed" -> {
                    // Si la hora ya pasó hoy, programar para mañana
                    if (calendar.timeInMillis <= System.currentTimeMillis()) {
                        calendar.add(Calendar.DAY_OF_MONTH, 1)
                    }

                    try {
                        // Para Android 15+, usar setAlarmClock para máxima prioridad
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                            val showIntent = Intent(context, com.example.doc.MainActivity::class.java)
                            val showPendingIntent = PendingIntent.getActivity(
                                context,
                                0,
                                showIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            )
                            val alarmClockInfo = AlarmManager.AlarmClockInfo(
                                calendar.timeInMillis,
                                showPendingIntent
                            )
                            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
                            Timber.d("📱 Android 15+: Alarma DIARIA configurada con setAlarmClock")
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            alarmManager.setExactAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                calendar.timeInMillis,
                                pendingIntent
                            )
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                            alarmManager.setExact(
                                AlarmManager.RTC_WAKEUP,
                                calendar.timeInMillis,
                                pendingIntent
                            )
                        } else {
                            alarmManager.set(
                                AlarmManager.RTC_WAKEUP,
                                calendar.timeInMillis,
                                pendingIntent
                            )
                        }

                        val hoursUntil = (calendar.timeInMillis - System.currentTimeMillis()) / (1000 * 60 * 60)
                        Timber.d("✅ Alarma DIARIA programada para las $hour:$minute (primera en $hoursUntil horas)")
                    } catch (e: Exception) {
                        Timber.e(e, "❌ Error al programar alarma diaria")
                    }
                }

                "weekly_fixed" -> {
                    val dayOfWeek = json.optInt("dayOfWeek", Calendar.SUNDAY) // 1=Domingo, 2=Lunes, etc.
                    
                    // Configurar para el día de la semana especificado
                    calendar.apply {
                        set(Calendar.DAY_OF_WEEK, dayOfWeek)
                        
                        // Si el día/hora ya pasó esta semana, programar para la próxima semana
                        if (timeInMillis <= System.currentTimeMillis()) {
                            add(Calendar.WEEK_OF_YEAR, 1)
                        }
                    }

                    try {
                        // Para Android 15+, usar setAlarmClock para máxima prioridad
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                            val showIntent = Intent(context, com.example.doc.MainActivity::class.java)
                            val showPendingIntent = PendingIntent.getActivity(
                                context,
                                0,
                                showIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            )
                            val alarmClockInfo = AlarmManager.AlarmClockInfo(
                                calendar.timeInMillis,
                                showPendingIntent
                            )
                            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
                            Timber.d("📱 Android 15+: Alarma SEMANAL configurada con setAlarmClock")
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            alarmManager.setExactAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                calendar.timeInMillis,
                                pendingIntent
                            )
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                            alarmManager.setExact(
                                AlarmManager.RTC_WAKEUP,
                                calendar.timeInMillis,
                                pendingIntent
                            )
                        } else {
                            alarmManager.set(
                                AlarmManager.RTC_WAKEUP,
                                calendar.timeInMillis,
                                pendingIntent
                            )
                        }

                        val dayName = when (dayOfWeek) {
                            Calendar.MONDAY -> "Lunes"
                            Calendar.TUESDAY -> "Martes"
                            Calendar.WEDNESDAY -> "Miércoles"
                            Calendar.THURSDAY -> "Jueves"
                            Calendar.FRIDAY -> "Viernes"
                            Calendar.SATURDAY -> "Sábado"
                            Calendar.SUNDAY -> "Domingo"
                            else -> "Desconocido"
                        }
                        
                        val daysUntil = (calendar.timeInMillis - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)
                        Timber.d("✅ Alarma SEMANAL programada para $dayName a las $hour:$minute (primera en $daysUntil días)")
                    } catch (e: Exception) {
                        Timber.e(e, "❌ Error al programar alarma semanal")
                    }
                }

                "monthly_fixed" -> {
                    val dayOfMonth = json.optInt("dayOfMonth", 1)
                    
                    calendar.apply {
                        set(Calendar.DAY_OF_MONTH, dayOfMonth)
                        
                        // Si el día/hora ya pasó este mes, programar para el próximo mes
                        if (timeInMillis <= System.currentTimeMillis()) {
                            add(Calendar.MONTH, 1)
                        }
                    }

                    try {
                        // Para Android 15+, usar setAlarmClock para máxima prioridad
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                            val showIntent = Intent(context, com.example.doc.MainActivity::class.java)
                            val showPendingIntent = PendingIntent.getActivity(
                                context,
                                0,
                                showIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            )
                            val alarmClockInfo = AlarmManager.AlarmClockInfo(
                                calendar.timeInMillis,
                                showPendingIntent
                            )
                            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
                            Timber.d("📱 Android 15+: Alarma MENSUAL configurada con setAlarmClock")
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            alarmManager.setExactAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                calendar.timeInMillis,
                                pendingIntent
                            )
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                            alarmManager.setExact(
                                AlarmManager.RTC_WAKEUP,
                                calendar.timeInMillis,
                                pendingIntent
                            )
                        } else {
                            alarmManager.set(
                                AlarmManager.RTC_WAKEUP,
                                calendar.timeInMillis,
                                pendingIntent
                            )
                        }

                        val daysUntil = (calendar.timeInMillis - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)
                        Timber.d("✅ Alarma MENSUAL programada para día $dayOfMonth a las $hour:$minute (primera en $daysUntil días)")
                    } catch (e: Exception) {
                        Timber.e(e, "❌ Error al programar alarma mensual")
                    }
                }

                else -> {
                    Timber.w("⚠️ Tipo de notificación desconocido: $notifType")
                }
            }
        }

        /**
         * Sobrecarga para compatibilidad con código antiguo (solo hora/minuto)
         */
        fun scheduleHealthReminder(context: Context, hour: Int, minute: Int) {
            val notifDetails = "{\"hour\":$hour,\"minute\":$minute}"
            scheduleHealthReminder(context, "daily_fixed", notifDetails)
        }

        /**
         * Programar alarmas exactas para recordatorios de ejercicio
         */
        fun scheduleExerciseReminders(context: Context) {
            scheduleExerciseReminder(context, 10, 0, EXERCISE_MORNING_REQUEST_CODE, true)
            scheduleExerciseReminder(context, 18, 0, EXERCISE_AFTERNOON_REQUEST_CODE, false)
        }

        /**
         * Programar una alarma de ejercicio individual (método público)
         */
        fun scheduleExerciseReminder(context: Context, hour: Int, minute: Int, timeId: String) {
            val requestCode = when (timeId) {
                "morning" -> EXERCISE_MORNING_REQUEST_CODE
                "afternoon" -> EXERCISE_AFTERNOON_REQUEST_CODE
                else -> EXERCISE_MORNING_REQUEST_CODE
            }
            val isMorning = timeId == "morning"
            scheduleExerciseReminder(context, hour, minute, requestCode, isMorning)
        }

        private fun scheduleExerciseReminder(
            context: Context,
            hour: Int,
            minute: Int,
            requestCode: Int,
            isMorning: Boolean
        ) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                action = ACTION_EXERCISE_REMINDER
                putExtra("is_morning", isMorning)
            }
            
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                
                // Si la hora ya pasó hoy, programar para mañana
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_MONTH, 1)
                }
            }

            try {
                // Para Android 15+, usar setAlarmClock para máxima prioridad
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                    val showIntent = Intent(context, com.example.doc.MainActivity::class.java)
                    val showPendingIntent = PendingIntent.getActivity(
                        context,
                        0,
                        showIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    val alarmClockInfo = AlarmManager.AlarmClockInfo(
                        calendar.timeInMillis,
                        showPendingIntent
                    )
                    alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
                    Timber.d("📱 Android 15+: Alarma de ejercicio configurada con setAlarmClock")
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }

                val timeType = if (isMorning) "mañana" else "tarde"
                val hoursUntil = (calendar.timeInMillis - System.currentTimeMillis()) / (1000 * 60 * 60)
                Timber.d("Alarma de ejercicio ($timeType) programada para las $hour:$minute (en $hoursUntil horas)")
            } catch (e: Exception) {
                Timber.e(e, "Error al programar alarma de ejercicio")
            }
        }

        /**
         * Cancelar todas las alarmas (para sistema híbrido)
         */
        fun cancelAllAlarms(context: Context) {
            Timber.d("🔴 Cancelando TODAS las alarmas (modo híbrido)")
            cancelAllReminders(context)
        }

        /**
         * Cancelar todas las alarmas
         */
        fun cancelAllReminders(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            
            // Cancelar alarma de salud
            val healthIntent = Intent(context, NotificationReceiver::class.java).apply {
                action = ACTION_HEALTH_REMINDER
            }
            val healthPendingIntent = PendingIntent.getBroadcast(
                context,
                HEALTH_REQUEST_CODE,
                healthIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(healthPendingIntent)

            // Cancelar alarmas de ejercicio
            val exerciseIntent = Intent(context, NotificationReceiver::class.java).apply {
                action = ACTION_EXERCISE_REMINDER
            }
            val morningPendingIntent = PendingIntent.getBroadcast(
                context,
                EXERCISE_MORNING_REQUEST_CODE,
                exerciseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val afternoonPendingIntent = PendingIntent.getBroadcast(
                context,
                EXERCISE_AFTERNOON_REQUEST_CODE,
                exerciseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(morningPendingIntent)
            alarmManager.cancel(afternoonPendingIntent)

            Timber.d("Todas las alarmas canceladas")
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        Timber.d("📢 NotificationReceiver onReceive: ${intent.action}")

        // Adquirir WakeLock para despertar el dispositivo (crítico para Android 12+)
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            WAKELOCK_TAG
        )

        // Adquirir WakeLock por 3 minutos (suficiente para ejecutar el worker)
        wakeLock.acquire(3 * 60 * 1000L)

        try {
            when (intent.action) {
                ACTION_HEALTH_REMINDER -> {
                    Timber.d("⏰ Alarma de recordatorio de salud recibida - Enviando notificación")

                    // Ejecutar worker para enviar notificación inmediatamente
                    val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>().build()
                    WorkManager.getInstance(context).enqueue(workRequest)

                    // Obtener tipo y detalles de la notificación
                    var notifType = intent.getStringExtra("notif_type")
                    var notifDetails = intent.getStringExtra("notif_details")

                    // Si no vienen en el intent, obtener de SharedPreferences
                    if (notifType == null || notifDetails == null) {
                        val prefs = context.getSharedPreferences("doc_prefs", Context.MODE_PRIVATE)
                        val userId = prefs.getInt("user_id", -1)
                        if (userId != -1) {
                            notifType = prefs.getString("notif_type_$userId", "daily_fixed") ?: "daily_fixed"
                            notifDetails = prefs.getString("notif_details_$userId", "{\"hour\":20,\"minute\":0}")
                                ?: "{\"hour\":20,\"minute\":0}"
                        }
                    }

                    // Reprogramar la próxima alarma según el tipo
                    if (notifType != null && notifDetails != null) {
                        Timber.d("🔄 Reprogramando próxima alarma: type=$notifType, details=$notifDetails")
                        scheduleNextHealthReminder(context, notifType, notifDetails)
                    } else {
                        Timber.w("⚠️ No se pudo obtener configuración de notificaciones para reprogramar")
                    }

                    Timber.d("✅ Notificación de salud enviada y próxima alarma programada")
                }

                ACTION_EXERCISE_REMINDER -> {
                    Timber.d("⏰ Alarma de recordatorio de ejercicio recibida - Enviando notificación")

                    // Ejecutar worker para enviar notificación inmediatamente
                    val workRequest = OneTimeWorkRequestBuilder<ExerciseNotificationWorker>().build()
                    WorkManager.getInstance(context).enqueue(workRequest)

                    // Determinar si es alarma de mañana o tarde
                    val isMorning = intent.getBooleanExtra("is_morning", true)
                    val hour = if (isMorning) 10 else 18
                    val requestCode = if (isMorning) EXERCISE_MORNING_REQUEST_CODE else EXERCISE_AFTERNOON_REQUEST_CODE
                    val timeType = if (isMorning) "mañana" else "tarde"

                    // Reprogramar para mañana a la misma hora (no hoy de nuevo)
                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    val nextIntent = Intent(context, NotificationReceiver::class.java).apply {
                        action = ACTION_EXERCISE_REMINDER
                        putExtra("is_morning", isMorning)
                    }

                    val nextPendingIntent = PendingIntent.getBroadcast(
                        context,
                        requestCode,
                        nextIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    // Calcular para mañana a la misma hora
                    val calendar = Calendar.getInstance().apply {
                        timeInMillis = System.currentTimeMillis()
                        set(Calendar.HOUR_OF_DAY, hour)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                        add(Calendar.DAY_OF_MONTH, 1) // Siempre mañana
                    }

                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                            val showIntent = Intent(context, com.example.doc.MainActivity::class.java)
                            val showPendingIntent = PendingIntent.getActivity(
                                context,
                                0,
                                showIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            )
                            val alarmClockInfo = AlarmManager.AlarmClockInfo(
                                calendar.timeInMillis,
                                showPendingIntent
                            )
                            alarmManager.setAlarmClock(alarmClockInfo, nextPendingIntent)
                            Timber.d("📱 Android 15+: Próxima alarma de ejercicio configurada con setAlarmClock")
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            alarmManager.setExactAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                calendar.timeInMillis,
                                nextPendingIntent
                            )
                        } else {
                            alarmManager.setExact(
                                AlarmManager.RTC_WAKEUP,
                                calendar.timeInMillis,
                                nextPendingIntent
                            )
                        }

                        val hoursUntil = (calendar.timeInMillis - System.currentTimeMillis()) / (1000 * 60 * 60)
                        Timber.d("🔄 Próxima alarma de ejercicio ($timeType) programada para mañana a las $hour:00 (en $hoursUntil horas)")
                    } catch (e: Exception) {
                        Timber.e(e, "Error al reprogramar alarma de ejercicio")
                    }

                    Timber.d("✅ Notificación de ejercicio enviada y próxima alarma programada")
                }

                ACTION_BOOT_COMPLETED,
                Intent.ACTION_LOCKED_BOOT_COMPLETED,
                "android.intent.action.QUICKBOOT_POWERON" -> {
                    Timber.d("🔄 Dispositivo reiniciado, reprogramando alarmas")

                    // Reprogramar todas las alarmas después de reiniciar
                    val prefs = context.getSharedPreferences("doc_prefs", Context.MODE_PRIVATE)
                    val userId = prefs.getInt("user_id", -1)
                    if (userId != -1) {
                        val notifEnabled = prefs.getBoolean("notif_enabled_$userId", false)
                        if (notifEnabled) {
                            val notifType = prefs.getString("notif_type_$userId", "daily_fixed") ?: "daily_fixed"
                            val notifDetails = prefs.getString("notif_details_$userId", "{\"hour\":20,\"minute\":0}")
                                ?: "{\"hour\":20,\"minute\":0}"

                            scheduleHealthReminder(context, notifType, notifDetails)
                            scheduleExerciseReminders(context)

                            Timber.d("✅ Alarmas reprogramadas después de reinicio")
                        }
                    }
                }
            }
        } finally {
            // Liberar WakeLock siempre
            if (wakeLock.isHeld) {
                wakeLock.release()
                Timber.d("🔓 WakeLock liberado")
            }
        }
    }
}
