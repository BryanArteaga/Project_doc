package com.example.doc.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_19_20 = object : Migration(19, 20) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Crear índice único en ejercicios_realizados (usuarioId, ejercicioId, fecha)
        database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_ejercicios_realizados_usuarioId_ejercicioId_fecha ON ejercicios_realizados(usuarioId, ejercicioId, fecha)")
    }
}
