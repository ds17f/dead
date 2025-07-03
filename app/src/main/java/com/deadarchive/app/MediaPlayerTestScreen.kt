package com.deadarchive.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.deadarchive.core.media.MediaPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@UnstableApi
@HiltViewModel
class MediaPlayerTestViewModel @Inject constructor(
    private val mediaPlayer: MediaPlayer,
    private val showRepository: com.deadarchive.core.data.repository.ShowRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val _message = MutableStateFlow("Ready to test Dead Archive Media Player")
    val message: StateFlow<String> = _message.asStateFlow()
    
    private val _logs = MutableStateFlow(mutableListOf<String>())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()
    
    private val _showLogs = MutableStateFlow(false)
    val showLogs: StateFlow<Boolean> = _showLogs.asStateFlow()
    
    val isPlaying = mediaPlayer.isPlaying
    val currentPosition = mediaPlayer.currentPosition
    val duration = mediaPlayer.duration
    val playbackState = mediaPlayer.playbackState
    val lastError = mediaPlayer.lastError
    
    init {
        // Start position updates
        viewModelScope.launch {
            while (true) {
                mediaPlayer.updatePosition()
                delay(1000) // Update every second
            }
        }
        
        // Monitor for errors
        viewModelScope.launch {
            lastError.collect { error ->
                error?.let {
                    addLog("PLAYER ERROR: ${it.errorCodeName} (${it.errorCode})")
                    addLog("ERROR MESSAGE: ${it.message}")
                    addLog("ERROR CAUSE: ${it.cause?.message ?: "No cause specified"}")
                    
                    // Log detailed error information
                    when (it.errorCode) {
                        1001 -> addLog("ERROR TYPE: Remote connection failed")
                        1002 -> addLog("ERROR TYPE: Timeout")
                        2003 -> addLog("ERROR TYPE: Content not supported")
                        2004 -> addLog("ERROR TYPE: Content malformed")
                        else -> addLog("ERROR TYPE: Other error code ${it.errorCode}")
                    }
                }
            }
        }
    }
    
    fun toggleLogs() {
        _showLogs.value = !_showLogs.value
    }
    
    fun copyLogsToClipboard() {
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val allLogs = _logs.value.joinToString("\n")
        val clip = ClipData.newPlainText("Dead Archive Debug Logs", allLogs)
        clipboardManager.setPrimaryClip(clip)
        addLog("✅ Logs copied to clipboard (${_logs.value.size} entries)")
    }
    
    private fun addLog(message: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        val logEntry = "[$timestamp] $message"
        
        val currentLogs = _logs.value.toMutableList()
        currentLogs.add(logEntry)
        
        // Keep only last 50 log entries
        if (currentLogs.size > 50) {
            currentLogs.removeAt(0)
        }
        
        _logs.value = currentLogs
    }
    
    fun playTestTrack() {
        viewModelScope.launch {
            try {
                _message.value = "🎵 Loading Grateful Dead track..."
                addLog("Starting Soldier Field '95 stream test using metadata API")
                
                // Use Archive.org metadata API to get proper streaming URL
                val concertId = "gd1995-07-09.sbd.miller.114369.flac16"  // Soldier Field 1995 - known working
                addLog("Recording ID: $concertId (Soldier Field 1995-07-09)")
                
                _message.value = "🌐 Fetching metadata from Archive.org..."
                addLog("Fetching metadata from Archive.org...")
                
                val streamingUrl = showRepository.getPreferredStreamingUrl(concertId)
                
                if (streamingUrl != null) {
                    addLog("Generated streaming URL: $streamingUrl")
                    
                    _message.value = "🌐 Connecting to Archive.org..."
                    addLog("Attempting to connect with proper streaming URL...")
                    
                    mediaPlayer.playTrack(
                        url = streamingUrl,
                        title = "Touch of Grey",
                        artist = "Grateful Dead"
                    )
                    addLog("MediaPlayer.playTrack() called with API-generated URL")
                    
                    _message.value = "🎸 Loading: Touch of Grey (Soldier Field '95)"
                    
                    // Give it a moment to start buffering
                    delay(2000)
                    
                    val state = mediaPlayer.playbackState.value
                    addLog("Player state after 2s: ${getPlaybackStateString(state)} ($state)")
                    
                    if (state == Player.STATE_BUFFERING) {
                        _message.value = "📡 Buffering Soldier Field '95..."
                        addLog("Player is buffering - good sign!")
                    } else if (state == Player.STATE_READY) {
                        _message.value = "🎸 Ready to play Soldier Field '95!"
                        addLog("Player is ready - stream loaded successfully!")
                    } else {
                        _message.value = "⚠️ Player state: ${getPlaybackStateString(state)}. Check network connection?"
                        addLog("Unexpected player state - possible network issue")
                    }
                } else {
                    _message.value = "❌ Unable to get streaming URL from Archive.org metadata"
                    addLog("ERROR: Could not generate streaming URL from metadata")
                }
                
            } catch (e: Exception) {
                _message.value = "❌ Error: ${e.message ?: "Unknown error"}\n💡 Try checking network connection"
                addLog("ERROR: ${e.message ?: "Unknown error"}")
                addLog("Stack trace: ${e.stackTraceToString()}")
            }
        }
    }
    
    fun playPauseToggle() {
        if (isPlaying.value) {
            mediaPlayer.pause()
            _message.value = "⏸️ Paused"
        } else {
            mediaPlayer.play()
            _message.value = "▶️ Playing"
        }
    }
    
    fun playTestMP3() {
        viewModelScope.launch {
            try {
                _message.value = "🎵 Testing second concert..."
                addLog("Starting Soldier Field '95 (different version) test")
                
                // Use a different version of the same working concert
                val concertId = "gd1995-07-09.schoeps.wklitz.95444.flac1648"  // Same show, different taper
                addLog("Recording ID: $concertId (Soldier Field 1995 - audience recording)")
                
                _message.value = "🌐 Fetching metadata from Archive.org..."
                addLog("Fetching metadata for audience recording...")
                
                val streamingUrl = showRepository.getPreferredStreamingUrl(concertId)
                
                if (streamingUrl != null) {
                    addLog("Generated streaming URL: $streamingUrl")
                    
                    _message.value = "🌐 Connecting to Archive.org..."
                    addLog("Attempting to connect with proper streaming URL...")
                    
                    mediaPlayer.playTrack(
                        url = streamingUrl,
                        title = "Touch of Grey",
                        artist = "Grateful Dead"
                    )
                    addLog("MediaPlayer.playTrack() called with API-generated URL")
                    
                    _message.value = "🎸 Loading: Touch of Grey (Audience Recording)"
                    
                    // Give it a moment to start buffering
                    delay(3000)
                    
                    val state = mediaPlayer.playbackState.value
                    addLog("Player state after 3s: ${getPlaybackStateString(state)} ($state)")
                    
                    when (state) {
                        Player.STATE_BUFFERING -> {
                            _message.value = "📡 Buffering audience recording..."
                            addLog("Player is buffering - good sign!")
                        }
                        Player.STATE_READY -> {
                            _message.value = "🎸 Audience recording ready to play!"
                            addLog("Player is ready - stream loaded successfully!")
                        }
                        Player.STATE_IDLE -> {
                            _message.value = "⚠️ Player idle - possible network or URL issue"
                            addLog("Player is idle - network or URL issue")
                        }
                        else -> {
                            _message.value = "⚠️ Player state: ${getPlaybackStateString(state)}"
                            addLog("Unexpected player state")
                        }
                    }
                } else {
                    _message.value = "❌ Unable to get streaming URL from Archive.org metadata"
                    addLog("ERROR: Could not generate streaming URL from metadata")
                }
                
            } catch (e: Exception) { 
                _message.value = "❌ Error: ${e.message ?: "Unknown error"}\n💡 Check network connection or try different format"
                addLog("ERROR: ${e.message ?: "Unknown error"}")
                addLog("Stack trace: ${e.stackTraceToString()}")
            }
        }
    }
    
    fun stopPlayback() {
        mediaPlayer.stop()
        _message.value = "⏹️ Stopped"
    }
    
    fun seekForward() {
        val newPosition = currentPosition.value + 30000 // +30 seconds
        mediaPlayer.seekTo(newPosition)
        _message.value = "⏭️ Seeked forward 30s"
    }
    
    fun testDirectStream() {
        viewModelScope.launch {
            try {
                _message.value = "🎵 Testing third version..."
                addLog("Starting Soldier Field '95 (16-bit version) test")
                
                // Use the third version from search results
                val concertId = "gd1995-07-09.schoeps.wklitz.95445.flac16"  // Same show, 16-bit version
                addLog("Recording ID: $concertId (Soldier Field 1995 - 16-bit FLAC)")
                
                _message.value = "🌐 Fetching metadata from Archive.org..."
                addLog("Fetching metadata for 16-bit version...")
                
                val streamingUrl = showRepository.getPreferredStreamingUrl(concertId)
                
                if (streamingUrl != null) {
                    addLog("Generated streaming URL: $streamingUrl")
                    
                    _message.value = "🌐 Connecting to Archive.org..."
                    addLog("Attempting to connect with proper streaming URL...")
                    
                    mediaPlayer.playTrack(
                        url = streamingUrl,
                        title = "Touch of Grey",
                        artist = "Grateful Dead"
                    )
                    addLog("MediaPlayer.playTrack() called with API-generated URL")
                    
                    _message.value = "🎸 Loading: Touch of Grey (16-bit FLAC)"
                    
                    // Give it a moment to start buffering
                    delay(3000)
                    
                    val state = mediaPlayer.playbackState.value
                    addLog("Player state after 3s: ${getPlaybackStateString(state)} ($state)")
                    
                    when (state) {
                        Player.STATE_BUFFERING -> {
                            _message.value = "📡 Buffering 16-bit version..."
                            addLog("Player is buffering - good sign!")
                        }
                        Player.STATE_READY -> {
                            _message.value = "🎸 16-bit version ready to play!"
                            addLog("Player is ready - stream loaded successfully!")
                        }
                        Player.STATE_IDLE -> {
                            _message.value = "⚠️ Player idle - possible network or URL issue"
                            addLog("Player is idle - network or URL issue")
                        }
                        else -> {
                            _message.value = "⚠️ Player state: ${getPlaybackStateString(state)}"
                            addLog("Unexpected player state")
                        }
                    }
                } else {
                    _message.value = "❌ Unable to get streaming URL from Archive.org metadata"
                    addLog("ERROR: Could not generate streaming URL from metadata")
                }
                
            } catch (e: Exception) { 
                _message.value = "❌ Error: ${e.message ?: "Unknown error"}\n💡 Check network connection or try different format"
                addLog("ERROR: ${e.message ?: "Unknown error"}")
                addLog("Stack trace: ${e.stackTraceToString()}")
            }
        }
    }
    
    fun seekBackward() {
        val newPosition = maxOf(0, currentPosition.value - 30000) // -30 seconds
        mediaPlayer.seekTo(newPosition)
        _message.value = "⏮️ Seeked backward 30s"
    }
    
    fun playLocalTest() {
        viewModelScope.launch {
            try {
                _message.value = "🎵 Loading local test file..."
                addLog("Starting local MP3 test")
                
                // Play the local MP3 file from assets
                addLog("Playing local asset: gd77-05-08aud-d1t09.mp3")
                
                mediaPlayer.playLocalAsset(
                    assetFileName = "gd77-05-08aud-d1t09.mp3",
                    title = "Fire on the Mountain (Local Test)",
                    artist = "Grateful Dead"
                )
                addLog("MediaPlayer.playLocalAsset() called")
                
                _message.value = "🎸 Loading: Local Fire on the Mountain"
                
                // Give it a moment to start buffering
                delay(2000)
                
                val state = mediaPlayer.playbackState.value
                addLog("Local player state after 2s: ${getPlaybackStateString(state)} ($state)")
                
                if (state == Player.STATE_BUFFERING) {
                    _message.value = "📡 Buffering local file..."
                    addLog("Local player is buffering - good sign!")
                } else if (state == Player.STATE_READY) {
                    _message.value = "🎸 Local file ready to play!"
                    addLog("Local player is ready - file loaded successfully!")
                } else {
                    _message.value = "⚠️ Local player state: ${getPlaybackStateString(state)}"
                    addLog("Local unexpected player state - possible file issue")
                }
                
            } catch (e: Exception) {
                _message.value = "❌ Local Error: ${e.message ?: "Unknown error"}\n💡 Check local file"
                addLog("LOCAL ERROR: ${e.message ?: "Unknown error"}")
                addLog("Local Stack trace: ${e.stackTraceToString()}")
            }
        }
    }
}

@UnstableApi
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaPlayerTestScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: MediaPlayerTestViewModel = hiltViewModel()
) {
    val message by viewModel.message.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val showLogs by viewModel.showLogs.collectAsState()
    
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
                text = "Media Player Test",
                style = MaterialTheme.typography.headlineMedium
            )
        }
        
        // Steal Your Face Logo
        Image(
            painter = painterResource(R.drawable.steal_your_face),
            contentDescription = "Steal Your Face",
            modifier = Modifier.size(80.dp)
        )
        
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
        
        // Playback status
        Card {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text("Status: ${getPlaybackStateString(playbackState)}")
                Text("Playing: ${if (isPlaying) "Yes" else "No"}")
                Text("Position: ${formatTime(currentPosition)} / ${formatTime(duration)}")
                
                if (duration > 0) {
                    LinearProgressIndicator(
                        progress = { (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                }
            }
        }
        
        // Control buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.playTestTrack() },
                modifier = Modifier.weight(1f)
            ) {
                Text("🎸 SBD")
            }
            Button(
                onClick = { viewModel.playTestMP3() },
                modifier = Modifier.weight(1f)
            ) {
                Text("🎤 AUD")
            }
            Button(
                onClick = { viewModel.testDirectStream() },
                modifier = Modifier.weight(1f)
            ) {
                Text("💎 16bit")
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.seekBackward() },
                modifier = Modifier.weight(1f)
            ) {
                Text("⏮️ -30s")
            }
            
            Button(
                onClick = { viewModel.playPauseToggle() },
                modifier = Modifier.weight(1f)
            ) {
                Text(if (isPlaying) "⏸️ Pause" else "▶️ Play")
            }
            
            Button(
                onClick = { viewModel.seekForward() },
                modifier = Modifier.weight(1f)
            ) {
                Text("⏭️ +30s")
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.stopPlayback() },
                modifier = Modifier.weight(1f)
            ) {
                Text("⏹️ Stop")
            }
            
            Button(
                onClick = { viewModel.playLocalTest() },
                modifier = Modifier.weight(1f)
            ) {
                Text("📁 Local")
            }
        }
        
        // Log control buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.toggleLogs() },
                modifier = Modifier.weight(1f)
            ) {
                Text(if (showLogs) "📜 Hide Logs" else "📜 Show Logs")
            }
            
            Button(
                onClick = { viewModel.copyLogsToClipboard() },
                modifier = Modifier.weight(1f)
            ) {
                Text("📋 Copy Logs")
            }
        }
        
        // Log output section
        if (showLogs) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "Debug Logs",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        reverseLayout = true
                    ) {
                        items(logs.reversed()) { log ->
                            Text(
                                text = log,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
        
        Text(
            text = "🎸 Test Soldier Field 1995-07-09 in three formats!\nSoundboard, Audience & 16-bit versions streaming from Archive.org",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

private fun getPlaybackStateString(state: Int): String {
    return when (state) {
        Player.STATE_IDLE -> "Idle"
        Player.STATE_BUFFERING -> "Buffering"
        Player.STATE_READY -> "Ready"
        Player.STATE_ENDED -> "Ended"
        else -> "Unknown"
    }
}

private fun formatTime(timeMs: Long): String {
    if (timeMs <= 0) return "0:00"
    
    val totalSeconds = timeMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    
    return "${minutes}:${seconds.toString().padStart(2, '0')}"
}