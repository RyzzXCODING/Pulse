package com.alpoyy.pulse.gui.component;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;

public class GlassToggle {
    private final int x, y, width, height;
    private boolean value;
    private final Runnable onChange;
    private final String label;
    private final String labelOn;
    private final String labelOff;
    private final boolean showLabels;
    
    private boolean hovered = false;
    private float thumbAnim = 0f;
    private long lastAnimTime = 0;
    
    public static class Builder {
        private int x, y, width = 56, height = 28;
        private boolean value = false;
        private Runnable onChange = v -> {};
        private String label = "";
        private String labelOn = "ON";
        private String labelOff = "OFF";
        private boolean showLabels = false;
        
        public Builder(int x, int y) { this.x = x; this.y = y; }
        public Builder width(int width) { this.width = width; return this; }
        public Builder height(int height) { this.height = height; return this; }
        public Builder value(boolean value) { this.value = value; return this; }
        public Builder onChange(Runnable onChange) { this.onChange = onChange; return this; }
        public Builder label(String label) { this.label = label; return this; }
        public Builder labels(String on, String off) { this.labelOn = on; this.labelOff = off; return this; }
        public Builder showLabels(boolean show) { this.showLabels = show; return this; }
        
        public GlassToggle build() {
            return new GlassToggle(this);
        }
    }
    
    private GlassToggle(Builder builder) {
        this.x = builder.x;
        this.y = builder.y;
        this.width = builder.width;
        this.height = builder.height;
        this.value = builder.value;
        this.onChange = builder.onChange;
        this.label = builder.label;
        this.labelOn = builder.labelOn;
        this.labelOff = builder.labelOff;
        this.showLabels = builder.showLabels;
        this.thumbAnim = value ? 1f : 0f;
    }
    
    public void render(DrawContext context, PulseTheme theme, int mouseX, int mouseY) {
        hovered = isMouseOver(mouseX, mouseY);
        
        // Animate thumb
        long now = System.currentTimeMillis();
        float target = value ? 1f : 0f;
        float delta = (now - lastAnimTime) / 150f;
        if (thumbAnim < target) {
            thumbAnim = Math.min(target, thumbAnim + delta);
        } else if (thumbAnim > target) {
            thumbAnim = Math.max(target, thumbAnim - delta);
        }
        lastAnimTime = now;
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        int radius = height / 2;
        int accentColor = theme.getAccentColor();
        int successColor = theme.statusSuccess();
        
        // Label
        if (!label.isEmpty()) {
            int labelX = x + width + theme.spacingM();
            int labelY = y + (height - 8) / 2;
            context.drawTextWithShadow(theme.client.textRenderer, label, labelX, labelY, theme.textPrimary());
        }
        
        // Track background
        int trackColor;
        int trackBorderColor;
        
        if (value) {
            trackColor = (int) (lerp(theme.bgMedium(theme.panelOpacity()), successColor | 0xFF000000, thumbAnim));
            trackBorderColor = (int) (lerp(theme.borderSubtle(), successColor, thumbAnim));
        } else {
            trackColor = theme.bgMedium(theme.panelOpacity());
            trackBorderColor = theme.borderSubtle();
        }
        
        GlassRenderer.fillRoundedRect(context, x, y, width, height, radius, trackColor);
        GlassRenderer.drawRoundedRectOutline(context, x, y, width, height, radius, 1, trackBorderColor);
        
        // Inner glow when on
        if (value && theme.glowIntensity() > 0.1f) {
            int highlight = ((int) (theme.glowIntensity() * 80) << 24) | (successColor & 0xFFFFFF);
            GlassRenderer.fillRoundedRect(context, x + 1, y + 1, width - 2, 2, radius - 1, highlight);
            GlassRenderer.fillRoundedRect(context, x + 1, y + 1, 2, height - 2, radius - 1, highlight);
        }
        
        // Thumb
        int thumbSize = height - 4;
        int thumbX = x + 2 + (int) ((width - 4 - thumbSize) * thumbAnim);
        int thumbY = y + 2;
        
        // Thumb shadow
        if (theme.glowIntensity() > 0) {
            int shadowColor = (int) (theme.glowIntensity() * 60) << 24 | 0x000000;
            GlassRenderer.fillRoundedRect(context, thumbX + 1, thumbY + 1, thumbSize, thumbSize, thumbSize / 2, shadowColor);
        }
        
        // Thumb background
        int thumbColor = value ? 0xFFFFFFFF : 0xFFEEEEEE;
        if (hovered) thumbColor = 0xFFFFFFFF;
        GlassRenderer.fillRoundedRect(context, thumbX, thumbY, thumbSize, thumbSize, thumbSize / 2, thumbColor);
        
        // Thumb border
        int thumbBorderColor = value ? successColor : theme.borderSubtle();
        if (hovered) thumbBorderColor = value ? successColor : theme.borderAccentSoft();
        GlassRenderer.drawRoundedRectOutline(context, thumbX, thumbY, thumbSize, thumbSize, thumbSize / 2, 1, thumbBorderColor);
        
        // Labels on track
        if (showLabels) {
            String onText = labelOn;
            String offText = labelOff;
            
            // ON label (right side, visible when on)
            int onTextX = x + width - theme.spacingM() - theme.client.textRenderer.getWidth(onText);
            int onTextY = y + (height - 8) / 2;
            int onAlpha = (int) (255 * thumbAnim);
            int onColor = (onAlpha << 24) | (theme.textOnAccent() & 0xFFFFFF);
            context.drawTextWithShadow(theme.client.textRenderer, onText, onTextX, onTextY, onColor);
            
            // OFF label (left side, visible when off)
            int offTextX = x + theme.spacingM();
            int offTextY = y + (height - 8) / 2;
            int offAlpha = (int) (255 * (1 - thumbAnim));
            int offColor = (offAlpha << 24) | (theme.textMuted() & 0xFFFFFF);
            context.drawTextWithShadow(theme.client.textRenderer, offText, offTextX, offTextY, offColor);
        }
        
        RenderSystem.disableBlend();
    }
    
    private int lerp(int a, int b, float t) {
        int ar = (a >> 16) & 0xFF;
        int ag = (a >> 8) & 0xFF;
        int ab = a & 0xFF;
        int aa = (a >> 24) & 0xFF;
        
        int br = (b >> 16) & 0xFF;
        int bg = (b >> 8) & 0xFF;
        int bb = b & 0xFF;
        int ba = (b >> 24) & 0xFF;
        
        int r = (int) (ar + (br - ar) * t);
        int g = (int) (ag + (bg - ag) * t);
        int bl = (int) (ab + (bb - ab) * t);
        int al = (int) (aa + (ba - aa) * t);
        
        return (al << 24) | (r << 16) | (g << 8) | bl;
    }
    
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        if (isMouseOver(mouseX, mouseY)) {
            value = !value;
            onChange.run();
            playClickSound();
            return true;
        }
        return false;
    }
    
    private boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
    
    private void playClickSound() {
        MinecraftClient.getInstance().getSoundManager().play(
            PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0f)
        );
    }
    
    public boolean getValue() { return value; }
    public void setValue(boolean value) { this.value = value; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public boolean isHovered() { return hovered; }
    
    public static Builder builder(int x, int y) {
        return new Builder(x, y);
    }
}