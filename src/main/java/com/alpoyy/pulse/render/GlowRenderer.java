package com.alpoyy.pulse.render;

import com.alpoyy.pulse.config.PulseConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;

public class GlowRenderer {
    
    public static void renderGlowRect(DrawContext context, int x, int y, int width, int height, 
                                       int color, float intensity, int radius) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        int alpha = (int) (intensity * 255);
        int glowColor = (alpha << 24) | (color & 0xFFFFFF);
        
        // Render multiple layers for glow effect
        for (int i = radius; i > 0; i--) {
            float layerAlpha = intensity * (1.0f - (float) i / radius) * 0.3f;
            int layerColor = ((int) (layerAlpha * 255) << 24) | (color & 0xFFFFFF);
            
            context.fill(x - i, y - i, x + width + i, y + height + i, layerColor);
        }
        
        // Main rect
        context.fill(x, y, x + width, y + height, glowColor);
        
        RenderSystem.disableBlend();
    }
    
    public static void renderGlowBorder(DrawContext context, int x, int y, int width, int height,
                                         int color, float intensity, int thickness) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        int alpha = (int) (intensity * 255);
        int glowColor = (alpha << 24) | (color & 0xFFFFFF);
        
        // Top
        context.fill(x, y, x + width, y + thickness, glowColor);
        // Bottom
        context.fill(x, y + height - thickness, x + width, y + height, glowColor);
        // Left
        context.fill(x, y, x + thickness, y + height, glowColor);
        // Right
        context.fill(x + width - thickness, y, x + width, y + height, glowColor);
        
        RenderSystem.disableBlend();
    }
    
    public static void renderPanelBackground(DrawContext context, int x, int y, int width, int height,
                                              PulseConfig config, int cornerRadius) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        // Base panel
        int bgAlpha = (int) (config.backgroundOpacity * 255);
        int bgColor = (bgAlpha << 24) | 0x1A1A2E;
        
        if (cornerRadius > 0) {
            // Rounded panel approximation
            context.fill(x + cornerRadius, y, x + width - cornerRadius, y + height, bgColor);
            context.fill(x, y + cornerRadius, x + width, y + height - cornerRadius, bgColor);
        } else {
            context.fill(x, y, x + width, y + height, bgColor);
        }
        
        // Subtle accent border
        int borderAlpha = (int) (config.glowIntensity * 100);
        int accentColor = config.getAccentColor().getRGB() & 0xFFFFFF;
        int borderColor = (borderAlpha << 24) | accentColor;
        
        context.fill(x, y, x + width, y + 1, borderColor);
        context.fill(x, y + height - 1, x + width, y + height, borderColor);
        context.fill(x, y, x + 1, y + height, borderColor);
        context.fill(x + width - 1, y, x + width, y + height, borderColor);
        
        // Inner glow highlight
        if (config.glowIntensity > 0.1f) {
            int glowAlpha = (int) (config.glowIntensity * 40);
            int glowColor = (glowAlpha << 24) | accentColor;
            context.fill(x + 1, y + 1, x + width - 1, y + 2, glowColor);
            context.fill(x + 1, y + 1, x + 2, y + height - 1, glowColor);
        }
        
        RenderSystem.disableBlend();
    }
    
    public static void renderButton(DrawContext context, int x, int y, int width, int height,
                                     String text, boolean hovered, boolean pressed,
                                     PulseConfig config, net.minecraft.client.MinecraftClient client) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        int accentColor = config.getAccentColor().getRGB() & 0xFFFFFF;
        int secondaryColor = config.getSecondaryAccentColor().getRGB() & 0xFFFFFF;
        
        // Background
        int bgAlpha = pressed ? 200 : (hovered ? 180 : 120);
        int bgColor = (bgAlpha << 24) | 0x2D2D44;
        context.fill(x, y, x + width, y + height, bgColor);
        
        // Border
        int borderAlpha = hovered ? 200 : 100;
        int borderColor = (borderAlpha << 24) | accentColor;
        context.fill(x, y, x + width, y + 1, borderColor);
        context.fill(x, y + height - 1, x + width, y + height, borderColor);
        context.fill(x, y, x + 1, y + height, borderColor);
        context.fill(x + width - 1, y, x + width, y + height, borderColor);
        
        // Text
        int textColor = hovered ? 0xFFFFFFFF : 0xFFCCCCCC;
        int textX = x + (width - client.textRenderer.getWidth(text)) / 2;
        int textY = y + (height - 8) / 2;
        context.drawTextWithShadow(client.textRenderer, text, textX, textY, textColor);
        
        RenderSystem.disableBlend();
    }
    
    public static void renderSlider(DrawContext context, int x, int y, int width, int height,
                                     float value, PulseConfig config, boolean hovered) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        int accentColor = config.getAccentColor().getRGB() & 0xFFFFFF;
        
        // Track
        int trackY = y + height / 2 - 2;
        context.fill(x, trackY, x + width, trackY + 4, 0xFF1A1A2E);
        
        // Fill
        int fillWidth = (int) (width * value);
        int fillAlpha = hovered ? 255 : 200;
        int fillColor = (fillAlpha << 24) | accentColor;
        context.fill(x, trackY, x + fillWidth, trackY + 4, fillColor);
        
        // Handle
        int handleX = x + fillWidth - 6;
        int handleY = y + height / 2 - 6;
        int handleColor = hovered ? 0xFFFFFFFF : 0xFFCCCCCC;
        context.fill(handleX, handleY, handleX + 12, handleY + 12, handleColor);
        
        // Handle border
        context.fill(handleX, handleY, handleX + 12, handleY + 1, accentColor | 0xFF000000);
        context.fill(handleX, handleY + 11, handleX + 12, handleY + 12, accentColor | 0xFF000000);
        context.fill(handleX, handleY, handleX + 1, handleY + 12, accentColor | 0xFF000000);
        context.fill(handleX + 11, handleY, handleX + 12, handleY + 12, accentColor | 0xFF000000);
        
        RenderSystem.disableBlend();
    }
}