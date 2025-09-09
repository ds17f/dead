plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            // Coroutines
            implementation(libs.kotlin.coroutines.core)
            
            // Ktor HTTP Client
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            
            // SQLDelight
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines.extensions)
            
            // Koin DI
            implementation(libs.koin.core)
            
            // Compose Multiplatform
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            
            // Navigation
            implementation(libs.compose.navigation)
        }
        
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        
        androidMain.dependencies {
            // Android Coroutines
            implementation(libs.kotlin.coroutines.android)
            
            // Ktor Android
            implementation(libs.ktor.client.okhttp)
            
            // SQLDelight Android Driver
            implementation(libs.sqldelight.android.driver)
            
            // Koin Android
            implementation(libs.koin.android)
            implementation(libs.koin.compose)
        }
        
        iosMain.dependencies {
            // Ktor iOS
            implementation(libs.ktor.client.darwin)
            
            // SQLDelight iOS Driver
            implementation(libs.sqldelight.native.driver)
        }
    }
}

android {
    namespace = "com.deadarchive.kmm.shared"
    compileSdk = 35
    
    defaultConfig {
        minSdk = 24
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    buildFeatures {
        compose = true
    }
}

sqldelight {
    databases {
        create("DeadlyDatabase") {
            packageName.set("com.deadarchive.kmm.shared.database")
            srcDirs("src/commonMain/sqldelight")
        }
    }
}

compose.resources {
    publicResClass = false
    packageOfResClass = "com.deadarchive.kmm.shared.resources"
    generateResClass = auto
}