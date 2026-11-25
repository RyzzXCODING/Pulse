package com.alpoyy.pulse.gui.component;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class GlassTextField {
    private final int x, y, width, height;
    private String text = "";
    private final String placeholder;
    private final Runnable onChange;
    private final boolean password;
    private final int maxLength;
    private final String label;
    
    private boolean focused = false;
    private boolean hovered = false;
    private int cursorPos = 0;
    private int selectionStart = -1;
    private long lastBlink = 0;
    private boolean showCursor = true;
    
    public static class Builder {
        private int x, y, width = 200, height = 36;
        private String text = "";
        private String placeholder = "";
        private Runnable onChange = s -> {};
        private boolean password = false;
        private int maxLength = 100;
        private String label = "";
        
        public Builder(int x, int y) { this.x = x; this.y = y; }
        public Builder width(int width) { this.width = width; return this; }
        public Builder height(int height) { this.height = height; return this; }
        public Builder text(String text) { this.text = text; return this; }
        public Builder placeholder(String placeholder) { this.placeholder = placeholder; return this; }
        public Builder onChange(Runnable onChange) { this.onChange = onChange; return this; }
        public Builder password(boolean password) { this.password = password; return this; }
        public Builder maxLength(int maxLength) { this.maxLength = maxLength; return this; }
        public Builder label(String label) { this.label = label; return this; }
        
        public GlassTextField build() {
            return new GlassTextField(this);
        }
    }
    
    private GlassTextField(Builder builder) {
        this.x = builder.x;
        this.y = builder.y;
        this.width = builder.width;
        this.height = builder.height;
        this.text = builder.text;
        this.placeholder = builder.placeholder;
        this.onChange = builder.onChange;
        this.password = builder.password;
        this.maxLength = builder.maxLength;
        this.label = builder.label;
        this.cursorPos = text.length();
    }
    
    public void render(DrawContext context, PulseTheme theme, int mouseX, int mouseY) {
        hovered = isMouseOver(mouseX, mouseY);
        
        // Cursor blink
        long now = System.currentTimeMillis();
        if (now - lastBlink > 500) {
            showCursor = !showCursor;
            lastBlink = now;
        }
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        int radius = theme.radiusMedium();
        int accentColor = theme.getAccentColor();
        
        // Label
        if (!label.isEmpty()) {
            context.drawTextWithShadow(theme.client.textRenderer, label, x, y - 14, theme.textSecondary());
        }
        
        // Background
        int bgColor = focused ? theme.bgMedium(theme.panelOpacity() + 0.05f) : 
                      (hovered ? theme.bgHover(theme.panelOpacity()) : theme.bgDark(theme.panelOpacity()));
        GlassRenderer.fillRoundedRect(context, x, y, width, height, radius, bgColor);
        
        // Border
        int borderColor = focused ? theme.borderAccent() : 
                         (hovered ? theme.borderAccentSoft() : theme.borderSubtle());
        int borderThickness = focused ? 2 : 1;
        GlassRenderer.drawRoundedRectOutline(context, x, y, width, height, radius, borderThickness, borderColor);
        
        // Inner highlight when focused
        if (focused && theme.glowIntensity() > 0.1f) {
            int highlight = theme.innerGlow();
            GlassRenderer.fillRoundedRect(context, x + 1, y + 1, width - 2, 2, radius - 1, highlight);
            GlassRenderer.fillRoundedRect(context, x + 1, y + 1, 2, height - 2, radius - 1, highlight);
        }
        
        // Text content
        int textX = x + theme.spacingM();
        int textY = y + (height - 8) / 2;
        int maxTextWidth = width - theme.spacingM() * 2;
        
        String displayText = password ? "*".repeat(text.length()) : text;
        
        if (displayText.isEmpty() && !focused && !placeholder.isEmpty()) {
            // Placeholder
            context.drawTextWithShadow(theme.client.textRenderer, placeholder, textX, textY, theme.textDisabled());
        } else {
            // Actual text with clipping
            if (theme.client.textRenderer.getWidth(displayText) > maxTextWidth) {
                // Scroll text if cursor is near edge
                int cursorPixel = theme.client.textRenderer.getWidth(displayText.substring(0, cursorPos));
                int offset = Math.max(0, cursorPixel - maxTextWidth + 10);
                
                context.getMatrices().push();
                context.getMatrices().translate(textX - offset, textY, 0);
                // Enable scissor for text clipping
                context.enableScissor(textX, y + 2, x + width - theme.spacingM(), y + height - 2);
                context.drawTextWithShadow(theme.client.textRenderer, displayText, 0, 0, theme.textPrimary());
                context.disableScissor();
                context.getMatrices().pop();
            } else {
                context.drawTextWithShadow(theme.client.textRenderer, displayText, textX, textY, theme.textPrimary());
            }
            
            // Cursor
            if (focused && showCursor) {
                int cursorPixel = theme.client.textRenderer.getWidth(displayText.substring(0, cursorPos));
                int cursorX = textX + cursorPixel;
                
                // Ensure cursor is visible
                if (cursorX >= textX && cursorX <= x + width - theme.spacingM()) {
                    context.fill(cursorX, y + 4, cursorX + 1, y + height - 4, theme.accent(1f));
                }
            }
            
            // Selection highlight
            if (selectionStart >= 0 && selectionStart != cursorPos) {
                int start = Math.min(selectionStart, cursorPos);
                int end = Math.max(selectionStart, cursorPos);
                int selStartX = textX + theme.client.textRenderer.getWidth(displayText.substring(0, start));
                int selEndX = textX + theme.client.textRenderer.getWidth(displayText.substring(0, end));
                context.fill(selStartX, y + 2, selEndX, y + height - 2, theme.accent(0.3f));
            }
        }
        
        RenderSystem.disableBlend();
    }
    
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        
        boolean wasFocused = focused;
        focused = isMouseOver(mouseX, mouseY);
        
        if (focused) {
            // Calculate cursor position from click
            int textX = x + theme.spacingM();
            double relX = mouseX - textX;
            cursorPos = getCursorPosFromX(relX);
            selectionStart = -1;
        }
        
        return focused;
    }
    
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!focused) return false;
        
        boolean ctrl = (modifiers & 0x80) != 0; // GLFW_MOD_CONTROL
        boolean shift = (modifiers & 0x01) != 0; // GLFW_MOD_SHIFT
        
        switch (keyCode) {
            case 259 -> { // Backspace
                if (selectionStart >= 0 && selectionStart != cursorPos) {
                    deleteSelection();
                } else if (cursorPos > 0) {
                    text = text.substring(0, cursorPos - 1) + text.substring(cursorPos);
                    cursorPos--;
                }
                onChange.run();
                return true;
            }
            case 261 -> { // Delete
                if (selectionStart >= 0 && selectionStart != cursorPos) {
                    deleteSelection();
                } else if (cursorPos < text.length()) {
                    text = text.substring(0, cursorPos) + text.substring(cursorPos + 1);
                }
                onChange.run();
                return true;
            }
            case 263 -> { // Left
                if (shift && selectionStart < 0) selectionStart = cursorPos;
                cursorPos = Math.max(0, cursorPos - (ctrl ? getWordLeft(cursorPos) : 1));
                if (!shift) selectionStart = -1;
                return true;
            }
            case 262 -> { // Right
                if (shift && selectionStart < 0) selectionStart = cursorPos;
                cursorPos = Math.min(text.length(), cursorPos + (ctrl ? getWordRight(cursorPos) : 1));
                if (!shift) selectionStart = -1;
                return true;
            }
            case 268 -> { // Home
                if (shift && selectionStart < 0) selectionStart = cursorPos;
                cursorPos = 0;
                if (!shift) selectionStart = -1;
                return true;
            }
            case 269 -> { // End
                if (shift && selectionStart < 0) selectionStart = cursorPos;
                cursorPos = text.length();
                if (!shift) selectionStart = -1;
                return true;
            }
            case 67 -> { // C (copy)
                if (ctrl && selectionStart >= 0) {
                    copySelection();
                    return true;
                }
                break;
            }
            case 86 -> { // V (paste)
                if (ctrl) {
                    pasteFromClipboard();
                    onChange.run();
                    return true;
                }
                break;
            }
            case 88 -> { // X (cut)
                if (ctrl && selectionStart >= 0) {
                    copySelection();
                    deleteSelection();
                    onChange.run();
                    return true;
                }
                break;
            }
            case 65 -> { // A (select all)
                if (ctrl) {
                    selectionStart = 0;
                    cursorPos = text.length();
                    return true;
                }
                break;
            }
        }
        
        return false;
    }
    
    public boolean charTyped(char chr, int modifiers) {
        if (!focused) return false;
        if (chr < 32 || chr > 126) return false; // Printable ASCII only
        if (text.length() >= maxLength) return false;
        
        if (selectionStart >= 0 && selectionStart != cursorPos) {
            deleteSelection();
        }
        
        text = text.substring(0, cursorPos) + chr + text.substring(cursorPos);
        cursorPos++;
        onChange.run();
        return true;
    }
    
    private int getCursorPosFromX(double relX) {
        String displayText = password ? "*".repeat(text.length()) : text;
        for (int i = 0; i <= displayText.length(); i++) {
            int w = theme.client.textRenderer.getWidth(displayText.substring(0, i));
            if (w > relX) return i;
        }
        return displayText.length();
    }
    
    private int getWordLeft(int pos) {
        int start = pos;
        while (start > 0 && Character.isWhitespace(text.charAt(start - 1))) start--;
        while (start > 0 && !Character.isWhitespace(text.charAt(start - 1))) start--;
        return pos - start;
    }
    
    private int getWordRight(int pos) {
        int end = pos;
        while (end < text.length() && Character.isWhitespace(text.charAt(end))) end++;
        while (end < text.length() && !Character.isWhitespace(text.charAt(end))) end++;
        return end - pos;
    }
    
    private void deleteSelection() {
        int start = Math.min(selectionStart, cursorPos);
        int end = Math.max(selectionStart, cursorPos);
        text = text.substring(0, start) + text.substring(end);
        cursorPos = start;
        selectionStart = -1;
    }
    
    private void copySelection() {
        int start = Math.min(selectionStart, cursorPos);
        int end = Math.max(selectionStart, cursorPos);
        String selected = text.substring(start, end);
        MinecraftClient.getInstance().keyboard.setClipboard(selected);
    }
    
    private void pasteFromClipboard() {
        String clipboard = MinecraftClient.getInstance().keyboard.getClipboard();
        if (clipboard != null) {
            clipboard = clipboard.replaceAll("[\\r\\n]", "");
            if (text.length() + clipboard.length() > maxLength) {
                clipboard = clipboard.substring(0, maxLength - text.length());
            }
            if (selectionStart >= 0 && selectionStart != cursorPos) {
                deleteSelection();
            }
            text = text.substring(0, cursorPos) + clipboard + text.substring(cursorPos);
            cursorPos += clipboard.length();
        }
    }
    
    private boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
    
    public String getText() { return text; }
    public void setText(String text) { 
        this.text = text; 
        this.cursorPos = text.length();
        this.selectionStart = -1;
    }
    public boolean isFocused() { return focused; }
    public void setFocused(boolean focused) { this.focused = focused; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public boolean isHovered() { return hovered; }
    
    public static Builder builder(int x, int y) {
        return new Builder(x, y);
    }
}