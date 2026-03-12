package com.example.doc.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.doc.data.daos.*
import com.example.doc.data.entities.*
import com.example.doc.data.migrations.MIGRATION_18_19
import com.example.doc.data.migrations.MIGRATION_19_20
import com.example.doc.data.migrations.MIGRATION_20_21
import com.example.doc.data.migrations.MIGRATION_21_22
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

@Database(
    entities = [
        UsuarioEntity::class,
        RegistroSaludEntity::class,
        EjercicioEntity::class,
        EjercicioRealizadoEntity::class,
        ConsejoEntity::class,
        AsignacionEjercicioEntity::class
    ],
    version = 23,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun usuarioDao(): UsuarioDao
    abstract fun registroSaludDao(): RegistroSaludDao
    abstract fun ejercicioDao(): EjercicioDao
    abstract fun ejercicioRealizadoDao(): EjercicioRealizadoDao
    abstract fun consejoDao(): ConsejoDao
    abstract fun asignacionEjercicioDao(): AsignacionEjercicioDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "medicini_db"
                )
                    .addCallback(AppDatabaseCallback(context, scope))
                    .addMigrations(MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21, MIGRATION_21_22)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}