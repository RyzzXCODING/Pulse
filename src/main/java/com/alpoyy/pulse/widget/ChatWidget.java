package com.alpoyy.pulse.widget;

import com.alpoyy.pulse.Pulse;
import com.alpoyy.pulse.config.ConfigManager;
import com.alpoyy.pulse.config.PulseConfig;
import com.alpoyy.pulse.live.LiveChatEvent;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class ChatWidget extends Widget {
    private static final int DEFAULT_WIDTH = 320;
    private static final int DEFAULT_HEIGHT = 200;
    
    private final List<ChatMessage> messages = new ArrayList<>();
    private int maxMessages = 50;
    
    public ChatWidget() {
        super("chat", "LIVE Chat", 10, 420, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }
    
    public void addMessage(LiveChatEvent event) {
        if (event == null || !event.isChatMessage()) return;
        
        ChatMessage msg = new ChatMessage(
            event.getUsername(),
            event.getMessage(),
            event.getTimestamp(),
            event.isModerator(),
            event.isSubscriber(),
            event.getBadge(),
            event.getColor()
        );
        
        messages.add(0, msg); // Add to front (newest first)
        
        // Trim to max
        while (messages.size() > maxMessages) {
            messages.remove(messages.size() - 1);
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
        int secondaryColor = config.getSecondaryAccentColor().getRGB() & 0xFFFFFF;
        
        // Render background
        renderBackground(context, x, y, w, h, config.backgroundOpacity, config.glowIntensity, accentColor);
        
        // Title
        String title = "LIVE Chat";
        int titleX = x + 10;
        int titleY = y + 8;
        context.drawTextWithShadow(Pulse.CLIENT.textRenderer, title, titleX, titleY, 0xFFFFFFFF);
        
        // Message count
        String countText = messages.size() + " messages";
        int countX = x + w - Pulse.CLIENT.textRenderer.getWidth(countText) - 10;
        context.drawTextWithShadow(Pulse.CLIENT.textRenderer, countText, countX, titleY, 0xFFAAAAAA);
        
        // Messages (render from bottom up for chat-like appearance)
        int msgY = y + h - 10;
        int lineHeight = 14;
        
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatMessage msg = messages.get(i);
            msgY -= lineHeight;
            
            if (msgY < titleY + 18) break; // Don't overlap title
            
            // Username with badge
            String prefix = "";
            if (msg.isModerator()) prefix += "🛡 ";
            if (msg.isSubscriber()) prefix += "⭐ ";
            if (msg.getBadge() != null && !msg.getBadge().isEmpty()) prefix += msg.getBadge() + " ";
            
            String userText = prefix + msg.getUsername() + ": ";
            int userColor = msg.getColor() != null ? parseColor(msg.getColor()) : 0xFFFFFFFF;
            
            context.drawTextWithShadow(Pulse.CLIENT.textRenderer, userText, titleX, msgY, userColor);
            
            // Message text
            int userWidth = Pulse.CLIENT.textRenderer.getWidth(userText);
            String msgText = msg.getMessage();
            int maxMsgWidth = w - 20 - userWidth;
            
            if (Pulse.CLIENT.textRenderer.getWidth(msgText) > maxMsgWidth) {
                msgText = Pulse.CLIENT.textRenderer.trimToWidth(msgText, maxMsgWidth) + "...";
            }
            
            context.drawTextWithShadow(Pulse.CLIENT.textRenderer, msgText, titleX + userWidth, msgY, 0xFFCCCCCC);
        }
        
        // Scroll indicator
        if (messages.size() > (h - 30) / lineHeight) {
            String scrollText = "↑ Scroll";
            int scrollX = x + w - Pulse.CLIENT.textRenderer.getWidth(scrollText) - 10;
            context.drawTextWithShadow(Pulse.CLIENT.textRenderer, scrollText, scrollX, y + h - 14, 0xFF666666);
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
        
        this.maxMessages = config.maxChatMessages;
        
        PulseConfig.WidgetPosition pos = config.widgetPositions.get(getId());
        if (pos != null) {
            setX(pos.x);
            setY(pos.y);
        }
    }
    
    private int parseColor(String colorStr) {
        try {
            if (colorStr.startsWith("#")) {
                return java.awt.Color.decode(colorStr).getRGB() & 0xFFFFFF | 0xFF000000;
            }
        } catch (Exception e) {
            // Ignore
        }
        return 0xFFFFFFFF;
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
    
    private static class ChatMessage {
        private final String username;
        private final String message;
        private final java.time.Instant timestamp;
        private final boolean isModerator;
        private final boolean isSubscriber;
        private final String badge;
        private final String color;
        
        public ChatMessage(String username, String message, java.time.Instant timestamp,
                          boolean isModerator, boolean isSubscriber, String badge, String color) {
            this.username = username;
            this.message = message;
            this.timestamp = timestamp;
            this.isModerator = isModerator;
            this.isSubscriber = isSubscriber;
            this.badge = badge;
            this.color = color;
        }
        
        public String getUsername() { return username; }
        public String getMessage() { return message; }
        public java.time.Instant getTimestamp() { return timestamp; }
        public boolean isModerator() { return isModerator; }
        public boolean isSubscriber() { return isSubscriber; }
        public String getBadge() { return badge; }
        public String getColor() { return color; }
    }
}