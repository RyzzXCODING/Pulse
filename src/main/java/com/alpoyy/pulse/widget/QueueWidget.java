package com.alpoyy.pulse.widget;

import com.alpoyy.pulse.Pulse;
import com.alpoyy.pulse.config.ConfigManager;
import com.alpoyy.pulse.config.PulseConfig;
import com.alpoyy.pulse.request.RequestManager;
import com.alpoyy.pulse.request.SongRequest;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

import java.awt.Color;
import java.util.List;

public class QueueWidget extends Widget {
    private static final int DEFAULT_WIDTH = 280;
    private static final int DEFAULT_HEIGHT = 160;
    private static final int MAX_VISIBLE = 5;
    
    public QueueWidget() {
        super("queue", "Song Queue", 10, 250, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }
    
    @Override
    public void render(DrawContext context, RenderTickCounter tickCounter, float widgetScale) {
        if (!isVisible()) return;
        
        ConfigManager configManager = ConfigManager.getInstance();
        PulseConfig config = configManager.getConfig();
        RequestManager requestManager = RequestManager.getInstance();
        
        int x = this.x;
        int y = this.y;
        int w = (int) (width * widgetScale);
        int h = (int) (height * widgetScale);
        
        int accentColor = config.getAccentColor().getRGB() & 0xFFFFFF;
        int secondaryColor = config.getSecondaryAccentColor().getRGB() & 0xFFFFFF;
        
        // Render background
        renderBackground(context, x, y, w, h, config.backgroundOpacity, config.glowIntensity, accentColor);
        
        // Title
        String title = "Song Queue";
        int titleX = x + 10;
        int titleY = y + 8;
        context.drawTextWithShadow(Pulse.CLIENT.textRenderer, title, titleX, titleY, 0xFFFFFFFF);
        
        // Queue count
        int pendingCount = requestManager.getPendingCount();
        String countText = pendingCount + " pending";
        int countX = x + w - Pulse.CLIENT.textRenderer.getWidth(countText) - 10;
        context.drawTextWithShadow(Pulse.CLIENT.textRenderer, countText, countX, titleY, 0xFFAAAAAA);
        
        // Current playing
        SongRequest current = requestManager.getCurrentRequest();
        int itemY = titleY + 18;
        
        if (current != null) {
            // Now playing header
            context.drawTextWithShadow(Pulse.CLIENT.textRenderer, "▶ Now Playing:", titleX, itemY, secondaryColor | 0xFF000000);
            itemY += 12;
            
            String currentText = current.getDisplayName();
            if (currentText.length() > 35) {
                currentText = currentText.substring(0, 32) + "...";
            }
            context.drawTextWithShadow(Pulse.CLIENT.textRenderer, currentText, titleX + 10, itemY, 0xFFFFFFFF);
            
            String requesterText = "by " + current.getRequester();
            context.drawTextWithShadow(Pulse.CLIENT.textRenderer, requesterText, titleX + 10, itemY + 11, 0xFF888888);
            itemY += 26;
        }
        
        // Upcoming queue
        List<SongRequest> pending = requestManager.getPendingRequests();
        if (!pending.isEmpty()) {
            context.drawTextWithShadow(Pulse.CLIENT.textRenderer, "Upcoming:", titleX, itemY, 0xFFAAAAAA);
            itemY += 12;
            
            int count = 0;
            for (SongRequest request : pending) {
                if (count >= MAX_VISIBLE) {
                    int remaining = pending.size() - count;
                    String moreText = "+" + remaining + " more...";
                    context.drawTextWithShadow(Pulse.CLIENT.textRenderer, moreText, titleX + 10, itemY, 0xFF888888);
                    break;
                }
                
                String reqText = (count + 1) + ". " + request.getDisplayName();
                if (reqText.length() > 38) {
                    reqText = reqText.substring(0, 35) + "...";
                }
                
                int statusColor = getStatusColor(request.getStatus());
                context.drawTextWithShadow(Pulse.CLIENT.textRenderer, reqText, titleX + 10, itemY, statusColor);
                
                String reqBy = "   @" + request.getRequester();
                context.drawTextWithShadow(Pulse.CLIENT.textRenderer, reqBy, titleX + 10, itemY + 10, 0xFF666666);
                
                itemY += 22;
                count++;
            }
        } else if (current == null) {
            // Empty queue
            String emptyText = "Queue is empty";
            int emptyX = x + (w - Pulse.CLIENT.textRenderer.getWidth(emptyText)) / 2;
            int emptyY = y + h / 2;
            context.drawTextWithShadow(Pulse.CLIENT.textRenderer, emptyText, emptyX, emptyY, 0xFF888888);
        }
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
    
    private int getStatusColor(SongRequest.RequestStatus status) {
        return switch (status) {
            case PLAYING -> 0xFF00FF00;
            case QUEUED, AVAILABLE -> 0xFFFFFFFF;
            case SEARCHING -> 0xFFFFAA00;
            case UNAVAILABLE -> 0xFFFF6666;
            case REJECTED -> 0xFFFF0000;
            case FAILED -> 0xFFFF0000;
            case COMPLETED -> 0xFF888888;
            default -> 0xFFFFFFFF;
        };
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