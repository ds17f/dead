package com.deadarchive.v2.core.network.github.di

import com.deadarchive.v2.core.network.github.api.GitHubReleasesApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class V2GitHub

@Module
@InstallIn(SingletonComponent::class)
object GitHubNetworkModule {
    
    @Provides
    @Singleton
    @V2GitHub
    fun provideV2OkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }
    
    @Provides
    @Singleton
    @V2GitHub
    fun provideV2Json(): Json {
        return Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
    }
    
    @Provides
    @Singleton
    @V2GitHub
    fun provideV2Retrofit(
        @V2GitHub okHttpClient: OkHttpClient,
        @V2GitHub json: Json
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }
    
    @Provides
    @Singleton
    fun provideGitHubReleasesApi(@V2GitHub retrofit: Retrofit): GitHubReleasesApi {
        return retrofit.create(GitHubReleasesApi::class.java)
    }
}