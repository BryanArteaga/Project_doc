package com.example.doc.utils

object Constants {

    // 🔌 Backend API Configuration
    const val API_BASE_URL = "http://192.168.0.196:3000/api/"  // 10.0.2.2 es el localhost del emulador Android

    object Database {
        const val NAME = "medicini_db"
        const val VERSION = 23
    }

    object Preferences {
        const val NAME = "medicini_prefs"
        const val USER_ID_KEY = "user_id"
        const val LAST_UPDATE_DATE_KEY = "last_update_date"
        const val CURRENT_EXERCISE_IDS_KEY = "current_exercise_ids"
    }

    object WorkManager {
        const val NOTIFICATION_TAG = "medicini_notif"
        const val DAILY_FIXED_WORK = "daily_fixed"
        const val WEEKLY_FIXED_WORK = "weekly_fixed"
        const val MONTHLY_FIXED_WORK = "monthly_fixed"
    }

    object Validation {
        const val MIN_EDAD = 1
        const val MAX_EDAD = 120
        const val MIN_PESO = 20.0
        const val MAX_PESO = 300.0
        const val MIN_ALTURA = 0.5
        const val MAX_ALTURA = 2.5
        const val MIN_GLUCOSA = 40
        const val MAX_GLUCOSA = 600
        const val MIN_COLESTEROL = 100
        const val MAX_COLESTEROL = 500
        const val MIN_CALORIAS = 0
        const val MAX_CALORIAS = 10000
        const val CEDULA_LENGTH = 10
    }

    object Health {
        const val DEFAULT_NOTIFICATION_HOUR = 20
        const val DEFAULT_NOTIFICATION_MINUTE = 0
        const val RECOMMENDED_DAILY_EXERCISE_MINUTES = 30
    }

    object Enfermedades {
        const val HIPERTENSION = "Hipertensión"
        const val DIABETES = "Diabetes"
        const val OBESIDAD = "Obesidad"
        const val COLESTEROL_ALTO = "Colesterol alto"
    }

    object NotificationTypes {
        const val DAILY_FIXED = "daily_fixed"
        const val WEEKLY_FIXED = "weekly_fixed"
        const val MONTHLY_FIXED = "monthly_fixed"
    }
}

