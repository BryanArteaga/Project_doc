package com.example.doc.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_23_24 = object : Migration(23, 24) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // ✅ Agregar columnas urlImagen y urlVideo a tabla ejercicios
        database.execSQL("ALTER TABLE ejercicios ADD COLUMN urlImagen TEXT")
        database.execSQL("ALTER TABLE ejercicios ADD COLUMN urlVideo TEXT")
    }
}
