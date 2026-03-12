package com.example.doc.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_21_22 = object : Migration(21, 22) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // ✅ Agregar columna ejercicioMinutos a registros_salud
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
                ejercicioMinutos INTEGER NOT NULL DEFAULT 0,
                estadoAnimo TEXT,
                notas TEXT,
                registradoEn INTEGER NOT NULL,
                syncId TEXT,
                isSynced INTEGER NOT NULL DEFAULT 0,
                lastSyncTimestamp INTEGER
            )
        """.trimIndent())

        // ✅ Copiar datos e intentar convertir ejercicio (string) a ejercicioMinutos (int)
        // Si ejercicio es numérico (ej: "90"), usa ese valor. Si no, usa 0
        database.execSQL("""
            INSERT INTO registros_salud_new 
            SELECT id, usuarioId, fecha, hora, presionSistolica, presionDiastolica, 
                   glucosa, colesterol, temperatura, peso, calorias, 
                   ejercicio, 
                   CASE 
                       WHEN ejercicio IS NULL THEN 0
                       WHEN typeof(CAST(ejercicio AS INTEGER)) = 'integer' THEN CAST(ejercicio AS INTEGER)
                       ELSE 0
                   END,
                   estadoAnimo, notas, registradoEn, syncId, isSynced, lastSyncTimestamp
            FROM registros_salud
        """.trimIndent())

        database.execSQL("DROP TABLE registros_salud")
        database.execSQL("ALTER TABLE registros_salud_new RENAME TO registros_salud")
        database.execSQL("CREATE INDEX idx_registros_salud_usuarioId ON registros_salud(usuarioId)")
    }
}
