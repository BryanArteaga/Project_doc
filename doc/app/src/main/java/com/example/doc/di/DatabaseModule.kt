package com.example.doc.di

import android.content.Context
import androidx.room.Room
import com.example.doc.data.AppDatabase
import com.example.doc.data.AppDatabaseCallback
import com.example.doc.data.daos.AsignacionEjercicioDao
import com.example.doc.data.daos.ConsejoDao
import com.example.doc.data.daos.EjercicioDao
import com.example.doc.data.daos.EjercicioRealizadoDao
import com.example.doc.data.daos.RegistroSaludDao
import com.example.doc.data.daos.UsuarioDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
@Suppress("unused")
object DatabaseModule {

    @Provides
    @Singleton
    fun provideCoroutineScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
        scope: CoroutineScope
    ): AppDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "medicini_db"
        )
            .addCallback(AppDatabaseCallback(context, scope))
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideUsuarioDao(database: AppDatabase): UsuarioDao {
        return database.usuarioDao()
    }

    @Provides
    @Singleton
    fun provideRegistroSaludDao(database: AppDatabase): RegistroSaludDao {
        return database.registroSaludDao()
    }

    @Provides
    @Singleton
    fun provideEjercicioDao(database: AppDatabase): EjercicioDao {
        return database.ejercicioDao()
    }

    @Provides
    @Singleton
    fun provideConsejoDao(database: AppDatabase): ConsejoDao {
        return database.consejoDao()
    }

    @Provides
    @Singleton
    fun provideEjercicioRealizadoDao(database: AppDatabase): EjercicioRealizadoDao {
        return database.ejercicioRealizadoDao()
    }

    @Provides
    @Singleton
    fun provideAsignacionEjercicioDao(database: AppDatabase): AsignacionEjercicioDao {
        return database.asignacionEjercicioDao()
    }
}

