package com.deadarchive.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deadarchive.core.database.DownloadDao
import com.deadarchive.core.database.DownloadEntity
import com.deadarchive.core.database.FavoriteDao
import com.deadarchive.core.database.FavoriteEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class DatabaseTestViewModel @Inject constructor(
    private val downloadDao: DownloadDao,
    private val favoriteDao: FavoriteDao
) : ViewModel() {
    
    private val _downloads = MutableStateFlow<List<DownloadEntity>>(emptyList())
    val downloads: StateFlow<List<DownloadEntity>> = _downloads.asStateFlow()
    
    private val _favorites = MutableStateFlow<List<FavoriteEntity>>(emptyList())
    val favorites: StateFlow<List<FavoriteEntity>> = _favorites.asStateFlow()
    
    private val _message = MutableStateFlow("")
    val message: StateFlow<String> = _message.asStateFlow()
    
    init {
        loadData()
    }
    
    private fun loadData() {
        viewModelScope.launch {
            downloadDao.getAllDownloads().collect { _downloads.value = it }
        }
        viewModelScope.launch {
            favoriteDao.getAllFavorites().collect { _favorites.value = it }
        }
    }
    
    fun addTestDownload() {
        viewModelScope.launch {
            try {
                val testDownload = DownloadEntity(
                    id = "test-${System.currentTimeMillis()}",
                    recordingId = "gd1977-05-08.sbd.miller.97245.flac16",
                    trackFilename = "gd77-05-08d1t01.flac",
                    status = "QUEUED",
                    progress = 0f,
                    bytesDownloaded = 0L,
                    totalBytes = 45000000L,
                    localPath = null,
                    errorMessage = null,
                    startedTimestamp = System.currentTimeMillis(),
                    completedTimestamp = null
                )
                downloadDao.insertDownload(testDownload)
                _message.value = "Test download added successfully"
            } catch (e: Exception) {
                _message.value = "Error adding download: ${e.message}"
            }
        }
    }
    
    fun addTestFavorite() {
        viewModelScope.launch {
            try {
                val testFavorite = FavoriteEntity(
                    id = "fav-${System.currentTimeMillis()}",
                    type = "CONCERT",
                    recordingId = "gd1977-05-08.sbd.miller.97245.flac16",
                    trackFilename = null,
                    addedTimestamp = System.currentTimeMillis(),
                    notes = "Classic Cornell '77 show!"
                )
                favoriteDao.insertFavorite(testFavorite)
                _message.value = "Test favorite added successfully"
            } catch (e: Exception) {
                _message.value = "Error adding favorite: ${e.message}"
            }
        }
    }
    
    fun updateTestDownloadProgress() {
        viewModelScope.launch {
            try {
                val downloads = _downloads.value
                if (downloads.isNotEmpty()) {
                    val download = downloads.first()
                    downloadDao.updateDownloadProgress(
                        id = download.id,
                        progress = 0.75f,
                        bytesDownloaded = (download.totalBytes * 0.75).toLong()
                    )
                    _message.value = "Download progress updated to 75%"
                } else {
                    _message.value = "No downloads to update"
                }
            } catch (e: Exception) {
                _message.value = "Error updating progress: ${e.message}"
            }
        }
    }
    
    fun clearDownloads() {
        viewModelScope.launch {
            try {
                _downloads.value.forEach { downloadDao.deleteDownload(it) }
                _message.value = "All downloads cleared"
            } catch (e: Exception) {
                _message.value = "Error clearing downloads: ${e.message}"
            }
        }
    }
    
    fun clearFavorites() {
        viewModelScope.launch {
            try {
                _favorites.value.forEach { favoriteDao.deleteFavorite(it) }
                _message.value = "All favorites cleared"
            } catch (e: Exception) {
                _message.value = "Error clearing favorites: ${e.message}"
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatabaseTestScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: DatabaseTestViewModel = hiltViewModel()
) {
    val downloads by viewModel.downloads.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val message by viewModel.message.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onNavigateBack,
                modifier = Modifier.padding(end = 16.dp)
            ) {
                Text("← Back")
            }
            Text(
                text = "Database Test Screen",
                style = MaterialTheme.typography.headlineMedium
            )
        }
        
        if (message.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = message,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        // Test buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.addTestDownload() },
                modifier = Modifier.weight(1f)
            ) {
                Text("Add Download")
            }
            Button(
                onClick = { viewModel.addTestFavorite() },
                modifier = Modifier.weight(1f)
            ) {
                Text("Add Favorite")
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.updateTestDownloadProgress() },
                modifier = Modifier.weight(1f)
            ) {
                Text("Update Progress")
            }
            Button(
                onClick = { viewModel.clearDownloads() },
                modifier = Modifier.weight(1f)
            ) {
                Text("Clear Downloads")
            }
        }
        
        Button(
            onClick = { viewModel.clearFavorites() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Clear Favorites")
        }
        
        // Data display
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = "Downloads (${downloads.size})",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            
            items(downloads) { download ->
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text("ID: ${download.id}")
                        Text("Recording: ${download.recordingId}")
                        Text("Track: ${download.trackFilename}")
                        Text("Status: ${download.status}")
                        Text("Progress: ${(download.progress * 100).toInt()}%")
                    }
                }
            }
            
            item {
                Text(
                    text = "Favorites (${favorites.size})",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
            
            items(favorites) { favorite ->
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text("ID: ${favorite.id}")
                        Text("Type: ${favorite.type}")
                        Text("Recording: ${favorite.recordingId}")
                        if (favorite.trackFilename != null) {
                            Text("Track: ${favorite.trackFilename}")
                        }
                        if (favorite.notes?.isNotEmpty() == true) {
                            Text("Notes: ${favorite.notes}")
                        }
                    }
                }
            }
        }
    }
}