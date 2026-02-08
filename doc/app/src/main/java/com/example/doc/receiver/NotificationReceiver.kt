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

class NotificationReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_HEALTH_REMINDER = "com.example.doc.HEALTH_REMINDER"
        const val ACTION_EXERCISE_REMINDER = "com.example.doc.EXERCISE_REMINDER"
        const val ACTION_BOOT_COMPLETED = Intent.ACTION_BOOT_COMPLETED
        
        private const val HEALTH_REQUEST_CODE = 1001
        private const val EXERCISE_MORNING_REQUEST_CODE = 1002
        private const val EXERCISE_AFTERNOON_REQUEST_CODE = 1003
        private const val WAKELOCK_TAG = "DOC:NotificationWakeLock"

        fun scheduleHealthReminder(context: Context, notifType: String, notifDetails: String) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            
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
                context, HEALTH_REQUEST_CODE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val json = try {
                org.json.JSONObject(notifDetails)
            } catch (e: Exception) {
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

            when (notifType) {
                "daily_fixed" -> {
                    if (calendar.timeInMillis <= System.currentTimeMillis()) {
                        calendar.add(Calendar.DAY_OF_MONTH, 1)
                    }
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                            val alarmClockInfo = AlarmManager.AlarmClockInfo(calendar.timeInMillis, null)
                            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
                            Timber.d("Android 15+: Alarma DIARIA programada a las $hour:$minute")
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            alarmManager.setExactAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                calendar.timeInMillis,
                                pendingIntent
                            )
                            Timber.d("Android 6-14: Alarma DIARIA programada a las $hour:$minute")
                        } else {
                            alarmManager.setExact(
                                AlarmManager.RTC_WAKEUP,
                                calendar.timeInMillis,
                                pendingIntent
                            )
                            Timber.d("Android <6: Alarma DIARIA programada a las $hour:$minute")
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Error al programar alarma diaria")
                    }
                }
                "weekly_fixed" -> {
                    val dayOfWeek = json.optInt("dayOfWeek", Calendar.SUNDAY)
                    
                    calendar.apply {
                        set(Calendar.DAY_OF_WEEK, dayOfWeek)
                        if (timeInMillis <= System.currentTimeMillis()) {
                            add(Calendar.WEEK_OF_YEAR, 1)
                        }
                    }

                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                            val alarmClockInfo = AlarmManager.AlarmClockInfo(calendar.timeInMillis, null)
                            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
                            Timber.d("Android 15+: Alarma SEMANAL programada")
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            alarmManager.setExactAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                calendar.timeInMillis,
                                pendingIntent
                            )
                            Timber.d("Android 6-14: Alarma SEMANAL programada")
                        } else {
                            alarmManager.setExact(
                                AlarmManager.RTC_WAKEUP,
                                calendar.timeInMillis,
                                pendingIntent
                            )
                            Timber.d("Android <6: Alarma SEMANAL programada")
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Error al programar alarma semanal")
                    }
                }
                "monthly_fixed" -> {
                    val dayOfMonth = json.optInt("dayOfMonth", 1)
                    
                    calendar.apply {
                        set(Calendar.DAY_OF_MONTH, dayOfMonth)
                        if (timeInMillis <= System.currentTimeMillis()) {
                            add(Calendar.MONTH, 1)
                        }
                    }

                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                            val alarmClockInfo = AlarmManager.AlarmClockInfo(calendar.timeInMillis, null)
                            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
                            Timber.d("Android 15+: Alarma MENSUAL programada")
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            alarmManager.setExactAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                calendar.timeInMillis,
                                pendingIntent
                            )
                            Timber.d("Android 6-14: Alarma MENSUAL programada")
                        } else {
                            alarmManager.setExact(
                                AlarmManager.RTC_WAKEUP,
                                calendar.timeInMillis,
                                pendingIntent
                            )
                            Timber.d("Android <6: Alarma MENSUAL programada")
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Error al programar alarma mensual")
                    }
                }
            }
        }

        fun scheduleHealthReminder(context: Context, hour: Int, minute: Int) {
            val notifDetails = "{\"hour\":$hour,\"minute\":$minute}"
            scheduleHealthReminder(context, "daily_fixed", notifDetails)
        }

        fun scheduleExerciseReminders(context: Context) {
            scheduleExerciseReminder(context, 10, 0, EXERCISE_MORNING_REQUEST_CODE, true)
            scheduleExerciseReminder(context, 18, 0, EXERCISE_AFTERNOON_REQUEST_CODE, false)
        }

        fun scheduleExerciseReminder(context: Context, hour: Int, minute: Int, timeId: String) {
            val requestCode = when (timeId) {
                "morning" -> EXERCISE_MORNING_REQUEST_CODE
                "afternoon" -> EXERCISE_AFTERNOON_REQUEST_CODE
                else -> EXERCISE_MORNING_REQUEST_CODE
            }
            scheduleExerciseReminder(context, hour, minute, requestCode, timeId == "morning")
        }

        private fun scheduleExerciseReminder(
            context: Context, hour: Int, minute: Int, requestCode: Int, isMorning: Boolean
        ) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                action = ACTION_EXERCISE_REMINDER
                putExtra("is_morning", isMorning)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_MONTH, 1)
                }
            }

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                    val alarmClockInfo = AlarmManager.AlarmClockInfo(calendar.timeInMillis, null)
                    alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent
                    )
                }
                Timber.d("Alarma de ejercicio programada correctamente para las $hour:$minute")
            } catch (e: Exception) {
                Timber.e(e, "Error al programar alarma")
            }
        }

        fun cancelAllAlarms(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            
            val healthIntent = Intent(context, NotificationReceiver::class.java).apply {
                action = ACTION_HEALTH_REMINDER
            }
            val healthPendingIntent = PendingIntent.getBroadcast(
                context, HEALTH_REQUEST_CODE, healthIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(healthPendingIntent)

            val exerciseIntent = Intent(context, NotificationReceiver::class.java).apply {
                action = ACTION_EXERCISE_REMINDER
            }
            val morningPendingIntent = PendingIntent.getBroadcast(
                context, EXERCISE_MORNING_REQUEST_CODE, exerciseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val afternoonPendingIntent = PendingIntent.getBroadcast(
                context, EXERCISE_AFTERNOON_REQUEST_CODE, exerciseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(morningPendingIntent)
            alarmManager.cancel(afternoonPendingIntent)
            Timber.d("Todas las alarmas canceladas")
        }

        fun cancelAllReminders(context: Context) {
            cancelAllAlarms(context)
        }

        private fun scheduleNextHealthReminder(context: Context, notifType: String, notifDetails: String) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                action = ACTION_HEALTH_REMINDER
                putExtra("notif_type", notifType)
                putExtra("notif_details", notifDetails)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, HEALTH_REQUEST_CODE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val json = try {
                org.json.JSONObject(notifDetails)
            } catch (e: Exception) {
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

            when (notifType) {
                "daily_fixed" -> calendar.add(Calendar.DAY_OF_MONTH, 1)
                "weekly_fixed" -> {
                    val dayOfWeek = json.optInt("dayOfWeek", Calendar.SUNDAY)
                    calendar.set(Calendar.DAY_OF_WEEK, dayOfWeek)
                    calendar.add(Calendar.WEEK_OF_YEAR, 1)
                }
                "monthly_fixed" -> {
                    val dayOfMonth = json.optInt("dayOfMonth", 1)
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    calendar.add(Calendar.MONTH, 1)
                }
            }

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                    val alarmClockInfo = AlarmManager.AlarmClockInfo(calendar.timeInMillis, null)
                    alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                }
                Timber.d("Próxima alarma programada correctamente")
            } catch (e: Exception) {
                Timber.e(e, "Error programando próxima alarma")
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        Timber.d("NotificationReceiver.onReceive: ${intent.action}")

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_TAG)
        wakeLock.acquire(3 * 60 * 1000L)

        try {
            when (intent.action) {
                ACTION_HEALTH_REMINDER -> {
                    Timber.d("Enviando notificación de salud")
                    val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>().build()
                    WorkManager.getInstance(context).enqueue(workRequest)

                    var notifType = intent.getStringExtra("notif_type")
                    var notifDetails = intent.getStringExtra("notif_details")

                    if (notifType == null || notifDetails == null) {
                        val prefs = context.getSharedPreferences("doc_prefs", Context.MODE_PRIVATE)
                        val userId = prefs.getInt("user_id", -1)
                        if (userId != -1) {
                            notifType = prefs.getString("notif_type_$userId", "daily_fixed") ?: "daily_fixed"
                            notifDetails = prefs.getString("notif_details_$userId", "{\"hour\":20,\"minute\":0}") ?: "{\"hour\":20,\"minute\":0}"
                        }
                    }

                    if (notifType != null && notifDetails != null) {
                        scheduleNextHealthReminder(context, notifType, notifDetails)
                    }
                }

                ACTION_EXERCISE_REMINDER -> {
                    Timber.d("Enviando notificación de ejercicio")
                    val workRequest = OneTimeWorkRequestBuilder<ExerciseNotificationWorker>().build()
                    WorkManager.getInstance(context).enqueue(workRequest)

                    val isMorning = intent.getBooleanExtra("is_morning", true)
                    val hour = if (isMorning) 10 else 18
                    val requestCode = if (isMorning) EXERCISE_MORNING_REQUEST_CODE else EXERCISE_AFTERNOON_REQUEST_CODE

                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    val nextIntent = Intent(context, NotificationReceiver::class.java).apply {
                        action = ACTION_EXERCISE_REMINDER
                        putExtra("is_morning", isMorning)
                    }
                    val nextPendingIntent = PendingIntent.getBroadcast(
                        context, requestCode, nextIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    val calendar = Calendar.getInstance().apply {
                        timeInMillis = System.currentTimeMillis()
                        set(Calendar.HOUR_OF_DAY, hour)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                        add(Calendar.DAY_OF_MONTH, 1)
                    }

                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                            val alarmClockInfo = AlarmManager.AlarmClockInfo(calendar.timeInMillis, null)
                            alarmManager.setAlarmClock(alarmClockInfo, nextPendingIntent)
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, nextPendingIntent)
                        } else {
                            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, nextPendingIntent)
                        }
                        Timber.d("Próxima alarma de ejercicio programada")
                    } catch (e: Exception) {
                        Timber.e(e, "Error reprogramando")
                    }
                }

                ACTION_BOOT_COMPLETED, Intent.ACTION_LOCKED_BOOT_COMPLETED, "android.intent.action.QUICKBOOT_POWERON" -> {
                    val prefs = context.getSharedPreferences("doc_prefs", Context.MODE_PRIVATE)
                    val userId = prefs.getInt("user_id", -1)
                    if (userId != -1) {
                        val notifEnabled = prefs.getBoolean("notif_enabled_$userId", false)
                        if (notifEnabled) {
                            val notifType = prefs.getString("notif_type_$userId", "daily_fixed") ?: "daily_fixed"
                            val notifDetails = prefs.getString("notif_details_$userId", "{\"hour\":20,\"minute\":0}") ?: "{\"hour\":20,\"minute\":0}"
                            scheduleHealthReminder(context, notifType, notifDetails)
                            scheduleExerciseReminders(context)
                            Timber.d("Alarmas reprogramadas después de reinicio")
                        }
                    }
                }
            }
        } finally {
            if (wakeLock.isHeld) {
                wakeLock.release()
            }
        }
    }
}
