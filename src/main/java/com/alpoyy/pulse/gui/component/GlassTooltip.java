package com.alpoyy.pulse.gui.component;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.MinecraftClient;

public class GlassTooltip {
    private final String text;
    private final int maxWidth;
    private int x, y;
    private boolean visible = false;
    private long showTime = 0;
    private float animProgress = 0f;
    
    public GlassTooltip(String text) {
        this(text, 250);
    }
    
    public GlassTooltip(String text, int maxWidth) {
        this.text = text;
        this.maxWidth = maxWidth;
    }
    
    public void show(int mouseX, int mouseY) {
        this.x = mouseX + 16;
        this.y = mouseY + 8;
        this.visible = true;
        this.showTime = System.currentTimeMillis();
        this.animProgress = 0f;
    }
    
    public void hide() {
        this.visible = false;
        this.animProgress = 0f;
    }
    
    public void updatePosition(int mouseX, int mouseY) {
        if (!visible) return;
        
        int screenWidth = MinecraftClient.getInstance().getWindow().getScaledWidth();
        int screenHeight = MinecraftClient.getInstance().getWindow().getScaledHeight();
        
        // Calculate tooltip dimensions
        int padding = 10;
        int textWidth = Math.min(maxWidth, MinecraftClient.getInstance().textRenderer.getWidth(text));
        int lines = (int) Math.ceil((double) MinecraftClient.getInstance().textRenderer.getWidth(text) / maxWidth);
        int height = padding * 2 + lines * 12;
        
        // Adjust position to stay on screen
        int tooltipX = mouseX + 16;
        int tooltipY = mouseY + 8;
        
        if (tooltipX + textWidth + padding * 2 > screenWidth) {
            tooltipX = mouseX - textWidth - padding * 2 - 16;
        }
        if (tooltipY + height > screenHeight) {
            tooltipY = screenHeight - height - 8;
        }
        if (tooltipX < 8) tooltipX = 8;
        if (tooltipY < 8) tooltipY = 8;
        
        this.x = tooltipX;
        this.y = tooltipY;
    }
    
    public void render(DrawContext context, PulseTheme theme) {
        if (!visible || text == null || text.isEmpty()) return;
        
        // Animation
        long now = System.currentTimeMillis();
        float target = 1f;
        float delta = (now - showTime) / 150f;
        if (animProgress < target) {
            animProgress = Math.min(target, animProgress + delta);
        }
        
        if (animProgress <= 0) return;
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        int padding = 10;
        int radius = theme.radiusSmall();
        int accentColor = theme.getAccentColor();
        
        // Calculate dimensions
        int textWidth = Math.min(maxWidth, theme.client.textRenderer.getWidth(text));
        int lines = theme.client.textRenderer.wrapLines(text, maxWidth).size();
        int width = textWidth + padding * 2;
        int height = padding * 2 + lines * 12;
        
        // Apply animation scale
        int renderX = x;
        int renderY = y;
        int renderWidth = (int) (width * animProgress);
        int renderHeight = (int) (height * animProgress);
        
        // Background
        GlassRenderer.fillRoundedRect(context, renderX, renderY, renderWidth, renderHeight, radius, theme.bgDeep(0.95f));
        
        // Border
        GlassRenderer.drawRoundedRectOutline(context, renderX, renderY, renderWidth, renderHeight, radius, 1, theme.borderAccentSoft());
        
        // Inner highlight
        if (theme.glowIntensity() > 0.1f) {
            int highlight = theme.innerGlow();
            GlassRenderer.fillRoundedRect(context, renderX + 1, renderY + 1, renderWidth - 2, 2, radius - 1, highlight);
            GlassRenderer.fillRoundedRect(context, renderX + 1, renderY + 1, 2, renderHeight - 2, radius - 1, highlight);
        }
        
        // Text
        if (animProgress > 0.3f) {
            context.getMatrices().push();
            context.getMatrices().translate(renderX + padding, renderY + padding, 0);
            
            // Draw wrapped text
            var wrappedLines = theme.client.textRenderer.wrapLines(text, maxWidth);
            for (int i = 0; i < wrappedLines.size(); i++) {
                String line = wrappedLines.get(i).getString();
                context.drawTextWithShadow(theme.client.textRenderer, line, 0, i * 12, theme.textPrimary());
            }
            
            context.getMatrices().pop();
        }
        
        RenderSystem.disableBlend();
    }
    
    public boolean isVisible() { return visible; }
    public String getText() { return text; }
    public int getX() { return x; }
    public int getY() { return y; }
    
    public static GlassTooltip of(String text) {
        return new GlassTooltip(text);
    }
    
    public static GlassTooltip of(String text, int maxWidth) {
        return new GlassTooltip(text, maxWidth);
    }
}