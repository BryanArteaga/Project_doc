package com.example.doc.di

import android.content.Context
import com.example.doc.security.HealthDataEncryption
import com.example.doc.security.KeyManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
@Suppress("unused")
object SecurityModule {

    @Provides
    @Singleton
    fun provideKeyManager(
        @ApplicationContext context: Context
    ): KeyManager {
        return KeyManager(context)
    }

    @Provides
    @Singleton
    fun provideHealthDataEncryption(
        keyManager: KeyManager
    ): HealthDataEncryption {
        return HealthDataEncryption(keyManager)
    }
}

