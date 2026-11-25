package com.alpoyy.pulse.gui.component;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.MinecraftClient;

public class GlassCard {
    public enum Variant {
        DEFAULT,
        ELEVATED,
        OUTLINED,
        FILLED
    }
    
    private final int x, y, width, height;
    private final Variant variant;
    private final int radius;
    private final Runnable contentRenderer;
    private final boolean hoverable;
    private final Runnable onClick;
    private final String tooltip;
    
    private boolean hovered = false;
    private float hoverAnim = 0f;
    private long lastAnimTime = 0;
    
    public static class Builder {
        private int x, y, width = 200, height = 100;
        private Variant variant = Variant.DEFAULT;
        private int radius = 12;
        private Runnable contentRenderer = () -> {};
        private boolean hoverable = false;
        private Runnable onClick = null;
        private String tooltip = null;
        
        public Builder(int x, int y) { this.x = x; this.y = y; }
        public Builder width(int width) { this.width = width; return this; }
        public Builder height(int height) { this.height = height; return this; }
        public Builder variant(Variant v) { this.variant = v; return this; }
        public Builder radius(int radius) { this.radius = radius; return this; }
        public Builder content(Runnable renderer) { this.contentRenderer = renderer; return this; }
        public Builder hoverable(boolean h) { this.hoverable = h; return this; }
        public Builder onClick(Runnable click) { this.onClick = click; this.hoverable = true; return this; }
        public Builder tooltip(String tooltip) { this.tooltip = tooltip; return this; }
        
        public GlassCard build() {
            return new GlassCard(this);
        }
    }
    
    private GlassCard(Builder builder) {
        this.x = builder.x;
        this.y = builder.y;
        this.width = builder.width;
        this.height = builder.height;
        this.variant = builder.variant;
        this.radius = builder.radius;
        this.contentRenderer = builder.contentRenderer;
        this.hoverable = builder.hoverable;
        this.onClick = builder.onClick;
        this.tooltip = builder.tooltip;
    }
    
    public void render(DrawContext context, PulseTheme theme, int mouseX, int mouseY) {
        boolean wasHovered = hovered;
        hovered = hoverable && isMouseOver(mouseX, mouseY);
        
        // Hover animation
        long now = System.currentTimeMillis();
        float target = hovered ? 1f : 0f;
        float delta = (now - lastAnimTime) / 200f;
        if (hoverAnim < target) hoverAnim = Math.min(target, hoverAnim + delta);
        else if (hoverAnim > target) hoverAnim = Math.max(target, hoverAnim - delta);
        lastAnimTime = now;
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        int accentColor = theme.getAccentColor();
        
        switch (variant) {
            case DEFAULT -> renderDefault(context, theme);
            case ELEVATED -> renderElevated(context, theme);
            case OUTLINED -> renderOutlined(context, theme);
            case FILLED -> renderFilled(context, theme);
        }
        
        // Render content
        context.getMatrices().push();
        context.getMatrices().translate(x, y, 0);
        if (contentRenderer != null) {
            contentRenderer.run();
        }
        context.getMatrices().pop();
        
        RenderSystem.disableBlend();
    }
    
    private void renderDefault(DrawContext context, PulseTheme theme) {
        GlassRenderer.renderGlassPanel(context, 0, 0, width, height, radius, theme);
    }
    
    private void renderElevated(DrawContext context, PulseTheme theme) {
        GlassRenderer.renderGlassPanelElevated(context, 0, 0, width, height, radius, theme);
    }
    
    private void renderOutlined(DrawContext context, PulseTheme theme) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        // Background
        GlassRenderer.fillRoundedRect(context, 0, 0, width, height, radius, theme.bgDark(theme.panelOpacity()));
        
        // Border with hover animation
        int borderColor;
        if (hovered) {
            borderColor = theme.borderAccent();
        } else {
            borderColor = theme.borderSubtle();
        }
        GlassRenderer.drawRoundedRectOutline(context, 0, 0, width, height, radius, 1, borderColor);
        
        // Inner highlight on hover
        if (hovered && theme.glowIntensity() > 0.1f) {
            int highlight = theme.innerGlow();
            GlassRenderer.fillRoundedRect(context, 1, 1, width - 2, 2, radius - 1, highlight);
            GlassRenderer.fillRoundedRect(context, 1, 1, 2, height - 2, radius - 1, highlight);
        }
        
        RenderSystem.disableBlend();
    }
    
    private void renderFilled(DrawContext context, PulseTheme theme) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        int bgColor = hovered ? theme.accent(0.15f) : theme.bgMedium(theme.panelOpacity());
        int borderColor = hovered ? theme.borderAccentSoft() : theme.borderSubtle();
        
        GlassRenderer.fillRoundedRect(context, 0, 0, width, height, radius, bgColor);
        GlassRenderer.drawRoundedRectOutline(context, 0, 0, width, height, radius, 1, borderColor);
        
        RenderSystem.disableBlend();
    }
    
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (onClick != null && button == 0 && isMouseOver(mouseX, mouseY)) {
            onClick.run();
            return true;
        }
        return false;
    }
    
    private boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
    
    public boolean isHovered() { return hovered; }
    public String getTooltip() { return tooltip; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    
    public static Builder builder(int x, int y) {
        return new Builder(x, y);
    }
}