package com.alpoyy.pulse.gui.component;

import com.alpoyy.pulse.config.PulseConfig;
import net.minecraft.client.MinecraftClient;

import java.awt.Color;

public class PulseTheme {
    private final PulseConfig config;
    private final MinecraftClient client;
    
    // Base colors (dark glass theme)
    public static final int BG_DEEPEST = 0x0A0A0F;      // Almost black
    public static final int BG_DEEP = 0x12121A;         // Very dark charcoal
    public static final int BG_DARK = 0x1A1A2E;         // Dark navy
    public static final int BG_MEDIUM = 0x2D2D44;       // Medium dark
    public static final int BG_LIGHT = 0x3A3A5C;        // Lighter panel
    public static final int BG_HOVER = 0x4A4A7A;        // Hover state
    
    // Text colors
    public static final int TEXT_PRIMARY = 0xFFFFFFFF;  // White
    public static final int TEXT_SECONDARY = 0xFFCCCCCC; // Light gray
    public static final int TEXT_MUTED = 0xFF888888;    // Muted gray
    public static final int TEXT_DISABLED = 0xFF555555; // Disabled
    public static final int TEXT_ON_ACCENT = 0xFF1A1A2E; // Dark text on accent
    
    // Border/glow
    public static final int BORDER_SUBTLE = 0x33FFFFFF;  // Very subtle white border
    public static final int BORDER_ACCENT = 0x80FFFFFF;  // Accent border
    
    // Status colors
    public static final int STATUS_SUCCESS = 0xFF00D4AA; // Teal success
    public static final int STATUS_WARNING = 0xFFFFAA00; // Amber warning
    public static final int STATUS_ERROR = 0xFFFF4444;   // Red error
    public static final int STATUS_INFO = 0xFF00A8FF;    // Blue info
    public static final int STATUS_PLAYING = 0xFF00D4AA; // Playing green
    public static final int STATUS_PAUSED = 0xFFFFAA00;  // Paused amber
    public static final int STATUS_LIVE = 0xFFFF0044;    // Live red
    
    // Shadow
    public static final int SHADOW_1 = 0x40000000;       // Subtle shadow
    public static final int SHADOW_2 = 0x80000000;       // Medium shadow
    public static final int SHADOW_3 = 0xC0000000;       // Strong shadow
    
    public PulseTheme(PulseConfig config, MinecraftClient client) {
        this.config = config;
        this.client = client;
    }
    
    // Accent colors from config
    public int getAccentColor() {
        return config.getAccentColor().getRGB() & 0xFFFFFF;
    }
    
    public int getAccentColorARGB() {
        return config.getAccentColor().getRGB() | 0xFF000000;
    }
    
    public int getSecondaryAccentColor() {
        return config.getSecondaryAccentColor().getRGB() & 0xFFFFFF;
    }
    
    public int getSecondaryAccentColorARGB() {
        return config.getSecondaryAccentColor().getRGB() | 0xFF000000;
    }
    
    // Accent with alpha
    public int accent(float alpha) {
        return ((int) (alpha * 255) << 24) | getAccentColor();
    }
    
    public int secondaryAccent(float alpha) {
        return ((int) (alpha * 255) << 24) | getSecondaryAccentColor();
    }
    
    // Background with config opacity
    public int bgDeepest(float opacity) {
        return ((int) (opacity * 255) << 24) | BG_DEEPEST;
    }
    
    public int bgDeep(float opacity) {
        return ((int) (opacity * 255) << 24) | BG_DEEP;
    }
    
    public int bgDark(float opacity) {
        return ((int) (opacity * 255) << 24) | BG_DARK;
    }
    
    public int bgMedium(float opacity) {
        return ((int) (opacity * 255) << 24) | BG_MEDIUM;
    }
    
    public int bgHover(float opacity) {
        return ((int) (opacity * 255) << 24) | BG_HOVER;
    }
    
    // Border with glow intensity
    public int borderSubtle() {
        return ((int) (config.glowIntensity * 60) << 24) | 0xFFFFFF;
    }
    
    public int borderAccent() {
        return ((int) (config.glowIntensity * 180) << 24) | getAccentColor();
    }
    
    public int borderAccentSoft() {
        return ((int) (config.glowIntensity * 80) << 24) | getAccentColor();
    }
    
    // Inner glow highlight
    public int innerGlow() {
        if (config.glowIntensity <= 0.1f) return 0;
        return ((int) (config.glowIntensity * 50) << 24) | getAccentColor();
    }
    
    // Text colors
    public int textPrimary() { return TEXT_PRIMARY; }
    public int textSecondary() { return TEXT_SECONDARY; }
    public int textMuted() { return TEXT_MUTED; }
    public int textDisabled() { return TEXT_DISABLED; }
    public int textOnAccent() { return TEXT_ON_ACCENT; }
    
    // Status colors
    public int statusSuccess() { return STATUS_SUCCESS; }
    public int statusWarning() { return STATUS_WARNING; }
    public int statusError() { return STATUS_ERROR; }
    public int statusInfo() { return STATUS_INFO; }
    public int statusPlaying() { return STATUS_PLAYING; }
    public int statusPaused() { return STATUS_PAUSED; }
    public int statusLive() { return STATUS_LIVE; }
    
    // Shadows
    public int shadow1() { return SHADOW_1; }
    public int shadow2() { return SHADOW_2; }
    public int shadow3() { return SHADOW_3; }
    
    // Config values
    public float panelOpacity() { return config.backgroundOpacity; }
    public float uiOpacity() { return config.uiOpacity; }
    public float glowIntensity() { return config.glowIntensity; }
    public float widgetScale() { return config.widgetScale; }
    
    // Corner radius
    public int radiusSmall() { return 6; }
    public int radiusMedium() { return 10; }
    public int radiusLarge() { return 16; }
    public int radiusXL() { return 24; }
    
    // Spacing
    public int spacingXS() { return 4; }
    public int spacingS() { return 8; }
    public int spacingM() { return 12; }
    public int spacingL() { return 16; }
    public int spacingXL() { return 24; }
    public int spacingXXL() { return 32; }
    
    // Standard dimensions
    public int btnHeight() { return 36; }
    public int btnHeightSm() { return 28; }
    public int btnHeightLg() { return 44; }
    public int inputHeight() { return 36; }
    public int sidebarWidth() { return 220; }
    public int headerHeight() { return 56; }
    public int tabHeight() { return 44; }
    
    // Animation
    public long animFast() { return 100; }
    public long animNormal() { return 200; }
    public long animSlow() { return 350; }
}