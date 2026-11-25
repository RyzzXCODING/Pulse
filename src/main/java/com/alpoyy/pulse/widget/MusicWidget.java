package com.alpoyy.pulse.widget;

import com.alpoyy.pulse.Pulse;
import com.alpoyy.pulse.config.ConfigManager;
import com.alpoyy.pulse.config.PulseConfig;
import com.alpoyy.pulse.music.PlaybackController;
import com.alpoyy.pulse.music.Song;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Identifier;

import java.awt.Color;

public class MusicWidget extends Widget {
    private static final int DEFAULT_WIDTH = 280;
    private static final int DEFAULT_HEIGHT = 120;
    
    private Song lastSong;
    private long lastPosition;
    private long lastDuration;
    
    public MusicWidget() {
        super("music", "Music Player", 10, 10, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }
    
    @Override
    public void render(DrawContext context, RenderTickCounter tickCounter, float widgetScale) {
        if (!isVisible()) return;
        
        ConfigManager configManager = ConfigManager.getInstance();
        PulseConfig config = configManager.getConfig();
        PlaybackController controller = PlaybackController.getInstance();
        
        int x = this.x;
        int y = this.y;
        int w = (int) (width * widgetScale);
        int h = (int) (height * widgetScale);
        
        int accentColor = config.getAccentColor().getRGB() & 0xFFFFFF;
        int secondaryColor = config.getSecondaryAccentColor().getRGB() & 0xFFFFFF;
        
        // Render background
        renderBackground(context, x, y, w, h, config.backgroundOpacity, config.glowIntensity, accentColor);
        
        // Get current song
        Song currentSong = controller.getCurrentSong();
        boolean playing = controller.isPlaying();
        long position = controller.getCurrentPosition();
        long duration = controller.getDuration();
        
        if (currentSong != null) {
            // Album artwork placeholder (left side)
            int artSize = Math.min(h - 10, 100);
            int artX = x + 5;
            int artY = y + 5;
            
            // Draw artwork background
            context.fill(artX, artY, artX + artSize, artY + artSize, 0xFF0D0D1A);
            
            // Draw border
            context.fill(artX, artY, artX + artSize, artY + 1, accentColor | 0x80000000);
            context.fill(artX, artY + artSize - 1, artX + artSize, artY + artSize, accentColor | 0x80000000);
            context.fill(artX, artY, artX + 1, artY + artSize, accentColor | 0x80000000);
            context.fill(artX + artSize - 1, artY, artX + artSize, artY + artSize, accentColor | 0x80000000);
            
            // Song info (right of artwork)
            int textX = artX + artSize + 10;
            int textY = artY + 5;
            int textWidth = w - (textX - x) - 10;
            
            // Title
            String title = currentSong.getTitle();
            context.drawTextWithShadow(Pulse.CLIENT.textRenderer, title, textX, textY, 0xFFFFFFFF);
            
            // Artist
            String artist = currentSong.getArtist();
            context.drawTextWithShadow(Pulse.CLIENT.textRenderer, artist, textX, textY + 14, 0xFFAAAAAA);
            
            // Source badge
            String source = currentSong.getSource().name();
            context.drawTextWithShadow(Pulse.CLIENT.textRenderer, "[" + source + "]", textX, textY + 28, secondaryColor | 0xFF000000);
            
            // Progress bar
            if (duration > 0) {
                int barY = y + h - 25;
                int barWidth = w - 20;
                int barX = x + 10;
                
                // Background
                context.fill(barX, barY, barX + barWidth, barY + 4, 0xFF0D0D1A);
                
                // Progress
                float progress = (float) position / duration;
                int progressWidth = (int) (barWidth * progress);
                context.fill(barX, barY, barX + progressWidth, barY + 4, accentColor | 0xFF000000);
                
                // Time text
                String timeText = formatTime(position) + " / " + formatTime(duration);
                context.drawTextWithShadow(Pulse.CLIENT.textRenderer, timeText, barX, barY - 12, 0xFF888888);
            }
            
            // Playback status indicator
            String statusText = playing ? "▶ Playing" : "⏸ Paused";
            int statusColor = playing ? 0xFF00FF00 : 0xFFFFAA00;
            context.drawTextWithShadow(Pulse.CLIENT.textRenderer, statusText, textX, textY + 42, statusColor);
            
        } else {
            // No song playing
            String text = "No music playing";
            int textX = x + (w - Pulse.CLIENT.textRenderer.getWidth(text)) / 2;
            int textY = y + h / 2 - 5;
            context.drawTextWithShadow(Pulse.CLIENT.textRenderer, text, textX, textY, 0xFF888888);
        }
        
        // Volume indicator (small)
        if (currentSong != null) {
            int volX = x + w - 30;
            int volY = y + 5;
            float vol = controller.getVolume();
            String volText = "♪" + (int)(vol * 100) + "%";
            context.drawTextWithShadow(Pulse.CLIENT.textRenderer, volText, volX, volY, 0xFF888888);
        }
    }
    
    @Override
    public void renderEditor(DrawContext context, RenderTickCounter tickCounter, float widgetScale) {
        render(context, tickCounter, widgetScale);
        
        // Draw editor handles
        ConfigManager configManager = ConfigManager.getInstance();
        PulseConfig config = configManager.getConfig();
        int accentColor = config.getAccentColor().getRGB() & 0xFFFFFF;
        
        int x = this.x;
        int y = this.y;
        int w = (int) (width * widgetScale);
        int h = (int) (height * widgetScale);
        
        // Selection border
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
    
    @Override
    public void onClick(double mouseX, double mouseY, int button) {
        if (button == 0) { // Left click
            PlaybackController controller = PlaybackController.getInstance();
            controller.togglePlayPause();
        }
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
    
    private String formatTime(long ms) {
        long seconds = ms / 1000;
        long minutes = seconds / 60;
        seconds %= 60;
        return String.format("%d:%02d", minutes, seconds);
    }
}