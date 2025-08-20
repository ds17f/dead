package com.deadly.core.data.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.deadly.core.model.AppUpdate
import com.deadly.core.model.UpdateDownloadState
import com.deadly.core.model.UpdateStatus
import com.deadly.core.model.UpdateInstallationState
import com.deadly.core.network.GitHubApiService
import com.deadly.core.settings.api.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of UpdateService for app update functionality.
 * 
 * Integrates with GitHub API to check for releases and download APK files.
 * Uses PackageInstaller for installation and SettingsRepository for preferences.
 */
@Singleton
class UpdateServiceImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gitHubApiService: GitHubApiService,
    private val settingsRepository: SettingsRepository,
    private val okHttpClient: OkHttpClient,
    private val installationStateManager: InstallationStateManager
) : UpdateService {
    
    companion object {
        private const val TAG = "UpdateServiceImpl"
        private const val REPO_OWNER = "ds17f"
        private const val REPO_NAME = "dead"
        private const val INSTALL_REQUEST_CODE = 1001
    }
    
    private val _downloadStates = MutableStateFlow<Map<String, UpdateDownloadState>>(emptyMap())
    
    override suspend fun checkForUpdates(): Result<UpdateStatus> {
        return withContext(Dispatchers.IO) {
            try {
                val currentVersion = getCurrentAppVersion()
                Log.d(TAG, "🔍 Starting update check")
                Log.d(TAG, "📱 Current app version: $currentVersion")
                Log.d(TAG, "🌐 Checking repository: https://github.com/$REPO_OWNER/$REPO_NAME")
                
                val response = gitHubApiService.getLatestRelease(REPO_OWNER, REPO_NAME)
                if (!response.isSuccessful) {
                    val error = "GitHub API error: ${response.code()} ${response.message()}"
                    Log.e(TAG, "❌ $error")
                    return@withContext Result.failure(Exception(error))
                }
                
                val release = response.body()
                if (release == null) {
                    Log.e(TAG, "❌ Empty response from GitHub API")
                    return@withContext Result.failure(Exception("Empty response from GitHub API"))
                }
                
                Log.d(TAG, "✅ Found release: ${release.tagName} (${release.name})")
                Log.d(TAG, "📅 Published: ${release.publishedAt}")
                Log.d(TAG, "🏷️ Pre-release: ${release.prerelease}, Draft: ${release.draft}")
                
                // Skip pre-releases and drafts
                if (release.prerelease || release.draft) {
                    Log.d(TAG, "⏭️ Skipping pre-release or draft")
                    return@withContext Result.success(
                        UpdateStatus.noUpdateAvailable(System.currentTimeMillis())
                    )
                }
                
                // Find APK asset
                Log.d(TAG, "🔍 Looking for APK in ${release.assets.size} assets...")
                release.assets.forEach { asset ->
                    Log.d(TAG, "📦 Asset: ${asset.name} (${asset.formattedSize})")
                }
                
                val apkAsset = release.assets.find { it.isApk }
                if (apkAsset == null) {
                    Log.d(TAG, "❌ No APK found in release assets")
                    return@withContext Result.success(
                        UpdateStatus.noUpdateAvailable(System.currentTimeMillis())
                    )
                }
                
                Log.d(TAG, "✅ Found APK asset: ${apkAsset.name} (${apkAsset.formattedSize})")
                Log.d(TAG, "🔗 Download URL: ${apkAsset.browserDownloadUrl}")
                
                // Extract version from tag (remove 'v' prefix if present)
                val latestVersion = release.tagName.removePrefix("v")
                
                Log.d(TAG, "🔢 Version comparison:")
                Log.d(TAG, "   Current: $currentVersion")
                Log.d(TAG, "   Latest:  $latestVersion")
                
                val update = AppUpdate(
                    version = latestVersion,
                    versionCode = extractVersionCodeFromRelease(release),
                    downloadUrl = apkAsset.browserDownloadUrl,
                    releaseNotes = release.body,
                    publishedAt = release.publishedAt,
                    assetName = apkAsset.name
                )
                
                val isNewer = update.isNewerThan(currentVersion)
                val isSkipped = isVersionSkipped(latestVersion)
                
                Log.d(TAG, "📊 Update check result:")
                Log.d(TAG, "   Is newer: $isNewer")
                Log.d(TAG, "   Is skipped: $isSkipped")
                
                // Update last check timestamp
                settingsRepository.updateLastUpdateCheck(System.currentTimeMillis())
                
                if (isNewer) {
                    Log.d(TAG, "🎉 Update available: $latestVersion")
                    Result.success(UpdateStatus.updateAvailable(update, isSkipped, System.currentTimeMillis()))
                } else {
                    Log.d(TAG, "✅ App is up to date")
                    Result.success(UpdateStatus.noUpdateAvailable(System.currentTimeMillis()))
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error checking for updates", e)
                Result.failure(e)
            }
        }
    }
    
    override suspend fun downloadUpdate(update: AppUpdate): Result<File> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting download: ${update.assetName}")
                
                // Update download state to downloading
                updateDownloadState(update.version, UpdateDownloadState(isDownloading = true))
                
                val request = Request.Builder()
                    .url(update.downloadUrl)
                    .build()
                
                val response = okHttpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    val error = "Download failed: ${response.code} ${response.message}"
                    Log.e(TAG, error)
                    updateDownloadState(update.version, UpdateDownloadState(error = error))
                    return@withContext Result.failure(Exception(error))
                }
                
                val body = response.body
                if (body == null) {
                    val error = "Empty response body"
                    Log.e(TAG, error)
                    updateDownloadState(update.version, UpdateDownloadState(error = error))
                    return@withContext Result.failure(Exception(error))
                }
                
                val totalBytes = body.contentLength()
                val downloadDir = File(context.cacheDir, "updates")
                if (!downloadDir.exists()) {
                    downloadDir.mkdirs()
                }
                
                val apkFile = File(downloadDir, update.assetName)
                var downloadedBytes = 0L
                
                body.byteStream().use { inputStream ->
                    FileOutputStream(apkFile).use { outputStream ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        
                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead
                            
                            val progress = if (totalBytes > 0) {
                                downloadedBytes.toFloat() / totalBytes.toFloat()
                            } else {
                                0f
                            }
                            
                            updateDownloadState(
                                update.version,
                                UpdateDownloadState(
                                    isDownloading = true,
                                    progress = progress,
                                    downloadedBytes = downloadedBytes,
                                    totalBytes = totalBytes
                                )
                            )
                        }
                    }
                }
                
                Log.d(TAG, "Download completed: ${apkFile.absolutePath}")
                
                // Update download state to completed
                updateDownloadState(
                    update.version,
                    UpdateDownloadState(
                        isDownloading = false,
                        progress = 1f,
                        downloadedBytes = downloadedBytes,
                        totalBytes = totalBytes,
                        downloadedFile = apkFile.absolutePath
                    )
                )
                
                Result.success(apkFile)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error downloading update", e)
                updateDownloadState(update.version, UpdateDownloadState(error = e.message))
                Result.failure(e)
            }
        }
    }
    
    override fun getDownloadProgress(update: AppUpdate): Flow<UpdateDownloadState> {
        return _downloadStates.asStateFlow().map { states ->
            states[update.version] ?: UpdateDownloadState()
        }
    }
    
    override suspend fun installUpdate(apkFile: File): Result<Unit> {
        return withContext(Dispatchers.Main) {
            try {
                Log.d(TAG, "Installing update: ${apkFile.absolutePath}")
                
                // Detailed file validation and logging
                Log.d(TAG, "📁 File validation:")
                Log.d(TAG, "  Absolute path: ${apkFile.absolutePath}")
                Log.d(TAG, "  File exists: ${apkFile.exists()}")
                Log.d(TAG, "  File size: ${if (apkFile.exists()) apkFile.length() else "N/A"} bytes")
                Log.d(TAG, "  File readable: ${apkFile.canRead()}")
                Log.d(TAG, "  Parent directory: ${apkFile.parent}")
                Log.d(TAG, "  Parent exists: ${apkFile.parentFile?.exists()}")
                
                // Update installation state to installing
                installationStateManager.updateState(UpdateInstallationState(
                    isInstalling = true,
                    statusMessage = "Validating APK file at ${apkFile.absolutePath}..."
                ))
                
                // Check if we can install packages (Android 8.0+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (!context.packageManager.canRequestPackageInstalls()) {
                        Log.w(TAG, "App does not have permission to install packages, opening settings")
                        
                        // Open the "Install from unknown sources" settings screen
                        try {
                            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                                data = Uri.parse("package:${context.packageName}")
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                            
                            installationStateManager.updateState(UpdateInstallationState(
                                isError = true,
                                errorMessage = "Please enable 'Install from unknown sources' for Dead Archive in the settings that just opened, then try installing the update again."
                            ))
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to open unknown sources settings", e)
                            installationStateManager.updateState(UpdateInstallationState(
                                isError = true,
                                errorMessage = "Please enable 'Install from unknown sources' in Settings > Apps > Dead Archive > Advanced > Install unknown apps."
                            ))
                        }
                        
                        return@withContext Result.failure(Exception("Permission required"))
                    }
                }
                
                if (!apkFile.exists()) {
                    val error = "APK file not found: ${apkFile.absolutePath}"
                    Log.e(TAG, error)
                    installationStateManager.updateState(UpdateInstallationState(
                        isError = true,
                        errorMessage = error
                    ))
                    return@withContext Result.failure(Exception(error))
                }
                
                if (!apkFile.canRead()) {
                    val error = "Cannot read APK file: ${apkFile.absolutePath}"
                    Log.e(TAG, error)
                    installationStateManager.updateState(UpdateInstallationState(
                        isError = true,
                        errorMessage = error
                    ))
                    return@withContext Result.failure(Exception(error))
                }
                
                // Focus on making system installer work properly
                return@withContext installWithSystemUI(apkFile)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error installing update", e)
                installationStateManager.updateState(UpdateInstallationState(
                    isError = true,
                    errorMessage = "Installation failed: ${e.message}"
                ))
                Result.failure(e)
            }
        }
    }
    
    override fun getInstallationStatus(): Flow<UpdateInstallationState> {
        return installationStateManager.installationState
    }
    
    override suspend fun skipUpdate(version: String) {
        Log.d(TAG, "Skipping version: $version")
        settingsRepository.addSkippedVersion(version)
    }
    
    override fun getSkippedVersions(): Flow<Set<String>> {
        return settingsRepository.getSkippedVersions()
    }
    
    override suspend fun isVersionSkipped(version: String): Boolean {
        return settingsRepository.getSkippedVersions().first().contains(version)
    }
    
    override suspend fun clearSkippedVersions() {
        Log.d(TAG, "Clearing all skipped versions")
        settingsRepository.clearSkippedVersions()
    }
    
    override fun getLastUpdateCheck(): Flow<Long> {
        return settingsRepository.getLastUpdateCheck()
    }
    
    override fun isAutoUpdateCheckEnabled(): Flow<Boolean> {
        return settingsRepository.isAutoUpdateCheckEnabled()
    }
    
    override suspend fun setAutoUpdateCheckEnabled(enabled: Boolean) {
        Log.d(TAG, "Setting auto update check enabled: $enabled")
        settingsRepository.setAutoUpdateCheckEnabled(enabled)
    }
    
    private fun updateDownloadState(version: String, state: UpdateDownloadState) {
        _downloadStates.value = _downloadStates.value.toMutableMap().apply {
            put(version, state)
        }
    }
    
    private suspend fun installWithSystemUI(apkFile: File): Result<Unit> {
        return withContext(Dispatchers.Main) {
            try {
                Log.d(TAG, "🚀 Attempting system installer UI for: ${apkFile.absolutePath}")
                
                // Check permissions first
                Log.d(TAG, "🔐 Permission checks:")
                Log.d(TAG, "  Android version: ${Build.VERSION.SDK_INT}")
                Log.d(TAG, "  Can request package installs: ${if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.packageManager.canRequestPackageInstalls() else "N/A (< API 26)"}")
                
                // For Android 8.0+, ensure we can install packages
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (!context.packageManager.canRequestPackageInstalls()) {
                        Log.w(TAG, "❌ Cannot install packages - opening settings")
                        // Open settings instead of failing
                        try {
                            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                                data = Uri.parse("package:${context.packageName}")
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                            Log.d(TAG, "Opened unknown sources settings")
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to open unknown sources settings", e)
                        }
                        
                        installationStateManager.updateState(UpdateInstallationState(
                            isError = true,
                            errorMessage = "Please enable 'Install from unknown sources' for Dead Archive in the settings that opened, then try installing again."
                        ))
                        return@withContext Result.failure(SecurityException("Need REQUEST_INSTALL_PACKAGES permission"))
                    }
                }
                
                // Use FileProvider to create content:// URI for better compatibility
                val uri = try {
                    androidx.core.content.FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        apkFile
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "FileProvider failed, falling back to file:// URI", e)
                    android.net.Uri.fromFile(apkFile)
                }
                Log.d(TAG, "📁 Using URI: $uri")
                
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                
                Log.d(TAG, "🎯 Intent details:")
                Log.d(TAG, "  Action: ${intent.action}")
                Log.d(TAG, "  Data: ${intent.data}")
                Log.d(TAG, "  Type: ${intent.type}")
                Log.d(TAG, "  Flags: ${intent.flags}")
                
                // Check if any app can handle this intent
                val resolveInfos = context.packageManager.queryIntentActivities(intent, 0)
                Log.d(TAG, "📱 Available handlers: ${resolveInfos.size}")
                resolveInfos.forEach { resolveInfo ->
                    Log.d(TAG, "  - ${resolveInfo.activityInfo.packageName}/${resolveInfo.activityInfo.name}")
                }
                
                if (resolveInfos.isEmpty()) {
                    throw Exception("No app can handle APK installation intent")
                }
                
                installationStateManager.updateState(UpdateInstallationState(
                    isInstalling = true,
                    statusMessage = "Launching system installer..."
                ))
                
                context.startActivity(intent)
                Log.d(TAG, "✅ System installer intent launched successfully")
                
                installationStateManager.updateState(UpdateInstallationState(
                    isInstalling = true,
                    statusMessage = "System installer launched. Please follow the prompts to complete installation."
                ))
                
                Result.success(Unit)
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ System installer failed: ${e.message}", e)
                installationStateManager.updateState(UpdateInstallationState(
                    isError = true,
                    errorMessage = "Failed to launch system installer: ${e.message}"
                ))
                Result.failure(e)
            }
        }
    }
    
    private fun getCurrentAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "0.0.0"
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current app version", e)
            "0.0.0"
        }
    }
    
    private fun extractVersionCodeFromRelease(release: com.deadly.core.network.model.GitHubRelease): Int {
        // Try to extract version code from tag or body
        // For now, just return 0 as we primarily use version name for comparison
        return 0
    }
}

/**
 * Broadcast receiver for handling PackageInstaller results.
 * Registered in AndroidManifest.xml to receive installation status callbacks.
 */
class UpdateInstallReceiver : android.content.BroadcastReceiver() {
    
    companion object {
        private const val TAG = "UpdateInstallReceiver"
        private const val NOTIFICATION_ID = 1002
        private const val CHANNEL_ID = "app_updates"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "🔔 BroadcastReceiver triggered!")
        Log.d(TAG, "Intent action: ${intent.action}")
        Log.d(TAG, "Intent extras: ${intent.extras?.keySet()?.joinToString(", ")}")
        
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
        val otherPackageName = intent.getStringExtra(PackageInstaller.EXTRA_OTHER_PACKAGE_NAME)
        val sessionId = intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1)
        val legacyStatus = intent.getStringExtra("android.content.pm.extra.LEGACY_STATUS")
        
        Log.d(TAG, "📦 Installation callback details:")
        Log.d(TAG, "  Status: $status")
        Log.d(TAG, "  Message: $message")
        Log.d(TAG, "  Other package name: $otherPackageName")
        Log.d(TAG, "  Session ID: $sessionId")
        Log.d(TAG, "  Legacy status: $legacyStatus")
        
        // Log all extras for debugging
        intent.extras?.let { extras ->
            Log.d(TAG, "  All extras:")
            for (key in extras.keySet()) {
                try {
                    val value = extras.get(key)
                    Log.d(TAG, "    $key = $value (${value?.javaClass?.simpleName})")
                } catch (e: Exception) {
                    Log.d(TAG, "    $key = <error reading value>")
                }
            }
        }
        
        // Check if the status extra actually exists
        val hasStatusExtra = intent.hasExtra(PackageInstaller.EXTRA_STATUS)
        Log.d(TAG, "Intent has STATUS extra: $hasStatusExtra")
        
        // If no STATUS extra, we might still have a valid status from getIntExtra with the default
        // Let's process it anyway if it's a known status code
        Log.d(TAG, "Processing status: $status (hasExtra: $hasStatusExtra)")
        Log.d(TAG, "Status constants for reference:")
        Log.d(TAG, "  STATUS_SUCCESS = ${PackageInstaller.STATUS_SUCCESS}")
        Log.d(TAG, "  STATUS_FAILURE = ${PackageInstaller.STATUS_FAILURE}")
        Log.d(TAG, "  STATUS_PENDING_USER_ACTION = ${PackageInstaller.STATUS_PENDING_USER_ACTION}")
        Log.d(TAG, "  STATUS_FAILURE_ABORTED = ${PackageInstaller.STATUS_FAILURE_ABORTED}")
        Log.d(TAG, "  STATUS_FAILURE_BLOCKED = ${PackageInstaller.STATUS_FAILURE_BLOCKED}")
        Log.d(TAG, "  STATUS_FAILURE_CONFLICT = ${PackageInstaller.STATUS_FAILURE_CONFLICT}")
        Log.d(TAG, "  STATUS_FAILURE_INCOMPATIBLE = ${PackageInstaller.STATUS_FAILURE_INCOMPATIBLE}")
        Log.d(TAG, "  STATUS_FAILURE_INVALID = ${PackageInstaller.STATUS_FAILURE_INVALID}")
        Log.d(TAG, "  STATUS_FAILURE_STORAGE = ${PackageInstaller.STATUS_FAILURE_STORAGE}")
        
        // Get the installation state manager from the application context
        val stateManager = try {
            val applicationContext = context.applicationContext
            dagger.hilt.EntryPoints.get(applicationContext, InstallationStateManagerEntryPoint::class.java).installationStateManager()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get InstallationStateManager", e)
            null
        }
        
        when (status) {
            PackageInstaller.STATUS_SUCCESS -> {
                Log.d(TAG, "✅ Installation successful")
                stateManager?.updateState(UpdateInstallationState(
                    isSuccess = true,
                    statusMessage = "Update installed successfully! Please restart the app to use the new version."
                ))
                showNotification(
                    context, 
                    "Update Installed", 
                    "Dead Archive has been updated successfully. Restart the app to use the new version.",
                    isSuccess = true
                )
            }
            
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                Log.d(TAG, "⏳ Installation pending user action")
                stateManager?.updateState(UpdateInstallationState(
                    isInstalling = true,
                    statusMessage = "Waiting for user approval... Check your notifications or recent apps for the installation prompt."
                ))
                
                val confirmIntent = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                if (confirmIntent != null) {
                    try {
                        confirmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(confirmIntent)
                        Log.d(TAG, "Started confirmation activity")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to start confirmation activity", e)
                        stateManager?.updateState(UpdateInstallationState(
                            isError = true,
                            errorMessage = "Please check your notifications or recent apps for the installation prompt, or enable 'Install from unknown sources' in Settings."
                        ))
                        showNotification(
                            context,
                            "Update Installation",
                            "Please check your notifications or recent apps for the installation prompt, or enable 'Install from unknown sources' in Settings.",
                            isSuccess = false
                        )
                    }
                } else {
                    Log.w(TAG, "No confirmation intent provided - user needs to manually approve")
                    stateManager?.updateState(UpdateInstallationState(
                        isInstalling = true,
                        statusMessage = "Please check your notifications, recent apps, or system dialogs for the installation approval prompt."
                    ))
                    showNotification(
                        context,
                        "Update Installation",
                        "Please check your notifications, recent apps, or system dialogs for the installation approval prompt.",
                        isSuccess = false
                    )
                }
            }
            
            PackageInstaller.STATUS_FAILURE_ABORTED -> {
                Log.e(TAG, "❌ Installation aborted by user")
                stateManager?.updateState(UpdateInstallationState(
                    isError = true,
                    errorMessage = "Installation was cancelled by user. You can try installing the update again."
                ))
                showNotification(
                    context,
                    "Update Cancelled",
                    "App update was cancelled. You can try again from Settings.",
                    isSuccess = false
                )
            }
            
            PackageInstaller.STATUS_FAILURE_BLOCKED -> {
                Log.e(TAG, "❌ Installation blocked")
                
                // Try to open settings automatically
                openUnknownSourcesSettings(context)
                
                stateManager?.updateState(UpdateInstallationState(
                    isError = true,
                    errorMessage = "Installation was blocked. Please enable 'Install from unknown sources' for Dead Archive in the settings that opened, then try installing again."
                ))
                showNotification(
                    context,
                    "Update Blocked",
                    "Installation was blocked. The settings screen should have opened - please enable 'Install from unknown sources' for Dead Archive.",
                    isSuccess = false
                )
            }
            
            PackageInstaller.STATUS_FAILURE_CONFLICT -> {
                Log.e(TAG, "❌ Installation conflict: $message")
                stateManager?.updateState(UpdateInstallationState(
                    isError = true,
                    errorMessage = "Installation failed due to version conflict. Please try downloading the update again."
                ))
                showNotification(
                    context,
                    "Update Failed",
                    "Installation failed due to version conflict. Please try downloading the update again.",
                    isSuccess = false
                )
            }
            
            PackageInstaller.STATUS_FAILURE_INCOMPATIBLE -> {
                Log.e(TAG, "❌ Installation incompatible: $message")
                stateManager?.updateState(UpdateInstallationState(
                    isError = true,
                    errorMessage = "This update is not compatible with your device."
                ))
                showNotification(
                    context,
                    "Update Failed",
                    "This update is not compatible with your device.",
                    isSuccess = false
                )
            }
            
            PackageInstaller.STATUS_FAILURE_INVALID -> {
                Log.e(TAG, "❌ Installation invalid: $message")
                stateManager?.updateState(UpdateInstallationState(
                    isError = true,
                    errorMessage = "The update file is corrupted. Please try downloading again."
                ))
                showNotification(
                    context,
                    "Update Failed",
                    "The update file is corrupted. Please try downloading again.",
                    isSuccess = false
                )
            }
            
            PackageInstaller.STATUS_FAILURE_STORAGE -> {
                Log.e(TAG, "❌ Installation storage failure: $message")
                stateManager?.updateState(UpdateInstallationState(
                    isError = true,
                    errorMessage = "Not enough storage space to install the update. Please free up space and try again."
                ))
                showNotification(
                    context,
                    "Update Failed",
                    "Not enough storage space to install the update. Please free up space and try again.",
                    isSuccess = false
                )
            }
            
            else -> {
                Log.e(TAG, "❌ Installation failed with unknown status: $status, message: $message")
                stateManager?.updateState(UpdateInstallationState(
                    isError = true,
                    errorMessage = message ?: "Installation failed for unknown reason. Please try again."
                ))
                showNotification(
                    context,
                    "Update Failed",
                    message ?: "Installation failed for unknown reason. Please try again.",
                    isSuccess = false
                )
            }
        }
    }
    
    private fun showNotification(context: Context, title: String, content: String, isSuccess: Boolean) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            
            // Create notification channel for Android 8.0+
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val channel = android.app.NotificationChannel(
                    CHANNEL_ID,
                    "App Updates",
                    android.app.NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Notifications about app updates"
                }
                notificationManager.createNotificationChannel(channel)
            }
            
            val notification = android.app.Notification.Builder(context, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setAutoCancel(true)
                .setStyle(android.app.Notification.BigTextStyle().bigText(content))
                .build()
            
            notificationManager.notify(NOTIFICATION_ID, notification)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show notification", e)
        }
    }
    
    private fun openUnknownSourcesSettings(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                Log.d(TAG, "Opened unknown sources settings")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open unknown sources settings", e)
        }
    }
}

@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
@dagger.hilt.EntryPoint
interface InstallationStateManagerEntryPoint {
    fun installationStateManager(): InstallationStateManager
}