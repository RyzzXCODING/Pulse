package com.alpoyy.pulse.gui;

import com.alpoyy.pulse.Pulse;
import com.alpoyy.pulse.config.ConfigManager;
import com.alpoyy.pulse.config.PulseConfig;
import com.alpoyy.pulse.gui.component.*;
import com.alpoyy.pulse.live.TikTokLiveConnection;
import com.alpoyy.pulse.music.PlaybackController;
import com.alpoyy.pulse.music.Song;
import com.alpoyy.pulse.music.Playlist;
import com.alpoyy.pulse.platform.PlatformManager;
import com.alpoyy.pulse.request.RequestManager;
import com.alpoyy.pulse.request.SongRequest;
import com.alpoyy.pulse.widget.WidgetManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import java.util.List;
import java.util.function.Consumer;

public class DashboardScreen extends Screen {
    private final PulseTheme theme;
    private GlassTab navTabs;
    private GlassScrollContainer contentScroll;
    private GlassTextField searchField;
    private GlassTooltip activeTooltip = null;
    
    // Tab content state
    private int libraryScrollY = 0;
    private int playlistScrollY = 0;
    private int requestsScrollY = 0;
    private int settingsScrollY = 0;
    
    // Live connection reference
    private TikTokLiveConnection liveConnection;
    
    public DashboardScreen(Text title) {
        super(title);
        this.theme = new PulseTheme(ConfigManager.getInstance().getConfig(), client);
    }
    
    @Override
    protected void init() {
        super.init();
        
        // Initialize navigation tabs (vertical sidebar)
        navTabs = GlassTab.builder(0, 0, theme)
            .orientation(GlassTab.Orientation.VERTICAL)
            .onTabChange(this::onTabChanged)
            .build();
        
        navTabs.addTab("music", Text.translatable("gui.pulse.tab.music").getString(), 
            () -> GlassRenderer.drawMusicIcon(null, 0, 0, 20, theme.getAccentColor()));
        navTabs.addTab("library", Text.translatable("gui.pulse.tab.library").getString(),
            () -> GlassRenderer.drawLibraryIcon(null, 0, 0, 20, theme.getAccentColor()));
        navTabs.addTab("playlists", Text.translatable("gui.pulse.tab.playlists").getString(),
            () -> GlassRenderer.drawPlaylistIcon(null, 0, 0, 20, theme.getAccentColor()));
        navTabs.addTab("requests", Text.translatable("gui.pulse.tab.requests").getString(),
            () -> GlassRenderer.drawRequestsIcon(null, 0, 0, 20, theme.getAccentColor()));
        navTabs.addTab("live", Text.translatable("gui.pulse.tab.live").getString(),
            () -> GlassRenderer.drawLiveIcon(null, 0, 0, 20, theme.getAccentColor()));
        navTabs.addTab("widgets", Text.translatable("gui.pulse.tab.widgets").getString(),
            () -> GlassRenderer.drawWidgetsIcon(null, 0, 0, 20, theme.getAccentColor()));
        navTabs.addTab("connections", Text.translatable("gui.pulse.tab.connections").getString(),
            () -> GlassRenderer.drawConnectionsIcon(null, 0, 0, 20, theme.getAccentColor()));
        navTabs.addTab("settings", Text.translatable("gui.pulse.tab.settings").getString(),
            () -> GlassRenderer.drawSettingsIcon(null, 0, 0, 20, theme.getAccentColor()));
        
        navTabs.setSelectedIndex(0);
        
        // Get live connection reference
        liveConnection = getLiveConnection();
        
        // Search field (will be positioned in render)
        searchField = GlassTextField.builder(0, 0)
            .width(280)
            .height(36)
            .placeholder(Text.translatable("gui.pulse.search.placeholder").getString())
            .onChange(() -> { /* search logic */ })
            .build();
    }
    
    private void onTabChanged(int index) {
        // Reset scroll when tab changes
        libraryScrollY = 0;
        playlistScrollY = 0;
        requestsScrollY = 0;
        settingsScrollY = 0;
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        PulseConfig config = ConfigManager.getInstance().getConfig();
        theme.config = config; // Update theme with latest config
        
        // Render base background
        renderBackground(context, mouseX, mouseY, delta);
        
        // Full-screen dark overlay with UI opacity
        context.fill(0, 0, width, height, theme.bgDeepest(theme.uiOpacity()));
        
        // Calculate layout
        int sidebarWidth = theme.sidebarWidth();
        int headerHeight = theme.headerHeight();
        int contentX = sidebarWidth;
        int contentY = headerHeight;
        int contentWidth = width - sidebarWidth;
        int contentHeight = height - headerHeight;
        
        // ============ RENDER SIDEBAR ============
        renderSidebar(context, sidebarWidth, height);
        
        // ============ RENDER HEADER ============
        renderHeader(context, contentX, headerHeight, contentWidth);
        
        // ============ RENDER CONTENT AREA ============
        context.enableScissor(contentX, contentY, contentX + contentWidth, contentY + contentHeight);
        
        context.getMatrices().push();
        context.getMatrices().translate(contentX, contentY, 0);
        
        renderTabContent(context, contentWidth, contentHeight, mouseX - contentX, mouseY - contentY);
        
        context.getMatrices().pop();
        context.disableScissor();
        
        // ============ RENDER WIDGETS (if not in editor mode) ============
        if (!WidgetManager.getInstance().isEditorMode()) {
            WidgetManager.getInstance().renderWidgets(context, null);
        }
        
        // ============ RENDER TOOLTIP ============
        if (activeTooltip != null && activeTooltip.isVisible()) {
            activeTooltip.updatePosition(mouseX, mouseY);
            activeTooltip.render(context, theme);
        }
        
        super.render(context, mouseX, mouseY, delta);
    }
    
    private void renderSidebar(DrawContext context, int sidebarWidth, int height) {
        // Sidebar background
        GlassRenderer.renderGlassPanelElevated(context, 0, 0, sidebarWidth, height, theme.radiusLarge(), theme);
        
        // Pulse logo/branding at top
        int logoY = theme.spacingXL();
        context.getMatrices().push();
        context.getMatrices().translate(theme.spacingL(), logoY, 0);
        
        // Logo icon
        GlassRenderer.drawMusicIcon(context, 0, 0, 32, theme.getAccentColor());
        
        // Title
        context.drawTextWithShadow(client.textRenderer, "Pulse", 40, 8, theme.textPrimary());
        context.drawTextWithShadow(client.textRenderer, "v" + Pulse.VERSION, 40, 22, theme.textMuted());
        
        context.getMatrices().pop();
        
        // Navigation tabs
        int tabsY = logoY + 56;
        navTabs.x = 0;
        navTabs.y = tabsY;
        navTabs.render(context, mouseX, mouseY);
        
        // Bottom section - Live connection status & widget editor toggle
        int bottomY = height - 140;
        
        // Live connection indicator
        if (liveConnection != null) {
            renderLiveConnectionStatus(context, bottomY, sidebarWidth);
            bottomY += 60;
        }
        
        // Widget editor toggle
        boolean editorMode = WidgetManager.getInstance().isEditorMode();
        String editorText = editorMode ? 
            Text.translatable("gui.pulse.widgets.exit_editor").getString() :
            Text.translatable("gui.pulse.widgets.edit_layout").getString();
        
        GlassButton.Builder builder = GlassButton.builder(theme.spacingM(), bottomY)
            .width(sidebarWidth - theme.spacingM() * 2)
            .text(editorText)
            .onClick(() -> {
                WidgetManager.getInstance().setEditorMode(!editorMode);
                playClickSound();
            })
            .style(editorMode ? GlassButton.Style.PRIMARY : GlassButton.Style.SECONDARY)
            .size(GlassButton.Size.NORMAL);
        
        GlassButton editorBtn = builder.build();
        editorBtn.render(context, theme, mouseX, mouseY);
    }
    
    private void renderLiveConnectionStatus(DrawContext context, int y, int width) {
        if (liveConnection == null) return;
        
        int panelX = theme.spacingM();
        int panelY = y;
        int panelWidth = width - theme.spacingM() * 2;
        int panelHeight = 50;
        
        GlassRenderer.renderGlassPanel(context, panelX, panelY, panelWidth, panelHeight, theme.radiusMedium(), theme);
        
        boolean connected = liveConnection.getState() == TikTokLiveConnection.ConnectionState.CONNECTED;
        boolean connecting = liveConnection.getState() == TikTokLiveConnection.ConnectionState.CONNECTING;
        boolean error = liveConnection.getState() == TikTokLiveConnection.ConnectionState.ERROR;
        
        int statusColor = connected ? theme.statusPlaying() : 
                         (connecting ? theme.statusWarning() : 
                         (error ? theme.statusError() : theme.textMuted()));
        
        String statusText = connected ? Text.translatable("gui.pulse.live.status.connected").getString() :
                             (connecting ? Text.translatable("gui.pulse.live.status.connecting").getString() :
                             (error ? Text.translatable("gui.pulse.live.status.error").getString() :
                             Text.translatable("gui.pulse.live.status.disconnected").getString()));
        
        context.getMatrices().push();
        context.getMatrices().translate(panelX + theme.spacingM(), panelY + theme.spacingM(), 0);
        
        // Status dot
        GlassRenderer.drawDot(context, 0, 4, 8, statusColor);
        
        // Username
        String userText = "@" + (liveConnection.getUsername() != null ? liveConnection.getUsername() : "Not set");
        context.drawTextWithShadow(client.textRenderer, userText, 14, 0, theme.textPrimary());
        
        // Status text
        context.drawTextWithShadow(client.textRenderer, statusText, 14, 14, statusColor);
        
        context.getMatrices().pop();
    }
    
    private void renderHeader(DrawContext context, int contentX, int headerHeight, int contentWidth) {
        // Header background
        GlassRenderer.renderGlassPanelElevated(context, contentX, 0, contentWidth, headerHeight, theme.radiusLarge(), theme);
        
        context.getMatrices().push();
        context.getMatrices().translate(contentX, 0, 0);
        
        // Page title
        String tabTitle = getCurrentTabTitle();
        context.drawTextWithShadow(client.textRenderer, tabTitle, theme.spacingXL(), (headerHeight - 16) / 2, theme.textPrimary());
        
        // Search field (right side)
        int searchX = contentWidth - 300;
        int searchY = (headerHeight - 36) / 2;
        searchField.x = searchX;
        searchField.y = searchY;
        searchField.render(context, theme, mouseX - contentX - searchX, mouseY - searchY);
        
        context.getMatrices().pop();
    }
    
    private String getCurrentTabTitle() {
        return switch (navTabs.getSelectedId()) {
            case "music" -> Text.translatable("gui.pulse.tab.music").getString();
            case "library" -> Text.translatable("gui.pulse.tab.library").getString();
            case "playlists" -> Text.translatable("gui.pulse.tab.playlists").getString();
            case "requests" -> Text.translatable("gui.pulse.tab.requests").getString();
            case "live" -> Text.translatable("gui.pulse.tab.live").getString();
            case "widgets" -> Text.translatable("gui.pulse.tab.widgets").getString();
            case "connections" -> Text.translatable("gui.pulse.tab.connections").getString();
            case "settings" -> Text.translatable("gui.pulse.tab.settings").getString();
            default -> "Pulse";
        };
    }
    
    private void renderTabContent(DrawContext context, int width, int height, int relMouseX, int relMouseY) {
        String currentTab = navTabs.getSelectedId();
        
        switch (currentTab) {
            case "music" -> renderMusicTab(context, width, height, relMouseX, relMouseY);
            case "library" -> renderLibraryTab(context, width, height, relMouseX, relMouseY);
            case "playlists" -> renderPlaylistsTab(context, width, height, relMouseX, relMouseY);
            case "requests" -> renderRequestsTab(context, width, height, relMouseX, relMouseY);
            case "live" -> renderLiveTab(context, width, height, relMouseX, relMouseY);
            case "widgets" -> renderWidgetsTab(context, width, height, relMouseX, relMouseY);
            case "connections" -> renderConnectionsTab(context, width, height, relMouseX, relMouseY);
            case "settings" -> renderSettingsTab(context, width, height, relMouseX, relMouseY);
        }
    }
    
    // ============ MUSIC TAB ============
    
    private void renderMusicTab(DrawContext context, int width, int height, int mouseX, int mouseY) {
        PlaybackController controller = PlaybackController.getInstance();
        Song currentSong = controller.getCurrentSong();
        
        int padding = theme.spacingXL();
        int cardWidth = width - padding * 2;
        int y = padding;
        
        // ===== NOW PLAYING CARD =====
        int nowPlayingHeight = 160;
        GlassCard nowPlayingCard = GlassCard.builder(padding, y)
            .width(cardWidth)
            .height(nowPlayingHeight)
            .variant(GlassCard.Variant.ELEVATED)
            .content(() -> renderNowPlayingContent(context, cardWidth, nowPlayingHeight, currentSong, controller))
            .build();
        nowPlayingCard.render(context, theme, mouseX - padding, mouseY - y);
        y += nowPlayingHeight + theme.spacingL();
        
        // ===== PLAYBACK CONTROLS CARD =====
        int controlsHeight = 80;
        GlassCard controlsCard = GlassCard.builder(padding, y)
            .width(cardWidth)
            .height(controlsHeight)
            .variant(GlassCard.Variant.DEFAULT)
            .content(() -> renderPlaybackControls(context, cardWidth, controlsHeight, controller))
            .build();
        controlsCard.render(context, theme, mouseX - padding, mouseY - y);
        y += controlsHeight + theme.spacingL();
        
        // ===== QUEUE PREVIEW CARD =====
        int queueHeight = Math.min(height - y - padding, 200);
        if (queueHeight > 60) {
            GlassCard queueCard = GlassCard.builder(padding, y)
                .width(cardWidth)
                .height(queueHeight)
                .variant(GlassCard.Variant.DEFAULT)
                .content(() -> renderQueuePreview(context, cardWidth, queueHeight))
                .build();
            queueCard.render(context, theme, mouseX - padding, mouseY - y);
        }
    }
    
    private void renderNowPlayingContent(DrawContext context, int width, int height, Song song, PlaybackController controller) {
        if (song == null) {
            // Empty state
            String text = Text.translatable("gui.pulse.music.no_song").getString();
            int textX = (width - client.textRenderer.getWidth(text)) / 2;
            int textY = height / 2 - 5;
            context.drawTextWithShadow(client.textRenderer, text, textX, textY, theme.textMuted());
            return;
        }
        
        int artSize = height - theme.spacingM() * 2;
        int artX = theme.spacingM();
        int artY = theme.spacingM();
        
        // Album artwork area
        GlassRenderer.fillRoundedRect(context, artX, artY, artSize, artSize, theme.radiusMedium(), theme.bgDeep(theme.panelOpacity()));
        GlassRenderer.drawRoundedRectOutline(context, artX, artY, artSize, artSize, theme.radiusMedium(), 1, theme.borderSubtle());
        
        // Artwork placeholder - could load actual image here
        GlassRenderer.drawMusicIcon(context, artX + artSize/2 - 16, artY + artSize/2 - 16, 32, theme.getAccentColor());
        
        // Song info
        int infoX = artX + artSize + theme.spacingL();
        int infoY = artY + theme.spacingM();
        int infoWidth = width - infoX - theme.spacingM();
        
        // Title
        String title = song.getTitle();
        if (client.textRenderer.getWidth(title) > infoWidth) {
            title = client.textRenderer.trimToWidth(title, infoWidth) + "...";
        }
        context.drawTextWithShadow(client.textRenderer, title, infoX, infoY, theme.textPrimary());
        
        // Artist
        String artist = song.getArtist();
        if (client.textRenderer.getWidth(artist) > infoWidth) {
            artist = client.textRenderer.trimToWidth(artist, infoWidth) + "...";
        }
        context.drawTextWithShadow(client.textRenderer, artist, infoX, infoY + 22, theme.textSecondary());
        
        // Source badge
        String source = "[" + song.getSource().name() + "]";
        context.drawTextWithShadow(client.textRenderer, source, infoX, infoY + 44, theme.getAccentColorARGB());
        
        // Progress bar
        long duration = controller.getDuration();
        long position = controller.getCurrentPosition();
        if (duration > 0) {
            int barY = height - 35;
            int barX = theme.spacingM();
            int barWidth = width - theme.spacingM() * 2;
            
            // Track
            GlassRenderer.fillRoundedRect(context, barX, barY, barWidth, 4, 2, theme.bgDeep(theme.panelOpacity()));
            
            // Progress
            float progress = (float) position / duration;
            int fillWidth = (int) (barWidth * progress);
            GlassRenderer.fillRoundedRect(context, barX, barY, fillWidth, 4, 2, theme.accent(1f));
            
            // Time text
            String timeText = formatTime(position) + " / " + formatTime(duration);
            context.drawTextWithShadow(client.textRenderer, timeText, barX, barY - 14, theme.textMuted());
        }
    }
    
    private void renderPlaybackControls(DrawContext context, int width, int height, PlaybackController controller) {
        int btnSize = 40;
        int btnSpacing = theme.spacingM();
        int totalWidth = btnSize * 5 + btnSpacing * 4;
        int startX = (width - totalWidth) / 2;
        int btnY = (height - btnSize) / 2;
        
        boolean playing = controller.isPlaying();
        
        // Previous
        renderIconButton(context, startX, btnY, btnSize, btnSize, 
            () -> GlassRenderer.drawSkipPrevIcon(context, 0, 0, 20, theme.textPrimary()),
            controller::playPrevious, false);
        
        // Play/Pause
        int playX = startX + btnSize + btnSpacing;
        renderIconButton(context, playX, btnY, btnSize, btnSize,
            () -> {
                if (playing) GlassRenderer.drawPauseIcon(context, 0, 0, 24, theme.textOnAccent());
                else GlassRenderer.drawPlayIcon(context, 0, 0, 24, theme.textOnAccent());
            },
            controller::togglePlayPause, true);
        
        // Stop
        int stopX = playX + btnSize + btnSpacing;
        renderIconButton(context, stopX, btnY, btnSize, btnSize,
            () -> GlassRenderer.fillRoundedRect(context, 0, 0, 20, 20, 4, theme.textPrimary()),
            controller::stop, false);
        
        // Next
        int nextX = stopX + btnSize + btnSpacing;
        renderIconButton(context, nextX, btnY, btnSize, btnSize,
            () -> GlassRenderer.drawSkipNextIcon(context, 0, 0, 20, theme.textPrimary()),
            controller::playNext, false);
        
        // Shuffle
        int shuffleX = nextX + btnSize + btnSpacing;
        renderIconButton(context, shuffleX, btnY, btnSize, btnSize,
            () -> GlassRenderer.drawShuffleIcon(context, 0, 0, 20, theme.textPrimary(), controller.isShuffle()),
            () -> controller.setShuffle(!controller.isShuffle()), false);
        
        // Volume slider (right side)
        int volX = width - 120;
        int volY = (height - 24) / 2;
        GlassSlider volumeSlider = GlassSlider.builder(volX, volY)
            .width(100)
            .value(controller.getVolume())
            .onChange(() -> controller.setMusicVolume(controller.getVolume()))
            .showValue(false)
            .build();
        volumeSlider.render(context, theme, mouseX - volX, mouseY - volY);
    }
    
    private void renderIconButton(DrawContext context, int x, int y, int w, int h, 
                                   Runnable iconDrawer, Runnable onClick, boolean primary) {
        GlassButton btn = GlassButton.builder(x, y)
            .width(w)
            .size(GlassButton.Size.NORMAL)
            .icon(h - 8, iconDrawer)
            .onClick(() -> { onClick.run(); playClickSound(); })
            .style(primary ? GlassButton.Style.PRIMARY : GlassButton.Style.SECONDARY)
            .build();
        btn.render(context, theme, mouseX - x, mouseY - y);
    }
    
    private void renderQueuePreview(DrawContext context, int width, int height) {
        RequestManager requestManager = RequestManager.getInstance();
        List<SongRequest> pending = requestManager.getPendingRequests();
        
        context.drawTextWithShadow(client.textRenderer, Text.translatable("gui.pulse.queue.upcoming").getString(), 
            theme.spacingM(), theme.spacingM(), theme.textPrimary());
        
        if (pending.isEmpty()) {
            String emptyText = Text.translatable("gui.pulse.queue.empty").getString();
            int textX = (width - client.textRenderer.getWidth(emptyText)) / 2;
            int textY = height / 2 - 5;
            context.drawTextWithShadow(client.textRenderer, emptyText, textX, textY, theme.textMuted());
            return;
        }
        
        int itemY = theme.spacingM() * 2 + 16;
        int maxItems = (height - itemY - theme.spacingM()) / 28;
        
        for (int i = 0; i < Math.min(pending.size(), maxItems); i++) {
            SongRequest req = pending.get(i);
            
            // Item background on hover
            int itemX = theme.spacingM();
            int itemWidth = width - theme.spacingM() * 2;
            int itemHeight = 24;
            
            if (isMouseOverItem(mouseX, mouseY, itemX, itemY, itemWidth, itemHeight)) {
                GlassRenderer.fillRoundedRect(context, itemX, itemY, itemWidth, itemHeight, theme.radiusSmall(), theme.bgHover(theme.panelOpacity()));
            }
            
            // Queue number
            String numText = String.valueOf(i + 1);
            context.drawTextWithShadow(client.textRenderer, numText, itemX + 8, itemY + 4, theme.textMuted());
            
            // Song name
            String songText = req.getDisplayName();
            int maxSongWidth = itemWidth - 60;
            if (client.textRenderer.getWidth(songText) > maxSongWidth) {
                songText = client.textRenderer.trimToWidth(songText, maxSongWidth) + "...";
            }
            context.drawTextWithShadow(client.textRenderer, songText, itemX + 32, itemY + 4, theme.textPrimary());
            
            // Requester
            String requesterText = "@" + req.getRequester();
            int reqX = itemX + itemWidth - client.textRenderer.getWidth(requesterText) - 8;
            context.drawTextWithShadow(client.textRenderer, requesterText, reqX, itemY + 4, theme.textMuted());
            
            itemY += itemHeight + 2;
        }
        
        if (pending.size() > maxItems) {
            String moreText = "+ " + (pending.size() - maxItems) + " " + Text.translatable("gui.pulse.queue.more").getString();
            context.drawTextWithShadow(client.textRenderer, moreText, theme.spacingM() + 8, itemY + 4, theme.textMuted());
        }
    }
    
    // ============ LIBRARY TAB ============
    
    private void renderLibraryTab(DrawContext context, int width, int height, int mouseX, int mouseY) {
        int padding = theme.spacingXL();
        int contentWidth = width - padding * 2;
        int y = padding;
        
        // Header with scan button
        GlassCard headerCard = GlassCard.builder(padding, y)
            .width(contentWidth)
            .height(60)
            .variant(GlassCard.Variant.DEFAULT)
            .content(() -> renderLibraryHeader(context, contentWidth, 60))
            .build();
        headerCard.render(context, theme, mouseX - padding, mouseY - y);
        y += 70;
        
        // Library content
        int listHeight = height - y - padding;
        GlassScrollContainer scroll = new GlassScrollContainer(padding, y, contentWidth, listHeight, 
            () -> renderLibraryList(context, contentWidth, listHeight));
        scroll.setContentHeight(getLibraryContentHeight());
        scroll.render(context, theme, mouseX - padding, mouseY - y);
    }
    
    private void renderLibraryHeader(DrawContext context, int width, int height) {
        context.drawTextWithShadow(client.textRenderer, Text.translatable("gui.pulse.library.title").getString(), 
            theme.spacingM(), (height - 16) / 2, theme.textPrimary());
        
        // Stats
        com.alpoyy.pulse.music.MusicLibrary library = new com.alpoyy.pulse.music.MusicLibrary();
        String stats = Text.translatable("gui.pulse.library.stats", library.getSongCount(), library.getPlaylistCount()).getString();
        context.drawTextWithShadow(client.textRenderer, stats, theme.spacingM(), (height - 16) / 2 + 20, theme.textMuted());
        
        // Scan button
        int btnX = width - 180 - theme.spacingM();
        int btnY = (height - 36) / 2;
        GlassButton scanBtn = GlassButton.builder(btnX, btnY)
            .width(180)
            .text(Text.translatable("gui.pulse.library.scan").getString())
            .onClick(() -> { library.scanMusicDirectory(); playClickSound(); })
            .style(GlassButton.Style.PRIMARY)
            .build();
        scanBtn.render(context, theme, mouseX - btnX, mouseY - btnY);
    }
    
    private void renderLibraryList(DrawContext context, int width, int height) {
        com.alpoyy.pulse.music.MusicLibrary library = new com.alpoyy.pulse.music.MusicLibrary();
        List<Song> songs = library.getSongs();
        
        if (songs.isEmpty()) {
            String emptyText = Text.translatable("gui.pulse.library.empty").getString();
            int textX = (width - client.textRenderer.getWidth(emptyText)) / 2;
            int textY = height / 2 - 5;
            context.drawTextWithShadow(client.textRenderer, emptyText, textX, textY, theme.textMuted());
            return;
        }
        
        int itemHeight = 56;
        int itemY = 0;
        
        for (Song song : songs) {
            int itemX = 0;
            int itemWidth = width;
            
            // Item background on hover
            if (isMouseOverItem(mouseX, mouseY, itemX, itemY, itemWidth, itemHeight)) {
                GlassRenderer.fillRoundedRect(context, itemX, itemY, itemWidth, itemHeight, theme.radiusSmall(), theme.bgHover(theme.panelOpacity()));
            }
            
            // Artwork
            int artSize = itemHeight - 8;
            int artX = 4;
            int artY = 4;
            GlassRenderer.fillRoundedRect(context, artX, artY, artSize, artSize, theme.radiusSmall(), theme.bgDeep(theme.panelOpacity()));
            GlassRenderer.drawMusicIcon(context, artX + artSize/2 - 12, artY + artSize/2 - 12, 24, theme.getAccentColor());
            
            // Info
            int infoX = artX + artSize + 12;
            int infoY = artY + 4;
            
            String title = song.getTitle();
            if (client.textRenderer.getWidth(title) > width - infoX - 12) {
                title = client.textRenderer.trimToWidth(title, width - infoX - 12) + "...";
            }
            context.drawTextWithShadow(client.textRenderer, title, infoX, infoY, theme.textPrimary());
            
            String artist = song.getArtist();
            if (client.textRenderer.getWidth(artist) > width - infoX - 12) {
                artist = client.textRenderer.trimToWidth(artist, width - infoX - 12) + "...";
            }
            context.drawTextWithShadow(client.textRenderer, artist, infoX, infoY + 20, theme.textSecondary());
            
            // Duration
            String durText = song.getFormattedDuration();
            int durX = width - client.textRenderer.getWidth(durText) - 12;
            context.drawTextWithShadow(client.textRenderer, durText, durX, infoY, theme.textMuted());
            
            // Source badge
            String source = song.getSource().name();
            context.drawTextWithShadow(client.textRenderer, source, durX, infoY + 20, theme.getAccentColorARGB());
            
            // Play button on hover
            if (isMouseOverItem(mouseX, mouseY, itemX, itemY, itemWidth, itemHeight)) {
                int playX = width - 44;
                int playY = (itemHeight - 24) / 2;
                GlassButton playBtn = GlassButton.builder(playX, playY)
                    .width(24)
                    .size(GlassButton.Size.SMALL)
                    .icon(24, () -> GlassRenderer.drawPlayIcon(context, 0, 0, 16, theme.textOnAccent()))
                    .onClick(() -> { PlaybackController.getInstance().playLocal(song); playClickSound(); })
                    .style(GlassButton.Style.PRIMARY)
                    .build();
                playBtn.render(context, theme, mouseX - playX, mouseY - playY);
            }
            
            itemY += itemHeight + 4;
        }
    }
    
    private int getLibraryContentHeight() {
        com.alpoyy.pulse.music.MusicLibrary library = new com.alpoyy.pulse.music.MusicLibrary();
        return library.getSongs().size() * 60 + 20;
    }
    
    // ============ PLAYLISTS TAB ============
    
    private void renderPlaylistsTab(DrawContext context, int width, int height, int mouseX, int mouseY) {
        int padding = theme.spacingXL();
        int contentWidth = width - padding * 2;
        int y = padding;
        
        // Header with create button
        GlassCard headerCard = GlassCard.builder(padding, y)
            .width(contentWidth)
            .height(60)
            .variant(GlassCard.Variant.DEFAULT)
            .content(() -> renderPlaylistsHeader(context, contentWidth, 60))
            .build();
        headerCard.render(context, theme, mouseX - padding, mouseY - y);
        y += 70;
        
        // Playlists grid
        int listHeight = height - y - padding;
        GlassScrollContainer scroll = new GlassScrollContainer(padding, y, contentWidth, listHeight,
            () -> renderPlaylistsGrid(context, contentWidth, listHeight));
        scroll.setContentHeight(getPlaylistsContentHeight());
        scroll.render(context, theme, mouseX - padding, mouseY - y);
    }
    
    private void renderPlaylistsHeader(DrawContext context, int width, int height) {
        context.drawTextWithShadow(client.textRenderer, Text.translatable("gui.pulse.playlists.title").getString(),
            theme.spacingM(), (height - 16) / 2, theme.textPrimary());
        
        // Create playlist button
        int btnX = width - 180 - theme.spacingM();
        int btnY = (height - 36) / 2;
        GlassButton createBtn = GlassButton.builder(btnX, btnY)
            .width(180)
            .text(Text.translatable("gui.pulse.playlists.create").getString())
            .onClick(() -> { /* TODO: show create playlist dialog */ playClickSound(); })
            .style(GlassButton.Style.PRIMARY)
            .build();
        createBtn.render(context, theme, mouseX - btnX, mouseY - btnY);
    }
    
    private void renderPlaylistsGrid(DrawContext context, int width, int height) {
        com.alpoyy.pulse.music.MusicLibrary library = new com.alpoyy.pulse.music.MusicLibrary();
        List<Playlist> playlists = library.getPlaylists();
        
        if (playlists.isEmpty()) {
            String emptyText = Text.translatable("gui.pulse.playlists.empty").getString();
            int textX = (width - client.textRenderer.getWidth(emptyText)) / 2;
            int textY = height / 2 - 5;
            context.drawTextWithShadow(client.textRenderer, emptyText, textX, textY, theme.textMuted());
            return;
        }
        
        int cardWidth = 220;
        int cardHeight = 140;
        int spacing = theme.spacingM();
        int cols = Math.max(1, (width + spacing) / (cardWidth + spacing));
        
        int x = 0, y = 0;
        for (int i = 0; i < playlists.size(); i++) {
            Playlist playlist = playlists.get(i);
            
            int cardX = x;
            int cardY = y;
            
            GlassCard card = GlassCard.builder(cardX, cardY)
                .width(cardWidth)
                .height(cardHeight)
                .variant(GlassCard.Variant.ELEVATED)
                .hoverable(true)
                .content(() -> renderPlaylistCard(context, cardWidth, cardHeight, playlist))
                .onClick(() -> { /* TODO: open playlist */ playClickSound(); })
                .build();
            card.render(context, theme, mouseX - cardX, mouseY - cardY);
            
            x += cardWidth + spacing;
            if (x + cardWidth > width) {
                x = 0;
                y += cardHeight + spacing;
            }
        }
    }
    
    private void renderPlaylistCard(DrawContext context, int width, int height, Playlist playlist) {
        // Artwork
        int artSize = height - 60;
        int artX = (width - artSize) / 2;
        int artY = theme.spacingM();
        
        GlassRenderer.fillRoundedRect(context, artX, artY, artSize, artSize, theme.radiusMedium(), theme.bgDeep(theme.panelOpacity()));
        GlassRenderer.drawPlaylistIcon(context, artX + artSize/2 - 16, artY + artSize/2 - 16, 32, theme.getAccentColor());
        
        // Name
        String name = playlist.getName();
        if (client.textRenderer.getWidth(name) > width - 16) {
            name = client.textRenderer.trimToWidth(name, width - 16) + "...";
        }
        int nameX = (width - client.textRenderer.getWidth(name)) / 2;
        context.drawTextWithShadow(client.textRenderer, name, nameX, artY + artSize + 8, theme.textPrimary());
        
        // Description
        String desc = playlist.getSongCount() + " songs • " + playlist.getFormattedTotalDuration();
        int descX = (width - client.textRenderer.getWidth(desc)) / 2;
        context.drawTextWithShadow(client.textRenderer, desc, descX, artY + artSize + 24, theme.textSecondary());
    }
    
    private int getPlaylistsContentHeight() {
        com.alpoyy.pulse.music.MusicLibrary library = new com.alpoyy.pulse.music.MusicLibrary();
        int count = library.getPlaylists().size();
        int cardHeight = 140;
        int spacing = theme.spacingM();
        int cols = Math.max(1, (width - theme.spacingXL() * 2 + spacing) / (220 + spacing));
        return ((count + cols - 1) / cols) * (cardHeight + spacing) + 20;
    }
    
    // ============ REQUESTS TAB ============
    
    private void renderRequestsTab(DrawContext context, int width, int height, int mouseX, int mouseY) {
        int padding = theme.spacingXL();
        int contentWidth = width - padding * 2;
        int y = padding;
        
        // Header with stats and controls
        GlassCard headerCard = GlassCard.builder(padding, y)
            .width(contentWidth)
            .height(70)
            .variant(GlassCard.Variant.DEFAULT)
            .content(() -> renderRequestsHeader(context, contentWidth, 70))
            .build();
        headerCard.render(context, theme, mouseX - padding, mouseY - y);
        y += 80;
        
        // Queue list
        int listHeight = height - y - padding;
        GlassScrollContainer scroll = new GlassScrollContainer(padding, y, contentWidth, listHeight,
            () -> renderRequestsList(context, contentWidth, listHeight));
        scroll.setContentHeight(getRequestsContentHeight());
        scroll.render(context, theme, mouseX - padding, mouseY - y);
    }
    
    private void renderRequestsHeader(DrawContext context, int width, int height) {
        RequestManager rm = RequestManager.getInstance();
        
        context.drawTextWithShadow(client.textRenderer, Text.translatable("gui.pulse.requests.title").getString(),
            theme.spacingM(), theme.spacingM(), theme.textPrimary());
        
        String stats = Text.translatable("gui.pulse.requests.stats", rm.getQueueSize(), rm.getPendingCount()).getString();
        context.drawTextWithShadow(client.textRenderer, stats, theme.spacingM(), theme.spacingM() + 22, theme.textSecondary());
        
        // Control buttons
        int btnY = (height - 36) / 2;
        int btnX = width - theme.spacingM();
        
        // Clear queue
        GlassButton clearBtn = GlassButton.builder(btnX - 100, btnY)
            .width(90)
            .text(Text.translatable("gui.pulse.requests.clear").getString())
            .onClick(() -> { rm.clearQueue(); playClickSound(); })
            .style(GlassButton.Style.DANGER)
            .build();
        clearBtn.render(context, theme, mouseX - (btnX - 100), mouseY - btnY);
        
        btnX -= 100 + theme.spacingS();
        
        // Skip
        GlassButton skipBtn = GlassButton.builder(btnX - 90, btnY)
            .width(90)
            .text(Text.translatable("gui.pulse.requests.skip").getString())
            .onClick(() -> { rm.skipCurrent(); playClickSound(); })
            .style(GlassButton.Style.SECONDARY)
            .build();
        skipBtn.render(context, theme, mouseX - (btnX - 90), mouseY - btnY);
    }
    
    private void renderRequestsList(DrawContext context, int width, int height) {
        RequestManager rm = RequestManager.getInstance();
        List<SongRequest> queue = rm.getQueue();
        
        if (queue.isEmpty()) {
            String emptyText = Text.translatable("gui.pulse.requests.empty").getString();
            int textX = (width - client.textRenderer.getWidth(emptyText)) / 2;
            int textY = height / 2 - 5;
            context.drawTextWithShadow(client.textRenderer, emptyText, textX, textY, theme.textMuted());
            return;
        }
        
        int itemHeight = 52;
        int itemY = 0;
        
        for (SongRequest req : queue) {
            int itemX = 0;
            int itemWidth = width;
            
            // Status color
            int statusColor = getRequestStatusColor(req.getStatus());
            
            // Item background on hover
            if (isMouseOverItem(mouseX, mouseY, itemX, itemY, itemWidth, itemHeight)) {
                GlassRenderer.fillRoundedRect(context, itemX, itemY, itemWidth, itemHeight, theme.radiusSmall(), theme.bgHover(theme.panelOpacity()));
            }
            
            // Status indicator
            GlassRenderer.fillRoundedRect(context, 8, itemY + 8, 4, itemHeight - 16, 2, statusColor | 0xFF000000);
            
            // Song info
            int infoX = 20;
            int infoY = itemY + 6;
            
            String songText = req.getDisplayName();
            if (client.textRenderer.getWidth(songText) > width - 100) {
                songText = client.textRenderer.trimToWidth(songText, width - 100) + "...";
            }
            context.drawTextWithShadow(client.textRenderer, songText, infoX, infoY, theme.textPrimary());
            
            String requesterText = "@" + req.getRequester();
            context.drawTextWithShadow(client.textRenderer, requesterText, infoX, infoY + 18, theme.textSecondary());
            
            // Status badge
            String statusText = req.getStatus().name();
            int statusX = width - client.textRenderer.getWidth(statusText) - 16;
            context.drawTextWithShadow(client.textRenderer, statusText, statusX, infoY, statusColor | 0xFF000000);
            
            // Queue position
            if (req.getQueuePosition() > 0) {
                String posText = "#" + req.getQueuePosition();
                int posX = statusX - client.textRenderer.getWidth(posText) - 16;
                context.drawTextWithShadow(client.textRenderer, posText, posX, infoY, theme.textMuted());
            }
            
            itemY += itemHeight + 4;
        }
    }
    
    private int getRequestsContentHeight() {
        RequestManager rm = RequestManager.getInstance();
        return rm.getQueue().size() * 56 + 20;
    }
    
    // ============ LIVE TAB ============
    
    private void renderLiveTab(DrawContext context, int width, int height, int mouseX, int mouseY) {
        int padding = theme.spacingXL();
        int contentWidth = width - padding * 2;
        int y = padding;
        
        if (liveConnection == null) {
            // Not configured
            GlassCard card = GlassCard.builder(padding, y)
                .width(contentWidth)
                .height(200)
                .variant(GlassCard.Variant.ELEVATED)
                .content(() -> renderLiveNotConfigured(context, contentWidth, 200))
                .build();
            card.render(context, theme, mouseX - padding, mouseY - y);
            return;
        }
        
        // Connection status card
        GlassCard statusCard = GlassCard.builder(padding, y)
            .width(contentWidth)
            .height(140)
            .variant(GlassCard.Variant.ELEVATED)
            .content(() -> renderLiveStatus(context, contentWidth, 140))
            .build();
        statusCard.render(context, theme, mouseX - padding, mouseY - y);
        y += 150;
        
        // Configuration card
        GlassCard configCard = GlassCard.builder(padding, y)
            .width(contentWidth)
            .height(180)
            .variant(GlassCard.Variant.DEFAULT)
            .content(() -> renderLiveConfig(context, contentWidth, 180))
            .build();
        configCard.render(context, theme, mouseX - padding, mouseY - y);
    }
    
    private void renderLiveNotConfigured(DrawContext context, int width, int height) {
        // Icon
        GlassRenderer.drawLiveIcon(context, width/2 - 32, 30, 64, theme.statusError());
        
        // Text
        String title = Text.translatable("gui.pulse.live.not_configured").getString();
        if (client.textRenderer.getWidth(title) == 0) title = "TikTok LIVE Not Configured";
        int titleX = (width - client.textRenderer.getWidth(title)) / 2;
        context.drawTextWithShadow(client.textRenderer, title, titleX, 110, theme.textPrimary());
        
        String desc = "Configure your EulerStream API key and TikTok username in Settings > Connections";
        if (client.textRenderer.getWidth(desc) > width - 32) {
            // Would need text wrapping
        }
        int descX = (width - client.textRenderer.getWidth(desc)) / 2;
        context.drawTextWithShadow(client.textRenderer, desc, descX, 130, theme.textSecondary());
        
        // Configure button
        int btnX = (width - 200) / 2;
        GlassButton configBtn = GlassButton.builder(btnX, 160)
            .width(200)
            .text("Open Connections")
            .onClick(() -> { navTabs.setSelectedId("connections"); playClickSound(); })
            .style(GlassButton.Style.PRIMARY)
            .build();
        configBtn.render(context, theme, mouseX - btnX, mouseY - 160);
    }
    
    private void renderLiveStatus(DrawContext context, int width, int height) {
        boolean connected = liveConnection.getState() == TikTokLiveConnection.ConnectionState.CONNECTED;
        boolean connecting = liveConnection.getState() == TikTokLiveConnection.ConnectionState.CONNECTING;
        boolean error = liveConnection.getState() == TikTokLiveConnection.ConnectionState.ERROR;
        
        int statusColor = connected ? theme.statusPlaying() : 
                         (connecting ? theme.statusWarning() : 
                         (error ? theme.statusError() : theme.textMuted()));
        
        // Title
        context.drawTextWithShadow(client.textRenderer, Text.translatable("gui.pulse.live.title").getString(),
            theme.spacingM(), theme.spacingM(), theme.textPrimary());
        
        // Username
        String userText = "@" + (liveConnection.getUsername() != null ? liveConnection.getUsername() : "Not set");
        context.drawTextWithShadow(client.textRenderer, userText, theme.spacingM(), theme.spacingM() + 24, theme.textSecondary());
        
        // Status with indicator
        int dotX = theme.spacingM();
        int dotY = theme.spacingM() + 50;
        GlassRenderer.drawDot(context, dotX, dotY + 2, 10, statusColor);
        
        String statusText = connected ? Text.translatable("gui.pulse.live.status.connected").getString() :
                             (connecting ? Text.translatable("gui.pulse.live.status.connecting").getString() :
                             (error ? Text.translatable("gui.pulse.live.status.error").getString() :
                             Text.translatable("gui.pulse.live.status.disconnected").getString()));
        context.drawTextWithShadow(client.textRenderer, statusText, dotX + 16, dotY, statusColor);
        
        // Error message
        if (error && liveConnection.getLastError() != null) {
            String errText = liveConnection.getLastError();
            if (client.textRenderer.getWidth(errText) > width - 32) {
                errText = client.textRenderer.trimToWidth(errText, width - 32) + "...";
            }
            context.drawTextWithShadow(client.textRenderer, errText, theme.spacingM(), dotY + 24, theme.statusError());
        }
        
        // Connect/Disconnect button (right side)
        int btnX = width - 160 - theme.spacingM();
        int btnY = (height - 36) / 2;
        String btnText = connected ? Text.translatable("gui.pulse.live.disconnect").getString() :
                          Text.translatable("gui.pulse.live.connect").getString();
        GlassButton.Style btnStyle = connected ? GlassButton.Style.DANGER : GlassButton.Style.PRIMARY;
        
        GlassButton connBtn = GlassButton.builder(btnX, btnY)
            .width(160)
            .text(btnText)
            .onClick(() -> { 
                if (connected) liveConnection.disconnect(); 
                else liveConnection.connect();
                playClickSound(); 
            })
            .style(btnStyle)
            .build();
        connBtn.render(context, theme, mouseX - btnX, mouseY - btnY);
    }
    
    private void renderLiveConfig(DrawContext context, int width, int height) {
        context.drawTextWithShadow(client.textRenderer, "Configuration", theme.spacingM(), theme.spacingM(), theme.textPrimary());
        
        // API Key field
        int fieldY = theme.spacingM() + 30;
        String apiKey = liveConnection.getApiKey();
        String maskedKey = apiKey != null && !apiKey.isEmpty() ? 
            apiKey.substring(0, Math.min(8, apiKey.length())) + "****" : "Not set";
        
        context.drawTextWithShadow(client.textRenderer, "EulerStream API Key", theme.spacingM(), fieldY, theme.textSecondary());
        context.drawTextWithShadow(client.textRenderer, maskedKey, theme.spacingM(), fieldY + 20, theme.textMuted());
        
        // Username field
        fieldY += 60;
        context.drawTextWithShadow(client.textRenderer, "TikTok Username", theme.spacingM(), fieldY, theme.textSecondary());
        context.drawTextWithShadow(client.textRenderer, "@" + liveConnection.getUsername(), theme.spacingM(), fieldY + 20, theme.textMuted());
        
        // Auto-reconnect toggle
        fieldY += 60;
        ConfigManager configManager = ConfigManager.getInstance();
        GlassToggle autoReconnect = GlassToggle.builder(theme.spacingM(), fieldY)
            .label("Auto Reconnect")
            .value(configManager.getConfig().autoReconnect)
            .onChange(() -> {
                configManager.getConfig().autoReconnect = !configManager.getConfig().autoReconnect;
                configManager.saveAsync();
            })
            .build();
        autoReconnect.render(context, theme, mouseX - theme.spacingM(), mouseY - fieldY);
    }
    
    // ============ WIDGETS TAB ============
    
    private void renderWidgetsTab(DrawContext context, int width, int height, int mouseX, int mouseY) {
        int padding = theme.spacingXL();
        int contentWidth = width - padding * 2;
        int y = padding;
        
        // Header
        context.drawTextWithShadow(client.textRenderer, Text.translatable("gui.pulse.widgets.title").getString(),
            padding, y, theme.textPrimary());
        y += 40;
        
        WidgetManager wm = WidgetManager.getInstance();
        
        // Widget list
        for (var entry : wm.getAllWidgets().entrySet()) {
            var widget = entry.getValue();
            
            GlassCard card = GlassCard.builder(padding, y)
                .width(contentWidth)
                .height(56)
                .variant(GlassCard.Variant.DEFAULT)
                .hoverable(true)
                .content(() -> renderWidgetListItem(context, contentWidth, 56, widget))
                .build();
            card.render(context, theme, mouseX - padding, mouseY - y);
            y += 62;
        }
        
        // Action buttons
        y += theme.spacingM();
        int btnY = y;
        int btnWidth = 180;
        int btnSpacing = theme.spacingM();
        int startX = (contentWidth - btnWidth * 2 - btnSpacing) / 2;
        
        // Edit layout
        boolean editorMode = wm.isEditorMode();
        GlassButton editBtn = GlassButton.builder(startX, btnY)
            .width(btnWidth)
            .text(editorMode ? Text.translatable("gui.pulse.widgets.exit_editor").getString() :
                           Text.translatable("gui.pulse.widgets.edit_layout").getString())
            .onClick(() -> { wm.setEditorMode(!editorMode); playClickSound(); })
            .style(editorMode ? GlassButton.Style.PRIMARY : GlassButton.Style.SECONDARY)
            .build();
        editBtn.render(context, theme, mouseX - startX, mouseY - btnY);
        
        // Reset layout
        GlassButton resetBtn = GlassButton.builder(startX + btnWidth + btnSpacing, btnY)
            .width(btnWidth)
            .text(Text.translatable("gui.pulse.widgets.reset_layout").getString())
            .onClick(() -> { wm.resetLayout(); playClickSound(); })
            .style(GlassButton.Style.SECONDARY)
            .build();
        resetBtn.render(context, theme, mouseX - (startX + btnWidth + btnSpacing), mouseY - btnY);
    }
    
    private void renderWidgetListItem(DrawContext context, int width, int height, com.alpoyy.pulse.widget.Widget widget) {
        // Visibility indicator
        int indicatorColor = widget.isVisible() ? theme.statusPlaying() : theme.textMuted();
        GlassRenderer.fillRoundedRect(context, 8, (height - 16) / 2, 16, 16, 4, indicatorColor | 0xFF000000);
        
        // Widget name
        context.drawTextWithShadow(client.textRenderer, widget.getName(), 36, (height - 8) / 2, theme.textPrimary());
        
        // Position/size info
        String info = "(" + widget.getX() + ", " + widget.getY() + ") " + widget.getWidth() + "x" + widget.getHeight();
        context.drawTextWithShadow(client.textRenderer, info, 36, (height - 8) / 2 + 18, theme.textMuted());
        
        // Toggle button (right side)
        int toggleX = width - 60;
        int toggleY = (height - 28) / 2;
        GlassToggle toggle = GlassToggle.builder(toggleX, toggleY)
            .value(widget.isVisible())
            .onChange(() -> {
                widget.setVisible(!widget.isVisible());
                ConfigManager.getInstance().saveAsync();
            })
            .showLabels(true)
            .labels("", "")
            .build();
        toggle.render(context, theme, mouseX - toggleX, mouseY - toggleY);
    }
    
    // ============ CONNECTIONS TAB ============
    
    private void renderConnectionsTab(DrawContext context, int width, int height, int mouseX, int mouseY) {
        int padding = theme.spacingXL();
        int contentWidth = width - padding * 2;
        int y = padding;
        
        context.drawTextWithShadow(client.textRenderer, Text.translatable("gui.pulse.connections.title").getString(),
            padding, y, theme.textPrimary());
        y += 40;
        
        PlatformManager pm = PlatformManager.getInstance();
        
        for (var entry : pm.getAllConnectors().entrySet()) {
            var connector = entry.getValue();
            
            GlassCard card = GlassCard.builder(padding, y)
                .width(contentWidth)
                .height(80)
                .variant(GlassCard.Variant.ELEVATED)
                .content(() -> renderConnectionCard(context, contentWidth, 80, connector))
                .build();
            card.render(context, theme, mouseX - padding, mouseY - y);
            y += 90;
        }
    }
    
    private void renderConnectionCard(DrawContext context, int width, int height, PlatformConnector connector) {
        boolean authenticated = connector.isAuthenticated();
        int statusColor = authenticated ? theme.statusPlaying() : theme.textMuted();
        
        // Platform icon
        GlassRenderer.drawConnectionsIcon(context, 16, (height - 32) / 2, 32, theme.getAccentColor());
        
        // Name
        context.drawTextWithShadow(client.textRenderer, connector.getPlatformName(), 60, (height - 16) / 2 - 10, theme.textPrimary());
        
        // Status
        String statusText = authenticated ? Text.translatable("gui.pulse.connections.connected").getString() :
                                           Text.translatable("gui.pulse.connections.disconnected").getString();
        context.drawTextWithShadow(client.textRenderer, statusText, 60, (height - 16) / 2 + 10, statusColor);
        
        // Connect/Disconnect button
        int btnX = width - 140;
        int btnY = (height - 36) / 2;
        String btnText = authenticated ? Text.translatable("gui.pulse.connections.disconnect").getString() :
                                        Text.translatable("gui.pulse.connections.connect").getString();
        GlassButton.Style style = authenticated ? GlassButton.Style.DANGER : GlassButton.Style.PRIMARY;
        
        GlassButton btn = GlassButton.builder(btnX, btnY)
            .width(120)
            .text(btnText)
            .onClick(() -> {
                if (authenticated) connector.disconnect();
                else connector.authenticate();
                playClickSound();
            })
            .style(style)
            .build();
        btn.render(context, theme, mouseX - btnX, mouseY - btnY);
    }
    
    // ============ SETTINGS TAB ============
    
    private void renderSettingsTab(DrawContext context, int width, int height, int mouseX, int mouseY) {
        int padding = theme.spacingXL();
        int contentWidth = width - padding * 2;
        int y = padding;
        
        GlassScrollContainer scroll = new GlassScrollContainer(padding, y, contentWidth, height - y - padding,
            () -> renderSettingsContent(context, contentWidth));
        scroll.setContentHeight(getSettingsContentHeight());
        scroll.render(context, theme, mouseX - padding, mouseY - y);
    }
    
    private void renderSettingsContent(DrawContext context, int width) {
        int y = 0;
        int labelWidth = 250;
        int controlX = labelWidth + theme.spacingL();
        int controlWidth = width - controlX - theme.spacingM();
        int rowHeight = 44;
        int spacing = theme.spacingM();
        
        ConfigManager configManager = ConfigManager.getInstance();
        PulseConfig config = configManager.getConfig();
        
        // --- APPEARANCE SECTION ---
        renderSectionHeader(context, width, y, "gui.pulse.settings.appearance");
        y += 36;
        
        // UI Opacity
        renderSettingRow(context, width, y, labelWidth, controlX, controlWidth, rowHeight,
            Text.translatable("gui.pulse.settings.ui_opacity").getString(),
            () -> GlassSlider.builder(controlX, y + (rowHeight - 24) / 2)
                .width(controlWidth)
                .value(config.uiOpacity)
                .range(0.1f, 1.0f)
                .onChange(() -> configManager.saveAsync())
                .build().render(context, theme, mouseX - controlX, mouseY - y - (rowHeight - 24) / 2));
        y += rowHeight + spacing;
        
        // Widget Scale
        renderSettingRow(context, width, y, labelWidth, controlX, controlWidth, rowHeight,
            Text.translatable("gui.pulse.settings.widget_scale").getString(),
            () -> GlassSlider.builder(controlX, y + (rowHeight - 24) / 2)
                .width(controlWidth)
                .value(config.widgetScale)
                .range(0.5f, 2.0f)
                .onChange(() -> configManager.saveAsync())
                .build().render(context, theme, mouseX - controlX, mouseY - y - (rowHeight - 24) / 2));
        y += rowHeight + spacing;
        
        // Glow Intensity
        renderSettingRow(context, width, y, labelWidth, controlX, controlWidth, rowHeight,
            Text.translatable("gui.pulse.settings.glow_intensity").getString(),
            () -> GlassSlider.builder(controlX, y + (rowHeight - 24) / 2)
                .width(controlWidth)
                .value(config.glowIntensity)
                .range(0.0f, 1.0f)
                .onChange(() -> configManager.saveAsync())
                .build().render(context, theme, mouseX - controlX, mouseY - y - (rowHeight - 24) / 2));
        y += rowHeight + spacing;
        
        // Background Opacity
        renderSettingRow(context, width, y, labelWidth, controlX, controlWidth, rowHeight,
            Text.translatable("gui.pulse.settings.background_opacity").getString(),
            () -> GlassSlider.builder(controlX, y + (rowHeight - 24) / 2)
                .width(controlWidth)
                .value(config.backgroundOpacity)
                .range(0.0f, 1.0f)
                .onChange(() -> configManager.saveAsync())
                .build().render(context, theme, mouseX - controlX, mouseY - y - (rowHeight - 24) / 2));
        y += rowHeight + spacing;
        
        // Accent Color
        renderSettingRow(context, width, y, labelWidth, controlX, controlWidth, rowHeight,
            Text.translatable("gui.pulse.settings.accent_color").getString(),
            () -> {
                int colorX = controlX;
                int colorY = y + (rowHeight - 30) / 2;
                GlassRenderer.fillRoundedRect(context, colorX, colorY, 30, 30, 6, config.getAccentColor().getRGB() | 0xFF000000);
                GlassRenderer.drawRoundedRectOutline(context, colorX, colorY, 30, 30, 6, 2, theme.borderAccentSoft());
            });
        y += rowHeight + spacing * 2;
        
        // --- MUSIC SECTION ---
        renderSectionHeader(context, width, y, "gui.pulse.settings.music");
        y += 36;
        
        // Master Volume
        renderSettingRow(context, width, y, labelWidth, controlX, controlWidth, rowHeight,
            Text.translatable("gui.pulse.settings.master_volume").getString(),
            () -> GlassSlider.builder(controlX, y + (rowHeight - 24) / 2)
                .width(controlWidth)
                .value(config.masterVolume)
                .range(0.0f, 1.0f)
                .onChange(() -> configManager.saveAsync())
                .build().render(context, theme, mouseX - controlX, mouseY - y - (rowHeight - 24) / 2));
        y += rowHeight + spacing;
        
        // Music Volume
        renderSettingRow(context, width, y, labelWidth, controlX, controlWidth, rowHeight,
            Text.translatable("gui.pulse.settings.music_volume").getString(),
            () -> GlassSlider.builder(controlX, y + (rowHeight - 24) / 2)
                .width(controlWidth)
                .value(config.musicVolume)
                .range(0.0f, 1.0f)
                .onChange(() -> configManager.saveAsync())
                .build().render(context, theme, mouseX - controlX, mouseY - y - (rowHeight - 24) / 2));
        y += rowHeight + spacing * 2;
        
        // --- SONG REQUESTS SECTION ---
        renderSectionHeader(context, width, y, "gui.pulse.settings.requests");
        y += 36;
        
        // Max Queue Size
        renderSettingRow(context, width, y, labelWidth, controlX, controlWidth, rowHeight,
            Text.translatable("gui.pulse.settings.max_queue_size").getString(),
            () -> GlassSlider.builder(controlX, y + (rowHeight - 24) / 2)
                .width(controlWidth)
                .value(config.maxQueueSize / 100f)
                .range(0.1f, 1.0f)
                .onChange(() -> { config.maxQueueSize = (int) (config.maxQueueSize / 100f * 100); configManager.saveAsync(); })
                .build().render(context, theme, mouseX - controlX, mouseY - y - (rowHeight - 24) / 2));
        y += rowHeight + spacing;
        
        // Duplicate Prevention
        renderSettingRow(context, width, y, labelWidth, controlX, controlWidth, rowHeight,
            Text.translatable("gui.pulse.settings.duplicate_prevention").getString(),
            () -> GlassToggle.builder(controlX, y + (rowHeight - 28) / 2)
                .value(config.duplicatePrevention)
                .onChange(() -> { config.duplicatePrevention = !config.duplicatePrevention; configManager.saveAsync(); })
                .build().render(context, theme, mouseX - controlX, mouseY - y - (rowHeight - 28) / 2));
        y += rowHeight + spacing;
        
        // Auto Clear Completed
        renderSettingRow(context, width, y, labelWidth, controlX, controlWidth, rowHeight,
            Text.translatable("gui.pulse.settings.auto_clear_completed").getString(),
            () -> GlassToggle.builder(controlX, y + (rowHeight - 28) / 2)
                .value(config.autoClearCompleted)
                .onChange(() -> { config.autoClearCompleted = !config.autoClearCompleted; configManager.saveAsync(); })
                .build().render(context, theme, mouseX - controlX, mouseY - y - (rowHeight - 28) / 2));
        y += rowHeight + spacing * 2;
        
        // --- CONNECTIONS SECTION ---
        renderSectionHeader(context, width, y, "gui.pulse.settings.connections");
        y += 36;
        
        // Auto Reconnect
        renderSettingRow(context, width, y, labelWidth, controlX, controlWidth, rowHeight,
            Text.translatable("gui.pulse.settings.auto_reconnect").getString(),
            () -> GlassToggle.builder(controlX, y + (rowHeight - 28) / 2)
                .value(config.autoReconnect)
                .onChange(() -> { config.autoReconnect = !config.autoReconnect; configManager.saveAsync(); })
                .build().render(context, theme, mouseX - controlX, mouseY - y - (rowHeight - 28) / 2));
        y += rowHeight + spacing;
        
        // Show Chat Widget
        renderSettingRow(context, width, y, labelWidth, controlX, controlWidth, rowHeight,
            Text.translatable("gui.pulse.settings.show_chat_widget").getString(),
            () -> GlassToggle.builder(controlX, y + (rowHeight - 28) / 2)
                .value(config.showChatWidget)
                .onChange(() -> { config.showChatWidget = !config.showChatWidget; configManager.saveAsync(); })
                .build().render(context, theme, mouseX - controlX, mouseY - y - (rowHeight - 28) / 2));
    }
    
    private void renderSectionHeader(DrawContext context, int width, int y, String translationKey) {
        String text = Text.translatable(translationKey).getString();
        context.drawTextWithShadow(client.textRenderer, text, 0, y, theme.textPrimary());
        // Underline
        GlassRenderer.fillRoundedRect(context, 0, y + 20, client.textRenderer.getWidth(text) + 8, 2, 1, theme.accent(0.5f));
    }
    
    private void renderSettingRow(DrawContext context, int width, int y, int labelWidth, int controlX, 
                                   int controlWidth, int rowHeight, String label, Runnable controlRenderer) {
        // Label
        context.drawTextWithShadow(client.textRenderer, label, 0, y + (rowHeight - 8) / 2, theme.textPrimary());
        
        // Control
        context.getMatrices().push();
        context.getMatrices().translate(controlX, 0, 0);
        controlRenderer.run();
        context.getMatrices().pop();
    }
    
    private int getSettingsContentHeight() {
        return 800; // Approximate
    }
    
    // ============ HELPER METHODS ============
    
    private int getRequestStatusColor(SongRequest.RequestStatus status) {
        return switch (status) {
            case PLAYING -> theme.statusPlaying();
            case QUEUED, AVAILABLE -> theme.textPrimary();
            case SEARCHING -> theme.statusWarning();
            case UNAVAILABLE -> 0xFFFF6666;
            case REJECTED, FAILED -> theme.statusError();
            case COMPLETED -> theme.textMuted();
            default -> theme.textPrimary();
        };
    }
    
    private String formatTime(long ms) {
        long seconds = ms / 1000;
        long minutes = seconds / 60;
        seconds %= 60;
        return String.format("%d:%02d", minutes, seconds);
    }
    
    private boolean isMouseOverItem(int mouseX, int mouseY, int itemX, int itemY, int itemWidth, int itemHeight) {
        return mouseX >= itemX && mouseX <= itemX + itemWidth && 
               mouseY >= itemY && mouseY <= itemY + itemHeight;
    }
    
    private TikTokLiveConnection getLiveConnection() {
        // Access through the request manager which has the connection
        // For now return null - will be wired up properly
        return null;
    }
    
    private void playClickSound() {
        client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0f));
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Sidebar tabs
        if (mouseX < theme.sidebarWidth()) {
            if (navTabs.mouseClicked(mouseX, mouseY, button)) return true;
            
            // Check editor button
            // ... handled by render
        }
        
        // Search field
        int headerHeight = theme.headerHeight();
        int contentX = theme.sidebarWidth();
        int searchX = width - 300;
        int searchY = (headerHeight - 36) / 2;
        if (mouseX >= contentX + searchX && mouseX <= contentX + searchX + 280 &&
            mouseY >= searchY && mouseY <= searchY + 36) {
            if (searchField.mouseClicked(mouseX - contentX - searchX, mouseY - searchY, button)) {
                return true;
            }
        }
        
        // Content area clicks (delegated to specific tab renderers)
        // For now, let super handle it
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }
    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return super.mouseReleased(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchField.keyPressed(keyCode, scanCode, modifiers)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (searchField.charTyped(chr, modifiers)) return true;
        return super.charTyped(chr, modifiers);
    }
    
    @Override
    public void close() {
        WidgetManager.getInstance().setEditorMode(false);
        super.close();
    }
    
    @Override
    public boolean shouldPause() {
        return false;
    }
}