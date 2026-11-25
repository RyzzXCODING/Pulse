package com.alpoyy.pulse.widget;

import com.alpoyy.pulse.config.PulseConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

public abstract class Widget {
    protected final String id;
    protected final String name;
    protected int x;
    protected int y;
    protected int width;
    protected int height;
    protected boolean visible = true;
    protected boolean enabled = true;
    protected boolean dragging = false;
    protected int dragOffsetX;
    protected int dragOffsetY;
    
    public Widget(String id, String name, int x, int y, int width, int height) {
        this.id = id;
        this.name = name;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }
    
    public abstract void render(DrawContext context, RenderTickCounter tickCounter, float widgetScale);
    
    public abstract void renderEditor(DrawContext context, RenderTickCounter tickCounter, float widgetScale);
    
    public void onClick(double mouseX, double mouseY, int button) {
        // Override in subclasses
    }
    
    public void onDrag(double mouseX, double mouseY) {
        if (dragging) {
            this.x = (int) (mouseX - dragOffsetX);
            this.y = (int) (mouseY - dragOffsetY);
        }
    }
    
    public void startDrag(double mouseX, double mouseY) {
        this.dragging = true;
        this.dragOffsetX = (int) (mouseX - x);
        this.dragOffsetY = (int) (mouseY - y);
    }
    
    public void stopDrag() {
        this.dragging = false;
    }
    
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
    
    public void applyConfig(PulseConfig config) {
        // Override in subclasses to apply config
    }
    
    public void savePosition() {
        // Called when position is saved
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public int getX() {
        return x;
    }
    
    public void setX(int x) {
        this.x = x;
    }
    
    public int getY() {
        return y;
    }
    
    public void setY(int y) {
        this.y = y;
    }
    
    public int getWidth() {
        return width;
    }
    
    public void setWidth(int width) {
        this.width = width;
    }
    
    public int getHeight() {
        return height;
    }
    
    public void setHeight(int height) {
        this.height = height;
    }
    
    public boolean isVisible() {
        return visible;
    }
    
    public void setVisible(boolean visible) {
        this.visible = visible;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public boolean isDragging() {
        return dragging;
    }
    
    protected void renderBackground(DrawContext context, int x, int y, int width, int height, 
                                    float backgroundOpacity, float glowIntensity, int accentColor) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        // Main background panel
        int bgAlpha = (int) (backgroundOpacity * 255);
        int bgColor = (bgAlpha << 24) | 0x1A1A2E; // Dark navy base
        context.fill(x, y, x + width, y + height, bgColor);
        
        // Subtle border
        int borderAlpha = (int) (glowIntensity * 100);
        int borderColor = (borderAlpha << 24) | (accentColor & 0xFFFFFF);
        context.fill(x, y, x + width, y + 1, borderColor); // Top
        context.fill(x, y + height - 1, x + width, y + height, borderColor); // Bottom
        context.fill(x, y, x + 1, y + height, borderColor); // Left
        context.fill(x + width - 1, y, x + width, y + height, borderColor); // Right
        
        // Subtle glow effect (inner shadow)
        if (glowIntensity > 0.1f) {
            int glowAlpha = (int) (glowIntensity * 50);
            int glowColor = (glowAlpha << 24) | (accentColor & 0xFFFFFF);
            context.fill(x + 1, y + 1, x + width - 1, y + 2, glowColor);
            context.fill(x + 1, y + 1, x + 2, y + height - 1, glowColor);
        }
        
        RenderSystem.disableBlend();
    }
    
    protected void renderRoundedBackground(DrawContext context, int x, int y, int width, int height, 
                                           int radius, float backgroundOpacity, float glowIntensity, int accentColor) {
        // Simple rounded rect approximation using multiple fills
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        int bgAlpha = (int) (backgroundOpacity * 255);
        int bgColor = (bgAlpha << 24) | 0x1A1A2E;
        
        // Main body
        context.fill(x + radius, y, x + width - radius, y + height, bgColor);
        context.fill(x, y + radius, x + width, y + height - radius, bgColor);
        
        // Corners (simple approximation)
        context.fill(x + radius, y + radius, x + width - radius, y + height - radius, bgColor);
        
        // Border
        int borderAlpha = (int) (glowIntensity * 100);
        int borderColor = (borderAlpha << 24) | (accentColor & 0xFFFFFF);
        
        context.fill(x + radius, y, x + width - radius, y + 1, borderColor);
        context.fill(x + radius, y + height - 1, x + width - radius, y + height, borderColor);
        context.fill(x, y + radius, x + 1, y + height - radius, borderColor);
        context.fill(x + width - 1, y + radius, x + width, y + height - radius, borderColor);
        
        RenderSystem.disableBlend();
    }
}