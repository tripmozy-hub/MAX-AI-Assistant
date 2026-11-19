package com.max.assistant.workspace

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import kotlin.math.roundToInt

// In-memory cache for video thumbnails
private val thumbnailMemoryCache = android.util.LruCache<String, ImageBitmap>(60)

@Composable
fun rememberAsyncImage(url: String): ImageBitmap? {
    var imageBitmap by remember(url) { mutableStateOf(thumbnailMemoryCache.get(url)) }

    LaunchedEffect(url) {
        if (url.isBlank() || thumbnailMemoryCache.get(url) != null) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            try {
                val conn = URL(url).openConnection() as java.net.HttpURLConnection
                conn.connectTimeout = 4000
                conn.readTimeout = 4000
                conn.setRequestProperty("User-Agent", "MAX-Assistant-Android")
                if (conn.responseCode == 200) {
                    val bytes = conn.inputStream.use { it.readBytes() }
                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (bmp != null) {
                        val img = bmp.asImageBitmap()
                        thumbnailMemoryCache.put(url, img)
                        withContext(Dispatchers.Main) {
                            imageBitmap = img
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }
    return imageBitmap
}

/**
 * Full-screen holographic multi-window workspace for MAX.
 */
@Composable
fun HolographicWorkspaceView(
    workspaceState: WorkspaceUiState,
    onCloseWorkspace: () -> Unit,
    onToggleCompactCore: () -> Unit,
    onCreateTab: (String) -> Unit,
    onSwitchTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onRenameTab: (String, String) -> Unit,
    onSetLayoutMode: (String, WorkspaceLayoutMode) -> Unit,
    onClosePanel: (String, String) -> Unit,
    onMinimizePanel: (String, String) -> Unit,
    onMaximizePanel: (String, String) -> Unit,
    onRestorePanel: (String, String) -> Unit,
    onUpdatePosition: (String, String, Float, Float) -> Unit,
    onPlayVideo: (VideoItem) -> Unit,
    onSummarizeVideo: (VideoItem) -> Unit,
    onSearchVideos: (String) -> Unit,
    onSpeakSummary: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val activeTab = workspaceState.activeTab ?: return
    var searchInput by remember { mutableStateOf("") }
    var showNewTabDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF02060C))
    ) {
        // 1. Subtle Holographic Grid Pattern Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = 40.dp.toPx()
            val gridColor = Color(0xFF00E5FF).copy(alpha = 0.04f)
            var x = 0f
            while (x < size.width) {
                drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                x += step
            }
            var y = 0f
            while (y < size.height) {
                drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                y += step
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // 2. Top Header Bar: Title, Search, Tab Management & Mode Selectors
            WorkspaceHeader(
                activeTab = activeTab,
                tabs = workspaceState.tabs,
                searchInput = searchInput,
                onSearchInputChange = { searchInput = it },
                onExecuteSearch = {
                    if (searchInput.isNotBlank()) {
                        onSearchVideos(searchInput)
                        searchInput = ""
                    }
                },
                onSwitchTab = onSwitchTab,
                onCloseTab = onCloseTab,
                onNewTabClick = { showNewTabDialog = true },
                onSetLayoutMode = { mode -> onSetLayoutMode(activeTab.id, mode) },
                onToggleCompactCore = onToggleCompactCore,
                onCloseWorkspace = onCloseWorkspace
            )

            // 3. Main Multi-Window Body Canvas
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                val nonMinimizedPanels = activeTab.panels.filter { !it.isMinimized }
                val expandedPanel = nonMinimizedPanels.find { it.isExpanded }

                if (expandedPanel != null) {
                    // Full-Focus Expanded Window View
                    HolographicPanelCard(
                        panel = expandedPanel,
                        tabId = activeTab.id,
                        isFreeform = false,
                        onClose = { onClosePanel(activeTab.id, expandedPanel.id) },
                        onMinimize = { onMinimizePanel(activeTab.id, expandedPanel.id) },
                        onToggleExpand = { onRestorePanel(activeTab.id, expandedPanel.id) },
                        onPlayVideo = onPlayVideo,
                        onSummarizeVideo = onSummarizeVideo,
                        onSpeakSummary = onSpeakSummary,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    )
                } else if (activeTab.layoutMode == WorkspaceLayoutMode.FREEFORM_FLOATING) {
                    // Freeform Draggable / Floating Window Space
                    Box(modifier = Modifier.fillMaxSize()) {
                        nonMinimizedPanels.forEach { panel ->
                            key(panel.id) {
                                HolographicPanelCard(
                                    panel = panel,
                                    tabId = activeTab.id,
                                    isFreeform = true,
                                    onClose = { onClosePanel(activeTab.id, panel.id) },
                                    onMinimize = { onMinimizePanel(activeTab.id, panel.id) },
                                    onToggleExpand = { onMaximizePanel(activeTab.id, panel.id) },
                                    onDrag = { dx, dy -> onUpdatePosition(activeTab.id, panel.id, dx, dy) },
                                    onPlayVideo = onPlayVideo,
                                    onSummarizeVideo = onSummarizeVideo,
                                    onSpeakSummary = onSpeakSummary,
                                    modifier = Modifier
                                        .offset { IntOffset(panel.offsetX.roundToInt(), panel.offsetY.roundToInt()) }
                                        .size(panel.widthDp.dp, panel.heightDp.dp)
                                )
                            }
                        }
                    }
                } else {
                    // Responsive Grid / Video Wall Mode
                    if (nonMinimizedPanels.isEmpty()) {
                        EmptyWorkspaceState(
                            onSearchSuggestion = { query -> onSearchVideos(query) }
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(12.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(nonMinimizedPanels, key = { it.id }) { panel ->
                                HolographicPanelCard(
                                    panel = panel,
                                    tabId = activeTab.id,
                                    isFreeform = false,
                                    onClose = { onClosePanel(activeTab.id, panel.id) },
                                    onMinimize = { onMinimizePanel(activeTab.id, panel.id) },
                                    onToggleExpand = { onMaximizePanel(activeTab.id, panel.id) },
                                    onPlayVideo = onPlayVideo,
                                    onSummarizeVideo = onSummarizeVideo,
                                    onSpeakSummary = onSpeakSummary,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 280.dp, max = 460.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 4. Minimized Window Dock / Tray (bottom telemetry strip)
            val minimizedPanels = activeTab.panels.filter { it.isMinimized }
            if (minimizedPanels.isNotEmpty()) {
                MinimizedWindowsTray(
                    minimizedPanels = minimizedPanels,
                    onRestorePanel = { pId -> onRestorePanel(activeTab.id, pId) }
                )
            }
        }

        // New Tab Dialog
        if (showNewTabDialog) {
            var newTabTitleInput by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showNewTabDialog = false },
                containerColor = Color(0xFF06101D),
                titleContentColor = Color(0xFF00E5FF),
                textContentColor = Color(0xFFC0D0E0),
                title = { Text("INITIALIZE NEW RESEARCH TAB", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                text = {
                    OutlinedTextField(
                        value = newTabTitleInput,
                        onValueChange = { newTabTitleInput = it },
                        label = { Text("Tab Title / Research Topic") },
                        placeholder = { Text("e.g. Space Propulsion, Quantum AI") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00E5FF),
                            unfocusedBorderColor = Color(0xFF00E5FF).copy(alpha = 0.3f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (newTabTitleInput.isNotBlank()) {
                                onCreateTab(newTabTitleInput.trim())
                                showNewTabDialog = false
                            }
                        }
                    ) {
                        Text("INITIALIZE", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showNewTabDialog = false }) {
                        Text("CANCEL", color = Color(0xFF8899AA))
                    }
                }
            )
        }
    }
}

/**
 * Top header containing title, tab scroll strip, search bar, and layout mode selector.
 */
@Composable
private fun WorkspaceHeader(
    activeTab: WorkspaceTab,
    tabs: List<WorkspaceTab>,
    searchInput: String,
    onSearchInputChange: (String) -> Unit,
    onExecuteSearch: () -> Unit,
    onSwitchTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onNewTabClick: () -> Unit,
    onSetLayoutMode: (WorkspaceLayoutMode) -> Unit,
    onToggleCompactCore: () -> Unit,
    onCloseWorkspace: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF050B14))
            .border(BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.2f)))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // Top line: Branding & Action icons
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(Color(0xFF00E5FF), CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "MAX HOLOGRAPHIC WORKSPACE",
                    color = Color(0xFF00E5FF),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Layout Mode selector chips
                IconButton(
                    onClick = {
                        val nextMode = when (activeTab.layoutMode) {
                            WorkspaceLayoutMode.GRID_2X -> WorkspaceLayoutMode.QUADRANT
                            WorkspaceLayoutMode.QUADRANT -> WorkspaceLayoutMode.FREEFORM_FLOATING
                            WorkspaceLayoutMode.FREEFORM_FLOATING -> WorkspaceLayoutMode.GRID_2X
                            else -> WorkspaceLayoutMode.GRID_2X
                        }
                        onSetLayoutMode(nextMode)
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = when (activeTab.layoutMode) {
                            WorkspaceLayoutMode.FREEFORM_FLOATING -> Icons.Default.Layers
                            WorkspaceLayoutMode.QUADRANT -> Icons.Default.Dashboard
                            else -> Icons.Default.ViewAgenda
                        },
                        contentDescription = "Layout Mode",
                        tint = Color(0xFF00E5FF)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))
                IconButton(
                    onClick = onCloseWorkspace,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Workspace",
                        tint = Color(0xFFFF5252)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Search Bar row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchInput,
                onValueChange = onSearchInputChange,
                placeholder = {
                    Text(
                        "Search videos, deep research, or topics...",
                        color = Color(0xFF607D8B),
                        fontSize = 12.sp
                    )
                },
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(18.dp))
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00E5FF),
                    unfocusedBorderColor = Color(0xFF00E5FF).copy(alpha = 0.25f),
                    focusedContainerColor = Color(0xFF081220),
                    unfocusedContainerColor = Color(0xFF081220),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onExecuteSearch,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Text("SCAN", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Tab Strip Row
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(tabs, key = { it.id }) { tab ->
                val isActive = tab.id == activeTab.id
                Surface(
                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                    color = if (isActive) Color(0xFF0A1828) else Color(0xFF070E18),
                    border = BorderStroke(
                        1.dp,
                        if (isActive) Color(0xFF00E5FF) else Color(0xFF00E5FF).copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.clickable { onSwitchTab(tab.id) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = tab.title,
                            color = if (isActive) Color(0xFF00E5FF) else Color(0xFF90A4AE),
                            fontSize = 11.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Tab",
                            tint = if (isActive) Color(0xFF00E5FF) else Color(0xFF607D8B),
                            modifier = Modifier
                                .size(14.dp)
                                .clickable { onCloseTab(tab.id) }
                        )
                    }
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF0A1828),
                    border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f)),
                    modifier = Modifier.clickable { onNewTabClick() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "New Tab", tint = Color(0xFF00E5FF), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("NEW TAB", color = Color(0xFF00E5FF), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Reusable Holographic Window Panel Card.
 */
@Composable
fun HolographicPanelCard(
    panel: WorkspacePanelState,
    tabId: String,
    isFreeform: Boolean,
    onClose: () -> Unit,
    onMinimize: () -> Unit,
    onToggleExpand: () -> Unit,
    onPlayVideo: (VideoItem) -> Unit,
    onSummarizeVideo: (VideoItem) -> Unit,
    onSpeakSummary: (String) -> Unit,
    onDrag: ((Float, Float) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF071220)),
        border = BorderStroke(1.dp, Brush.linearGradient(listOf(Color(0xFF00E5FF).copy(alpha = 0.6f), Color(0xFF0052FF).copy(alpha = 0.3f)))),
        modifier = modifier
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Panel Header Bar (Draggable in freeform mode)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0A1B2F))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .then(
                        if (isFreeform && onDrag != null) {
                            Modifier.pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    onDrag(dragAmount.x, dragAmount.y)
                                }
                            }
                        } else Modifier
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (panel.panelType) {
                            PanelType.VIDEO_SEARCH -> Icons.Default.Videocam
                            PanelType.VIDEO_PLAYER -> Icons.Default.PlayCircle
                            PanelType.RESEARCH_INFO -> Icons.Default.MenuBook
                            PanelType.CHARTS_GRAPHS -> Icons.Default.Analytics
                            PanelType.AI_SUMMARY -> Icons.Default.AutoAwesome
                            else -> Icons.Default.Window
                        },
                        contentDescription = null,
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = panel.title,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Window Controls
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onMinimize, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Remove, contentDescription = "Minimize", tint = Color(0xFF90A4AE), modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onToggleExpand, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = if (panel.isExpanded) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            contentDescription = "Expand",
                            tint = Color(0xFF90A4AE),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFFFF5252), modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Panel Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFF030914))
            ) {
                if (panel.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF00E5FF), strokeWidth = 2.dp)
                    }
                } else {
                    when (panel.panelType) {
                        PanelType.VIDEO_SEARCH -> VideoSearchPanelContent(
                            videos = panel.videoList,
                            onPlayVideo = onPlayVideo,
                            onSummarizeVideo = onSummarizeVideo
                        )
                        PanelType.VIDEO_PLAYER -> VideoPlayerPanelContent(
                            video = panel.activeVideo,
                            onSummarizeVideo = onSummarizeVideo
                        )
                        PanelType.RESEARCH_INFO -> ResearchReportPanelContent(
                            report = panel.researchReport
                        )
                        PanelType.CHARTS_GRAPHS -> HolographicChartPanelContent(
                            chart = panel.chartData
                        )
                        PanelType.AI_SUMMARY -> AiSummaryPanelContent(
                            summaryText = panel.summaryText,
                            onSpeakSummary = onSpeakSummary
                        )
                        else -> Text(
                            "Holographic Telemetry Node Online",
                            color = Color(0xFF8899AA),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Content for Video Search Results Panel.
 */
@Composable
private fun VideoSearchPanelContent(
    videos: List<VideoItem>,
    onPlayVideo: (VideoItem) -> Unit,
    onSummarizeVideo: (VideoItem) -> Unit
) {
    if (videos.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No video streams retrieved. Initiate a voice scan.", color = Color(0xFF607D8B), fontSize = 12.sp)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(videos, key = { it.id }) { video ->
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF081525),
                border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Video Thumbnail with Duration badge
                    Box(
                        modifier = Modifier
                            .width(110.dp)
                            .height(65.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF020A14))
                    ) {
                        val img = rememberAsyncImage(video.thumbnailUrl)
                        if (img != null) {
                            Image(
                                bitmap = img,
                                contentDescription = video.title,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Videocam, contentDescription = null, tint = Color(0xFF00E5FF).copy(alpha = 0.4f))
                            }
                        }

                        // Duration badge
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color.Black.copy(alpha = 0.75f),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(4.dp)
                        ) {
                            Text(
                                text = video.duration,
                                color = Color.White,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Video Info & Action buttons
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = video.title,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${video.channelTitle} • ${video.viewCount.ifBlank { video.source }}",
                            color = Color(0xFF90A4AE),
                            fontSize = 10.sp,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Play Button
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF00E5FF),
                                modifier = Modifier.clickable { onPlayVideo(video) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.Black, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("PLAY", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Summarize Button
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF0A2038),
                                border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f)),
                                modifier = Modifier.clickable { onSummarizeVideo(video) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = "Summarize", tint = Color(0xFF00E5FF), modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("SUMMARIZE", color = Color(0xFF00E5FF), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Embedded Video Player utilizing Android WebView with authorized YouTube embed iframe.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun VideoPlayerPanelContent(
    video: VideoItem?,
    onSummarizeVideo: (VideoItem) -> Unit
) {
    val context = LocalContext.current
    if (video == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No active video selected", color = Color(0xFF607D8B), fontSize = 12.sp)
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.Black)
        ) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            mediaPlaybackRequiresUserGesture = false
                            loadWithOverviewMode = true
                            useWideViewPort = true
                        }
                        webChromeClient = WebChromeClient()
                        webViewClient = WebViewClient()

                        val html = """
                            <!DOCTYPE html>
                            <html>
                            <head>
                                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                                <style>
                                    body, html { margin: 0; padding: 0; width: 100%; height: 100%; background: #000; overflow: hidden; }
                                    iframe { width: 100%; height: 100%; border: none; }
                                </style>
                            </head>
                            <body>
                                <iframe src="${video.embedUrl}" allow="autoplay; encrypted-media; picture-in-picture" allowfullscreen></iframe>
                            </body>
                            </html>
                        """.trimIndent()
                        loadDataWithBaseURL("https://www.youtube.com", html, "text/html", "UTF-8", null)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Under-player metadata and controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF06101D))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(video.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${video.channelTitle} • ${video.duration}", color = Color(0xFF00E5FF), fontSize = 10.sp)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconButton(
                    onClick = { onSummarizeVideo(video) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "Summarize", tint = Color(0xFF00E5FF), modifier = Modifier.size(16.dp))
                }

                IconButton(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(video.watchUrl))
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = "Open External", tint = Color(0xFF90A4AE), modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

/**
 * Content for Deep Research Reports Panel.
 */
@Composable
private fun ResearchReportPanelContent(
    report: com.max.assistant.research.ResearchReport?
) {
    if (report == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No active research report loaded.", color = Color(0xFF607D8B), fontSize = 12.sp)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "QUERY: ${report.query.uppercase()}",
                color = Color(0xFF00E5FF),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = report.spokenSummary,
                color = Color.White,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
        }

        if (report.detailedContent.isNotBlank()) {
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "DETAILED SYNTHESIS",
                    color = Color(0xFF90A4AE),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = report.detailedContent,
                    color = Color(0xFFCFD8DC),
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }

        if (report.citations.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "VERIFIED CITATIONS (${report.citations.size})",
                    color = Color(0xFF00E5FF),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
            items(report.citations) { cit ->
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF061322),
                    border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(cit.title, color = Color(0xFF00E5FF), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        if (cit.snippet.isNotBlank()) {
                            Text(cit.snippet, color = Color(0xFFB0BEC5), fontSize = 10.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Procedural Holographic Chart Panel.
 */
@Composable
private fun HolographicChartPanelContent(
    chart: ChartData?
) {
    if (chart == null || chart.values.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No telemetry telemetry vectors.", color = Color(0xFF607D8B), fontSize = 12.sp)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Text(chart.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(chart.subtitle, color = Color(0xFF00E5FF), fontSize = 10.sp, fontFamily = FontFamily.Monospace)

        Spacer(modifier = Modifier.height(12.dp))

        // Procedural Glow Line / Bar Canvas
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val maxVal = (chart.values.maxOrNull() ?: 100f).coerceAtLeast(1f)
            val padding = 16.dp.toPx()
            val w = size.width - padding * 2
            val h = size.height - padding * 2

            // Draw horizontal telemetry grid lines
            for (i in 0..4) {
                val y = padding + (h / 4f) * i
                drawLine(Color(0xFF00E5FF).copy(alpha = 0.15f), Offset(padding, y), Offset(size.width - padding, y), strokeWidth = 1f)
            }

            // Draw line chart points and smooth gradient fill
            val path = Path()
            val fillPath = Path()
            val stepX = w / (chart.values.size - 1).coerceAtLeast(1)

            chart.values.forEachIndexed { i, v ->
                val normY = padding + h - (v / maxVal) * h
                val ptX = padding + i * stepX
                if (i == 0) {
                    path.moveTo(ptX, normY)
                    fillPath.moveTo(ptX, size.height - padding)
                    fillPath.lineTo(ptX, normY)
                } else {
                    path.lineTo(ptX, normY)
                    fillPath.lineTo(ptX, normY)
                }
                if (i == chart.values.size - 1) {
                    fillPath.lineTo(ptX, size.height - padding)
                    fillPath.close()
                }

                // Data point glowing circle
                drawCircle(Color(0xFF00E5FF), radius = 4.dp.toPx(), center = Offset(ptX, normY))
                drawCircle(Color.White, radius = 2.dp.toPx(), center = Offset(ptX, normY))
            }

            // Gradient Under-Fill
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF00E5FF).copy(alpha = 0.25f), Color.Transparent)
                )
            )

            // Outer Neon Line
            drawPath(
                path = path,
                color = Color(0xFF00E5FF),
                style = Stroke(width = 2.5.dp.toPx())
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Label row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            chart.labels.forEach { l ->
                Text(l, color = Color(0xFF78909C), fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

/**
 * AI-Generated Summary Panel Content.
 */
@Composable
private fun AiSummaryPanelContent(
    summaryText: String?,
    onSpeakSummary: (String) -> Unit
) {
    if (summaryText.isNullOrBlank()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No summary synthesized.", color = Color(0xFF607D8B), fontSize = 12.sp)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("NEURAL SYNTHESIS REPORT", color = Color(0xFF00E5FF), fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }

            IconButton(onClick = { onSpeakSummary(summaryText) }, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.VolumeUp, contentDescription = "Read Aloud", tint = Color(0xFF00E5FF), modifier = Modifier.size(16.dp))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = summaryText,
            color = Color(0xFFECEFF1),
            fontSize = 12.sp,
            lineHeight = 18.sp
        )
    }
}

/**
 * Minimized Windows Tray at the bottom of the workspace.
 */
@Composable
private fun MinimizedWindowsTray(
    minimizedPanels: List<WorkspacePanelState>,
    onRestorePanel: (String) -> Unit
) {
    Surface(
        color = Color(0xFF040B14),
        border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item {
                Text("MINIMIZED TRAY:", color = Color(0xFF00E5FF), fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
            items(minimizedPanels, key = { it.id }) { panel ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF0A1D32),
                    border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f)),
                    modifier = Modifier.clickable { onRestorePanel(panel.id) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.OpenInFull, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(panel.title, color = Color.White, fontSize = 10.sp, maxLines = 1)
                    }
                }
            }
        }
    }
}

/**
 * Placeholder empty state with quick suggestion chips.
 */
@Composable
private fun EmptyWorkspaceState(
    onSearchSuggestion: (String) -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(Icons.Default.Layers, contentDescription = null, tint = Color(0xFF00E5FF).copy(alpha = 0.6f), modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "HOLOGRAPHIC WORKSPACE ACTIVE",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Say \"MAX, show me videos about AI\" or tap a quick scan below, Sir.",
                color = Color(0xFF90A4AE),
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Quick suggestion chips
            val suggestions = listOf("Artificial Intelligence", "Space Propulsion", "Robotics Atlas", "Quantum Computing")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                suggestions.forEach { s ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF07182C),
                        border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.35f)),
                        modifier = Modifier.clickable { onSearchSuggestion(s) }
                    ) {
                        Text(
                            text = s,
                            color = Color(0xFF00E5FF),
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}
