package com.deadarchive.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deadarchive.core.data.repository.ShowRepository
import com.deadarchive.core.data.repository.LibraryRepository
import com.deadarchive.core.model.Recording
import com.deadarchive.core.model.Show
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.system.measureTimeMillis

@HiltViewModel
class RepositoryTestViewModel @Inject constructor(
    private val showRepository: ShowRepository,
    private val libraryRepository: LibraryRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<RepositoryTestUiState>(RepositoryTestUiState.Idle)
    val uiState: StateFlow<RepositoryTestUiState> = _uiState.asStateFlow()
    
    private val _concerts = MutableStateFlow<List<Recording>>(emptyList())
    val concerts: StateFlow<List<Recording>> = _concerts.asStateFlow()
    
    private val _favorites = MutableStateFlow<List<Recording>>(emptyList())
    val favorites: StateFlow<List<Recording>> = _favorites.asStateFlow()
    
    private val _cacheStats = MutableStateFlow("")
    val cacheStats: StateFlow<String> = _cacheStats.asStateFlow()
    
    private val _testLog = MutableStateFlow<List<String>>(emptyList())
    val testLog: StateFlow<List<String>> = _testLog.asStateFlow()
    
    init {
        // Observe library in real-time
        viewModelScope.launch {
            showRepository.getLibraryRecordings().collect { libraryRecordings ->
                _favorites.value = libraryRecordings
                addLog("✨ Library updated: ${libraryRecordings.size} items")
            }
        }
        updateCacheStats()
    }
    
    private fun addLog(message: String) {
        val timestamp = System.currentTimeMillis() % 100000 // Last 5 digits for brevity
        val logEntry = "[$timestamp] $message"
        _testLog.value = (_testLog.value + logEntry).takeLast(20) // Keep last 20 entries
    }
    
    fun testSearchWithCaching(query: String = "1977") {
        viewModelScope.launch {
            _uiState.value = RepositoryTestUiState.Loading("Testing search with caching...")
            addLog("🔍 Starting search for '$query'")
            
            // Debug: Show what the actual search query will be
            val debugQuery = if (query.isBlank()) {
                "collection:GratefulDead"
            } else if (query.matches(Regex("\\d{4}"))) {
                "collection:GratefulDead AND date:$query*"
            } else {
                "collection:GratefulDead AND ($query)"
            }
            addLog("🔧 API Query: $debugQuery")
            
            try {
                val searchTime = measureTimeMillis {
                    showRepository.searchRecordings(query).take(2).collect { results ->
                        _concerts.value = results
                        addLog("📦 Received ${results.size} concerts")
                        
                        // Show years in the results to debug
                        val years = results.map { it.concertDate.take(4) }.distinct()
                        addLog("📅 Years in results: ${years.joinToString(", ")}")
                        
                        // Show if any are from cache vs fresh
                        val cacheIndicator = if (results.isNotEmpty()) "📊" else "⚡"
                        addLog("$cacheIndicator Results: ${results.take(2).joinToString { "${it.concertDate.take(4)}: ${it.title.take(30)}" }}")
                    }
                }
                
                addLog("⏱️ Search completed in ${searchTime}ms")
                _uiState.value = RepositoryTestUiState.Success("Search completed successfully")
                updateCacheStats()
                
            } catch (e: Exception) {
                addLog("❌ Search failed: ${e.message}")
                _uiState.value = RepositoryTestUiState.Error("Search failed: ${e.message}")
            }
        }
    }
    
    fun testOfflineBehavior() {
        viewModelScope.launch {
            _uiState.value = RepositoryTestUiState.Loading("Testing offline behavior...")
            addLog("📴 Testing offline behavior - using cached data")
            
            try {
                // Use repository method to get cached concerts
                showRepository.getAllCachedRecordings().take(1).collect { cachedRecordings ->
                    addLog("💾 Found ${cachedRecordings.size} cached concerts")
                    
                    if (cachedRecordings.isEmpty()) {
                        addLog("⚠️ No cached data found. Run search first to populate cache.")
                        _uiState.value = RepositoryTestUiState.Error("No cached data. Run search first.")
                    } else {
                        _concerts.value = cachedRecordings.take(10)
                        addLog("📱 Offline mode: Showing ${cachedRecordings.take(10).size} cached concerts")
                        
                        // Show some stats
                        val years = cachedRecordings.mapNotNull { it.concertDate.take(4) }.distinct().sorted()
                        addLog("📅 Cached years available: ${years.joinToString(", ")}")
                        _uiState.value = RepositoryTestUiState.Success("Offline test completed - showing cached data")
                    }
                }
                
            } catch (e: Exception) {
                addLog("❌ Offline test failed: ${e.message}")
                _uiState.value = RepositoryTestUiState.Error("Offline test failed: ${e.message}")
            }
        }
    }
    
    fun testConcurrentSearches() {
        viewModelScope.launch {
            _uiState.value = RepositoryTestUiState.Loading("Testing concurrent searches...")
            addLog("🚀 Starting 3 concurrent searches...")
            
            try {
                val queries = listOf("1977", "1972", "1995")
                val startTime = System.currentTimeMillis()
                
                // Launch concurrent searches
                val jobs = queries.map { query ->
                    viewModelScope.launch {
                        addLog("🔍 Starting search: $query")
                        showRepository.searchRecordings(query).first()
                        addLog("✅ Completed search: $query")
                    }
                }
                
                // Wait for all to complete
                jobs.forEach { it.join() }
                
                val totalTime = System.currentTimeMillis() - startTime
                addLog("⚡ All concurrent searches completed in ${totalTime}ms")
                _uiState.value = RepositoryTestUiState.Success("Concurrent searches completed")
                updateCacheStats()
                
            } catch (e: Exception) {
                addLog("❌ Concurrent test failed: ${e.message}")
                _uiState.value = RepositoryTestUiState.Error("Concurrent test failed: ${e.message}")
            }
        }
    }
    
    fun toggleFavorite(concert: Recording) {
        viewModelScope.launch {
            try {
                // Create a Show from the Recording for the new library system
                val show = Show(
                    date = concert.concertDate,
                    venue = concert.concertVenue,
                    location = concert.concertLocation,
                    year = concert.concertDate.take(4),
                    recordings = listOf(concert),
                    isInLibrary = concert.isInLibrary
                )
                val isInLibrary = libraryRepository.toggleShowLibrary(show)
                if (isInLibrary) {
                    addLog("💖 Added to library: ${concert.title.take(30)}")
                } else {
                    addLog("💔 Removed from library: ${concert.title.take(30)}")
                }
                
                // Refresh current concerts to update library status
                if (_concerts.value.isNotEmpty()) {
                    _concerts.value = _concerts.value.map { c ->
                        if (c.identifier == concert.identifier) {
                            c.copy(isInLibrary = isInLibrary)
                        } else c
                    }
                }
                
            } catch (e: Exception) {
                addLog("❌ Toggle library failed: ${e.message}")
            }
        }
    }
    
    fun testGetRecordingById() {
        viewModelScope.launch {
            if (_concerts.value.isEmpty()) {
                addLog("⚠️ No concerts available. Run search first.")
                return@launch
            }
            
            val concert = _concerts.value.first()
            addLog("🔍 Testing getRecordingById for: ${concert.identifier}")
            
            try {
                val fetchTime = measureTimeMillis {
                    val result = showRepository.getRecordingById(concert.identifier)
                    result?.let {
                        addLog("✅ Found concert: ${it.title}")
                        addLog("📊 In library: ${it.isInLibrary}")
                    } ?: addLog("❌ Recording not found")
                }
                
                addLog("⏱️ getRecordingById completed in ${fetchTime}ms")
                
            } catch (e: Exception) {
                addLog("❌ getRecordingById failed: ${e.message}")
            }
        }
    }
    
    private fun updateCacheStats() {
        viewModelScope.launch {
            try {
                showRepository.getAllCachedRecordings().take(1).collect { cachedRecordings ->
                    val libraryCount = _favorites.value.size
                    
                    _cacheStats.value = buildString {
                        appendLine("📊 Cache Statistics:")
                        appendLine("• Cached concerts: ${cachedRecordings.size}")
                        appendLine("• Library items: $libraryCount")
                        appendLine("• Last updated: ${System.currentTimeMillis() % 100000}")
                    }
                }
                
            } catch (e: Exception) {
                _cacheStats.value = "Error loading cache stats: ${e.message}"
            }
        }
    }
    
    fun clearCache() {
        viewModelScope.launch {
            try {
                // Clear current display and show message
                _concerts.value = emptyList()
                addLog("🗑️ Display cache cleared")
                addLog("ℹ️ Full cache clearing requires restart")
                updateCacheStats()
                
            } catch (e: Exception) {
                addLog("❌ Clear cache failed: ${e.message}")
            }
        }
    }
    
    fun clearLog() {
        _testLog.value = emptyList()
    }
}

sealed class RepositoryTestUiState {
    object Idle : RepositoryTestUiState()
    data class Loading(val message: String) : RepositoryTestUiState()
    data class Success(val message: String) : RepositoryTestUiState()
    data class Error(val message: String) : RepositoryTestUiState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepositoryTestScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: RepositoryTestViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val concerts by viewModel.concerts.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val cacheStats by viewModel.cacheStats.collectAsState()
    val testLog by viewModel.testLog.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
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
                text = "Repository Test",
                style = MaterialTheme.typography.headlineMedium
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Status card
        when (val state = uiState) {
            is RepositoryTestUiState.Loading -> {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(20.dp)
                                .padding(end = 12.dp)
                        )
                        Text(state.message)
                    }
                }
            }
            is RepositoryTestUiState.Success -> {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Text(
                        text = "✅ ${state.message}",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            is RepositoryTestUiState.Error -> {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = "❌ ${state.message}",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            is RepositoryTestUiState.Idle -> {
                Card {
                    Text(
                        text = "🧪 Repository Test Suite Ready",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Test buttons
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = "Test Operations",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.testSearchWithCaching("1977") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("🔍 Search 1977")
                    }
                    Button(
                        onClick = { viewModel.testOfflineBehavior() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("📴 Offline Test")
                    }
                }
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.testConcurrentSearches() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("🚀 Concurrent")
                    }
                    Button(
                        onClick = { viewModel.testGetRecordingById() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("🔍 Get By ID")
                    }
                }
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.clearCache() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text("🗑️ Clear Cache")
                    }
                    Button(
                        onClick = { viewModel.clearLog() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.outline
                        )
                    ) {
                        Text("📝 Clear Log")
                    }
                }
            }
            
            // Cache stats
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = cacheStats.ifEmpty { "Loading cache stats..." },
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            
            // Library section
            item {
                Text(
                    text = "📚 Library (${favorites.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
            
            items(favorites.take(3)) { concert ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = concert.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${concert.concertDate} • ${concert.concertVenue}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Recordings section
            item {
                Text(
                    text = "🎵 Recordings (${concerts.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
            
            items(concerts.take(5)) { concert ->
                Card {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = concert.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${concert.concertDate} • ${concert.concertVenue}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        IconButton(
                            onClick = { viewModel.toggleFavorite(concert) }
                        ) {
                            Text(
                                text = if (concert.isInLibrary) "📚" else "📖",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }
            
            // Test log
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📝 Test Log",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                    
                    val clipboardManager = LocalClipboardManager.current
                    Button(
                        onClick = {
                            val logText = testLog.reversed().joinToString("\n")
                            clipboardManager.setText(AnnotatedString(logText))
                        },
                        modifier = Modifier.padding(top = 16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text("📋 Copy Logs")
                    }
                }
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        testLog.reversed().forEach { logEntry ->
                            Text(
                                text = logEntry,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}