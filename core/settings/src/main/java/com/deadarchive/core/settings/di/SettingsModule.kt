package com.deadarchive.core.settings.di

import com.deadarchive.core.settings.data.SettingsRepository
import com.deadarchive.core.settings.data.SettingsRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dagger Hilt module for settings layer dependency injection.
 * 
 * This module provides settings repository implementation with proper singleton scoping
 * to ensure single instance across the application lifecycle.
 * 
 * Dependency relationships:
 * - SettingsRepository -> SettingsDataStore (DataStore Preferences wrapper)
 * 
 * The repository is scoped as @Singleton to:
 * - Maintain consistent state across ViewModels and UI components
 * - Enable proper reactive stream management for settings changes
 * - Reduce memory allocation overhead
 * - Ensure settings changes are immediately reflected throughout the app
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SettingsModule {
    
    /**
     * Binds SettingsRepository interface to its implementation.
     * Provides centralized settings management with DataStore persistence
     * and reactive updates across the application.
     */
    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        settingsRepositoryImpl: SettingsRepositoryImpl
    ): SettingsRepository
}