package com.alpoyy.pulse.widget;

import com.alpoyy.pulse.Pulse;
import com.alpoyy.pulse.config.ConfigManager;
import com.alpoyy.pulse.config.PulseConfig;
import com.alpoyy.pulse.live.TikTokLiveConnection;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

import java.awt.Color;

public class LiveWidget extends Widget {
    private static final int DEFAULT_WIDTH = 280;
    private static final int DEFAULT_HEIGHT = 100;
    
    public LiveWidget() {
        super("live", "TikTok LIVE", 10, 140, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }
    
    @Override
    public void render(DrawContext context, RenderTickCounter tickCounter, float widgetScale) {
        if (!isVisible()) return;
        
        ConfigManager configManager = ConfigManager.getInstance();
        PulseConfig config = configManager.getConfig();
        TikTokLiveConnection connection = getConnection();
        
        int x = this.x;
        int y = this.y;
        int w = (int) (width * widgetScale);
        int h = (int) (height * widgetScale);
        
        int accentColor = config.getAccentColor().getRGB() & 0xFFFFFF;
        int secondaryColor = config.getSecondaryAccentColor().getRGB() & 0xFFFFFF;
        int liveColor = 0xFFFF0000; // Red for LIVE
        int connectedColor = 0xFF00FF00; // Green for connected
        
        // Render background
        renderBackground(context, x, y, w, h, config.backgroundOpacity, config.glowIntensity, accentColor);
        
        // Title bar
        String title = "TikTok LIVE";
        int titleX = x + 10;
        int titleY = y + 8;
        context.drawTextWithShadow(Pulse.CLIENT.textRenderer, title, titleX, titleY, 0xFFFFFFFF);
        
        // Username
        String username = connection != null ? connection.getUsername() : "Not configured";
        if (username == null || username.isEmpty()) {
            username = "Not configured";
        }
        String userText = "@" + username;
        context.drawTextWithShadow(Pulse.CLIENT.textRenderer, userText, titleX, titleY + 16, 0xFFCCCCCC);
        
        // Connection status
        String statusText;
        int statusColor;
        boolean isLive = false;
        
        if (connection != null) {
            switch (connection.getState()) {
                case CONNECTED -> {
                    statusText = "● Connected";
                    statusColor = connectedColor;
                    isLive = connection.isLive();
                }
                case CONNECTING -> {
                    statusText = "◐ Connecting...";
                    statusColor = 0xFFFFAA00;
                }
                case ERROR -> {
                    statusText = "● Error";
                    statusColor = 0xFFFF0000;
                }
                default -> {
                    statusText = "○ Disconnected";
                    statusColor = 0xFF888888;
                }
            }
        } else {
            statusText = "○ Not configured";
            statusColor = 0xFF888888;
        }
        
        context.drawTextWithShadow(Pulse.CLIENT.textRenderer, statusText, titleX, titleY + 32, statusColor);
        
        // LIVE badge
        if (isLive) {
            String liveText = "🔴 LIVE";
            int liveX = x + w - Pulse.CLIENT.textRenderer.getWidth(liveText) - 10;
            int liveY = titleY;
            
            // Background for LIVE badge
            int badgeW = Pulse.CLIENT.textRenderer.getWidth(liveText) + 8;
            context.fill(liveX - 4, liveY - 2, liveX + badgeW, liveY + 12, 0xCCFF0000);
            context.drawTextWithShadow(Pulse.CLIENT.textRenderer, liveText, liveX, liveY, 0xFFFFFFFF);
        }
        
        // Error message if any
        if (connection != null && connection.getLastError() != null) {
            String error = connection.getLastError();
            if (error.length() > 40) {
                error = error.substring(0, 37) + "...";
            }
            context.drawTextWithShadow(Pulse.CLIENT.textRenderer, error, titleX, titleY + 48, 0xFFFF6666);
        }
        
        // Viewer count placeholder
        String viewersText = "Viewers: --";
        context.drawTextWithShadow(Pulse.CLIENT.textRenderer, viewersText, titleX, titleY + 64, 0xFF888888);
    }
    
    @Override
    public void renderEditor(DrawContext context, RenderTickCounter tickCounter, float widgetScale) {
        render(context, tickCounter, widgetScale);
        drawEditorBorder(context, widgetScale);
    }
    
    @Override
    public void applyConfig(PulseConfig config) {
        Boolean visible = config.widgetVisibility.get(getId());
        if (visible != null) {
            setVisible(visible);
        }
        
        PulseConfig.WidgetPosition pos = config.widgetPositions.get(getId());
        if (pos != null) {
            setX(pos.x);
            setY(pos.y);
        }
    }
    
    private TikTokLiveConnection getConnection() {
        // Access through reflection or static reference
        // For now, return null - will be set via external reference
        return null;
    }
    
    public void setConnection(TikTokLiveConnection connection) {
        // Store reference if needed
    }
    
    private void drawEditorBorder(DrawContext context, float widgetScale) {
        ConfigManager configManager = ConfigManager.getInstance();
        PulseConfig config = configManager.getConfig();
        int accentColor = config.getAccentColor().getRGB() & 0xFFFFFF;
        
        int x = this.x;
        int y = this.y;
        int w = (int) (width * widgetScale);
        int h = (int) (height * widgetScale);
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        context.fill(x - 2, y - 2, x + w + 2, y - 1, accentColor | 0xFF000000);
        context.fill(x - 2, y + h + 1, x + w + 2, y + h + 2, accentColor | 0xFF000000);
        context.fill(x - 2, y - 2, x - 1, y + h + 2, accentColor | 0xFF000000);
        context.fill(x + w + 1, y - 2, x + w + 2, y + h + 2, accentColor | 0xFF000000);
        RenderSystem.disableBlend();
        
        // Drag handle
        context.fill(x + 2, y + 2, x + 20, y + 12, 0x80FFFFFF);
        context.drawTextWithShadow(Pulse.CLIENT.textRenderer, "☰", x + 4, y + 3, 0xFFFFFFFF);
    }
}