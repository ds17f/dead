package com.deadarchive.core.network.di

import com.deadarchive.core.network.ArchiveApiService
import com.deadarchive.core.network.interceptor.ArchiveInterceptor
import com.deadarchive.core.network.interceptor.ErrorHandlingInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    /**
     * Provides JSON serializer configuration for API responses
     */
    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
        encodeDefaults = true
    }
    
    /**
     * Provides HTTP logging interceptor for debugging API calls
     */
    @Provides
    @Singleton
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor { message ->
            println("🌐 API_CAPTURE: $message")
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }
    
    /**
     * Provides Archive.org specific interceptor for request headers and rate limiting
     */
    @Provides
    @Singleton
    fun provideArchiveInterceptor(): ArchiveInterceptor {
        return ArchiveInterceptor()
    }
    
    /**
     * Provides error handling interceptor for API error responses
     */
    @Provides
    @Singleton
    fun provideErrorHandlingInterceptor(): ErrorHandlingInterceptor {
        return ErrorHandlingInterceptor()
    }
    
    /**
     * Provides configured OkHttpClient with interceptors and timeouts
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        archiveInterceptor: ArchiveInterceptor,
        errorHandlingInterceptor: ErrorHandlingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(archiveInterceptor)
            .addInterceptor(errorHandlingInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
    
    /**
     * Provides Retrofit instance configured for Archive.org API
     */
    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        json: Json
    ): Retrofit {
        val contentType = "application/json".toMediaType()
        
        return Retrofit.Builder()
            .baseUrl("https://archive.org/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }
    
    /**
     * Provides Archive.org API service implementation
     */
    @Provides
    @Singleton
    fun provideArchiveApiService(
        retrofit: Retrofit
    ): ArchiveApiService {
        return retrofit.create(ArchiveApiService::class.java)
    }
}