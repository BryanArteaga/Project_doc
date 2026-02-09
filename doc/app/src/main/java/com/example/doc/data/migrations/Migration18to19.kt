package com.example.doc.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_18_19 = object : Migration(18, 19) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Agregar columnas de sincronización a registros_salud
        database.execSQL("ALTER TABLE registros_salud ADD COLUMN syncId TEXT")
        database.execSQL("ALTER TABLE registros_salud ADD COLUMN isSynced INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE registros_salud ADD COLUMN lastSyncTimestamp INTEGER")

        // Agregar columnas de sincronización a ejercicios_realizados
        database.execSQL("ALTER TABLE ejercicios_realizados ADD COLUMN syncId TEXT")
        database.execSQL("ALTER TABLE ejercicios_realizados ADD COLUMN isSynced INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE ejercicios_realizados ADD COLUMN lastSyncTimestamp INTEGER")
    }
}
