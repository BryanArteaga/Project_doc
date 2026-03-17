package com.example.doc.di

import android.content.SharedPreferences
import com.example.doc.data.daos.ConsejoDao
import com.example.doc.data.daos.EjercicioDao
import com.example.doc.data.daos.RegistroSaludDao
import com.example.doc.data.daos.AsignacionEjercicioDao
import com.example.doc.network.ApiService
import com.example.doc.repository.ConsejoRepository
import com.example.doc.repository.EjercicioRepository
import com.example.doc.repository.RegistroSaludRepository
import com.example.doc.repository.UsuarioRepository
import com.example.doc.repository.EjercicioRealizadoRepository
import com.example.doc.repository.SyncRepository
import com.example.doc.data.daos.EjercicioRealizadoDao
import com.example.doc.data.daos.UsuarioDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
@Suppress("unused")
object RepositoryModule {

    @Provides
    @Singleton
    fun provideUsuarioRepository(usuarioDao: UsuarioDao): UsuarioRepository {
        return UsuarioRepository(usuarioDao)
    }

    @Provides
    @Singleton
    fun provideRegistroSaludRepository(registroSaludDao: RegistroSaludDao): RegistroSaludRepository {
        return RegistroSaludRepository(registroSaludDao)
    }

    @Provides
    @Singleton
    fun provideEjercicioRepository(
        ejercicioDao: EjercicioDao,
        asignacionEjercicioDao: AsignacionEjercicioDao
    ): EjercicioRepository {
        return EjercicioRepository(ejercicioDao, asignacionEjercicioDao)
    }

    @Provides
    @Singleton
    fun provideConsejoRepository(consejoDao: ConsejoDao): ConsejoRepository {
        return ConsejoRepository(consejoDao)
    }

    @Provides
    @Singleton
    fun provideEjercicioRealizadoRepository(ejercicioRealizadoDao: EjercicioRealizadoDao): EjercicioRealizadoRepository {
        return EjercicioRealizadoRepository(ejercicioRealizadoDao)
    }

    @Provides
    @Singleton
    fun provideSyncRepository(
        apiService: ApiService,
        registroSaludDao: RegistroSaludDao,
        ejercicioRealizadoDao: EjercicioRealizadoDao,
        asignacionEjercicioDao: AsignacionEjercicioDao,
        usuarioDao: UsuarioDao,
        ejercicioDao: EjercicioDao,
        consejoDao: ConsejoDao,
        sharedPreferences: SharedPreferences
    ): SyncRepository {
        return SyncRepository(
            apiService = apiService,
            registroSaludDao = registroSaludDao,
            ejercicioRealizadoDao = ejercicioRealizadoDao,
            asignacionEjercicioDao = asignacionEjercicioDao,
            usuarioDao = usuarioDao,
            ejercicioDao = ejercicioDao,
            consejoDao = consejoDao,
            sharedPreferences = sharedPreferences
        )
    }
}

