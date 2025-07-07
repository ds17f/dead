package com.deadarchive.feature.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.deadarchive.core.design.component.IconResources
import com.deadarchive.core.model.*

/**
 * Card component for displaying "Today in Grateful Dead History" on the home screen.
 * Shows a featured show from today's date in history with statistics.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayInHistoryCard(
    onShowClick: (Show) -> Unit = {},
    onViewAllClick: (TodayInHistory) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: TodayInHistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val displayMode by viewModel.displayMode.collectAsState()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(if (displayMode == HistoryDisplayMode.COMPACT) 180.dp else 240.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        when (val state = uiState) {
            is TodayInHistoryState.Loading -> {
                LoadingContent()
            }
            is TodayInHistoryState.Success -> {
                SuccessContent(
                    todayInHistory = state.todayInHistory,
                    displayMode = displayMode,
                    onShowClick = onShowClick,
                    onViewAllClick = onViewAllClick,
                    onShuffleClick = { viewModel.shuffleFeaturedShow() }
                )
            }
            is TodayInHistoryState.Error -> {
                ErrorContent(
                    message = state.message,
                    onRetry = { viewModel.refresh() }
                )
            }
            is TodayInHistoryState.NoShows -> {
                NoShowsContent()
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CircularProgressIndicator()
            Text(
                text = "Loading today's history...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun SuccessContent(
    todayInHistory: TodayInHistory,
    displayMode: HistoryDisplayMode,
    onShowClick: (Show) -> Unit,
    onViewAllClick: (TodayInHistory) -> Unit,
    onShuffleClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Header with date and statistics
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Today in Dead History",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (todayInHistory.shows.size > 1) {
                    IconButton(
                        onClick = onShuffleClick,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            painter = IconResources.PlayerControls.Shuffle(),
                            contentDescription = "Shuffle featured show",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Text(
                text = todayInHistory.dateFormatted,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = todayInHistory.statistics.summaryText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }

        // Featured show or statistics
        when (displayMode) {
            HistoryDisplayMode.COMPACT -> {
                todayInHistory.featuredShow?.let { show ->
                    FeaturedShowContent(
                        show = show,
                        onShowClick = onShowClick
                    )
                }
            }
            HistoryDisplayMode.SUMMARY -> {
                StatisticsContent(
                    statistics = todayInHistory.statistics,
                    yearsSpan = todayInHistory.yearsSpan
                )
            }
            HistoryDisplayMode.DETAILED -> {
                // For detailed mode, show "View All" button
                Button(
                    onClick = { onViewAllClick(todayInHistory) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("View All ${todayInHistory.totalShows} Shows")
                }
            }
        }

        // Footer with action buttons
        if (displayMode == HistoryDisplayMode.COMPACT && todayInHistory.totalShows > 1) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = { onViewAllClick(todayInHistory) }
                ) {
                    Text("View All ${todayInHistory.totalShows}")
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        painter = IconResources.Navigation.ArrowForward(),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FeaturedShowContent(
    show: Show,
    onShowClick: (Show) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onShowClick(show) },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = show.displayVenue,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    show.location?.let { location ->
                        Text(
                            text = location,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Text(
                    text = show.year ?: "",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (show.recordingCount > 0) {
                Text(
                    text = "${show.recordingCount} recording${if (show.recordingCount != 1) "s" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun StatisticsContent(
    statistics: HistoryStatistics,
    yearsSpan: IntRange?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatisticItem(
            label = "Shows",
            value = statistics.totalShows.toString()
        )
        StatisticItem(
            label = "Years",
            value = statistics.yearsWithShows.size.toString()
        )
        StatisticItem(
            label = "Venues",
            value = statistics.venueCount.toString()
        )
        yearsSpan?.let { span ->
            StatisticItem(
                label = "Span",
                value = "${span.first}-${span.last}"
            )
        }
    }
}

@Composable
private fun StatisticItem(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Error loading history",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onRetry) {
            Text("Retry")
        }
    }
}

@Composable
private fun NoShowsContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = IconResources.Content.CalendarToday(),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "No shows found",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Text(
            text = "No Grateful Dead shows on this date in history",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    }
}