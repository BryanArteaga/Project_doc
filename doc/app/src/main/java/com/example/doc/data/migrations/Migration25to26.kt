package com.example.doc.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_25_26 = object : Migration(25, 26) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Agregar campos de salarios específicos para Obesidad (IMC, Pulso)
        database.execSQL("ALTER TABLE registros_salud ADD COLUMN imc REAL")
        database.execSQL("ALTER TABLE registros_salud ADD COLUMN pulso INTEGER")
    }
}
