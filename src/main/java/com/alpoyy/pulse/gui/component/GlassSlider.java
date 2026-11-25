package com.alpoyy.pulse.gui.component;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.MinecraftClient;

public class GlassSlider {
    private final int x, y, width, height;
    private float value; // 0.0 to 1.0
    private final float min, max;
    private final Runnable onChange;
    private final boolean showValue;
    private final String label;
    private final String suffix;
    private final boolean discrete;
    private final int steps;
    
    private boolean dragging = false;
    private boolean hovered = false;
    private boolean trackHovered = false;
    
    public static class Builder {
        private int x, y, width = 200, height = 24;
        private float value = 0.5f;
        private float min = 0f, max = 1f;
        private Runnable onChange = v -> {};
        private boolean showValue = true;
        private String label = "";
        private String suffix = "";
        private boolean discrete = false;
        private int steps = 100;
        
        public Builder(int x, int y) { this.x = x; this.y = y; }
        public Builder width(int width) { this.width = width; return this; }
        public Builder height(int height) { this.height = height; return this; }
        public Builder value(float value) { this.value = value; return this; }
        public Builder range(float min, float max) { this.min = min; this.max = max; return this; }
        public Builder onChange(Runnable onChange) { this.onChange = onChange; return this; }
        public Builder showValue(boolean show) { this.showValue = show; return this; }
        public Builder label(String label) { this.label = label; return this; }
        public Builder suffix(String suffix) { this.suffix = suffix; return this; }
        public Builder discrete(int steps) { this.discrete = true; this.steps = steps; return this; }
        
        public GlassSlider build() {
            return new GlassSlider(this);
        }
    }
    
    private GlassSlider(Builder builder) {
        this.x = builder.x;
        this.y = builder.y;
        this.width = builder.width;
        this.height = builder.height;
        this.value = clamp(builder.value);
        this.min = builder.min;
        this.max = builder.max;
        this.onChange = builder.onChange;
        this.showValue = builder.showValue;
        this.label = builder.label;
        this.suffix = builder.suffix;
        this.discrete = builder.discrete;
        this.steps = builder.steps;
    }
    
    private float clamp(float v) {
        return Math.max(0f, Math.min(1f, v));
    }
    
    public void render(DrawContext context, PulseTheme theme, int mouseX, int mouseY) {
        hovered = isMouseOver(mouseX, mouseY);
        trackHovered = isTrackHovered(mouseX, mouseY);
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        int radius = height / 2;
        int trackY = y + height / 2 - 2;
        int trackHeight = 4;
        int accentColor = theme.getAccentColor();
        
        // Label
        if (!label.isEmpty()) {
            context.drawTextWithShadow(theme.client.textRenderer, label, x, y - 14, theme.textSecondary());
        }
        
        // Track background
        GlassRenderer.fillRoundedRect(context, x, trackY, width, trackHeight, 2, theme.bgDark(theme.panelOpacity()));
        
        // Track border
        GlassRenderer.drawRoundedRectOutline(context, x, trackY, width, trackHeight, 2, 1, theme.borderSubtle());
        
        // Fill
        int fillWidth = (int) (width * value);
        if (fillWidth > 0) {
            int fillColor = (hovered || trackHovered || dragging) ? theme.accent(1f) : theme.accent(0.9f);
            GlassRenderer.fillRoundedRect(context, x, trackY, fillWidth, trackHeight, 2, fillColor);
        }
        
        // Handle
        int handleSize = height;
        int handleX = x + fillWidth - handleSize / 2;
        int handleY = y + height / 2 - handleSize / 2;
        
        // Handle shadow
        if (theme.glowIntensity() > 0) {
            int shadowColor = (int) (theme.glowIntensity() * 60) << 24 | 0x000000;
            GlassRenderer.fillRoundedRect(context, handleX + 1, handleY + 1, handleSize, handleSize, handleSize / 2, shadowColor);
        }
        
        // Handle background
        int handleColor = (hovered || dragging) ? 0xFFFFFFFF : 0xFFEEEEEE;
        GlassRenderer.fillRoundedRect(context, handleX, handleY, handleSize, handleSize, handleSize / 2, handleColor);
        
        // Handle border
        int handleBorderColor = dragging ? theme.accent(1f) : (hovered ? theme.accent(0.6f) : theme.borderSubtle());
        GlassRenderer.drawRoundedRectOutline(context, handleX, handleY, handleSize, handleSize, handleSize / 2, 1, handleBorderColor);
        
        // Handle inner accent dot
        if (theme.glowIntensity() > 0.1f) {
            int dotSize = handleSize / 3;
            int dotX = handleX + (handleSize - dotSize) / 2;
            int dotY = handleY + (handleSize - dotSize) / 2;
            GlassRenderer.fillRoundedRect(context, dotX, dotY, dotSize, dotSize, dotSize / 2, theme.accent(0.3f));
        }
        
        // Value display
        if (showValue) {
            float actualValue = min + value * (max - min);
            String valueText;
            if (discrete) {
                valueText = String.valueOf((int) actualValue);
            } else if (max <= 1f && min >= 0f) {
                valueText = String.format("%.0f%%", actualValue * 100);
            } else {
                valueText = String.format("%.1f", actualValue);
            }
            
            if (!suffix.isEmpty()) {
                valueText += suffix;
            }
            
            int textWidth = theme.client.textRenderer.getWidth(valueText);
            int textX = x + width + theme.spacingS();
            int textY = y + (height - 8) / 2;
            context.drawTextWithShadow(theme.client.textRenderer, valueText, textX, textY, theme.textMuted());
        }
        
        RenderSystem.disableBlend();
    }
    
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        
        if (isHandleHovered(mouseX, mouseY) || isTrackHovered(mouseX, mouseY)) {
            dragging = true;
            updateValueFromMouse(mouseX);
            return true;
        }
        return false;
    }
    
    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (dragging && button == 0) {
            updateValueFromMouse(mouseX);
            return true;
        }
        return false;
    }
    
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging) {
            dragging = false;
            onChange.run();
            return true;
        }
        return false;
    }
    
    private boolean isHandleHovered(double mouseX, double mouseY) {
        int handleSize = height;
        int handleX = x + (int) (width * value) - handleSize / 2;
        int handleY = y + height / 2 - handleSize / 2;
        return mouseX >= handleX && mouseX <= handleX + handleSize && 
               mouseY >= handleY && mouseY <= handleY + handleSize;
    }
    
    private boolean isTrackHovered(double mouseX, double mouseY) {
        int trackY = y + height / 2 - 2;
        int trackHeight = 4;
        return mouseX >= x && mouseX <= x + width && 
               mouseY >= trackY && mouseY <= trackY + trackHeight;
    }
    
    private boolean isMouseOver(double mouseX, double mouseY) {
        return isHandleHovered(mouseX, mouseY) || isTrackHovered(mouseX, mouseY);
    }
    
    private void updateValueFromMouse(double mouseX) {
        float newValue = (float) ((mouseX - x) / (double) width);
        newValue = clamp(newValue);
        
        if (discrete) {
            newValue = Math.round(newValue * steps) / (float) steps;
        }
        
        if (Math.abs(newValue - value) > 0.001f) {
            value = newValue;
        }
    }
    
    public float getValue() { return min + value * (max - min); }
    public float getNormalizedValue() { return value; }
    public void setValue(float v) { this.value = clamp((v - min) / (max - min)); }
    public void setNormalizedValue(float v) { this.value = clamp(v); }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public boolean isHovered() { return hovered; }
    public boolean isDragging() { return dragging; }
    
    public static Builder builder(int x, int y) {
        return new Builder(x, y);
    }
}