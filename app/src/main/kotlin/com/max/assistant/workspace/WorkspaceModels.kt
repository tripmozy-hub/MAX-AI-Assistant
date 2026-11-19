package com.max.assistant.workspace

import com.max.assistant.research.ResearchReport
import java.util.UUID

/**
 * Supported types of content panels inside the MAX Holographic Workspace.
 */
enum class PanelType(val displayName: String) {
    VIDEO_SEARCH("Video Intelligence"),
    VIDEO_PLAYER("Media Playback"),
    WEB_SEARCH("Web Intelligence"),
    IMAGE_SEARCH("Visual Telemetry"),
    RESEARCH_INFO("Deep Research"),
    CHARTS_GRAPHS("Holographic Analytics"),
    AI_SUMMARY("Neural Synthesis"),
    SYSTEM_ACTIVITY("Live Activity HUD"),
    LIVE_TASK("Process Monitor")
}

/**
 * Responsive layout modes for workspace tabs.
 */
enum class WorkspaceLayoutMode(val displayName: String) {
    GRID_2X("Dual Column Grid"),
    GRID_3X("Tri-Column Matrix"),
    HERO_SPLIT("Hero + Telemetry"),
    QUADRANT("4-Quadrant Wall"),
    FREEFORM_FLOATING("Freeform Floating"),
    FULLSCREEN_FOCUS("Focus Mode")
}

/**
 * Representation of a video result.
 */
data class VideoItem(
    val id: String,
    val title: String,
    val channelTitle: String,
    val duration: String = "00:00",
    val description: String = "",
    val thumbnailUrl: String = "https://img.youtube.com/vi/$id/hqdefault.jpg",
    val embedUrl: String = "https://www.youtube.com/embed/$id?autoplay=1&enablejsapi=1&playsinline=1",
    val watchUrl: String = "https://www.youtube.com/watch?v=$id",
    val source: String = "YouTube",
    val viewCount: String = ""
)

/**
 * Representation of data for holographic charts and graphs.
 */
data class ChartData(
    val title: String,
    val subtitle: String = "Relative Neural Metric (%)",
    val labels: List<String>,
    val values: List<Float>,
    val chartType: String = "LINE" // LINE, BAR, RADIAL
)

/**
 * State of an individual holographic window panel.
 */
data class WorkspacePanelState(
    val id: String = UUID.randomUUID().toString(),
    val tabId: String,
    val title: String,
    val panelType: PanelType,
    val videoList: List<VideoItem> = emptyList(),
    val activeVideo: VideoItem? = null,
    val researchReport: ResearchReport? = null,
    val chartData: ChartData? = null,
    val summaryText: String? = null,
    val webUrl: String? = null,
    val isMinimized: Boolean = false,
    val isExpanded: Boolean = false,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val widthDp: Int = 340,
    val heightDp: Int = 280,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Browser-style search/research tab containing independent panels and layout.
 */
data class WorkspaceTab(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val searchQuery: String = "",
    val panels: List<WorkspacePanelState> = emptyList(),
    val layoutMode: WorkspaceLayoutMode = WorkspaceLayoutMode.GRID_2X,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Global UI state of the MAX Holographic Workspace.
 */
data class WorkspaceUiState(
    val isVisible: Boolean = false,
    val isCompactCore: Boolean = false,
    val tabs: List<WorkspaceTab> = emptyList(),
    val activeTabId: String = "",
    val closedTabsStack: List<WorkspaceTab> = emptyList()
) {
    val activeTab: WorkspaceTab?
        get() = tabs.find { it.id == activeTabId } ?: tabs.firstOrNull()
}
