package com.deadly.core.data

import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import javax.inject.Inject
import com.google.common.truth.Truth.assertThat

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class WorkManagerConfigurationTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var context: Context

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Before
    fun setup() {
        hiltRule.inject()
        
        // Initialize WorkManager for testing
        val config = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMaxSchedulerLimit(20)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
            
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
    }

    @Test
    fun `WorkManager should be initialized with custom configuration`() {
        val workManager = WorkManager.getInstance(context)
        
        // Verify WorkManager is initialized
        assertThat(workManager).isNotNull()
    }

    @Test
    fun `HiltWorkerFactory should be injected successfully`() {
        // Verify HiltWorkerFactory is injected
        assertThat(workerFactory).isNotNull()
    }
}