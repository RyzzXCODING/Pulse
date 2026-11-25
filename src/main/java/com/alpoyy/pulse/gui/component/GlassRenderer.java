package com.alpoyy.pulse.gui.component;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;

public class GlassRenderer {
    
    // ============ ROUNDED RECTANGLES ============
    
    public static void fillRoundedRect(DrawContext context, int x, int y, int width, int height, 
                                        int radius, int color) {
        if (radius <= 0) {
            context.fill(x, y, x + width, y + height, color);
            return;
        }
        
        radius = Math.min(radius, Math.min(width, height) / 2);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        // Center rectangle
        context.fill(x + radius, y, x + width - radius, y + height, color);
        
        // Left and right strips
        context.fill(x, y + radius, x + radius, y + height - radius, color);
        context.fill(x + width - radius, y + radius, x + width, y + height - radius, color);
        
        // Four corners (approximated with multiple fills)
        for (int i = 0; i < radius; i++) {
            int cornerY = (int) Math.sqrt(radius * radius - i * i);
            
            // Top-left
            context.fill(x + i, y + radius - cornerY, x + radius, y + radius - cornerY + 1, color);
            context.fill(x + radius - cornerY, y + i, x + radius - cornerY + 1, y + radius, color);
            
            // Top-right
            context.fill(x + width - radius, y + radius - cornerY, x + width - i, y + radius - cornerY + 1, color);
            context.fill(x + width - radius + cornerY - 1, y + i, x + width - radius + cornerY, y + radius, color);
            
            // Bottom-left
            context.fill(x + i, y + height - radius + cornerY, x + radius, y + height - radius + cornerY + 1, color);
            context.fill(x + radius - cornerY, y + height - radius, x + radius - cornerY + 1, y + height - i, color);
            
            // Bottom-right
            context.fill(x + width - radius, y + height - radius + cornerY, x + width - i, y + height - radius + cornerY + 1, color);
            context.fill(x + width - radius + cornerY - 1, y + height - radius, x + width - radius + cornerY, y + height - i, color);
        }
        
        RenderSystem.disableBlend();
    }
    
    public static void drawRoundedRectOutline(DrawContext context, int x, int y, int width, int height,
                                               int radius, int thickness, int color) {
        if (radius <= 0) {
            context.fill(x, y, x + width, y + thickness, color);
            context.fill(x, y + height - thickness, x + width, y + height, color);
            context.fill(x, y, x + thickness, y + height, color);
            context.fill(x + width - thickness, y, x + width, y + height, color);
            return;
        }
        
        radius = Math.min(radius, Math.min(width, height) / 2);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        // Top and bottom
        context.fill(x + radius, y, x + width - radius, y + thickness, color);
        context.fill(x + radius, y + height - thickness, x + width - radius, y + height, color);
        
        // Left and right
        context.fill(x, y + radius, x + thickness, y + height - radius, color);
        context.fill(x + width - thickness, y + radius, x + width, y + height - radius, color);
        
        // Corners
        for (int i = 0; i < radius; i++) {
            int cornerY = (int) Math.sqrt(radius * radius - i * i);
            
            // Top edges
            context.fill(x + i, y + radius - cornerY, x + radius, y + radius - cornerY + thickness, color);
            context.fill(x + width - radius, y + radius - cornerY, x + width - i, y + radius - cornerY + thickness, color);
            context.fill(x + radius - cornerY, y + i, x + radius - cornerY + thickness, y + radius, color);
            context.fill(x + width - radius + cornerY - thickness, y + i, x + width - radius + cornerY, y + radius, color);
            
            // Bottom edges
            context.fill(x + i, y + height - radius + cornerY - thickness, x + radius, y + height - radius + cornerY, color);
            context.fill(x + width - radius, y + height - radius + cornerY - thickness, x + width - i, y + height - radius + cornerY, color);
            context.fill(x + radius - cornerY, y + height - radius, x + radius - cornerY + thickness, y + height - i, color);
            context.fill(x + width - radius + cornerY - thickness, y + height - radius, x + width - radius + cornerY, y + height - i, color);
        }
        
        RenderSystem.disableBlend();
    }
    
    // ============ GLASS PANELS ============
    
    public static void renderGlassPanel(DrawContext context, int x, int y, int width, int height,
                                         int radius, PulseTheme theme) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        // Shadow layers (behind panel)
        if (theme.glowIntensity() > 0) {
            int shadowAlpha = (int) (theme.glowIntensity() * 40);
            int shadowColor = (shadowAlpha << 24) | 0x000000;
            fillRoundedRect(context, x + 2, y + 4, width, height, radius, shadowColor);
        }
        
        // Main panel background
        fillRoundedRect(context, x, y, width, height, radius, theme.bgDark(theme.panelOpacity()));
        
        // Inner highlight (top edge)
        if (theme.glowIntensity() > 0.1f) {
            int highlightColor = theme.innerGlow();
            // Top inner edge
            fillRoundedRect(context, x + 1, y + 1, width - 2, 2, radius - 1, highlightColor);
            // Left inner edge
            fillRoundedRect(context, x + 1, y + 1, 2, height - 2, radius - 1, highlightColor);
        }
        
        // Border
        int borderColor = theme.borderAccentSoft();
        drawRoundedRectOutline(context, x, y, width, height, radius, 1, borderColor);
        
        RenderSystem.disableBlend();
    }
    
    public static void renderGlassPanelElevated(DrawContext context, int x, int y, int width, int height,
                                                 int radius, PulseTheme theme) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        // Drop shadow (multiple layers for depth)
        for (int i = 4; i > 0; i--) {
            float alpha = theme.glowIntensity() * (0.12f - i * 0.02f);
            if (alpha <= 0) continue;
            int shadowColor = ((int) (alpha * 255) << 24) | 0x000000;
            fillRoundedRect(context, x + i, y + i + 2, width, height, radius, shadowColor);
        }
        
        // Main panel
        fillRoundedRect(context, x, y, width, height, radius, theme.bgDark(theme.panelOpacity()));
        
        // Subtle inner glow top
        if (theme.glowIntensity() > 0.1f) {
            int highlightColor = theme.innerGlow();
            fillRoundedRect(context, x + 1, y + 1, width - 2, 2, radius - 1, highlightColor);
            fillRoundedRect(context, x + 1, y + 1, 2, height - 2, radius - 1, highlightColor);
        }
        
        // Border
        drawRoundedRectOutline(context, x, y, width, height, radius, 1, theme.borderAccentSoft());
        
        // Secondary inner border (subtle)
        drawRoundedRectOutline(context, x + 1, y + 1, width - 2, height - 2, radius - 1, 1, theme.borderSubtle());
        
        RenderSystem.disableBlend();
    }
    
    public static void renderGlassPanelHover(DrawContext context, int x, int y, int width, int height,
                                              int radius, PulseTheme theme, boolean hovered, boolean focused) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        int bgColor;
        int borderColor;
        
        if (focused) {
            bgColor = theme.bgMedium(theme.panelOpacity() + 0.05f);
            borderColor = theme.borderAccent();
        } else if (hovered) {
            bgColor = theme.bgHover(theme.panelOpacity());
            borderColor = theme.borderAccentSoft();
        } else {
            bgColor = theme.bgDark(theme.panelOpacity());
            borderColor = theme.borderAccentSoft();
        }
        
        fillRoundedRect(context, x, y, width, height, radius, bgColor);
        
        // Inner highlight
        if (theme.glowIntensity() > 0.1f && (hovered || focused)) {
            int highlightColor = theme.innerGlow();
            fillRoundedRect(context, x + 1, y + 1, width - 2, 2, radius - 1, highlightColor);
            fillRoundedRect(context, x + 1, y + 1, 2, height - 2, radius - 1, highlightColor);
        }
        
        drawRoundedRectOutline(context, x, y, width, height, radius, 1, borderColor);
        
        RenderSystem.disableBlend();
    }
    
    // ============ GRADIENTS ============
    
    public static void renderVerticalGradient(DrawContext context, int x, int y, int width, int height,
                                               int colorTop, int colorBottom) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        int steps = Math.max(1, height / 2);
        for (int i = 0; i < steps; i++) {
            float progress = (float) i / steps;
            int alpha = (int) (255 * (1 - progress * 0.3f));
            
            int r = lerp((colorTop >> 16) & 0xFF, (colorBottom >> 16) & 0xFF, progress);
            int g = lerp((colorTop >> 8) & 0xFF, (colorBottom >> 8) & 0xFF, progress);
            int b = lerp(colorTop & 0xFF, colorBottom & 0xFF, progress);
            int a = lerp((colorTop >> 24) & 0xFF, (colorBottom >> 24) & 0xFF, progress);
            
            int color = (a << 24) | (r << 16) | (g << 8) | b;
            context.fill(x, y + i * height / steps, x + width, y + (i + 1) * height / steps, color);
        }
        
        RenderSystem.disableBlend();
    }
    
    public static void renderHorizontalGradient(DrawContext context, int x, int y, int width, int height,
                                                 int colorLeft, int colorRight) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        int steps = Math.max(1, width / 2);
        for (int i = 0; i < steps; i++) {
            float progress = (float) i / steps;
            
            int r = lerp((colorLeft >> 16) & 0xFF, (colorRight >> 16) & 0xFF, progress);
            int g = lerp((colorLeft >> 8) & 0xFF, (colorRight >> 8) & 0xFF, progress);
            int b = lerp(colorLeft & 0xFF, colorRight & 0xFF, progress);
            int a = lerp((colorLeft >> 24) & 0xFF, (colorRight >> 24) & 0xFF, progress);
            
            int color = (a << 24) | (r << 16) | (g << 8) | b;
            context.fill(x + i * width / steps, y, x + (i + 1) * width / steps, y + height, color);
        }
        
        RenderSystem.disableBlend();
    }
    
    private static int lerp(int a, int b, float t) {
        return (int) (a + (b - a) * t);
    }
    
    // ============ GLOW EFFECTS ============
    
    public static void renderOuterGlow(DrawContext context, int x, int y, int width, int height,
                                        int radius, int color, float intensity, int spread) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        int baseAlpha = (int) (intensity * 255);
        
        for (int i = spread; i > 0; i--) {
            float alpha = intensity * (1.0f - (float) i / spread) * 0.4f;
            if (alpha <= 0) continue;
            
            int glowColor = ((int) (alpha * 255) << 24) | (color & 0xFFFFFF);
            int expandedRadius = radius + i;
            
            fillRoundedRect(context, x - i, y - i, width + i * 2, height + i * 2, expandedRadius, glowColor);
        }
        
        RenderSystem.disableBlend();
    }
    
    public static void renderPulseGlow(DrawContext context, int x, int y, int width, int height,
                                        int radius, int color, float intensity, long time) {
        // Animated pulse effect
        float pulse = (float) (Math.sin(time / 800.0) * 0.5 + 0.5);
        float animatedIntensity = intensity * (0.5f + pulse * 0.5f);
        renderOuterGlow(context, x, y, width, height, radius, color, animatedIntensity, radius + 4);
    }
    
    // ============ ICONS (Vector-like) ============
    
    public static void drawPlayIcon(DrawContext context, int x, int y, int size, int color) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        int half = size / 2;
        // Triangle pointing right
        for (int i = 0; i <= half; i++) {
            int w = (int) (i * 0.9f);
            context.fill(x + half - w, y + half - i, x + half + w, y + half - i + 1, color);
        }
        
        RenderSystem.disableBlend();
    }
    
    public static void drawPauseIcon(DrawContext context, int x, int y, int size, int color) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        int barWidth = size / 5;
        int gap = size / 5;
        int leftX = x + (size - barWidth * 2 - gap) / 2;
        
        context.fill(leftX, y, leftX + barWidth, y + size, color);
        context.fill(leftX + barWidth + gap, y, leftX + barWidth * 2 + gap, y + size, color);
        
        RenderSystem.disableBlend();
    }
    
    public static void drawSkipNextIcon(DrawContext context, int x, int y, int size, int color) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        int barWidth = size / 5;
        int gap = size / 5;
        int leftX = x + (size - barWidth * 2 - gap) / 2;
        
        // First triangle
        for (int i = 0; i <= size / 2; i++) {
            int w = (int) (i * 0.7f);
            context.fill(leftX - w, y + size/2 - i, leftX, y + size/2 - i + 1, color);
        }
        
        // Bar
        context.fill(leftX + gap, y, leftX + gap + barWidth, y + size, color);
        
        // Second triangle
        for (int i = 0; i <= size / 2; i++) {
            int w = (int) (i * 0.7f);
            context.fill(leftX + gap + barWidth + gap + w, y + size/2 - i, 
                        leftX + gap + barWidth + gap + barWidth, y + size/2 - i + 1, color);
        }
        
        RenderSystem.disableBlend();
    }
    
    public static void drawSkipPrevIcon(DrawContext context, int x, int y, int size, int color) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        int barWidth = size / 5;
        int gap = size / 5;
        int centerX = x + size / 2;
        
        // Bar
        context.fill(centerX - barWidth - gap, y, centerX - gap, y + size, color);
        
        // Triangle pointing left
        for (int i = 0; i <= size / 2; i++) {
            int w = (int) (i * 0.7f);
            context.fill(centerX + gap, y + size/2 - i, centerX + gap + w, y + size/2 - i + 1, color);
        }
        
        // Triangle pointing left (second)
        for (int i = 0; i <= size / 2; i++) {
            int w = (int) (i * 0.7f);
            context.fill(centerX + gap + barWidth + gap, y + size/2 - i, 
                        centerX + gap + barWidth + gap + w, y + size/2 - i + 1, color);
        }
        
        RenderSystem.disableBlend();
    }
    
    public static void drawShuffleIcon(DrawContext context, int x, int y, int size, int color, boolean active) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        int c = active ? color : color & 0x88FFFFFF;
        
        // Two crossing arrows
        int cx = x + size / 2;
        int cy = y + size / 2;
        int r = size / 2 - 2;
        
        // Top-left to bottom-right
        context.fill(cx - r, cy - r, cx - r + 6, cy - r + 2, c);
        context.fill(cx - r, cy - r, cx - r + 2, cy - r + 6, c);
        
        context.fill(cx + r - 6, cy + r - 2, cx + r, cy + r, c);
        context.fill(cx + r - 2, cy + r - 6, cx + r, cy + r, c);
        
        // Top-right to bottom-left
        context.fill(cx + r - 6, cy - r, cx + r, cy - r + 2, c);
        context.fill(cx + r - 2, cy - r, cx + r, cy - r + 6, c);
        
        context.fill(cx - r, cy + r - 2, cx - r + 6, cy + r, c);
        context.fill(cx - r, cy + r - 6, cx - r + 2, cy + r, c);
        
        RenderSystem.disableBlend();
    }
    
    public static void drawRepeatIcon(DrawContext context, int x, int y, int size, int color, int mode) {
        // mode: 0=off, 1=all, 2=one
        if (mode == 0) {
            drawRepeatIconBase(context, x, y, size, color & 0x88FFFFFF);
            return;
        }
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        drawRepeatIconBase(context, x, y, size, color);
        
        if (mode == 2) {
            // Small "1" badge
            String text = "1";
            // Draw small 1 in corner
            context.fill(x + size - 6, y, x + size - 2, y + 8, color);
        }
        
        RenderSystem.disableBlend();
    }
    
    private static void drawRepeatIconBase(DrawContext context, int x, int y, int size, int color) {
        int cx = x + size / 2;
        int cy = y + size / 2;
        int r = size / 2 - 2;
        int thickness = 2;
        
        // Clockwise arrow
        // Top horizontal
        context.fill(cx - r, cy - r, cx + r, cy - r + thickness, color);
        // Right vertical
        context.fill(cx + r - thickness, cy - r, cx + r, cy + r, color);
        // Bottom horizontal (partial)
        context.fill(cx - r, cy + r - thickness, cx, cy + r, color);
        // Arrow head
        context.fill(cx, cy + r - 6, cx + 6, cy + r, color);
        context.fill(cx + 2, cy + r - 10, cx + 6, cy + r - 2, color);
    }
    
    public static void drawVolumeIcon(DrawContext context, int x, int y, int size, int color, float volume) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        int cx = x + size / 2;
        int cy = y + size / 2;
        
        // Speaker body
        context.fill(cx - 6, cy - 4, cx - 2, cy + 4, color);
        // Speaker cone
        for (int i = 0; i < 4; i++) {
            context.fill(cx - 2 + i, cy - 4 - i, cx - 1 + i, cy + 4 + i, color);
        }
        
        if (volume > 0) {
            // Wave arcs
            int waveCount = volume > 0.66f ? 3 : (volume > 0.33f ? 2 : 1);
            for (int w = 0; w < waveCount; w++) {
                int arcX = cx + 2 + w * 4;
                for (int i = 0; i < 4; i++) {
                    context.fill(arcX + i, cy - 4 - i - w * 2, arcX + i + 1, cy + 4 + i + w * 2, color);
                }
            }
        } else {
            // Muted slash
            context.fill(cx + 2, cy - 6, cx + 10, cy + 6, color);
            context.fill(cx + 2, cy - 6, cx + 4, cy - 4, color);
        }
        
        RenderSystem.disableBlend();
    }
    
    public static void drawSearchIcon(DrawContext context, int x, int y, int size, int color) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        int cx = x + size / 2;
        int cy = y + size / 2;
        int r = size / 3;
        
        // Circle
        for (int i = -r; i <= r; i++) {
            int w = (int) Math.sqrt(r * r - i * i);
            context.fill(cx - w, cy + i, cx + w, cy + i + 1, color);
        }
        
        // Handle
        context.fill(cx + r - 2, cy + r - 2, cx + r + 4, cy + r + 2, color);
        
        RenderSystem.disableBlend();
    }
    
    public static void drawSettingsIcon(DrawContext context, int x, int y, int size, int color) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        int cx = x + size / 2;
        int cy = y + size / 2;
        int r = size / 2 - 2;
        
        // Gear shape - 8 teeth
        for (int t = 0; t < 8; t++) {
            double angle = t * Math.PI / 4;
            int tx = cx + (int) (Math.cos(angle) * r);
            int ty = cy + (int) (Math.sin(angle) * r);
            int tx2 = cx + (int) (Math.cos(angle) * (r + 4));
            int ty2 = cy + (int) (Math.sin(angle) * (r + 4));
            
            // Draw line from inner to outer
            for (int i = 0; i <= 4; i++) {
                int lx = tx + (int) ((tx2 - tx) * i / 4f);
                int ly = ty + (int) ((ty2 - ty) * i / 4f);
                context.fill(lx, ly, lx + 2, ly + 2, color);
            }
        }
        
        // Inner circle
        for (int i = -r/2; i <= r/2; i++) {
            int w = (int) Math.sqrt((r/2) * (r/2) - i * i);
            context.fill(cx - w, cy + i, cx + w, cy + i + 1, color);
        }
        
        RenderSystem.disableBlend();
    }
    
    public static void drawMusicIcon(DrawContext context, int x, int y, int size, int color) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        int cx = x + size / 2;
        int cy = y + size / 2;
        
        // Two eighth notes
        // First note
        context.fill(cx - 8, cy - 10, cx - 6, cy + 2, color); // stem
        context.fill(cx - 10, cy - 10, cx - 6, cy - 8, color); // flag
        for (int i = 0; i < 4; i++) {
            context.fill(cx - 10 + i, cy + 2 - i, cx - 6 + i, cy + 4 - i, color); // note head
        }
        
        // Second note
        context.fill(cx + 2, cy - 10, cx + 4, cy + 2, color); // stem
        context.fill(cx, cy - 10, cx + 4, cy - 8, color); // flag
        for (int i = 0; i < 4; i++) {
            context.fill(cx + i, cy + 2 - i, cx + 4 + i, cy + 4 - i, color); // note head
        }
        
        RenderSystem.disableBlend();
    }
    
    public static void drawLibraryIcon(DrawContext context, int x, int y, int size, int color) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        int cx = x + size / 2;
        int cy = y + size / 2;
        int w = size / 2;
        int h = size / 2;
        
        // Book stack
        for (int i = 0; i < 3; i++) {
            int by = cy + h/2 - i * 6 - 3;
            context.fill(cx - w/2, by, cx + w/2, by + 4, color);
            // Book spine lines
            context.fill(cx - 2, by, cx + 2, by + 4, color);
        }
        
        RenderSystem.disableBlend();
    }
    
    public static void drawPlaylistIcon(DrawContext context, int x, int y, int size, int color) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        int cx = x + size / 2;
        int cy = y + size / 2;
        
        // Three lines with bullets
        for (int i = 0; i < 3; i++) {
            int ly = cy - 8 + i * 8;
            // Bullet
            context.fill(cx - 8, ly, cx - 4, ly + 4, color);
            // Line
            context.fill(cx, ly, cx + 10, ly + 4, color);
        }
        
        RenderSystem.disableBlend();
    }
    
    public static void drawRequestsIcon(DrawContext context, int x, int y, int size, int color) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        int cx = x + size / 2;
        int cy = y + size / 2;
        
        // Microphone icon
        context.fill(cx - 4, cy - 10, cx + 4, cy - 2, color); // mic body
        context.fill(cx - 6, cy - 2, cx + 6, cy, color); // mic base
        context.fill(cx - 1, cy, cx + 1, cy + 8, color); // stand
        context.fill(cx - 6, cy + 8, cx + 6, cy + 10, color); // stand base
        
        RenderSystem.disableBlend();
    }
    
    public static void drawLiveIcon(DrawContext context, int x, int y, int size, int color) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        int cx = x + size / 2;
        int cy = y + size / 2;
        
        // Broadcast/wifi icon
        for (int i = 0; i < 3; i++) {
            int r = 4 + i * 3;
            // Arc
            for (int a = -60; a <= 60; a += 10) {
                double rad = Math.toRadians(a);
                int px = cx + (int) (Math.cos(rad) * r);
                int py = cy + (int) (Math.sin(rad) * r);
                context.fill(px, py, px + 2, py + 2, color);
            }
        }
        // Center dot
        context.fill(cx - 2, cy - 2, cx + 2, cy + 2, color);
        
        RenderSystem.disableBlend();
    }
    
    public static void drawWidgetsIcon(DrawContext context, int x, int y, int size, int color) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        int cx = x + size / 2;
        int cy = y + size / 2;
        int w = size / 2 - 2;
        
        // Four squares
        context.fill(cx - w - 2, cy - w - 2, cx - 2, cy - 2, color);
        context.fill(cx + 2, cy - w - 2, cx + w + 2, cy - 2, color);
        context.fill(cx - w - 2, cy + 2, cx - 2, cy + w + 2, color);
        context.fill(cx + 2, cy + 2, cx + w + 2, cy + w + 2, color);
        
        RenderSystem.disableBlend();
    }
    
    public static void drawConnectionsIcon(DrawContext context, int x, int y, int size, int color) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        int cx = x + size / 2;
        int cy = y + size / 2;
        int r = size / 2 - 2;
        
        // Two nodes connected
        // Left node
        for (int i = -r/2; i <= r/2; i++) {
            int w = (int) Math.sqrt((r/2) * (r/2) - i * i);
            context.fill(cx - r - w, cy + i, cx - r + w, cy + i + 1, color);
        }
        
        // Right node
        for (int i = -r/2; i <= r/2; i++) {
            int w = (int) Math.sqrt((r/2) * (r/2) - i * i);
            context.fill(cx + r - w, cy + i, cx + r + w, cy + i + 1, color);
        }
        
        // Connection line
        context.fill(cx - r/2, cy - 1, cx + r/2, cy + 1, color);
        
        RenderSystem.disableBlend();
    }
    
    public static void drawCloseIcon(DrawContext context, int x, int y, int size, int color) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        int cx = x + size / 2;
        int cy = y + size / 2;
        int r = size / 2 - 3;
        
        // X shape
        for (int i = -r; i <= r; i++) {
            context.fill(cx + i - 1, cy + i - 1, cx + i + 1, cy + i + 1, color);
            context.fill(cx - i - 1, cy + i - 1, cx - i + 1, cy + i + 1, color);
        }
        
        RenderSystem.disableBlend();
    }
    
    public static void drawChevronRight(DrawContext context, int x, int y, int size, int color) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        int cx = x + size / 2;
        int cy = y + size / 2;
        int r = size / 2 - 2;
        
        // Chevron pointing right
        for (int i = -r; i <= r; i++) {
            int w = r - Math.abs(i);
            context.fill(cx - r + w, cy + i, cx - r + w + 3, cy + i + 1, color);
        }
        
        RenderSystem.disableBlend();
    }
    
    public static void drawChevronDown(DrawContext context, int x, int y, int size, int color) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        int cx = x + size / 2;
        int cy = y + size / 2;
        int r = size / 2 - 2;
        
        // Chevron pointing down
        for (int i = -r; i <= r; i++) {
            int w = r - Math.abs(i);
            context.fill(cx - w, cy - r + w, cx + w, cy - r + w + 1, color);
        }
        
        RenderSystem.disableBlend();
    }
    
    public static void drawDot(DrawContext context, int x, int y, int size, int color) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        int r = size / 2;
        for (int i = -r; i <= r; i++) {
            int w = (int) Math.sqrt(r * r - i * i);
            context.fill(x + r - w, y + r + i, x + r + w, y + r + i + 1, color);
        }
        
        RenderSystem.disableBlend();
    }
}