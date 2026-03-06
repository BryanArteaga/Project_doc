package com.example.doc.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_20_21 = object : Migration(20, 21) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 🔧 Convertir ejercicio de INTEGER a TEXT
        database.execSQL("""
            CREATE TABLE registros_salud_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                usuarioId INTEGER NOT NULL,
                fecha TEXT,
                hora TEXT,
                presionSistolica INTEGER,
                presionDiastolica INTEGER,
                glucosa INTEGER,
                colesterol INTEGER,
                temperatura REAL,
                peso REAL,
                calorias INTEGER,
                ejercicio TEXT,
                estadoAnimo TEXT,
                notas TEXT,
                registradoEn INTEGER NOT NULL,
                syncId TEXT,
                isSynced INTEGER NOT NULL DEFAULT 0,
                lastSyncTimestamp INTEGER
            )
        """.trimIndent())

        // Copiar datos (ejercicio será NULL para los valores viejos)
        database.execSQL("""
            INSERT INTO registros_salud_new 
            SELECT id, usuarioId, fecha, hora, presionSistolica, presionDiastolica, 
                   glucosa, colesterol, temperatura, peso, calorias, 
                   NULL, estadoAnimo, notas, registradoEn, syncId, isSynced, lastSyncTimestamp
            FROM registros_salud
        """.trimIndent())

        database.execSQL("DROP TABLE registros_salud")
        database.execSQL("ALTER TABLE registros_salud_new RENAME TO registros_salud")
        database.execSQL("CREATE INDEX idx_registros_salud_usuarioId ON registros_salud(usuarioId)")
    }
}
