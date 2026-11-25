package com.alpoyy.pulse.widget;

import com.alpoyy.pulse.Pulse;
import com.alpoyy.pulse.config.ConfigManager;
import com.alpoyy.pulse.config.PulseConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

import java.awt.Color;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class NotificationWidget extends Widget {
    private static final int DEFAULT_WIDTH = 280;
    private static final int DEFAULT_HEIGHT = 100;
    private static final int MAX_NOTIFICATIONS = 5;
    private static final long NOTIFICATION_DURATION = 5000; // 5 seconds
    
    private final List<Notification> notifications = new ArrayList<>();
    
    public NotificationWidget() {
        super("notifications", "Notifications", 300, 10, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }
    
    public void addNotification(String title, String message) {
        Notification notification = new Notification(title, message, Instant.now());
        notifications.add(0, notification);
        
        // Trim to max
        while (notifications.size() > MAX_NOTIFICATIONS) {
            notifications.remove(notifications.size() - 1);
        }
    }
    
    @Override
    public void render(DrawContext context, RenderTickCounter tickCounter, float widgetScale) {
        if (!isVisible()) return;
        
        ConfigManager configManager = ConfigManager.getInstance();
        PulseConfig config = configManager.getConfig();
        
        int x = this.x;
        int y = this.y;
        int w = (int) (width * widgetScale);
        int h = (int) (height * widgetScale);
        
        int accentColor = config.getAccentColor().getRGB() & 0xFFFFFF;
        
        // Render background (only if there are notifications)
        if (!notifications.isEmpty()) {
            renderBackground(context, x, y, w, h, config.backgroundOpacity, config.glowIntensity, accentColor);
        }
        
        Instant now = Instant.now();
        int notifY = y + 8;
        
        // Remove expired notifications
        notifications.removeIf(n -> now.isAfter(n.expiresAt));
        
        for (int i = 0; i < notifications.size(); i++) {
            Notification notif = notifications.get(i);
            long remaining = java.time.Duration.between(now, notif.expiresAt).toMillis();
            
            if (remaining <= 0) continue;
            
            // Fade out in last second
            float alpha = 1.0f;
            if (remaining < 1000) {
                alpha = remaining / 1000.0f;
            }
            
            int titleColor = (int) (alpha * 255) << 24 | 0xFFFFFF;
            int msgColor = (int) (alpha * 200) << 24 | 0xCCCCCC;
            
            // Title
            context.drawTextWithShadow(Pulse.CLIENT.textRenderer, notif.title, x + 10, notifY, titleColor);
            notifY += 12;
            
            // Message
            String msg = notif.message;
            if (Pulse.CLIENT.textRenderer.getWidth(msg) > w - 20) {
                msg = Pulse.CLIENT.textRenderer.trimToWidth(msg, w - 20) + "...";
            }
            context.drawTextWithShadow(Pulse.CLIENT.textRenderer, msg, x + 10, notifY, msgColor);
            notifY += 16;
            
            // Small gap between notifications
            notifY += 4;
        }
    }
    
    @Override
    public void renderEditor(DrawContext context, RenderTickCounter tickCounter, float widgetScale) {
        // In editor mode, show a placeholder
        ConfigManager configManager = ConfigManager.getInstance();
        PulseConfig config = configManager.getConfig();
        
        int x = this.x;
        int y = this.y;
        int w = (int) (width * widgetScale);
        int h = (int) (height * widgetScale);
        
        int accentColor = config.getAccentColor().getRGB() & 0xFFFFFF;
        
        // Render background
        renderBackground(context, x, y, w, h, config.backgroundOpacity, config.glowIntensity, accentColor);
        
        // Placeholder text
        String text = "Notifications Widget";
        int textX = x + (w - Pulse.CLIENT.textRenderer.getWidth(text)) / 2;
        int textY = y + h / 2 - 5;
        context.drawTextWithShadow(Pulse.CLIENT.textRenderer, text, textX, textY, 0xFF888888);
        
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
    
    private static class Notification {
        final String title;
        final String message;
        final Instant timestamp;
        final Instant expiresAt;
        
        public Notification(String title, String message, Instant timestamp) {
            this.title = title;
            this.message = message;
            this.timestamp = timestamp;
            this.expiresAt = timestamp.plusMillis(NOTIFICATION_DURATION);
        }
    }
}