package com.deadarchive.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    onNavigateToNetworkTest: () -> Unit,
    onNavigateToDatabaseTest: () -> Unit,
    onNavigateToMediaPlayerTest: () -> Unit,
    onNavigateToRepositoryTest: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically)
    ) {
        // Steal Your Face Logo
        Image(
            painter = painterResource(R.drawable.steal_your_face),
            contentDescription = "Steal Your Face - Grateful Dead Logo",
            modifier = Modifier
                .size(120.dp)
                .padding(bottom = 16.dp)
        )
        
        Text(
            text = "Dead Archive",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = "Grateful Dead Live Concert Archive",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Text(
            text = "Test Screens",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Button(
            onClick = onNavigateToNetworkTest,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                text = "Network Test",
                style = MaterialTheme.typography.titleMedium
            )
        }
        
        Button(
            onClick = onNavigateToDatabaseTest,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                text = "Database Test",
                style = MaterialTheme.typography.titleMedium
            )
        }
        
        Button(
            onClick = onNavigateToRepositoryTest,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary
            )
        ) {
            Text(
                text = "🧪 Repository Test",
                style = MaterialTheme.typography.titleMedium
            )
        }
        
        Button(
            onClick = onNavigateToMediaPlayerTest,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary
            )
        ) {
            Text(
                text = "🎸 Media Player Test",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}