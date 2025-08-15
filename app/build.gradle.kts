import java.util.Properties
import java.io.ByteArrayOutputStream

// Function to get git commit hash (configuration cache friendly)
fun getGitCommitHash(): Provider<String> {
    return try {
        providers.exec {
            commandLine("git", "rev-parse", "--short", "HEAD")
            isIgnoreExitValue = true
        }.standardOutput.asText.map { it.trim() }.orElse("worktree-build")
    } catch (e: Exception) {
        providers.provider { "worktree-build" }
    }
}

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("kotlin-kapt")
}

android {
    namespace = "com.deadarchive.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.deadarchive.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 60
        versionName = "0.45.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        
        // Add build config fields for version information
        buildConfigField("String", "VERSION_NAME", "\"${versionName}\"")
        buildConfigField("int", "VERSION_CODE", "${versionCode}")
        buildConfigField("long", "BUILD_TIME", "${System.currentTimeMillis()}L")
    }

    signingConfigs {
        create("release") {
            // Use standard properties file approach
            val signingPropsFile = rootProject.file("signing.properties")
            if (signingPropsFile.exists()) {
                val signingProps = Properties()
                signingProps.load(signingPropsFile.inputStream())
                
                storeFile = file(signingProps["storeFile"] as String)
                storePassword = signingProps["storePassword"] as String
                keyAlias = signingProps["keyAlias"] as String
                keyPassword = signingProps["keyPassword"] as String
                
                println("✅ Release signing configuration loaded from signing.properties")
            } else {
                println("⚠️ signing.properties not found - release builds will be unsigned")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            isDebuggable = true
            isMinifyEnabled = false
            
            // Add build type and git commit hash for debug builds
            buildConfigField("String", "BUILD_TYPE", "\"debug\"")
            buildConfigField("String", "GIT_COMMIT_HASH", "\"${getGitCommitHash().get()}\"")
        }
        
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            
            // Use release signing if available
            signingConfig = signingConfigs.getByName("release")
            
            // Add build type and no commit hash for release builds
            buildConfigField("String", "BUILD_TYPE", "\"release\"")
            buildConfigField("String", "GIT_COMMIT_HASH", "\"\"")
            
            // Optional: different app name for release
            // manifestPlaceholders["appName"] = "Dead Archive"
        }
        
        create("benchmark") {
            initWith(buildTypes.getByName("release"))
            matchingFallbacks += listOf("release")
            isDebuggable = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = rootProject.extra["compose_compiler_version"] as String
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(project(":app-v2"))
    implementation(project(":core:data-api"))
    implementation(project(":core:design"))
    implementation(project(":core:common"))
    implementation(project(":core:model"))
    implementation(project(":core:data"))
    implementation(project(":core:database"))
    implementation(project(":core:database-v2"))
    implementation(project(":core:network"))
    implementation(project(":core:media"))
    implementation(project(":core:settings"))
    implementation(project(":core:settings-api"))
    implementation(project(":core:backup"))
    implementation(project(":feature:browse"))
    implementation(project(":feature:player"))
    implementation(project(":feature:playlist"))
    implementation(project(":feature:library"))

    // Core Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.appcompat:appcompat:1.6.1")

    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.05.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Media3 (for annotations)
    implementation("androidx.media3:media3-common:1.3.1")
    
    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.hilt:hilt-work:1.1.0")
    
    // Hilt
    implementation("com.google.dagger:hilt-android:2.51.1")
    kapt("com.google.dagger:hilt-compiler:2.51.1")
    kapt("androidx.hilt:hilt-compiler:1.1.0")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.work:work-testing:2.9.0")
    testImplementation("org.robolectric:robolectric:4.11.1")
    testImplementation("com.google.dagger:hilt-android-testing:2.51.1")
    testImplementation("com.google.truth:truth:1.4.2")
    kaptTest("com.google.dagger:hilt-compiler:2.51.1")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.05.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

kapt {
    correctErrorTypes = true
}
