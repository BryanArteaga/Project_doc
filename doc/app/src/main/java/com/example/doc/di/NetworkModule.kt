package com.example.doc.di

import android.content.SharedPreferences
import com.example.doc.network.ApiService
import com.example.doc.utils.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton
import java.util.concurrent.TimeUnit
import com.google.gson.GsonBuilder
import com.example.doc.network.LongTimestampDeserializer

@Module
@InstallIn(SingletonComponent::class)
@Suppress("unused")
object NetworkModule {

    private const val CONNECT_TIMEOUT = 15L  // FIX: Reducido de 30s (evita ANR + UX)
    private const val READ_TIMEOUT = 15L     // FIX: Reducido de 30s
    private const val WRITE_TIMEOUT = 15L    // FIX: Reducido de 30s

    /**
     * Interceptor para agregar headers a todas las requests
     * 
     * NOTA: Por ahora no hay autenticación JWT en la app.
     * En producción, se debe agregar Bearer token aquí.
     */
    @Provides
    @Singleton
    fun provideAuthInterceptor(
        sharedPreferences: SharedPreferences
    ): Interceptor {
        return Interceptor { chain ->
            val token = sharedPreferences.getString("jwt_token", null)
            val originalRequest = chain.request()
            
            val requestBuilder = originalRequest.newBuilder()
            
            // Agregar Authorization header si existe token
            if (!token.isNullOrEmpty()) {
                requestBuilder.addHeader("Authorization", "Bearer $token")
            }
            
            requestBuilder.addHeader("Content-Type", "application/json")
            
            chain.proceed(requestBuilder.build())
        }
    }

    /**
     * Logging Interceptor para debuguear request/response
     */
    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    /**
     * OkHttpClient configurado
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: Interceptor,
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Retrofit Instance con Gson configurado para Android 7 compatibility
     */
    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient
    ): Retrofit {
        val gson = GsonBuilder()
            .registerTypeAdapter(Long::class.java, LongTimestampDeserializer())
            .create()
        
        return Retrofit.Builder()
            .baseUrl(Constants.API_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(okHttpClient)
            .build()
    }

    /**
     * ApiService para hacer llamadas a la API
     */
    @Provides
    @Singleton
    fun provideApiService(
        retrofit: Retrofit
    ): ApiService {
        return retrofit.create(ApiService::class.java)
    }
}
