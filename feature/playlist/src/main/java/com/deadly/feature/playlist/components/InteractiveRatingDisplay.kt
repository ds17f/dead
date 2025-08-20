package com.deadly.feature.playlist.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.deadly.core.design.component.IconResources
import com.deadly.core.design.component.CompactStarRating

/**
 * Interactive rating display that shows star rating with review count
 * and opens review details when tapped.
 */
@Composable
fun InteractiveRatingDisplay(
    rating: Float?,
    reviewCount: Int?,
    confidence: Float?,
    onShowReviews: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (rating == null) return
    
    Card(
        modifier = modifier
            .clickable { onShowReviews() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Star rating and numerical score
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CompactStarRating(
                    rating = rating,
                    confidence = confidence,
                    starSize = IconResources.Size.MEDIUM
                )
                
                Text(
                    text = String.format("%.1f", rating),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            // Review count and indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                reviewCount?.let { count ->
                    if (count > 0) {
                        Text(
                            text = "($count)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Icon(
                    painter = IconResources.Navigation.ChevronRight(),
                    contentDescription = "View reviews",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}