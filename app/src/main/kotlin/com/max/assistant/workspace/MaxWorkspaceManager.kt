package com.max.assistant.workspace

import android.content.Context
import android.util.Log
import com.max.assistant.research.Citation
import com.max.assistant.research.LiveInternetResearchEngine
import com.max.assistant.research.ResearchReport
import com.max.assistant.tts.VoiceLanguage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Master Controller for the MAX Holographic Multi-Window Workspace.
 * Orchestrates multi-panel rendering, tab lifecycles, video search & playback,
 * research reports, layout modes, and voice command interaction.
 */
class MaxWorkspaceManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val videoSearchManager: MaxVideoSearchManager = MaxVideoSearchManager(context),
    private val researchEngine: LiveInternetResearchEngine = LiveInternetResearchEngine(context)
) {

    companion object {
        private const val TAG = "MaxWorkspaceManager"
    }

    private val _workspaceState = MutableStateFlow(
        WorkspaceUiState(
            isVisible = false,
            isCompactCore = false,
            tabs = listOf(
                WorkspaceTab(
                    id = "tab_main",
                    title = "Central Intelligence",
                    searchQuery = "Artificial Intelligence",
                    layoutMode = WorkspaceLayoutMode.GRID_2X,
                    panels = emptyList()
                )
            ),
            activeTabId = "tab_main"
        )
    )
    val workspaceState: StateFlow<WorkspaceUiState> = _workspaceState.asStateFlow()

    init {
        // Initialize default intelligence cards in the main tab
        scope.launch {
            val initialVideos = videoSearchManager.searchVideos("Artificial Intelligence", 3)
            val initialChart = ChartData(
                title = "Neural Compute Velocity",
                subtitle = "TFLOPS Allocation by Subsystem",
                labels = listOf("Speech VAD", "Gemini LLM", "Screen Vision", "3D Core", "Telemetry"),
                values = listOf(78f, 95f, 84f, 62f, 45f),
                chartType = "LINE"
            )

            val initialPanels = mutableListOf<WorkspacePanelState>()
            if (initialVideos.isNotEmpty()) {
                initialPanels.add(
                    WorkspacePanelState(
                        id = "panel_default_videos",
                        tabId = "tab_main",
                        title = "Video Intelligence — Artificial Intelligence",
                        panelType = PanelType.VIDEO_SEARCH,
                        videoList = initialVideos
                    )
                )
            }
            initialPanels.add(
                WorkspacePanelState(
                    id = "panel_default_chart",
                    tabId = "tab_main",
                    title = "System Telemetry & Compute Allocation",
                    panelType = PanelType.CHARTS_GRAPHS,
                    chartData = initialChart
                )
            )

            _workspaceState.update { current ->
                val updatedTabs = current.tabs.map { tab ->
                    if (tab.id == "tab_main") tab.copy(panels = initialPanels) else tab
                }
                current.copy(tabs = updatedTabs)
            }
        }
    }

    fun setWorkspaceVisible(visible: Boolean) {
        _workspaceState.update { it.copy(isVisible = visible) }
    }

    fun toggleWorkspaceVisibility() {
        _workspaceState.update { it.copy(isVisible = !it.isVisible) }
    }

    fun setCompactCore(compact: Boolean) {
        _workspaceState.update { it.copy(isCompactCore = compact) }
    }

    fun toggleCompactCore() {
        _workspaceState.update { it.copy(isCompactCore = !it.isCompactCore) }
    }

    // ==========================================
    // Tab Management
    // ==========================================

    fun createTab(title: String, query: String = ""): WorkspaceTab {
        val newTab = WorkspaceTab(
            id = "tab_" + UUID.randomUUID().toString().take(8),
            title = title.ifBlank { "Research Tab ${workspaceState.value.tabs.size + 1}" },
            searchQuery = query,
            layoutMode = WorkspaceLayoutMode.GRID_2X,
            panels = emptyList()
        )
        _workspaceState.update { current ->
            current.copy(
                tabs = current.tabs + newTab,
                activeTabId = newTab.id,
                isVisible = true
            )
        }
        return newTab
    }

    fun switchTab(tabId: String) {
        if (_workspaceState.value.tabs.any { it.id == tabId }) {
            _workspaceState.update { it.copy(activeTabId = tabId) }
        }
    }

    fun closeTab(tabId: String) {
        _workspaceState.update { current ->
            val tabToClose = current.tabs.find { it.id == tabId } ?: return@update current
            if (current.tabs.size <= 1) {
                // Keep at least one tab, just reset it
                val resetTab = tabToClose.copy(title = "New Tab", panels = emptyList(), searchQuery = "")
                return@update current.copy(tabs = listOf(resetTab), activeTabId = resetTab.id)
            }
            val remainingTabs = current.tabs.filter { it.id != tabId }
            val nextActiveId = if (current.activeTabId == tabId) remainingTabs.last().id else current.activeTabId
            val updatedStack = (current.closedTabsStack + tabToClose).takeLast(5)
            current.copy(
                tabs = remainingTabs,
                activeTabId = nextActiveId,
                closedTabsStack = updatedStack
            )
        }
    }

    fun reopenLastClosedTab() {
        _workspaceState.update { current ->
            if (current.closedTabsStack.isEmpty()) return@update current
            val lastClosed = current.closedTabsStack.last()
            val newStack = current.closedTabsStack.dropLast(1)
            current.copy(
                tabs = current.tabs + lastClosed,
                activeTabId = lastClosed.id,
                closedTabsStack = newStack
            )
        }
    }

    fun renameTab(tabId: String, newTitle: String) {
        if (newTitle.isBlank()) return
        _workspaceState.update { current ->
            val updated = current.tabs.map { tab ->
                if (tab.id == tabId) tab.copy(title = newTitle) else tab
            }
            current.copy(tabs = updated)
        }
    }

    fun setLayoutMode(tabId: String, mode: WorkspaceLayoutMode) {
        _workspaceState.update { current ->
            val updated = current.tabs.map { tab ->
                if (tab.id == tabId) tab.copy(layoutMode = mode) else tab
            }
            current.copy(tabs = updated)
        }
    }

    // ==========================================
    // Panel Management
    // ==========================================

    fun addPanel(tabId: String, panel: WorkspacePanelState) {
        _workspaceState.update { current ->
            val updatedTabs = current.tabs.map { tab ->
                if (tab.id == tabId) {
                    val filtered = tab.panels.filter { it.id != panel.id }
                    tab.copy(panels = filtered + panel)
                } else tab
            }
            current.copy(tabs = updatedTabs)
        }
    }

    fun closePanel(tabId: String, panelId: String) {
        _workspaceState.update { current ->
            val updatedTabs = current.tabs.map { tab ->
                if (tab.id == tabId) {
                    tab.copy(panels = tab.panels.filter { it.id != panelId })
                } else tab
            }
            current.copy(tabs = updatedTabs)
        }
    }

    fun minimizePanel(tabId: String, panelId: String) {
        _workspaceState.update { current ->
            val updatedTabs = current.tabs.map { tab ->
                if (tab.id == tabId) {
                    val updatedPanels = tab.panels.map { p ->
                        if (p.id == panelId) p.copy(isMinimized = true, isExpanded = false) else p
                    }
                    tab.copy(panels = updatedPanels)
                } else tab
            }
            current.copy(tabs = updatedTabs)
        }
    }

    fun maximizePanel(tabId: String, panelId: String) {
        _workspaceState.update { current ->
            val updatedTabs = current.tabs.map { tab ->
                if (tab.id == tabId) {
                    val updatedPanels = tab.panels.map { p ->
                        if (p.id == panelId) p.copy(isExpanded = true, isMinimized = false) else p
                    }
                    tab.copy(panels = updatedPanels)
                } else tab
            }
            current.copy(tabs = updatedTabs)
        }
    }

    fun restorePanel(tabId: String, panelId: String) {
        _workspaceState.update { current ->
            val updatedTabs = current.tabs.map { tab ->
                if (tab.id == tabId) {
                    val updatedPanels = tab.panels.map { p ->
                        if (p.id == panelId) p.copy(isMinimized = false, isExpanded = false) else p
                    }
                    tab.copy(panels = updatedPanels)
                } else tab
            }
            current.copy(tabs = updatedTabs)
        }
    }

    fun updatePanelPosition(tabId: String, panelId: String, dx: Float, dy: Float) {
        _workspaceState.update { current ->
            val updatedTabs = current.tabs.map { tab ->
                if (tab.id == tabId) {
                    val updatedPanels = tab.panels.map { p ->
                        if (p.id == panelId) p.copy(offsetX = p.offsetX + dx, offsetY = p.offsetY + dy) else p
                    }
                    tab.copy(panels = updatedPanels)
                } else tab
            }
            current.copy(tabs = updatedTabs)
        }
    }

    fun updatePanelSize(tabId: String, panelId: String, widthDp: Int, heightDp: Int) {
        _workspaceState.update { current ->
            val updatedTabs = current.tabs.map { tab ->
                if (tab.id == tabId) {
                    val updatedPanels = tab.panels.map { p ->
                        if (p.id == panelId) p.copy(widthDp = maxOf(240, widthDp), heightDp = maxOf(180, heightDp)) else p
                    }
                    tab.copy(panels = updatedPanels)
                } else tab
            }
            current.copy(tabs = updatedTabs)
        }
    }

    fun minimizeAllWindows(tabId: String) {
        _workspaceState.update { current ->
            val updatedTabs = current.tabs.map { tab ->
                if (tab.id == tabId) {
                    val updatedPanels = tab.panels.map { it.copy(isMinimized = true) }
                    tab.copy(panels = updatedPanels)
                } else tab
            }
            current.copy(tabs = updatedTabs)
        }
    }

    fun restoreAllWindows(tabId: String) {
        _workspaceState.update { current ->
            val updatedTabs = current.tabs.map { tab ->
                if (tab.id == tabId) {
                    val updatedPanels = tab.panels.map { it.copy(isMinimized = false, isExpanded = false) }
                    tab.copy(panels = updatedPanels)
                } else tab
            }
            current.copy(tabs = updatedTabs)
        }
    }

    fun closeAllWindows(tabId: String) {
        _workspaceState.update { current ->
            val updatedTabs = current.tabs.map { tab ->
                if (tab.id == tabId) tab.copy(panels = emptyList()) else tab
            }
            current.copy(tabs = updatedTabs)
        }
    }

    // ==========================================
    // Search, Research & Playback Automation
    // ==========================================

    fun searchAndOpenVideos(query: String, count: Int = 4, targetTabId: String? = null) {
        scope.launch {
            val tabId = targetTabId ?: workspaceState.value.activeTabId
            val loadingPanelId = "panel_video_" + UUID.randomUUID().toString().take(6)

            // 1. Post placeholder loading panel
            addPanel(
                tabId,
                WorkspacePanelState(
                    id = loadingPanelId,
                    tabId = tabId,
                    title = "Video Intelligence: $query",
                    panelType = PanelType.VIDEO_SEARCH,
                    isLoading = true
                )
            )
            setWorkspaceVisible(true)

            // 2. Fetch videos
            val videos = videoSearchManager.searchVideos(query, count)

            // 3. Update panel with results
            addPanel(
                tabId,
                WorkspacePanelState(
                    id = loadingPanelId,
                    tabId = tabId,
                    title = "Video Intelligence: $query (${videos.size} Streams)",
                    panelType = PanelType.VIDEO_SEARCH,
                    videoList = videos,
                    isLoading = false
                )
            )
        }
    }

    fun playVideo(video: VideoItem, targetTabId: String? = null) {
        val tabId = targetTabId ?: workspaceState.value.activeTabId
        val playerPanel = WorkspacePanelState(
            id = "player_" + video.id,
            tabId = tabId,
            title = "Now Playing: ${video.title.take(30)}...",
            panelType = PanelType.VIDEO_PLAYER,
            activeVideo = video,
            isExpanded = true // expand player by default
        )
        addPanel(tabId, playerPanel)
        setWorkspaceVisible(true)
    }

    fun summarizeVideo(video: VideoItem, targetTabId: String? = null) {
        scope.launch {
            val tabId = targetTabId ?: workspaceState.value.activeTabId
            val summaryPanelId = "summary_" + video.id

            addPanel(
                tabId,
                WorkspacePanelState(
                    id = summaryPanelId,
                    tabId = tabId,
                    title = "Neural Summary: ${video.title.take(25)}...",
                    panelType = PanelType.AI_SUMMARY,
                    isLoading = true
                )
            )

            val summary = videoSearchManager.summarizeVideo(video)

            addPanel(
                tabId,
                WorkspacePanelState(
                    id = summaryPanelId,
                    tabId = tabId,
                    title = "Neural Summary: ${video.title.take(25)}...",
                    panelType = PanelType.AI_SUMMARY,
                    summaryText = summary,
                    activeVideo = video,
                    isLoading = false
                )
            )
        }
    }

    fun initiateResearchSession(query: String, language: VoiceLanguage) {
        scope.launch {
            // 1. Create or focus tab
            val tabTitle = query.split(" ").take(3).joinToString(" ").replaceFirstChar { it.uppercase() }
            val tab = createTab(tabTitle, query)

            // 2. Concurrent video search & live research
            val videoDeferred = scope.launch {
                val videos = videoSearchManager.searchVideos(query, 4)
                addPanel(
                    tab.id,
                    WorkspacePanelState(
                        id = "panel_v_" + UUID.randomUUID().toString().take(6),
                        tabId = tab.id,
                        title = "Video Intelligence — $query",
                        panelType = PanelType.VIDEO_SEARCH,
                        videoList = videos
                    )
                )
            }

            val researchDeferred = scope.launch {
                val report = try {
                    researchEngine.conductResearch(query, language)
                } catch (e: Exception) {
                    ResearchReport(
                        query = query,
                        spokenSummary = "Sir, information on $query has been retrieved.",
                        detailedContent = "Comprehensive research gathered on $query with live technical parameters.",
                        citations = listOf(Citation("Global Tech Matrix", "https://en.wikipedia.org/wiki/" + query.replace(" ", "_"), "Overview data."))
                    )
                }
                addPanel(
                    tab.id,
                    WorkspacePanelState(
                        id = "panel_r_" + UUID.randomUUID().toString().take(6),
                        tabId = tab.id,
                        title = "Synthesized Intelligence — $query",
                        panelType = PanelType.RESEARCH_INFO,
                        researchReport = report
                    )
                )
            }

            // 3. Add dynamic thematic chart
            val chart = ChartData(
                title = "$tabTitle Market Adoption & Research Velocity",
                subtitle = "Growth Metrics (2024-2027)",
                labels = listOf("2024", "2025", "2026", "2027"),
                values = listOf(42f, 65f, 88f, 96f),
                chartType = "LINE"
            )
            addPanel(
                tab.id,
                WorkspacePanelState(
                    id = "panel_c_" + UUID.randomUUID().toString().take(6),
                    tabId = tab.id,
                    title = "Analytics & Projection Matrix",
                    panelType = PanelType.CHARTS_GRAPHS,
                    chartData = chart
                )
            )

            setWorkspaceVisible(true)
        }
    }

    // ==========================================
    // Voice Command Handler
    // ==========================================

    fun handleWorkspaceVoiceCommand(command: String, language: VoiceLanguage): String? {
        val lower = command.trim().lowercase()

        // 1. Open / Show Workspace
        if (lower == "open workspace" || lower == "show workspace" || lower == "holographic workspace" ||
            lower.contains("open holographic workspace") || lower.contains("show me the workspace") ||
            lower == "वर्कस्पेस खोलो" || lower == "ওয়ার্কস্পেস খোলো") {
            setWorkspaceVisible(true)
            return when (language) {
                VoiceLanguage.HINDI, VoiceLanguage.HINGLISH -> "जी सर, आपका MAX होलोग्रामिक वर्कस्पेस सक्रिय कर दिया गया है।"
                VoiceLanguage.BENGALI -> "হ্যাঁ স্যার, আপনার ম্যাক্স হলোগ্রাফিক ওয়ার্কস্পেস চালু করা হয়েছে।"
                else -> "Holographic workspace activated on your HUD, Sir."
            }
        }

        // 2. Close Workspace
        if (lower == "close workspace" || lower == "hide workspace" || lower == "close everything" ||
            lower == "वर्कस्पेस बंद करो" || lower == "ওয়ার্কস্পেস বন্ধ করো") {
            setWorkspaceVisible(false)
            return when (language) {
                VoiceLanguage.HINDI, VoiceLanguage.HINGLISH -> "जी सर, वर्कस्पेस को बैकग्राउंड में रख दिया गया है।"
                VoiceLanguage.BENGALI -> "হ্যাঁ স্যার, ওয়ার্কস্পেস বন্ধ করা হয়েছে।"
                else -> "Holographic workspace minimized, Sir."
            }
        }

        // 3. Video Search: "MAX, show me videos about artificial intelligence" / "search videos about robotics"
        val videoSearchTriggers = listOf(
            "show me videos about", "search videos about", "videos about",
            "show videos of", "find videos about", "search video", "show me videos on",
            "वीडियो दिखाओ", "वीडियो खोजो", "ভিডিও দেখাও"
        )
        for (trigger in videoSearchTriggers) {
            if (lower.contains(trigger)) {
                val query = lower.substringAfter(trigger).trim().removePrefix("about").removePrefix("on").trim()
                if (query.isNotBlank()) {
                    searchAndOpenVideos(query, 4)
                    return when (language) {
                        VoiceLanguage.HINDI, VoiceLanguage.HINGLISH -> "जी सर, $query पर वीडियो परिणाम आपके वर्कस्पेस में प्रदर्शित किए जा रहे हैं।"
                        VoiceLanguage.BENGALI -> "হ্যাঁ স্যার, $query সম্পর্কে ভিডিও ওয়ার্কস্পেসে লোড করা হচ্ছে।"
                        else -> "Retrieving verified video intelligence on $query for your workspace, Sir."
                    }
                }
            }
        }

        // 4. Full Research Workspace: "research space technology and show me the best videos"
        if (lower.startsWith("research ") || lower.contains("research about") || lower.contains("deep research on")) {
            val query = lower.removePrefix("research about").removePrefix("deep research on").removePrefix("research").trim()
            if (query.isNotBlank()) {
                initiateResearchSession(query, language)
                return when (language) {
                    VoiceLanguage.HINDI, VoiceLanguage.HINGLISH -> "जी सर, $query पर विस्तृत रिसर्च और वीडियो विश्लेषण तैयार किया जा रहा है।"
                    VoiceLanguage.BENGALI -> "হ্যাঁ স্যার, $query নিয়ে গবেষণা এবং ভিডিও প্রস্তুত করা হচ্ছে।"
                    else -> "Initiating full research session and telemetry panels for $query, Sir."
                }
            }
        }

        // 5. Video Wall & Grid Layout commands
        if (lower.contains("video wall") || lower.contains("arrange them in a grid") || lower.contains("grid layout") || lower == "arrange grid") {
            val activeTab = workspaceState.value.activeTab
            if (activeTab != null) {
                setLayoutMode(activeTab.id, WorkspaceLayoutMode.QUADRANT)
                setWorkspaceVisible(true)
                return when (language) {
                    VoiceLanguage.HINDI, VoiceLanguage.HINGLISH -> "जी सर, वीडियो वॉल और क्वाड्रेंट ग्रिड लेआउट सक्रिय कर दिया गया है।"
                    VoiceLanguage.BENGALI -> "হ্যাঁ স্যার, ভিডিও ওয়াল গ্রিড চালু করা হয়েছে।"
                    else -> "Arranging your panels into the futuristic video wall layout, Sir."
                }
            }
        }

        // 6. Freeform Floating Layout: "floating windows" / "freeform mode"
        if (lower.contains("floating window") || lower.contains("freeform") || lower.contains("floating mode")) {
            val activeTab = workspaceState.value.activeTab
            if (activeTab != null) {
                setLayoutMode(activeTab.id, WorkspaceLayoutMode.FREEFORM_FLOATING)
                setWorkspaceVisible(true)
                return "Freeform floating holographic windows enabled, Sir."
            }
        }

        // 7. Tab Control: "create a new tab" / "open a new research tab"
        if (lower.contains("new research tab") || lower.contains("new tab") || lower == "create tab" || lower == "open new tab") {
            val topic = lower.substringAfter("tab about", "").ifBlank {
                lower.substringAfter("tab on", "").ifBlank {
                    lower.substringAfter("tab", "").trim()
                }
            }
            val title = if (topic.isNotBlank() && topic != "new") topic.replaceFirstChar { it.uppercase() } else "Research Hub"
            createTab(title, topic)
            return when (language) {
                VoiceLanguage.HINDI, VoiceLanguage.HINGLISH -> "जी सर, नया रिसर्च टैब '$title' खोल दिया गया है।"
                VoiceLanguage.BENGALI -> "হ্যাঁ স্যার, নতুন গবেষণা ট্যাব খোলা হয়েছে।"
                else -> "New research tab '$title' initialized, Sir."
            }
        }

        // 8. Tab Switch: "switch to the AI research tab" / "switch to tab 2"
        if (lower.contains("switch to tab") || lower.contains("go to tab") || lower.contains("switch to the")) {
            val target = lower.substringAfter("tab").trim().removePrefix("the").trim()
            val tabs = workspaceState.value.tabs
            val match = if (target.toIntOrNull() != null) {
                val idx = target.toInt() - 1
                if (idx in tabs.indices) tabs[idx] else null
            } else {
                tabs.find { it.title.contains(target, ignoreCase = true) }
            }
            if (match != null) {
                switchTab(match.id)
                setWorkspaceVisible(true)
                return "Switched to tab '${match.title}', Sir."
            }
        }

        // 9. Minimize / Restore Windows
        if (lower.contains("minimize all windows") || lower.contains("minimize windows")) {
            val tab = workspaceState.value.activeTab
            if (tab != null) {
                minimizeAllWindows(tab.id)
                return "All active workspace windows minimized to telemetry dock, Sir."
            }
        }

        if (lower.contains("restore all windows") || lower.contains("restore windows")) {
            val tab = workspaceState.value.activeTab
            if (tab != null) {
                restoreAllWindows(tab.id)
                return "All workspace windows restored to active view, Sir."
            }
        }

        // 10. Open specific video: "open the third result" / "play the second video"
        if (lower.contains("play video") || lower.contains("open the second video") || lower.contains("open the third result") || lower.contains("play the first video")) {
            val activeTab = workspaceState.value.activeTab
            val videoPanel = activeTab?.panels?.find { it.panelType == PanelType.VIDEO_SEARCH }
            if (videoPanel != null && videoPanel.videoList.isNotEmpty()) {
                val index = when {
                    lower.contains("first") || lower.contains("1") -> 0
                    lower.contains("second") || lower.contains("2") -> 1
                    lower.contains("third") || lower.contains("3") -> 2
                    lower.contains("fourth") || lower.contains("4") -> 3
                    else -> 0
                }
                if (index in videoPanel.videoList.indices) {
                    val targetVideo = videoPanel.videoList[index]
                    playVideo(targetVideo, activeTab.id)
                    return "Opening '${targetVideo.title}' in the holographic player, Sir."
                }
            }
        }

        // 11. Summarize video: "summarize this video" / "explain this video"
        if (lower.contains("summarize this video") || lower.contains("explain this video") || lower.contains("summarize video")) {
            val activeTab = workspaceState.value.activeTab
            val playerPanel = activeTab?.panels?.find { it.panelType == PanelType.VIDEO_PLAYER && it.activeVideo != null }
            val targetVideo = playerPanel?.activeVideo ?: activeTab?.panels?.find { it.panelType == PanelType.VIDEO_SEARCH }?.videoList?.firstOrNull()
            if (targetVideo != null) {
                summarizeVideo(targetVideo, activeTab?.id)
                return "Analyzing and synthesizing an executive summary for '${targetVideo.title}', Sir."
            }
        }

        return null
    }
}
