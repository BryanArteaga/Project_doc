package com.example.doc.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_26_27 = object : Migration(26, 27) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Agregar campo creadoEn a tabla consejos (timestamp en milisegundos)
        database.execSQL("ALTER TABLE consejos ADD COLUMN creadoEn INTEGER NOT NULL DEFAULT 0")
        
        // Agregar campo createdAt a tabla ejercicios (timestamp en milisegundos)
        database.execSQL("ALTER TABLE ejercicios ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
    }
}
