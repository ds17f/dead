package com.deadarchive.feature.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.deadarchive.core.model.Show
import com.deadarchive.core.design.component.IconResources

// Data models for search results
data class SuggestedSearch(
    val query: String
)

data class RecentSearch(
    val query: String,
    val timestamp: Long
)

/**
 * SearchResultsV2Screen - Full-screen search interface
 * 
 * This screen provides a comprehensive search experience with:
 * - Search input with back navigation
 * - Recent searches for quick access
 * - Suggested search terms
 * - Search results similar to LibraryV2 cards
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchResultsV2Screen(
    initialQuery: String = "",
    onNavigateBack: () -> Unit,
    onNavigateToShow: (Show) -> Unit,
    onNavigateToPlayer: (String) -> Unit,
    viewModel: SearchV2ViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top bar with back arrow and search input
        SearchResultsTopBar(
            searchQuery = uiState.searchQuery,
            onSearchQueryChange = viewModel::onSearchQueryChanged,
            onNavigateBack = onNavigateBack
        )
        
        // Search content
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (uiState.searchQuery.isEmpty()) {
                // Show recent searches when no query
                item {
                    RecentSearchesSection(
                        onSearchSelected = { query ->
                            viewModel.onSearchQueryChanged(query)
                        }
                    )
                }
            } else {
                // Show suggestions and results when typing
                item {
                    SuggestedSearchesSection(
                        query = uiState.searchQuery,
                        onSuggestionSelected = { suggestion ->
                            viewModel.onSearchQueryChanged(suggestion)
                        }
                    )
                }
                
                item {
                    SearchResultsSection(
                        query = uiState.searchQuery,
                        onShowSelected = onNavigateToShow,
                        onRecordingSelected = onNavigateToPlayer
                    )
                }
            }
        }
    }
}

/**
 * Top bar with back arrow and search input
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchResultsTopBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
                //.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            // Back arrow
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            
            //Spacer(modifier = Modifier.width(12.dp))
            
            // Search input - transparent background, compact vertical design with clear button
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { 
                    Text(
                        text = "What do you want to listen to?",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                },
                // No leading icon (removed magnifying glass)
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    @Composable {
                        IconButton(
                            onClick = { onSearchQueryChange("") }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Clear,
                                contentDescription = "Clear search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(x = (-8).dp), // Move the entire text field 8dp to the left
                singleLine = true,
                // More compact shape
                shape = RoundedCornerShape(8.dp),
                // Compact text style to fit in reduced height
                textStyle = MaterialTheme.typography.bodyMedium,//.copy(fontSize = 14.sp),
                colors = OutlinedTextFieldDefaults.colors(
                    // Transparent/invisible background - user types directly into the interface
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    // Invisible borders
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    // Visible text
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    }
}

/**
 * Recent searches section
 */
@Composable
private fun RecentSearchesSection(
    onSearchSelected: (String) -> Unit
) {
    // Mock recent searches - will be replaced with real data
    val recentSearches = listOf(
        RecentSearch("Grateful Dead 1977", System.currentTimeMillis()),
        RecentSearch("Cornell 5/8/77", System.currentTimeMillis() - 86400000),
        RecentSearch("Dick's Picks", System.currentTimeMillis() - 172800000)
    )
    
    Column {
        Text(
            text = "Recent Searches",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        recentSearches.forEach { recentSearch ->
            RecentSearchCard(
                search = recentSearch,
                onClick = { onSearchSelected(recentSearch.query) }
            )
        }
    }
}

/**
 * Suggested searches section
 */
@Composable
private fun SuggestedSearchesSection(
    query: String,
    onSuggestionSelected: (String) -> Unit
) {
    // Mock suggestions based on query - will be replaced with real search service
    val suggestions = listOf(
        SuggestedSearch("$query 1977"),
        SuggestedSearch("$query soundboard"),
        SuggestedSearch("$query audience")
    ).filter { it.query.lowercase() != query.lowercase() }
    
    if (suggestions.isNotEmpty()) {
        Column {
            Text(
                text = "Suggested Searches",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            suggestions.forEach { suggestion ->
                SuggestedSearchCard(
                    suggestion = suggestion,
                    onClick = { onSuggestionSelected(suggestion.query) }
                )
            }
        }
    }
}

/**
 * Search results section
 */
@Composable
private fun SearchResultsSection(
    query: String,
    onShowSelected: (Show) -> Unit,
    onRecordingSelected: (String) -> Unit
) {
    Column {
        Text(
            text = "Search Results",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        // Mock search results - will be replaced with real search service
        Text(
            text = "Searching for \"$query\"...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 24.dp)
        )
    }
}

/**
 * Recent search card component
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecentSearchCard(
    search: RecentSearch,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        border = null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Search icon in grey circle
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Text(
                    text = search.query,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * Suggested search card component with fill text arrow
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SuggestedSearchCard(
    suggestion: SuggestedSearch,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        border = null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Magnifying glass in grey circle
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Text(
                    text = suggestion.query,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            // Arrow pointing up to fill text
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = "Fill search text",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
