package com.alpoyy.pulse.gui.component;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

public class GlassButton {
    public enum Style {
        PRIMARY,      // Accent background
        SECONDARY,    // Glass panel
        GHOST,        // Transparent with border
        DANGER,       // Red accent
        SUCCESS,      // Green accent
        ICON_ONLY     // Just icon
    }
    
    public enum Size {
        SMALL(28),    // 28px height
        NORMAL(36),   // 36px height
        LARGE(44);    // 44px height
        
        public final int height;
        Size(int height) { this.height = height; }
    }
    
    private final int x, y, width, height;
    private final String text;
    private final Text textComponent;
    private final Runnable onClick;
    private final Style style;
    private final Size size;
    private final int iconSize;
    private final Runnable iconDrawer;
    private final boolean disabled;
    private final String tooltip;
    
    private boolean hovered = false;
    private boolean pressed = false;
    private long pressTime = 0;
    private float pressAnimation = 0f;
    
    // Builder pattern
    public static class Builder {
        private int x, y, width = 120;
        private String text = "";
        private Text textComponent = null;
        private Runnable onClick = () -> {};
        private Style style = Style.SECONDARY;
        private Size size = Size.NORMAL;
        private int iconSize = 16;
        private Runnable iconDrawer = null;
        private boolean disabled = false;
        private String tooltip = null;
        
        public Builder(int x, int y) {
            this.x = x;
            this.y = y;
        }
        
        public Builder text(String text) { this.text = text; return this; }
        public Builder text(Text text) { this.textComponent = text; return this; }
        public Builder onClick(Runnable onClick) { this.onClick = onClick; return this; }
        public Builder style(Style style) { this.style = style; return this; }
        public Builder size(Size size) { this.size = size; return this; }
        public Builder width(int width) { this.width = width; return this; }
        public Builder icon(int iconSize, Runnable drawer) { 
            this.iconSize = iconSize; 
            this.iconDrawer = drawer; 
            return this; 
        }
        public Builder disabled(boolean disabled) { this.disabled = disabled; return this; }
        public Builder tooltip(String tooltip) { this.tooltip = tooltip; return this; }
        
        public GlassButton build() {
            return new GlassButton(this);
        }
    }
    
    private GlassButton(Builder builder) {
        this.x = builder.x;
        this.y = builder.y;
        this.width = builder.width;
        this.height = builder.size.height;
        this.text = builder.text;
        this.textComponent = builder.textComponent;
        this.onClick = builder.onClick;
        this.style = builder.style;
        this.size = builder.size;
        this.iconSize = builder.iconSize;
        this.iconDrawer = builder.iconDrawer;
        this.disabled = builder.disabled;
        this.tooltip = builder.tooltip;
    }
    
    public void render(DrawContext context, PulseTheme theme, int mouseX, int mouseY) {
        if (disabled) {
            renderDisabled(context, theme);
            return;
        }
        
        boolean wasHovered = hovered;
        hovered = isMouseOver(mouseX, mouseY);
        
        // Press animation
        long now = System.currentTimeMillis();
        if (pressed) {
            pressAnimation = Math.min(1f, pressAnimation + (now - pressTime) / 50f);
            if (pressAnimation >= 1f) pressed = false;
        } else {
            pressAnimation = Math.max(0f, pressAnimation - (now - pressTime) / 100f);
        }
        pressTime = now;
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        int accentColor = theme.getAccentColor();
        int secondaryColor = theme.getSecondaryAccentColor();
        int radius = theme.radiusMedium();
        
        // Calculate colors based on style and state
        int bgColor, borderColor, textColor;
        float bgOpacity = theme.panelOpacity();
        
        switch (style) {
            case PRIMARY -> {
                if (hovered && !pressed) {
                    bgColor = theme.accent(0.95f);
                    borderColor = theme.accent(1f);
                } else if (pressed) {
                    bgColor = theme.accent(0.8f);
                    borderColor = theme.accent(0.9f);
                } else {
                    bgColor = theme.accent(0.85f);
                    borderColor = theme.accent(0.7f);
                }
                textColor = theme.textOnAccent();
            }
            case SECONDARY -> {
                if (hovered && !pressed) {
                    bgColor = theme.bgHover(bgOpacity + 0.1f);
                    borderColor = theme.borderAccentSoft();
                } else if (pressed) {
                    bgColor = theme.bgMedium(bgOpacity);
                    borderColor = theme.borderAccent();
                } else {
                    bgColor = theme.bgDark(bgOpacity);
                    borderColor = theme.borderSubtle();
                }
                textColor = hovered ? theme.textPrimary() : theme.textSecondary();
            }
            case GHOST -> {
                if (hovered && !pressed) {
                    bgColor = theme.accent(0.15f);
                    borderColor = theme.accent(0.6f);
                } else if (pressed) {
                    bgColor = theme.accent(0.25f);
                    borderColor = theme.accent(0.8f);
                } else {
                    bgColor = 0; // Transparent
                    borderColor = theme.borderSubtle();
                }
                textColor = hovered ? theme.textPrimary() : theme.textMuted();
            }
            case DANGER -> {
                if (hovered && !pressed) {
                    bgColor = (200 << 24) | 0xFF4444;
                    borderColor = (230 << 24) | 0xFF4444;
                } else if (pressed) {
                    bgColor = (180 << 24) | 0xCC3333;
                    borderColor = (200 << 24) | 0xCC3333;
                } else {
                    bgColor = (160 << 24) | 0xCC3333;
                    borderColor = (180 << 24) | 0xCC3333;
                }
                textColor = theme.textPrimary();
            }
            case SUCCESS -> {
                if (hovered && !pressed) {
                    bgColor = (200 << 24) | 0x00D4AA;
                    borderColor = (230 << 24) | 0x00D4AA;
                } else if (pressed) {
                    bgColor = (180 << 24) | 0x00AA88;
                    borderColor = (200 << 24) | 0x00AA88;
                } else {
                    bgColor = (160 << 24) | 0x00AA88;
                    borderColor = (180 << 24) | 0x00AA88;
                }
                textColor = theme.textOnAccent();
            }
            default -> { // ICON_ONLY
                if (hovered && !pressed) {
                    bgColor = theme.accent(0.15f);
                    borderColor = theme.accent(0.4f);
                } else if (pressed) {
                    bgColor = theme.accent(0.25f);
                    borderColor = theme.accent(0.6f);
                } else {
                    bgColor = 0;
                    borderColor = 0;
                }
                textColor = theme.textPrimary();
            }
        }
        
        // Press scale effect
        float scale = 1f - pressAnimation * 0.02f;
        int renderX = (int) (x + width * (1 - scale) / 2);
        int renderY = (int) (y + height * (1 - scale) / 2);
        int renderW = (int) (width * scale);
        int renderH = (int) (height * scale);
        
        // Background
        if (bgColor != 0) {
            GlassRenderer.fillRoundedRect(context, renderX, renderY, renderW, renderH, radius, bgColor);
        }
        
        // Border
        if (borderColor != 0) {
            GlassRenderer.drawRoundedRectOutline(context, renderX, renderY, renderW, renderH, radius, 1, borderColor);
        }
        
        // Inner highlight for glass effect
        if (theme.glowIntensity() > 0.1f && style != Style.GHOST && !pressed) {
            int highlightColor = theme.innerGlow();
            GlassRenderer.fillRoundedRect(context, renderX + 1, renderY + 1, renderW - 2, 2, radius - 1, highlightColor);
            GlassRenderer.fillRoundedRect(context, renderX + 1, renderY + 1, 2, renderH - 2, radius - 1, highlightColor);
        }
        
        // Icon
        if (iconDrawer != null) {
            int iconX = renderX + (renderW - iconSize) / 2;
            int iconY = renderY + (renderH - iconSize) / 2;
            
            if (!text.isEmpty() || textComponent != null) {
                iconX = renderX + theme.spacingM();
            }
            
            // Save and restore translation for icon
            context.getMatrices().push();
            context.getMatrices().translate(iconX, iconY, 0);
            iconDrawer.run();
            context.getMatrices().pop();
        }
        
        // Text
        String displayText = textComponent != null ? textComponent.getString() : text;
        if (!displayText.isEmpty()) {
            int textX = renderX + (iconDrawer != null ? theme.spacingM() * 2 + iconSize : 0);
            int textY = renderY + (renderH - 8) / 2;
            int maxWidth = renderW - (textX - renderX) - theme.spacingM();
            
            if (theme.client.textRenderer.getWidth(displayText) > maxWidth) {
                displayText = theme.client.textRenderer.trimToWidth(displayText, maxWidth) + "...";
            }
            
            context.drawTextWithShadow(theme.client.textRenderer, displayText, textX, textY, textColor);
        }
        
        RenderSystem.disableBlend();
    }
    
    private void renderDisabled(DrawContext context, PulseTheme theme) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        int radius = theme.radiusMedium();
        int bgColor = theme.bgMedium(theme.panelOpacity() * 0.5f);
        int borderColor = theme.borderSubtle() & 0x44FFFFFF;
        int textColor = theme.textDisabled();
        
        GlassRenderer.fillRoundedRect(context, x, y, width, height, radius, bgColor);
        GlassRenderer.drawRoundedRectOutline(context, x, y, width, height, radius, 1, borderColor);
        
        String displayText = textComponent != null ? textComponent.getString() : text;
        if (!displayText.isEmpty()) {
            int textX = x + (width - theme.client.textRenderer.getWidth(displayText)) / 2;
            int textY = y + (height - 8) / 2;
            context.drawTextWithShadow(theme.client.textRenderer, displayText, textX, textY, textColor);
        }
        
        RenderSystem.disableBlend();
    }
    
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (disabled || button != 0) return false;
        if (isMouseOver(mouseX, mouseY)) {
            pressed = true;
            pressAnimation = 0f;
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
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
    
    public void playClickSound() {
        MinecraftClient.getInstance().getSoundManager().play(
            PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0f)
        );
    }
    
    public String getTooltip() { return tooltip; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public void setPosition(int x, int y) { /* immutable, recreate */ }
    public boolean isHovered() { return hovered; }
    public boolean isDisabled() { return disabled; }
    
    // Static factory methods
    public static Builder builder(int x, int y) {
        return new Builder(x, y);
    }
    
    public static GlassButton primary(int x, int y, String text, Runnable onClick) {
        return new Builder(x, y).text(text).onClick(onClick).style(Style.PRIMARY).build();
    }
    
    public static GlassButton secondary(int x, int y, String text, Runnable onClick) {
        return new Builder(x, y).text(text).onClick(onClick).style(Style.SECONDARY).build();
    }
    
    public static GlassButton ghost(int x, int y, String text, Runnable onClick) {
        return new Builder(x, y).text(text).onClick(onClick).style(Style.GHOST).build();
    }
    
    public static GlassButton icon(int x, int y, int size, Runnable iconDrawer, Runnable onClick) {
        return new Builder(x, y).width(size).size(Size.NORMAL).icon(size, iconDrawer).onClick(onClick).style(Style.ICON_ONLY).build();
    }
}