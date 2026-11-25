package com.alpoyy.pulse.gui.component;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvents;

public class GlassIconButton {
    public enum Style {
        DEFAULT,
        PRIMARY,
        DANGER,
        GHOST
    }
    
    private final int x, y, size;
    private final Runnable iconDrawer;
    private final Runnable onClick;
    private final Style style;
    private final boolean disabled;
    private final boolean toggle;
    private final boolean toggled;
    private final String tooltip;
    
    private boolean hovered = false;
    private boolean pressed = false;
    private float pressAnim = 0f;
    private long pressTime = 0;
    
    public static class Builder {
        private int x, y, size = 36;
        private Runnable iconDrawer = null;
        private Runnable onClick = () -> {};
        private Style style = Style.DEFAULT;
        private boolean disabled = false;
        private boolean toggle = false;
        private boolean toggled = false;
        private String tooltip = null;
        
        public Builder(int x, int y) { this.x = x; this.y = y; }
        public Builder size(int size) { this.size = size; return this; }
        public Builder icon(Runnable drawer) { this.iconDrawer = drawer; return this; }
        public Builder onClick(Runnable onClick) { this.onClick = onClick; return this; }
        public Builder style(Style style) { this.style = style; return this; }
        public Builder disabled(boolean disabled) { this.disabled = disabled; return this; }
        public Builder toggle(boolean toggle) { this.toggle = toggle; return this; }
        public Builder toggled(boolean toggled) { this.toggled = toggled; return this; }
        public Builder tooltip(String tooltip) { this.tooltip = tooltip; return this; }
        
        public GlassIconButton build() {
            return new GlassIconButton(this);
        }
    }
    
    private GlassIconButton(Builder builder) {
        this.x = builder.x;
        this.y = builder.y;
        this.size = builder.size;
        this.iconDrawer = builder.iconDrawer;
        this.onClick = builder.onClick;
        this.style = builder.style;
        this.disabled = builder.disabled;
        this.toggle = builder.toggle;
        this.toggled = builder.toggled;
        this.tooltip = builder.tooltip;
    }
    
    public void render(DrawContext context, PulseTheme theme, int mouseX, int mouseY) {
        if (disabled) {
            renderDisabled(context, theme);
            return;
        }
        
        boolean wasHovered = hovered;
        hovered = isMouseOver(mouseX, mouseY);
        
        // Animations
        long now = System.currentTimeMillis();
        if (pressed) {
            pressAnim = Math.min(1f, pressAnim + (now - pressTime) / 50f);
            if (pressAnim >= 1f) pressed = false;
        } else {
            pressAnim = Math.max(0f, pressAnim - (now - pressTime) / 100f);
        }
        pressTime = now;
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        int accentColor = theme.getAccentColor();
        int radius = theme.radiusSmall();
        
        // Background color based on style and state
        int bgColor, borderColor, iconColor;
        
        boolean active = toggle && toggled;
        
        if (active) {
            bgColor = theme.accent(0.9f);
            borderColor = theme.accent(1f);
            iconColor = theme.textOnAccent();
        } else if (pressed) {
            bgColor = theme.bgMedium(theme.panelOpacity() + 0.1f);
            borderColor = theme.borderAccent();
            iconColor = theme.textPrimary();
        } else if (hovered) {
            switch (style) {
                case PRIMARY -> { bgColor = theme.accent(0.2f); borderColor = theme.accent(0.5f); }
                case DANGER -> { bgColor = (80 << 24) | 0xFF4444; borderColor = (120 << 24) | 0xFF4444; }
                case GHOST -> { bgColor = theme.bgMedium(theme.panelOpacity() * 0.5f); borderColor = theme.borderSubtle(); }
                default -> { bgColor = theme.bgMedium(theme.panelOpacity()); borderColor = theme.borderAccentSoft(); }
            }
            iconColor = theme.textPrimary();
        } else {
            switch (style) {
                case PRIMARY -> { bgColor = theme.accent(0.1f); borderColor = theme.accent(0.3f); }
                case DANGER -> { bgColor = 0; borderColor = 0; }
                case GHOST -> { bgColor = 0; borderColor = 0; }
                default -> { bgColor = theme.bgDark(theme.panelOpacity()); borderColor = theme.borderSubtle(); }
            }
            iconColor = theme.textMuted();
        }
        
        // Press scale
        float scale = 1f - pressAnim * 0.05f;
        int renderX = (int) (x + size * (1 - scale) / 2);
        int renderY = (int) (y + size * (1 - scale) / 2);
        int renderSize = (int) (size * scale);
        
        // Background
        if (bgColor != 0) {
            GlassRenderer.fillRoundedRect(context, renderX, renderY, renderSize, renderSize, radius, bgColor);
        }
        
        // Border
        if (borderColor != 0) {
            GlassRenderer.drawRoundedRectOutline(context, renderX, renderY, renderSize, renderSize, radius, 1, borderColor);
        }
        
        // Inner highlight
        if (theme.glowIntensity() > 0.1f && (hovered || active) && bgColor != 0) {
            int highlight = theme.innerGlow();
            GlassRenderer.fillRoundedRect(context, renderX + 1, renderY + 1, renderSize - 2, 2, radius - 1, highlight);
            GlassRenderer.fillRoundedRect(context, renderX + 1, renderY + 1, 2, renderSize - 2, radius - 1, highlight);
        }
        
        // Icon
        if (iconDrawer != null) {
            int iconSize = (int) (renderSize * 0.55f);
            int iconX = renderX + (renderSize - iconSize) / 2;
            int iconY = renderY + (renderSize - iconSize) / 2;
            
            context.getMatrices().push();
            context.getMatrices().translate(iconX, iconY, 0);
            
            // Temporarily override the icon color by using a custom color multiplier
            // Since we can't easily change the color, we'll render with the iconColor
            // The icon drawer should use a method that accepts color
            
            // For now, just call the drawer - it should handle color internally
            iconDrawer.run();
            
            context.getMatrices().pop();
        }
        
        RenderSystem.disableBlend();
    }
    
    private void renderDisabled(DrawContext context, PulseTheme theme) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        int radius = theme.radiusSmall();
        int bgColor = theme.bgMedium(theme.panelOpacity() * 0.3f);
        int borderColor = theme.borderSubtle() & 0x33FFFFFF;
        int iconColor = theme.textDisabled();
        
        GlassRenderer.fillRoundedRect(context, x, y, size, size, radius, bgColor);
        GlassRenderer.drawRoundedRectOutline(context, x, y, size, size, radius, 1, borderColor);
        
        if (iconDrawer != null) {
            int iconSize = size / 2;
            int iconX = x + (size - iconSize) / 2;
            int iconY = y + (size - iconSize) / 2;
            
            context.getMatrices().push();
            context.getMatrices().translate(iconX, iconY, 0);
            iconDrawer.run();
            context.getMatrices().pop();
        }
        
        RenderSystem.disableBlend();
    }
    
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (disabled || button != 0) return false;
        if (isMouseOver(mouseX, mouseY)) {
            pressed = true;
            pressAnim = 0f;
            pressTime = System.currentTimeMillis();
            playClickSound();
            onClick.run();
            return true;
        }
        return false;
    }
    
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (pressed) {
            pressed = false;
            return true;
        }
        return false;
    }
    
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + size && mouseY >= y && mouseY <= y + size;
    }
    
    private void playClickSound() {
        MinecraftClient.getInstance().getSoundManager().play(
            PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0f)
        );
    }
    
    public String getTooltip() { return tooltip; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getSize() { return size; }
    public boolean isHovered() { return hovered; }
    public boolean isDisabled() { return disabled; }
    public boolean isToggled() { return toggled; }
    public void setToggled(boolean toggled) { /* immutable */ }
    
    public static Builder builder(int x, int y) {
        return new Builder(x, y);
    }
}