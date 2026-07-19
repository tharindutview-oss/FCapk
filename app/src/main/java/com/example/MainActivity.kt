package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ChatDatabase
import com.example.data.ChatMessage
import com.example.data.ChatRepository
import com.example.network.ChatServerManager
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.WhatsappGreen
import com.example.ui.theme.WhatsappLightGreen
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var database: ChatDatabase
    private lateinit var repository: ChatRepository
    private lateinit var serverManager: ChatServerManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Core data structures
        database = ChatDatabase.getDatabase(this)
        repository = ChatRepository(database.chatMessageDao())
        serverManager = ChatServerManager(this, repository)

        setContent {
            MyApplicationTheme {
                ChatHubDashboard(serverManager, repository)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Stop server safely when app is terminated
        serverManager.stopServer {}
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatHubDashboard(
    serverManager: ChatServerManager,
    repository: ChatRepository
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Active Navigation Tab (0 = Console, 1 = Logs, 2 = Files, 3 = Settings)
    var selectedTab by remember { mutableStateOf(0) }

    // Server State
    var isServerRunning by remember { mutableStateOf(serverManager.isRunning) }
    var serverUrl by remember { mutableStateOf(serverManager.currentServerUrl) }
    var activeIp by remember { mutableStateOf(serverManager.getLocalIpAddress()) }

    // Chat messages stream
    val messages by repository.allMessagesFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    // Uploaded Files State (Refreshed on mount, tab change, or file deletion)
    var uploadedFiles by remember { mutableStateOf(serverManager.getUploadedFiles()) }

    // Infinite pulsing light animation for active status dot
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    // Start server automatically on creation
    LaunchedEffect(Unit) {
        if (!serverManager.isRunning) {
            serverManager.startServer(port = 8080) {
                isServerRunning = serverManager.isRunning
                serverUrl = serverManager.currentServerUrl
                activeIp = serverManager.getLocalIpAddress()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Header Leading circular icon matching the design's "📡"
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "📡", fontSize = 18.sp)
                        }

                        Column {
                            Text(
                                text = "Local Chat Hub",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (isServerRunning) "SERVER STATUS: RUNNING" else "SERVER STATUS: STOPPED",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White.copy(alpha = 0.8f),
                                letterSpacing = 1.5.sp
                            )
                        }
                    }
                },
                actions = {
                    // Right actions representing standard controls (Search / Menu)
                    IconButton(onClick = { Toast.makeText(context, "Scanning local subnet...", Toast.LENGTH_SHORT).show() }) {
                        Text("🔍", fontSize = 18.sp)
                    }
                    IconButton(onClick = { Toast.makeText(context, "Settings shortcut", Toast.LENGTH_SHORT).show() }) {
                        Text("⋮", fontSize = 18.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = WhatsappGreen
                )
            )
        },
        bottomBar = {
            // Elegant bottom navigation bar mirroring the Vibrant Palette theme
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(Color.White)
                    .border(width = 1.dp, color = Color(0xFFE2E8F0)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                BottomTabItem(
                    icon = "🏠",
                    label = "Console",
                    isSelected = selectedTab == 0,
                    onClick = { selectedTab = 0 }
                )
                BottomTabItem(
                    icon = "📋",
                    label = "Logs",
                    isSelected = selectedTab == 1,
                    onClick = { selectedTab = 1 }
                )
                BottomTabItem(
                    icon = "📂",
                    label = "Files",
                    isSelected = selectedTab == 2,
                    onClick = {
                        selectedTab = 2
                        uploadedFiles = serverManager.getUploadedFiles()
                    }
                )
                BottomTabItem(
                    icon = "⚙️",
                    label = "Settings",
                    isSelected = selectedTab == 3,
                    onClick = { selectedTab = 3 }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF0F2F5))
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> ConsoleTabContent(
                    isServerRunning = isServerRunning,
                    serverUrl = serverUrl,
                    activeIp = activeIp,
                    messagesSize = messages.size,
                    activeConnectionsCount = serverManager.activeConnectionsCount,
                    pulseAlpha = pulseAlpha,
                    lastMessages = messages.takeLast(2),
                    onToggleServer = {
                        if (isServerRunning) {
                            serverManager.stopServer {
                                isServerRunning = serverManager.isRunning
                                serverUrl = serverManager.currentServerUrl
                                activeIp = serverManager.getLocalIpAddress()
                            }
                        } else {
                            serverManager.startServer(port = 8080) {
                                isServerRunning = serverManager.isRunning
                                serverUrl = serverManager.currentServerUrl
                                activeIp = serverManager.getLocalIpAddress()
                            }
                        }
                    }
                )
                1 -> LogsTabContent(
                    messages = messages,
                    onDelete = { msg ->
                        scope.launch { repository.deleteMessage(msg.id) }
                    },
                    onClearAll = {
                        scope.launch {
                            repository.clearAll()
                            Toast.makeText(context, "Database wiped successfully! 🗑️", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                2 -> FilesTabContent(
                    uploadedFiles = uploadedFiles,
                    onDeleteFile = { file ->
                        if (file.delete()) {
                            uploadedFiles = serverManager.getUploadedFiles()
                            Toast.makeText(context, "File deleted!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Failed to delete file", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onClearAllFiles = {
                        val success = serverManager.clearUploadedFiles()
                        uploadedFiles = serverManager.getUploadedFiles()
                        if (success) {
                            Toast.makeText(context, "Uploads storage cleared!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Some files could not be deleted", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                3 -> SettingsTabContent(
                    serverManager = serverManager,
                    messagesCount = messages.size,
                    uploadedFilesCount = uploadedFiles.size,
                    totalFilesSize = uploadedFiles.sumOf { it.length() },
                    activeIp = activeIp,
                    isServerRunning = isServerRunning,
                    onWipeDb = {
                        scope.launch {
                            repository.clearAll()
                            Toast.makeText(context, "Database cleared!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onWipeFiles = {
                        serverManager.clearUploadedFiles()
                        uploadedFiles = serverManager.getUploadedFiles()
                        Toast.makeText(context, "All attachments cleared!", Toast.LENGTH_SHORT).show()
                    },
                    onRecheckIp = {
                        activeIp = serverManager.getLocalIpAddress()
                        Toast.makeText(context, "IP Address re-scanned!", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

// Bottom Navigation Tab Item styled exactly like Design mockup
@Composable
fun BottomTabItem(
    icon: String,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(if (isSelected) Color(0xFFD2E3FC) else Color.Transparent)
                .padding(horizontal = 24.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = icon,
                fontSize = 20.sp,
                color = if (isSelected) Color(0xFF00A884) else Color.Gray
            )
        }
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color(0xFF1E293B) else Color(0xFF64748B)
        )
    }
}

// TAB CONTENT 1: CONSOLE
@Composable
fun ConsoleTabContent(
    isServerRunning: Boolean,
    serverUrl: String,
    activeIp: String,
    messagesSize: Int,
    activeConnectionsCount: Int,
    pulseAlpha: Float,
    lastMessages: List<ChatMessage>,
    onToggleServer: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Gateway IP Card
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // Top header with pulsing status indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "NETWORK GATEWAY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8),
                        letterSpacing = 1.sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isServerRunning) Color(0xFF22C55E).copy(alpha = pulseAlpha)
                                    else Color(0xFFEF4444)
                                )
                        )
                        Text(
                            text = if (isServerRunning) "ACTIVE" else "STOPPED",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isServerRunning) Color(0xFF22C55E) else Color(0xFFEF4444)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Dashed/Bordered styled local gateway endpoint address
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            width = 1.5.dp,
                            color = Color(0x3300A884),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .background(Color(0xFFF8FAFC))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Other devices should browse to:",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = serverUrl,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF008069),
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Copy and Toggle controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Server Address", serverUrl)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Gateway URL copied! 📋", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = WhatsappGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("COPY LINK", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Button(
                        onClick = onToggleServer,
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.5.dp, WhatsappGreen),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = WhatsappGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (isServerRunning) "STOP" else "START",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Stats grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Card 1: Active Users
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFF1F5F9))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("👥", fontSize = 24.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$activeConnectionsCount",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = "ACTIVE USERS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            // Card 2: Messages DB size
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFF1F5F9))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("💬", fontSize = 24.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$messagesSize",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = "MESSAGES DB",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        }

        // Web Interface Preview Mockup
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "WEB INTERFACE PREVIEW",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B)
                    )
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFFDBEAFE))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "SQLITE SYNCED",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF2563EB)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // WhatsApp-like chat view mockup
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFE5DDD5))
                        .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(12.dp))
                ) {
                    // Chat window simulation
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (lastMessages.isEmpty()) {
                            // Empty state placeholder bubbles (mirrors the Design mockup)
                            MockBubble(
                                sender = "User 43.15",
                                text = "Has anyone uploaded the PDF?",
                                isMine = false,
                                timestamp = "04:15 PM"
                            )
                            MockBubble(
                                sender = "You",
                                text = "Yes, I just attached it to the hub! 📎",
                                isMine = true,
                                timestamp = "04:16 PM ✓✓"
                            )
                        } else {
                            // Display the real last messages dynamically
                            lastMessages.forEach { msg ->
                                val formattedTime = try {
                                    SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(msg.timestamp))
                                } catch (e: Exception) {
                                    ""
                                }
                                MockBubble(
                                    sender = msg.sender,
                                    text = msg.text.ifEmpty { "Attached File: ${msg.fileName ?: "Document"}" },
                                    isMine = msg.sender.lowercase() == "you" || msg.sender.lowercase() == "host",
                                    timestamp = formattedTime
                                )
                            }
                        }
                    }

                    // Mock Bottom Input Panel
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF0F2F5))
                            .border(width = 1.dp, color = Color(0x0C000000))
                            .padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(CircleShape)
                                .background(Color.White)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "😊 Type a message...",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                                Text(
                                    text = "📎",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00A884)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "➔",
                                fontSize = 11.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// Custom mockup preview chat bubble
@Composable
fun MockBubble(
    sender: String,
    text: String,
    isMine: Boolean,
    timestamp: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 220.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 8.dp,
                        topEnd = 8.dp,
                        bottomStart = if (isMine) 8.dp else 0.dp,
                        bottomEnd = if (isMine) 0.dp else 8.dp
                    )
                )
                .background(if (isMine) Color(0xFFDCF8C6) else Color.White)
                .padding(8.dp)
        ) {
            if (!isMine) {
                Text(
                    text = sender,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    color = Color(0xFF34B7F1),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = text,
                fontSize = 11.sp,
                color = Color(0xFF334155)
            )
            Text(
                text = timestamp,
                fontSize = 8.sp,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// TAB CONTENT 2: LOGS
@Composable
fun LogsTabContent(
    messages: List<ChatMessage>,
    onDelete: (ChatMessage) -> Unit,
    onClearAll: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Live SQLite Chat History",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Text(
                    text = "Total messages stored: ${messages.size}",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )
            }

            if (messages.isNotEmpty()) {
                Button(
                    onClick = onClearAll,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("CLEAR ALL", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        if (messages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("💬", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No Messages Yet",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Messages uploaded by local devices will appear here in real-time.",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .border(width = 1.dp, color = Color(0xFFE2E8F0), shape = RoundedCornerShape(20.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    NativeMessageBubble(msg) { onDelete(msg) }
                }
            }
        }
    }
}

// TAB CONTENT 3: FILES
@Composable
fun FilesTabContent(
    uploadedFiles: List<File>,
    onDeleteFile: (File) -> Unit,
    onClearAllFiles: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Local Uploads Explorer",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Text(
                    text = "Files saved locally: ${uploadedFiles.size}",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )
            }

            if (uploadedFiles.isNotEmpty()) {
                Button(
                    onClick = onClearAllFiles,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("CLEAR ALL", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        if (uploadedFiles.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📂", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No Uploaded Files",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Attachments uploaded by local clients will be browsable here.",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .border(width = 1.dp, color = Color(0xFFE2E8F0), shape = RoundedCornerShape(20.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uploadedFiles) { file ->
                    FileAttachmentRow(file = file, onDelete = { onDeleteFile(file) })
                }
            }
        }
    }
}

// Row component representing an uploaded file entry
@Composable
fun FileAttachmentRow(
    file: File,
    onDelete: () -> Unit
) {
    val fileExtension = file.extension.lowercase()
    val fileSizeInKb = remember(file) { file.length() / 1024 }
    val formattedDate = remember(file) {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(file.lastModified()))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF8FAFC))
            .border(width = 1.dp, color = Color(0xFFE2E8F0), shape = RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFE1F7F0)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (fileExtension) {
                        "jpg", "jpeg", "png", "gif", "webp" -> Icons.Default.Image
                        "mp4", "mkv", "avi" -> Icons.Default.PlayCircle
                        else -> Icons.Default.Description
                    },
                    contentDescription = "File Type",
                    tint = WhatsappGreen,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color(0xFF1E293B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$fileSizeInKb KB  •  $formattedDate",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
            }
        }

        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete File",
                tint = Color(0xFFEF4444),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// TAB CONTENT 4: SETTINGS
@Composable
fun SettingsTabContent(
    serverManager: ChatServerManager,
    messagesCount: Int,
    uploadedFilesCount: Int,
    totalFilesSize: Long,
    activeIp: String,
    isServerRunning: Boolean,
    onWipeDb: () -> Unit,
    onWipeFiles: () -> Unit,
    onRecheckIp: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "System Administration Panel",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B)
        )

        // Server Configuration Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Config", tint = WhatsappGreen)
                    Text("SERVER SETTINGS", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF64748B))
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Hotspot IP Gateway Address", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text(activeIp, fontSize = 12.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                    }
                    IconButton(onClick = onRecheckIp) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = WhatsappGreen)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column {
                    Text("Server Configuration Port", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text("8080 (Default for Local Hotspots)", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }

        // Storage details card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Storage, contentDescription = "Storage", tint = WhatsappGreen)
                    Text("STORAGE CAPACITY", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF64748B))
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total SQLite Database messages:", fontSize = 13.sp)
                    Text("$messagesCount", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Attachments saved on phone:", fontSize = 13.sp)
                    Text("$uploadedFilesCount files", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Combined attachments size:", fontSize = 13.sp)
                    val totalSizeKb = totalFilesSize / 1024
                    Text(
                        text = if (totalSizeKb > 1024) "${totalSizeKb / 1024} MB" else "$totalSizeKb KB",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onWipeDb,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Wipe Messages", fontSize = 11.sp)
                    }

                    Button(
                        onClick = onWipeFiles,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Wipe Attachments", fontSize = 11.sp)
                    }
                }
            }
        }

        // Project Description / Info card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = "Info", tint = WhatsappGreen)
                    Text("ABOUT LOCAL CHAT HUB", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF64748B))
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Chat Hub Host Admin v1.0",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "This application hosts a lightweight, multi-threaded Ktor server entirely on your device. Other clients on your portable Wi-Fi Hotspot network can browse to your IP to participate in real-time, low-latency chats and exchange images/videos without any cellular data usage.",
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = Color(0xFF475569)
                )
            }
        }
    }
}

@Composable
fun NativeMessageBubble(
    msg: ChatMessage,
    onDelete: () -> Unit
) {
    val formattedTime = remember(msg.timestamp) {
        try {
            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
            sdf.format(Date(msg.timestamp))
        } catch (e: Exception) {
            ""
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFF8F9FA))
                .border(1.dp, Color(0xFFEDEDED), RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            // Sender & Time Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = msg.sender,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = WhatsappGreen,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = formattedTime,
                    fontSize = 10.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Attachment Banner if present
            if (msg.fileUrl != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFE9F8F5))
                        .padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (msg.fileType) {
                            "image" -> Icons.Default.Image
                            "video" -> Icons.Default.PlayCircle
                            else -> Icons.Default.Description
                        },
                        contentDescription = "File Icon",
                        tint = WhatsappGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = msg.fileName ?: "Attachment",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.DarkGray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Message text
            if (msg.text.isNotEmpty()) {
                Text(
                    text = msg.text,
                    fontSize = 13.sp,
                    color = Color.DarkGray
                )
            }
        }

        // Native delete action
        IconButton(
            onClick = onDelete,
            modifier = Modifier.padding(start = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete Message",
                tint = Color.LightGray,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

