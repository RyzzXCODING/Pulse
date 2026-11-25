package com.alpoyy.pulse.gui.component;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.MinecraftClient;

import java.util.function.Consumer;

public class GlassScrollContainer {
    private final int x, y, width, height;
    private final Runnable contentRenderer;
    private int contentHeight = 0;
    private int scrollY = 0;
    private int maxScrollY = 0;
    private float scrollProgress = 0f; // 0 to 1
    private float smoothScrollY = 0f;
    
    private boolean hovered = false;
    private boolean dragging = false;
    private int dragStartY = 0;
    private int dragStartScroll = 0;
    private long lastScrollTime = 0;
    
    public GlassScrollContainer(int x, int y, int width, int height, Runnable contentRenderer) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.contentRenderer = contentRenderer;
    }
    
    public void setContentHeight(int contentHeight) {
        this.contentHeight = contentHeight;
        this.maxScrollY = Math.max(0, contentHeight - height);
        // Clamp scroll
        if (scrollY > maxScrollY) {
            scrollY = maxScrollY;
            smoothScrollY = scrollY;
        }
        updateProgress();
    }
    
    public void render(DrawContext context, PulseTheme theme, int mouseX, int mouseY) {
        hovered = isMouseOver(mouseX, mouseY);
        
        // Smooth scroll animation
        long now = System.currentTimeMillis();
        float delta = (now - lastScrollTime) / 200f;
        if (smoothScrollY < scrollY) {
            smoothScrollY = Math.min(scrollY, smoothScrollY + maxScrollY * delta);
        } else if (smoothScrollY > scrollY) {
            smoothScrollY = Math.max(scrollY, smoothScrollY - maxScrollY * delta);
        }
        lastScrollTime = now;
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        int radius = theme.radiusMedium();
        
        // Background panel
        GlassRenderer.renderGlassPanel(context, x, y, width, height, radius, theme);
        
        // Scissor for content
        context.enableScissor(x + 1, y + 1, x + width - 1, y + height - 1);
        
        // Render content with scroll offset
        context.getMatrices().push();
        context.getMatrices().translate(x + theme.spacingM(), y + theme.spacingM() - smoothScrollY, 0);
        if (contentRenderer != null) {
            contentRenderer.run();
        }
        context.getMatrices().pop();
        
        context.disableScissor();
        
        // Scrollbar
        if (maxScrollY > 0) {
            renderScrollbar(context, theme);
        }
        
        RenderSystem.disableBlend();
    }
    
    private void renderScrollbar(DrawContext context, PulseTheme theme) {
        int scrollbarWidth = 6;
        int scrollbarX = x + width - scrollbarWidth - 4;
        int scrollbarY = y + 4;
        int scrollbarHeight = height - 8;
        
        // Track
        GlassRenderer.fillRoundedRect(context, scrollbarX, scrollbarY, scrollbarWidth, scrollbarHeight, 3, theme.bgDark(theme.panelOpacity()));
        
        // Thumb
        float thumbRatio = (float) height / contentHeight;
        int thumbHeight = Math.max(30, (int) (scrollbarHeight * thumbRatio));
        int thumbY = scrollbarY + (int) (scrollProgress * (scrollbarHeight - thumbHeight));
        
        int thumbColor = (hovered || dragging) ? theme.accent(0.8f) : theme.accent(0.5f);
        GlassRenderer.fillRoundedRect(context, scrollbarX + 1, thumbY, scrollbarWidth - 2, thumbHeight, 2, thumbColor);
        
        // Thumb border on hover
        if (hovered || dragging) {
            GlassRenderer.drawRoundedRectOutline(context, scrollbarX + 1, thumbY, scrollbarWidth - 2, thumbHeight, 2, 1, theme.accent(1f));
        }
    }
    
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        
        // Check scrollbar click
        if (maxScrollY > 0) {
            int scrollbarWidth = 6;
            int scrollbarX = x + width - scrollbarWidth - 4;
            int scrollbarY = y + 4;
            int scrollbarHeight = height - 8;
            
            float thumbRatio = (float) height / contentHeight;
            int thumbHeight = Math.max(30, (int) (scrollbarHeight * thumbRatio));
            int thumbY = scrollbarY + (int) (scrollProgress * (scrollbarHeight - thumbHeight));
            
            if (mouseX >= scrollbarX && mouseX <= scrollbarX + scrollbarWidth &&
                mouseY >= thumbY && mouseY <= thumbY + thumbHeight) {
                dragging = true;
                dragStartY = (int) mouseY;
                dragStartScroll = scrollY;
                return true;
            }
        }
        
        return false;
    }
    
    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (dragging && button == 0) {
            float dragDelta = (float) (mouseY - dragStartY);
            float scrollDelta = dragDelta * (float) contentHeight / height;
            scrollY = Math.max(0, Math.max(0, dragStartScroll + (int) scrollDelta));
            scrollY = Math.min(scrollY, maxScrollY);
            updateProgress();
            return true;
        }
        return false;
    }
    
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging) {
            dragging = false;
            return true;
        }
        return false;
    }
    
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (!hovered || maxScrollY <= 0) return false;
        
        scrollY -= (int) (deltaY * 40);
        scrollY = Math.max(0, Math.min(scrollY, maxScrollY));
        updateProgress();
        return true;
    }
    
    private void updateProgress() {
        scrollProgress = maxScrollY > 0 ? (float) scrollY / maxScrollY : 0f;
    }
    
    private boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
    
    public int getScrollY() { return scrollY; }
    public void setScrollY(int y) { 
        this.scrollY = Math.max(0, Math.min(y, maxScrollY)); 
        updateProgress();
    }
    public int getMaxScrollY() { return maxScrollY; }
    public int getContentHeight() { return contentHeight; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public boolean isHovered() { return hovered; }
}