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
import com.deadarchive.core.data.repository.ConcertRepository
import com.deadarchive.core.database.ConcertDao
import com.deadarchive.core.database.FavoriteDao
import com.deadarchive.core.model.Concert
import com.deadarchive.core.model.FavoriteItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.system.measureTimeMillis

@HiltViewModel
class RepositoryTestViewModel @Inject constructor(
    private val concertRepository: ConcertRepository,
    private val concertDao: ConcertDao,
    private val favoriteDao: FavoriteDao
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<RepositoryTestUiState>(RepositoryTestUiState.Idle)
    val uiState: StateFlow<RepositoryTestUiState> = _uiState.asStateFlow()
    
    private val _concerts = MutableStateFlow<List<Concert>>(emptyList())
    val concerts: StateFlow<List<Concert>> = _concerts.asStateFlow()
    
    private val _favorites = MutableStateFlow<List<Concert>>(emptyList())
    val favorites: StateFlow<List<Concert>> = _favorites.asStateFlow()
    
    private val _cacheStats = MutableStateFlow("")
    val cacheStats: StateFlow<String> = _cacheStats.asStateFlow()
    
    private val _testLog = MutableStateFlow<List<String>>(emptyList())
    val testLog: StateFlow<List<String>> = _testLog.asStateFlow()
    
    init {
        // Observe favorites in real-time
        viewModelScope.launch {
            concertRepository.getFavoriteConcerts().collect { favoriteConcerts ->
                _favorites.value = favoriteConcerts
                addLog("✨ Favorites updated: ${favoriteConcerts.size} items")
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
                    concertRepository.searchConcerts(query).take(2).collect { results ->
                        _concerts.value = results
                        addLog("📦 Received ${results.size} concerts")
                        
                        // Show years in the results to debug
                        val years = results.map { it.date.take(4) }.distinct()
                        addLog("📅 Years in results: ${years.joinToString(", ")}")
                        
                        // Show if any are from cache vs fresh
                        val cacheIndicator = if (results.isNotEmpty()) "📊" else "⚡"
                        addLog("$cacheIndicator Results: ${results.take(2).joinToString { "${it.date.take(4)}: ${it.title.take(30)}" }}")
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
            addLog("📴 Testing offline behavior (simulated)")
            
            try {
                // First ensure we have some cached data - check for 1977 concerts specifically
                addLog("💾 Checking for cached 1977 concerts...")
                val cachedConcerts = concertDao.searchConcerts("1977")
                addLog("🔍 Found ${cachedConcerts.size} cached concerts matching '1977'")
                
                // Also check total cached concerts
                val totalCached = concertDao.getRecentConcerts(200)
                addLog("📊 Total cached concerts: ${totalCached.size}")
                
                // Show dates of cached concerts for debugging
                if (totalCached.isNotEmpty()) {
                    val cachedYears = totalCached.mapNotNull { it.date?.take(4) }.distinct().sorted()
                    addLog("📅 Cached years: ${cachedYears.joinToString(", ")}")
                }
                
                if (cachedConcerts.isEmpty()) {
                    // Try any cached concerts as fallback
                    val anyCached = concertDao.getRecentConcerts(10)
                    if (anyCached.isEmpty()) {
                        addLog("⚠️ No cached data found. Run search first to populate cache.")
                        _uiState.value = RepositoryTestUiState.Error("No cached data. Run search first.")
                        return@launch
                    } else {
                        addLog("✅ Found ${anyCached.size} cached concerts (not 1977-specific)")
                        // Convert cached entities to domain models for display
                        val concerts = anyCached.map { entity ->
                            entity.toConcert().copy(
                                isFavorite = favoriteDao.isConcertFavorite(entity.id)
                            )
                        }
                        _concerts.value = concerts
                        addLog("📱 Offline mode: Showing ${concerts.size} cached concerts")
                    }
                } else {
                    addLog("✅ Found ${cachedConcerts.size} cached 1977 concerts")
                    
                    // Convert cached entities to domain models for display
                    val concerts = cachedConcerts.take(10).map { entity ->
                        entity.toConcert().copy(
                            isFavorite = favoriteDao.isConcertFavorite(entity.id)
                        )
                    }
                    
                    _concerts.value = concerts
                    addLog("📱 Offline mode: Showing ${concerts.size} cached 1977 concerts")
                }
                
                _uiState.value = RepositoryTestUiState.Success("Offline test completed - showing cached data")
                
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
                        concertRepository.searchConcerts(query).first()
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
    
    fun toggleFavorite(concert: Concert) {
        viewModelScope.launch {
            try {
                if (concert.isFavorite) {
                    // Remove favorite
                    favoriteDao.deleteFavoriteById("concert_${concert.identifier}")
                    addLog("💔 Removed favorite: ${concert.title.take(30)}")
                } else {
                    // Add favorite
                    val favorite = FavoriteItem.fromConcert(concert)
                    val entity = com.deadarchive.core.database.FavoriteEntity.fromFavoriteItem(favorite)
                    favoriteDao.insertFavorite(entity)
                    addLog("💖 Added favorite: ${concert.title.take(30)}")
                }
                
                // Update the concerts table to reflect favorite status change
                val existingConcert = concertDao.getConcertById(concert.identifier)
                if (existingConcert != null) {
                    val updatedEntity = existingConcert.copy(isFavorite = !concert.isFavorite)
                    concertDao.insertConcert(updatedEntity)
                    addLog("🔄 Updated concert cache with favorite status")
                }
                
                // Refresh current concerts to update favorite status
                if (_concerts.value.isNotEmpty()) {
                    _concerts.value = _concerts.value.map { c ->
                        if (c.identifier == concert.identifier) {
                            c.copy(isFavorite = !c.isFavorite)
                        } else c
                    }
                }
                
            } catch (e: Exception) {
                addLog("❌ Toggle favorite failed: ${e.message}")
            }
        }
    }
    
    fun testGetConcertById() {
        viewModelScope.launch {
            if (_concerts.value.isEmpty()) {
                addLog("⚠️ No concerts available. Run search first.")
                return@launch
            }
            
            val concert = _concerts.value.first()
            addLog("🔍 Testing getConcertById for: ${concert.identifier}")
            
            try {
                val fetchTime = measureTimeMillis {
                    val result = concertRepository.getConcertById(concert.identifier)
                    result?.let {
                        addLog("✅ Found concert: ${it.title}")
                        addLog("📊 Is favorite: ${it.isFavorite}")
                    } ?: addLog("❌ Concert not found")
                }
                
                addLog("⏱️ getConcertById completed in ${fetchTime}ms")
                
            } catch (e: Exception) {
                addLog("❌ getConcertById failed: ${e.message}")
            }
        }
    }
    
    private fun updateCacheStats() {
        viewModelScope.launch {
            try {
                val recentConcerts = concertDao.getRecentConcerts(100)
                val favoriteCount = favoriteDao.getFavoriteCount()
                
                _cacheStats.value = buildString {
                    appendLine("📊 Cache Statistics:")
                    appendLine("• Cached concerts: ${recentConcerts.size}")
                    appendLine("• Total favorites: $favoriteCount")
                    appendLine("• Last updated: ${System.currentTimeMillis() % 100000}")
                }
                
            } catch (e: Exception) {
                _cacheStats.value = "Error loading cache stats: ${e.message}"
            }
        }
    }
    
    fun clearCache() {
        viewModelScope.launch {
            try {
                // Clear non-favorite concerts
                val cutoffTimestamp = System.currentTimeMillis() + (24 * 60 * 60 * 1000L) // Future timestamp to clear everything
                concertDao.cleanupOldCachedConcerts(cutoffTimestamp)
                addLog("🗑️ Cache cleared (favorites preserved)")
                updateCacheStats()
                _concerts.value = emptyList()
                
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
                        onClick = { viewModel.testGetConcertById() },
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
            
            // Favorites section
            item {
                Text(
                    text = "💖 Favorites (${favorites.size})",
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
                            text = "${concert.date} • ${concert.venue}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Concerts section
            item {
                Text(
                    text = "🎵 Concerts (${concerts.size})",
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
                                text = "${concert.date} • ${concert.venue}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        IconButton(
                            onClick = { viewModel.toggleFavorite(concert) }
                        ) {
                            Text(
                                text = if (concert.isFavorite) "💖" else "🤍",
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