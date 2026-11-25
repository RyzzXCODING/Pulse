package com.alpoyy.pulse.render;

import com.alpoyy.pulse.Pulse;
import com.alpoyy.pulse.config.PulseConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

public class PulseRenderer {
    private static final Map<String, Identifier> TEXTURE_CACHE = new HashMap<>();
    
    public static void renderDarkGlowPanel(DrawContext context, int x, int y, int width, int height, 
                                            PulseConfig config) {
        GlowRenderer.renderPanelBackground(context, x, y, width, height, config, 8);
    }
    
    public static void renderSidebar(DrawContext context, int x, int y, int width, int height,
                                      PulseConfig config) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        // Sidebar background - slightly darker
        int bgAlpha = (int) (config.backgroundOpacity * 255);
        int bgColor = (bgAlpha << 24) | 0x12121A;
        context.fill(x, y, x + width, y + height, bgColor);
        
        // Right border (separator)
        int borderAlpha = (int) (config.glowIntensity * 80);
        int accentColor = config.getAccentColor().getRGB() & 0xFFFFFF;
        int borderColor = (borderAlpha << 24) | accentColor;
        context.fill(x + width - 1, y, x + width, y + height, borderColor);
        
        RenderSystem.disableBlend();
    }
    
    public static void renderTabButton(DrawContext context, int x, int y, int width, int height,
                                        String text, boolean selected, boolean hovered,
                                        PulseConfig config) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        int accentColor = config.getAccentColor().getRGB() & 0xFFFFFF;
        int secondaryColor = config.getSecondaryAccentColor().getRGB() & 0xFFFFFF;
        
        if (selected) {
            // Selected tab - accent background
            int bgAlpha = 200;
            int bgColor = (bgAlpha << 24) | accentColor;
            context.fill(x, y, x + width, y + height, bgColor);
            
            // Bottom accent line
            context.fill(x, y + height - 2, x + width, y + height, 0xFFFFFFFF);
            
            int textColor = 0xFF1A1A2E;
            int textX = x + (width - Pulse.CLIENT.textRenderer.getWidth(text)) / 2;
            int textY = y + (height - 8) / 2;
            context.drawTextWithShadow(Pulse.CLIENT.textRenderer, text, textX, textY, textColor);
        } else {
            // Unselected tab
            int bgAlpha = hovered ? 100 : 40;
            int bgColor = (bgAlpha << 24) | 0x2D2D44;
            context.fill(x, y, x + width, y + height, bgColor);
            
            int textColor = hovered ? 0xFFFFFFFF : 0xFFAAAAAA;
            int textX = x + (width - Pulse.CLIENT.textRenderer.getWidth(text)) / 2;
            int textY = y + (height - 8) / 2;
            context.drawTextWithShadow(Pulse.CLIENT.textRenderer, text, textX, textY, textColor);
            
            // Hover underline
            if (hovered) {
                context.fill(x + 4, y + height - 1, x + width - 4, y + height, 
                    (150 << 24) | secondaryColor);
            }
        }
        
        RenderSystem.disableBlend();
    }
    
    public static void renderTextField(DrawContext context, int x, int y, int width, int height,
                                        String text, String placeholder, boolean focused,
                                        PulseConfig config) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        int accentColor = config.getAccentColor().getRGB() & 0xFFFFFF;
        
        // Background
        context.fill(x, y, x + width, y + height, 0xFF1A1A2E);
        
        // Border
        int borderAlpha = focused ? 255 : 100;
        int borderColor = (borderAlpha << 24) | accentColor;
        context.fill(x, y, x + width, y + 1, borderColor);
        context.fill(x, y + height - 1, x + width, y + height, borderColor);
        context.fill(x, y, x + 1, y + height, borderColor);
        context.fill(x + width - 1, y, x + width, y + height, borderColor);
        
        // Text
        int textColor = text.isEmpty() ? 0xFF666666 : 0xFFFFFFFF;
        String displayText = text.isEmpty() ? placeholder : text;
        int textX = x + 6;
        int textY = y + (height - 8) / 2;
        context.drawTextWithShadow(Pulse.CLIENT.textRenderer, displayText, textX, textY, textColor);
        
        // Cursor if focused
        if (focused && !text.isEmpty() && (System.currentTimeMillis() / 500) % 2 == 0) {
            int cursorX = textX + Pulse.CLIENT.textRenderer.getWidth(text);
            context.fill(cursorX, y + 4, cursorX + 1, y + height - 4, 0xFFFFFFFF);
        }
        
        RenderSystem.disableBlend();
    }
    
    public static void renderScrollbar(DrawContext context, int x, int y, int width, int height,
                                        float scrollProgress, float visibleRatio,
                                        PulseConfig config) {
        if (visibleRatio >= 1.0f) return; // No scrollbar needed
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        int accentColor = config.getAccentColor().getRGB() & 0xFFFFFF;
        
        // Track
        context.fill(x, y, x + width, y + height, 0xFF1A1A2E);
        
        // Thumb
        int thumbHeight = Math.max(20, (int) (height * visibleRatio));
        int thumbY = y + (int) ((height - thumbHeight) * scrollProgress);
        int thumbAlpha = 180;
        int thumbColor = (thumbAlpha << 24) | accentColor;
        context.fill(x + 2, thumbY, x + width - 2, thumbY + thumbHeight, thumbColor);
        
        RenderSystem.disableBlend();
    }
    
    public static void renderTooltip(DrawContext context, int x, int y, String text,
                                      PulseConfig config) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        int accentColor = config.getAccentColor().getRGB() & 0xFFFFFF;
        
        int padding = 6;
        int textWidth = Pulse.CLIENT.textRenderer.getWidth(text);
        int width = textWidth + padding * 2;
        int height = 16 + padding * 2;
        
        // Adjust position to stay on screen
        int screenWidth = Pulse.CLIENT.getWindow().getScaledWidth();
        int screenHeight = Pulse.CLIENT.getWindow().getScaledHeight();
        
        if (x + width > screenWidth) x = screenWidth - width - 4;
        if (y + height > screenHeight) y = screenHeight - height - 4;
        if (x < 4) x = 4;
        if (y < 4) y = 4;
        
        // Background
        context.fill(x, y, x + width, y + height, 0xE01A1A2E);
        
        // Border
        context.fill(x, y, x + width, y + 1, accentColor | 0xFF000000);
        context.fill(x, y + height - 1, x + width, y + height, accentColor | 0xFF000000);
        context.fill(x, y, x + 1, y + height, accentColor | 0xFF000000);
        context.fill(x + width - 1, y, x + width, y + height, accentColor | 0xFF000000);
        
        // Text
        context.drawTextWithShadow(Pulse.CLIENT.textRenderer, text, x + padding, y + padding, 0xFFFFFFFF);
        
        RenderSystem.disableBlend();
    }
    
    public static Identifier loadTexture(String path) {
        return TEXTURE_CACHE.computeIfAbsent(path, p -> {
            try (InputStream stream = PulseRenderer.class.getResourceAsStream("/assets/pulse/" + p)) {
                if (stream != null) {
                    BufferedImage image = ImageIO.read(stream);
                    // Note: Actual texture loading would use TextureManager
                    // This is a placeholder for the identifier
                }
            } catch (IOException e) {
                Pulse.LOGGER.warn("Failed to load texture: {}", p);
            }
            return Identifier.of("pulse", p);
        });
    }
    
    public static void renderIcon(DrawContext context, Identifier texture, int x, int y, 
                                   int width, int height, int u, int v, int uWidth, int vHeight,
                                   int textureWidth, int textureHeight) {
        context.drawTexture(RenderLayer::getGuiTextured, texture, x, y, u, v, width, height, textureWidth, textureHeight);
    }
}