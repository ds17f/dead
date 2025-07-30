package com.deadarchive.core.data.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.util.Log
import com.deadarchive.core.model.AppUpdate
import com.deadarchive.core.model.UpdateDownloadState
import com.deadarchive.core.model.UpdateStatus
import com.deadarchive.core.network.GitHubApiService
import com.deadarchive.core.settings.api.SettingsRepository
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
    private val okHttpClient: OkHttpClient
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
                
                if (!apkFile.exists()) {
                    return@withContext Result.failure(Exception("APK file not found"))
                }
                
                val packageInstaller = context.packageManager.packageInstaller
                val params = PackageInstaller.SessionParams(
                    PackageInstaller.SessionParams.MODE_FULL_INSTALL
                )
                
                val sessionId = packageInstaller.createSession(params)
                val session = packageInstaller.openSession(sessionId)
                
                try {
                    session.openWrite("package", 0, -1).use { output ->
                        apkFile.inputStream().use { input ->
                            input.copyTo(output)
                        }
                    }
                    
                    // Create install intent
                    val intent = Intent(context, UpdateInstallReceiver::class.java)
                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        INSTALL_REQUEST_CODE,
                        intent,
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        } else {
                            PendingIntent.FLAG_UPDATE_CURRENT
                        }
                    )
                    
                    Log.d(TAG, "Committing installation session")
                    session.commit(pendingIntent.intentSender)
                    
                    Result.success(Unit)
                    
                } finally {
                    session.close()
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error installing update", e)
                Result.failure(e)
            }
        }
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
    
    private fun getCurrentAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "0.0.0"
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current app version", e)
            "0.0.0"
        }
    }
    
    private fun extractVersionCodeFromRelease(release: com.deadarchive.core.network.model.GitHubRelease): Int {
        // Try to extract version code from tag or body
        // For now, just return 0 as we primarily use version name for comparison
        return 0
    }
}

/**
 * Broadcast receiver for handling PackageInstaller results.
 * This would need to be implemented as a separate class and registered in AndroidManifest.xml
 */
class UpdateInstallReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        when (status) {
            PackageInstaller.STATUS_SUCCESS -> {
                Log.d("UpdateInstallReceiver", "Installation successful")
                // Could show notification or trigger app restart
            }
            PackageInstaller.STATUS_FAILURE,
            PackageInstaller.STATUS_FAILURE_ABORTED,
            PackageInstaller.STATUS_FAILURE_BLOCKED,
            PackageInstaller.STATUS_FAILURE_CONFLICT,
            PackageInstaller.STATUS_FAILURE_INCOMPATIBLE,
            PackageInstaller.STATUS_FAILURE_INVALID,
            PackageInstaller.STATUS_FAILURE_STORAGE -> {
                val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                Log.e("UpdateInstallReceiver", "Installation failed: $message")
                // Could show error notification
            }
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                Log.d("UpdateInstallReceiver", "Installation pending user action")
                // User confirmation required - this is expected for non-system apps
            }
        }
    }
}